package dev.createspellbook.reactor;

import dev.createspellbook.Balance;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import java.util.*;

/** Server authority. Size is measured in tenths of a percent, never client input.
 * Paired cores pay maintenance per tick; a shrinking field releases stored energy.
 * Waste is produced only by paid maintenance, never by residual shutdown power. */
public final class ReactorOperation {
    public static final int MIN_POWER=0,MAX_POWER=120000000,NORMAL_SIZE=500;
    public enum State {IDLE,STARTING,RUNNING,STOPPING,NO_MANA,OUTPUT_BLOCKED,UNSTABLE,SUSPENDED}
    public enum Alarm {NONE,STRUCTURE,NO_MANA,STABILIZERS,WASTE_FULL,LEGACY_WASTE,RUNAWAY,TERMINAL}
    private final ReactorControllerBlockEntity owner;
    public final ReactorStabilizers stabilizers=new ReactorStabilizers(this::fuelChanged);
    public final ReactorResources resources;
    private List<ReactorPartBlockEntity> inlets=List.of(),outputs=List.of(),reaction=List.of(),energy=List.of();
    private ReactorShaftBlockEntity shaft;
    private int actual,size,pairs,reactionCount,energyCount,wearCredit;
    private int displayWorking,displayReserves,displayMana,displayMatter,displayManaCapacity,displayLegacy,displayRPM;
    private Alarm displayAlarm=Alarm.NONE;
    private int displayAccidentPhase,displayAccidentSeconds;
    private State state=State.IDLE;
    private boolean requested,unstable,mutating,loading,paidMaintenance;
    private double matterCredit;
    private long lastTick=Long.MIN_VALUE,produced,manaSpent,wearSpent,fieldEpoch;
    public long epoch(){return fieldEpoch;}
    public double accidentRadius(){var a=ReactorAccidents.forController(owner);return a==null?0:a.destructionRadius(owner.getLevel().getGameTime());}
    public ReactorOperation(ReactorControllerBlockEntity owner){this.owner=owner;resources=new ReactorResources(this::resourcesChanged,owner::lastLayout);}
    public static int capacity(int pairs,int tenths){return (int)((long)Math.clamp(pairs,0,289)*Math.clamp(tenths,0,1001)*4096/10);}
    public int target(){return capacity(pairs,NORMAL_SIZE);}
    public int actual(){return actual;}
    public int pairs(){return pairs;}
    public int reactionCount(){return reactionCount;}
    public int energyCount(){return energyCount;}
    public int sizeUnits(){var a=ReactorAccidents.forController(owner);return a!=null && !a.escaped?a.containedSize(owner.getLevel().getGameTime()):size;}
    public double sizePercent(){return sizeUnits()/10d;}
    public State state(){return state;}
    public boolean requested(){return requested;}
    public boolean unstable(){return unstable;}
    public long produced(){return produced;}
    public long manaSpent(){return manaSpent;}
    public long wearSpent(){return wearSpent;}
    public double workCredit(){return matterCredit;}
    public int energyCapacity(){return energy.stream().mapToInt(ReactorPartBlockEntity::energyCapacity).sum();}
    ReactorShaftBlockEntity source(){return shaft;}
    private boolean client(){return owner.getLevel()!=null && owner.getLevel().isClientSide;}
    private boolean server(){return owner.getLevel()!=null && !owner.getLevel().isClientSide;}
    public int working(){return client()?displayWorking:stabilizers.working();}
    public int reserves(){return client()?displayReserves:stabilizers.reserves();}
    /** Historical API retained for old snapshots/tests, not the new requirement. */
    public static int requiredSlots(int power){return power<=0?0:(int)(((long)power*8+4999999)/5000000);}
    public int required(){return pairs;}
    public int accidentPhase(){if(client())return displayAccidentPhase;var a=ReactorAccidents.forController(owner);return a==null?0:a.phase(owner.getLevel().getGameTime()).ordinal()+1;}
    public int accidentSeconds(){if(client())return displayAccidentSeconds;var a=ReactorAccidents.forController(owner);return a==null?0:(a.remaining(owner.getLevel().getGameTime())+19)/20;}
    public int manaStored(){return client()?displayMana:resources.mana.getFluidAmount();}
    public int storedMatter(){return client()?displayMatter:resources.count();}
    public int manaCapacity(){return client()?displayManaCapacity:resources.manaCapacity();}
    public int legacyPending(){return client()?displayLegacy:outputs.stream().mapToInt(ReactorPartBlockEntity::legacyMatter).sum();}
    public int rpm(){return client()?displayRPM:actual>0 && shaft!=null?(int)Math.abs(shaft.getSpeed()):0;}
    public double manaRate(){return paidMaintenance?pairs*100d:0;}
    public double matterRate(){return paidMaintenance && !unstable && storedMatter()<ReactorResources.MATTER_CAPACITY && legacyPending()==0?actual/1000000d*Balance.REACTOR_MATTER_RATE.get():0;}
    public Alarm alarm(){
        if(client())return displayAlarm;if(accidentPhase()==2)return Alarm.RUNAWAY;if(accidentPhase()==3)return Alarm.TERMINAL;
        if(!owner.isFormed() || pairs==0 && sizeUnits()>0)return Alarm.STRUCTURE;if(unstable)return Alarm.STABILIZERS;
        if(resources.full())return Alarm.WASTE_FULL;if(legacyPending()>0)return Alarm.LEGACY_WASTE;
        return state==State.NO_MANA?Alarm.NO_MANA:Alarm.NONE;
    }
    private void changed(boolean now){if(!loading)owner.operationChanged(now);}
    /** Manual SU targets are retired: the field and paired cores determine output. */
    public void setTarget(int ignored){}
    public void start(){if(server() && owner.isFormed() && pairs>0 && ReactorAccidents.get((net.minecraft.server.level.ServerLevel)owner.getLevel()).repairNow(owner)){long old=ReactorAccidents.get((net.minecraft.server.level.ServerLevel)owner.getLevel()).dischargedEpoch(owner.identity());if(fieldEpoch<=old){size=0;requested=false;}fieldEpoch=Math.max(fieldEpoch,old)+1;requested=true;changed(true);}}
    public void structureBroken(){if(server() && sizeUnits()>0)ReactorAccidents.trigger(owner,ReactorAccident.Cause.STRUCTURE);}
    public void accidentEscaped(){if(!server())return;requested=false;unstable=true;size=0;withdraw(State.UNSTABLE);}
    public void recoverSize(int units){size=Math.clamp(units,0,1000);unstable=false;changed(true);}
    public boolean repairContainment(int ignored){
        if(!server() || !owner.isFormed() || pairs<=0)return false;
        boolean before=mutating;mutating=true;try{stabilizers.replenish(pairs);}finally{mutating=before;}
        return stabilizers.working()>=pairs && !resources.full() && legacyPending()==0;
    }
    public void stop(){if(server()){requested=false;changed(true);}}
    public void bind(List<ReactorLinked> parts){
        var mana=new ArrayList<ReactorPartBlockEntity>();var matter=new ArrayList<ReactorPartBlockEntity>();var top=new ArrayList<ReactorPartBlockEntity>();var bottom=new ArrayList<ReactorPartBlockEntity>();
        for(var part:parts)if(part instanceof ReactorPartBlockEntity p){switch(p.role){case MANA->mana.add(p);case OUTPUT->matter.add(p);case REACTION_CORE->top.add(p);case ENERGY_CORE->bottom.add(p);default->{}}}else if(part instanceof ReactorShaftBlockEntity s)shaft=s;
        inlets=List.copyOf(mana);outputs=List.copyOf(matter);reaction=List.copyOf(top);energy=List.copyOf(bottom);reactionCount=top.size();energyCount=bottom.size();pairs=Math.min(reactionCount,energyCount);
        stabilizers.capacity(owner.lastLayout().inner()*owner.lastLayout().inner());resources.refreshCapacity();migrateLegacy();
    }
    private void migrateLegacy(){boolean before=mutating;mutating=true;try{for(var p:inlets)if(ready(p))p.migrate(resources);for(var p:outputs)if(ready(p))p.migrate(resources);}finally{mutating=before;}}
    public void suspend(){withdraw(State.SUSPENDED);inlets=List.of();outputs=List.of();reaction=List.of();energy=List.of();shaft=null;}
    private void withdraw(State reason){boolean notify=actual!=0 || state!=reason;actual=0;paidMaintenance=false;state=reason;for(var p:reaction)p.maintainReaction(0);for(var p:energy)p.setEnergyCapacity(0);if(shaft!=null)shaft.withdraw();if(notify)changed(true);}
    private void fuelChanged(){changed(false);}
    private void resourcesChanged(){changed(false);}
    private boolean ready(ReactorLinked part){return part!=null && part.reactorBinding().ownedBy(owner) && !((net.minecraft.world.level.block.entity.BlockEntity)part).isRemoved();}
    public void tick(){
        if(!server() || owner.getLevel().getGameTime()==lastTick)return;lastTick=owner.getLevel().getGameTime();
        if((size>0 || requested) && fieldEpoch<=ReactorAccidents.get((net.minecraft.server.level.ServerLevel)owner.getLevel()).dischargedEpoch(owner.identity())){accidentEscaped();return;}
        var accident=ReactorAccidents.forController(owner);
        if(accident!=null && accident.phase(lastTick)!=ReactorAccident.Phase.WARNING){accidentEscaped();return;}
        if(!owner.isFormed() || !ready(shaft)){if(sizeUnits()>0 && owner.issue()!=ReactorScanner.Issue.CHECKING && owner.issue()!=ReactorScanner.Issue.UNLOADED)ReactorAccidents.trigger(owner,ReactorAccident.Cause.STRUCTURE);withdraw(State.SUSPENDED);return;}
        if(legacyPending()>0)migrateLegacy();
        boolean before=mutating;mutating=true;try{stabilizers.replenish(pairs);}finally{mutating=before;}
        boolean lacksFuel=stabilizers.working()<pairs;
        boolean blocked=resources.full() || legacyPending()>0;
        if(sizeUnits()>0 && (lacksFuel || blocked))ReactorAccidents.trigger(owner,lacksFuel?ReactorAccident.Cause.STABILIZERS:ReactorAccident.Cause.WASTE);
        accident=ReactorAccidents.forController(owner);boolean fault=accident!=null;
        unstable=lacksFuel;
        boolean paid=pairs>0 && requested && resources.mana.getFluidAmount()>=pairs*5 && (sizeUnits()>0 || !blocked);
        paidMaintenance=paid;mutating=true;try{
            if(paid){int charge=pairs*5;resources.mana.drain(charge,FluidAction.EXECUTE);manaSpent+=charge;}
            if(fault)size=accident.containedSize(lastTick);
            else {int goal=paid?NORMAL_SIZE:0;int step=Balance.REACTOR_SIZE_STEP.get();size+=Math.clamp(goal-size,-step,step);}
            if(size>0 && pairs>0){
                wearCredit+=pairs;int points=wearCredit/20;wearCredit%=20;
                if(points>0){var wear=stabilizers.planWear(points,Math.min(pairs,stabilizers.working()));if(wear!=null){stabilizers.apply(wear);wearSpent+=points;}}
            }
            actual=capacity(pairs,size);
            if(paid && !lacksFuel && !blocked){
                matterCredit+=actual/1000000d*Balance.REACTOR_MATTER_RATE.get()/20;
                while(matterCredit>=1 && resources.acceptsMatter()){resources.emit();matterCredit-=1;produced++;}
            }
        }finally{mutating=false;}
        for(int i=0;i<reaction.size();i++)reaction.get(i).maintainReaction(i<pairs?actual/pairs+(i==pairs-1?actual%pairs:0):0);
        for(int i=0;i<energy.size();i++)if(i<pairs)energy.get(i).convertEnergy(reaction.get(i),actual/pairs+(i==pairs-1?actual%pairs:0));else energy.get(i).setEnergyCapacity(0);
        if(actual>0)shaft.supply(actual);else shaft.withdraw();
        var next=fault?(blocked?State.OUTPUT_BLOCKED:State.UNSTABLE):requested && lacksFuel?State.UNSTABLE:requested && blocked?State.OUTPUT_BLOCKED:requested && !paid?State.NO_MANA:size==0?State.IDLE:!requested?State.STOPPING:!paid?State.NO_MANA:size==NORMAL_SIZE?State.RUNNING:State.STARTING;
        boolean different=state!=next;state=next;changed(different || lastTick%10==0);
        damageChamber(lastTick);
    }
    /** Only the interior volume is hazardous; the shell and its exterior are excluded. */
    private void damageChamber(long now){
        if(!requested || actual<=0 || now%20!=0 || !owner.isFormed())return;
        float damage=Balance.REACTOR_CHAMBER_DAMAGE.get().floatValue();if(damage<=0)return;
        var bounds=owner.lastLayout().bounds();
        var chamber=new net.minecraft.world.phys.AABB(bounds.minX()+1,bounds.minY()+1,bounds.minZ()+1,bounds.maxX(),bounds.maxY(),bounds.maxZ());
        var level=owner.getLevel();
        for(var living:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,chamber,e->e.isAlive() && !e.isSpectator() && chamber.contains(e.getBoundingBox().getCenter()))){
            if(living instanceof net.minecraft.world.entity.player.Player player && player.isCreative())continue;
            var tag=living.getPersistentData();String key="CSBReactorChamberDamageTick";
            if(tag.contains(key) && now>=tag.getLong(key) && now-tag.getLong(key)<20)continue;
            tag.putLong(key,now);living.hurt(level.damageSources().magic(),damage);
        }
    }
    public void write(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        tag.putLong("FieldEpoch",fieldEpoch);tag.putBoolean("MaintenancePaid",paidMaintenance);tag.putInt("TargetSU",target());tag.putBoolean("Requested",requested);tag.putBoolean("Unstable",unstable);tag.putInt("FieldSize",sizeUnits());tag.putInt("CorePairs",pairs);tag.putInt("ReactionCount",reactionCount);tag.putInt("EnergyCount",energyCount);
        if(packet){tag.putInt("ActualSU",actual);tag.putString("RunState",state.name());tag.putInt("Working",working());tag.putInt("Reserves",reserves());tag.putInt("StoredMana",manaStored());tag.putInt("StoredMatter",storedMatter());tag.putInt("ManaCapacity",manaCapacity());tag.putInt("LegacyPending",legacyPending());tag.putInt("RPM",rpm());tag.putString("Alarm",alarm().name());tag.putInt("AccidentPhase",accidentPhase());tag.putInt("AccidentSeconds",accidentSeconds());}
        else{var storage=new CompoundTag();resources.write(storage,lookup);tag.put("Storage",storage);tag.put("Stabilizers",stabilizers.serializeNBT(lookup));tag.putDouble("MatterCredit",matterCredit);tag.putInt("WearCredit",wearCredit);}
        tag.putLong("Produced",produced);tag.putLong("ManaSpent",manaSpent);tag.putLong("WearSpent",wearSpent);
    }
    public void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        loading=true;try{
            fieldEpoch=Math.max(0,tag.getLong("FieldEpoch"));paidMaintenance=packet && tag.getBoolean("MaintenancePaid");requested=tag.getBoolean("Requested");unstable=tag.getBoolean("Unstable");size=Math.clamp(tag.getInt("FieldSize"),0,1001);pairs=Math.clamp(tag.getInt("CorePairs"),0,289);reactionCount=Math.max(0,tag.getInt("ReactionCount"));energyCount=Math.max(0,tag.getInt("EnergyCount"));
            produced=Math.max(0,tag.getLong("Produced"));manaSpent=Math.max(0,tag.getLong("ManaSpent"));wearSpent=Math.max(0,tag.getLong("WearSpent"));actual=0;state=State.SUSPENDED;
            if(packet){actual=Math.clamp(tag.getInt("ActualSU"),0,MAX_POWER);try{state=State.valueOf(tag.getString("RunState"));displayAlarm=Alarm.valueOf(tag.getString("Alarm"));}catch(IllegalArgumentException ignored){}displayWorking=Math.clamp(tag.getInt("Working"),0,289);displayReserves=Math.clamp(tag.getInt("Reserves"),0,289);displayMana=Math.max(0,tag.getInt("StoredMana"));displayMatter=Math.max(0,tag.getInt("StoredMatter"));displayManaCapacity=Math.max(0,tag.getInt("ManaCapacity"));displayLegacy=Math.max(0,tag.getInt("LegacyPending"));displayRPM=Math.max(0,tag.getInt("RPM"));displayAccidentPhase=Math.clamp(tag.getInt("AccidentPhase"),0,3);displayAccidentSeconds=Math.max(0,tag.getInt("AccidentSeconds"));}
            else{resources.read(tag.getCompound("Storage"),lookup);stabilizers.deserializeNBT(lookup,tag.getCompound("Stabilizers"));double credit=tag.getDouble("MatterCredit");matterCredit=Double.isFinite(credit)?Math.clamp(credit,0,64):0;wearCredit=Math.clamp(tag.getInt("WearCredit"),0,19);lastTick=Long.MIN_VALUE;}
        }finally{loading=false;}
    }
}
