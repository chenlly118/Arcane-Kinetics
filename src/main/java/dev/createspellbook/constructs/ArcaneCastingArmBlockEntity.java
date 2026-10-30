package dev.createspellbook.constructs;

import dev.createspellbook.*;
import dev.createspellbook.devices.*;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.*;

/** Upgrades the existing registry ID, inventory and absolute cooldown ledger in place. */
public final class ArcaneCastingArmBlockEntity extends KineticBlockEntity implements BlockConstructHost {
    private UUID owner;
    private ArcaneArmCaster caster;
    private boolean syncQueued,loadedFromDisk,lastPulseHigh;
    private int interval=Balance.ARM_INTERVAL.get();
    private boolean inverted;
    private EmitterMode mode=EmitterMode.PULSE;
    private float syncedYaw=180,syncedPitch;
    public final ScrollTemplate scroll=new ScrollTemplate(this::constructChanged);
    public final CastLedger ledger=new CastLedger();
    public final FluidTank tank=new FluidTank(Balance.ARM_CAPACITY.get(),fluid->fluid.is(Content.MANA.get())) {
        @Override protected void onContentsChanged() {setCapacity(Math.max(getFluidAmount(),Balance.ARM_CAPACITY.get()));constructChanged();}
    };
    public ArcaneCastingArmBlockEntity(BlockPos pos,BlockState state) {super(DeviceContent.CASTING_ARM_BE.get(),pos,state);}
    @Override public void initialize() {
        super.initialize();
        if(level!=null && !level.isClientSide) {
            // Keep the six-value property so legacy UP/DOWN block states still load.
            if(getBlockState().getValue(ArcaneCastingArmBlock.FACING).getAxis()==Direction.Axis.Y)
                level.setBlockAndUpdate(worldPosition,getBlockState().setValue(ArcaneCastingArmBlock.FACING,Direction.NORTH));
            if(loadedFromDisk){lastPulseHigh=pulseSignal();loadedFromDisk=false;}
            ensureCaster();
        }
    }
    public Direction facing() {var value=getBlockState().getValue(ArcaneCastingArmBlock.FACING);return value.getAxis()==Direction.Axis.Y?Direction.NORTH:value;}
    public Direction fluidSide() {return facing().getClockWise();}
    public IFluidHandler fluidPort(Direction side) {return side==null || side==fluidSide()?tank:null;}
    public IItemHandler scrollPort(Direction side) {return side==null || side==facing().getOpposite()?scroll:null;}
    /** The supplied nozzle ends 9/16 blocks beyond the front of its housing. */
    private Vec3 pivot() {return Vec3.atCenterOf(worldPosition).add(0,3d/16,0);}
    private Vec3 muzzleFor(Vec3 target) {return pivot().add((mode==EmitterMode.TURRET?target.subtract(pivot()).normalize():Vec3.atLowerCornerOf(facing().getNormal())).scale(1.08));}
    public Vec3 muzzle() {var target=caster==null?null:caster.session.target();return muzzleFor(target==null?pivot().add(Vec3.atLowerCornerOf(facing().getNormal())):target.getBoundingBox().getCenter());}
    public float aimYaw() {return level!=null && !level.isClientSide && caster!=null?caster.getYRot():syncedYaw;}
    public float aimPitch() {return level!=null && !level.isClientSide && caster!=null?caster.getXRot():syncedPitch;}
    private boolean pulseSignal() {return level!=null && level.hasNeighborSignal(worldPosition)!=inverted;}
    private boolean autoEnabled() {return level!=null && (inverted?level.hasNeighborSignal(worldPosition):!level.hasNeighborSignal(worldPosition));}
    public boolean hasRotation() {return Math.abs(getSpeed())>=1 && !isOverStressed();}
    @Override public boolean operating() {return level!=null && !isRemoved() && hasRotation() && (mode==EmitterMode.PULSE || autoEnabled());}
    @Override public void tick() {
        super.tick();if(level==null || level.isClientSide)return;ensureCaster();
        boolean high=pulseSignal(),edge=high && !lastPulseHigh;lastPulseHigh=high;
        if(!operating()) {
            if(caster!=null)caster.cancelCast();
            status(isOverStressed()?CastLedger.Status.OVERSTRESSED:!hasRotation()?CastLedger.Status.NO_ROTATION:CastLedger.Status.UNPOWERED);
        } else if(caster!=null && !caster.isCasting()) {
            long now=level.getGameTime();
            if(mode==EmitterMode.PULSE && !edge) {
                pulseIdleStatus(now);
            } else if(now>=ledger.nextAttempt) {
                ledger.nextAttempt=now+interval;
                LivingEntity target=null;
                if(mode!=EmitterMode.PULSE) {
                    var data=ScrollTemplate.read(scroll.getStackInSlot(0));
                    if(data.isEmpty()){status(CastLedger.Status.NO_SCROLL);sync();return;}
                    if(!ScrollTemplate.supported(data.get().getSpell(),data.get().getLevel())){status(CastLedger.Status.UNSUPPORTED);sync();return;}
                    int effective=data.get().getSpell().getLevelFor(data.get().getLevel(),caster);
                    if(!ScrollTemplate.supported(data.get().getSpell(),effective)){status(CastLedger.Status.UNSUPPORTED);sync();return;}
                    target=mode==EmitterMode.TURRET?findEnemy():findDepot(data.get().getSpell(),effective);
                    if(target==null){status(mode==EmitterMode.TURRET?CastLedger.Status.INVALID_TARGET:CastLedger.Status.NO_RECIPE);sync();return;}
                }
                caster.session.begin(target);setChanged();
            }
        }
        sync();
    }
    private void pulseIdleStatus(long now) {
        var data=ScrollTemplate.read(scroll.getStackInSlot(0));
        if(data.isEmpty()){status(CastLedger.Status.NO_SCROLL);return;}
        var spell=data.get().getSpell();int raw=data.get().getLevel();
        if(!ScrollTemplate.supported(spell,raw)){status(CastLedger.Status.UNSUPPORTED);return;}
        int effective=spell.getLevelFor(raw,caster);
        if(!ScrollTemplate.supported(spell,effective)){status(CastLedger.Status.UNSUPPORTED);return;}
        if(!spell.isEnabled()){status(CastLedger.Status.DISABLED);return;}
        if(ledger.remaining(spell.getSpellId(),now)>0){status(CastLedger.Status.COOLDOWN);return;}
        status(!canPay(spell.getManaCost(effective),false)?CastLedger.Status.NO_MANA:!visible(muzzle(),null)?CastLedger.Status.INVALID_TARGET:CastLedger.Status.WAITING_PULSE);
    }
    private void sync() {if(syncQueued){syncQueued=false;sendData();}}
    private void ensureCaster() {
        if(!(level instanceof ServerLevel server) || isRemoved() || isChunkUnloaded() || caster!=null && !caster.isRemoved())return;
        caster=DeviceContent.ARM_CASTER.get().create(server);if(caster!=null){caster.bind(this);server.addFreshEntity(caster);}
    }
    public boolean visible(Vec3 point,BlockPos allowedEnd) {
        if(level==null || !level.hasChunkAt(BlockPos.containing(point)))return false;
        // Start inside our own housing, ignoring only that block. Starting at
        // the projecting nozzle could otherwise skip an adjacent solid wall.
        var hit=level.clip(new ClipContext(pivot(),point,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,caster) {
            @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(BlockState state,BlockGetter world,BlockPos pos) {
                return pos.equals(worldPosition)?net.minecraft.world.phys.shapes.Shapes.empty():super.getBlockShape(state,world,pos);
            }
        });
        return hit.getType()==HitResult.Type.MISS || allowedEnd!=null && hit.getBlockPos().equals(allowedEnd);
    }
    private boolean validEnemy(LivingEntity target) {
        return ConstructTargeting.hostile(caster,target,constructPosition(),Balance.EMITTER_RANGE.get(),entity->visible(entity.getBoundingBox().getCenter(),null));
    }
    private LivingEntity findEnemy() {
        return ConstructTargeting.nearest(caster,constructPosition(),Balance.EMITTER_RANGE.get(),this::validEnemy);
    }
    private boolean eligible(SpellProcessingBehaviour depot,AbstractSpell spell,int spellLevel) {
        BlockPos delta=depot.getPos().subtract(worldPosition);var forward=facing().getNormal();int distance=delta.getX()*forward.getX()+delta.getZ()*forward.getZ();
        return distance>=2 && distance<=Balance.EMITTER_RANGE.get() && (delta.getY()==0 || delta.getY()==-1)
                && delta.getX()==forward.getX()*distance && delta.getZ()==forward.getZ()*distance
                && depot.matchingRecipe(spell.getSpellResource(),spell.getSchoolType().getId(),spellLevel).isPresent()
                && visible(Vec3.atCenterOf(depot.getPos()).add(0,.15,0),depot.getPos());
    }
    private LivingEntity findDepot(AbstractSpell spell,int spellLevel) {
        for(int distance=2;distance<=Balance.EMITTER_RANGE.get();distance++)for(int dy=0;dy>=-1;dy--) {
            var pos=worldPosition.relative(facing(),distance).offset(0,dy,0);
            if(level.hasChunkAt(pos) && SpellTarget.at(level,pos) instanceof SpellProcessingBehaviour depot && eligible(depot,spell,spellLevel)) {
                if(depot.probe()==null)depot.initialize();return depot.probe();
            }
        }
        return null;
    }
    @Override public boolean validCastTarget(LivingEntity target,AbstractSpell spell,int spellLevel) {
        if(mode==EmitterMode.PULSE)return target==null && visible(muzzle(),null);
        if(mode==EmitterMode.TURRET)return validEnemy(target);
        return target instanceof SpellDeviceProbe probe && level.hasChunkAt(probe.devicePos())
                && SpellTarget.at(level,probe.devicePos()) instanceof SpellProcessingBehaviour depot
                && depot.probe()==probe && eligible(depot,spell,spellLevel);
    }
    private void releaseCaster() {if(caster!=null){caster.cancelCast();caster.discard();caster=null;}}
    @Override public void onChunkUnloaded() {releaseCaster();super.onChunkUnloaded();}
    @Override public void invalidate() {releaseCaster();super.invalidate();}
    public int interval() {return interval;}
    public boolean inverted() {return inverted;}
    public EmitterMode mode() {return mode;}
    public void setMode(EmitterMode value) {if(mode==value)return;if(caster!=null)caster.cancelCast();mode=value;lastPulseHigh=pulseSignal();ledger.nextAttempt=level.getGameTime()+interval;constructChanged();}
    public void directionChanged() {if(caster!=null){caster.cancelCast();caster.aim();}constructChanged();}
    public void setInterval(int ticks) {interval=Math.clamp(ticks,1,12000);ledger.nextAttempt=level.getGameTime()+interval;constructChanged();}
    public void toggleRedstone() {if(caster!=null)caster.cancelCast();inverted=!inverted;lastPulseHigh=pulseSignal();constructChanged();}
    public void setOwner(Player player) {owner=player==null?null:player.getUUID();constructChanged();}
    @Override public UUID ownerId() {return owner;}
    @Override public Level constructLevel() {return level;}
    @Override public Vec3 constructPosition() {return Vec3.atCenterOf(worldPosition);}
    @Override public ScrollTemplate scroll() {return scroll;}
    @Override public CastLedger ledger() {return ledger;}
    @Override public ArcaneArmCaster caster() {return caster!=null && !caster.isRemoved()?caster:null;}
    @Override public com.simibubi.create.foundation.blockEntity.SmartBlockEntity blockEntity() {return this;}
    @Override public int storedMana() {return tank.getFluidAmount();}
    @Override public int manaCapacity() {return tank.getCapacity();}
    @Override public boolean canPay(int mana,boolean execute) {
        if(mana<0)return false;int amount=liquidCost(mana);if(tank.getFluidAmount()<amount)return false;
        return !execute || amount==0 || tank.drain(amount,FluidAction.EXECUTE).getAmount()==amount;
    }
    @Override public int cooldownMultiplier() {return 1;}
    @Override public void constructChanged() {setChanged();if(level!=null && !level.isClientSide)syncQueued=true;}
    @Override protected void write(CompoundTag tag,HolderLookup.Provider lookup,boolean packet) {
        super.write(tag,lookup,packet);tag.put("Scroll",scroll.serializeNBT(lookup));tag.put("Mana",tank.writeToNBT(lookup,new CompoundTag()));
        tag.put("CastingLedger",ledger.save());tag.putInt("Interval",interval);tag.putBoolean("Inverted",inverted);tag.putInt("EmitterMode",mode.ordinal());tag.putBoolean("LastPulseHigh",lastPulseHigh);if(owner!=null)tag.putUUID("Owner",owner);
        if(packet){tag.putFloat("AimYaw",aimYaw());tag.putFloat("AimPitch",aimPitch());}
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet) {
        super.read(tag,lookup,packet);scroll.deserializeNBT(lookup,tag.getCompound("Scroll"));tank.readFromNBT(lookup,tag.getCompound("Mana"));
        tank.setCapacity(Math.max(tank.getFluidAmount(),Balance.ARM_CAPACITY.get()));ledger.load(tag.getCompound("CastingLedger"));
        interval=tag.contains("Interval")?Math.clamp(tag.getInt("Interval"),1,12000):Balance.ARM_INTERVAL.get();inverted=tag.getBoolean("Inverted");owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;
        mode=EmitterMode.fromId(tag.getInt("EmitterMode"));lastPulseHigh=tag.getBoolean("LastPulseHigh");if(!packet)loadedFromDisk=true;
        syncedYaw=tag.contains("AimYaw")?tag.getFloat("AimYaw"):facing().toYRot();syncedPitch=tag.getFloat("AimPitch");
    }
}
