package dev.createspellbook.constructs;

import dev.createspellbook.*;
import dev.createspellbook.devices.DeviceContent;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import java.util.*;

/** Autonomous hostile-only caster. Mana is owned here, never in a drainable tank. */
public final class ArcaneAutoCasterBlockEntity extends SmartBlockEntity implements BlockConstructHost {
    private UUID owner,migrationId;
    private ArcaneArmCaster caster;
    private int mana;
    private boolean syncQueued;
    private long lastManaTick=Long.MIN_VALUE;
    private long recoveredMana;
    public final ScrollTemplate scroll=new ScrollTemplate(this::constructChanged);
    public final CastLedger ledger=new CastLedger();
    private final IFluidHandler inlet=new IFluidHandler() {
        @Override public int getTanks(){return 1;}
        private void check(int tank){if(tank!=0)throw new IndexOutOfBoundsException(tank);}
        @Override public FluidStack getFluidInTank(int tank){check(tank);return mana==0?FluidStack.EMPTY:new FluidStack(Content.MANA.get(),mana);}
        @Override public int getTankCapacity(int tank){check(tank);return manaCapacity();}
        @Override public boolean isFluidValid(int tank,FluidStack fluid){check(tank);return fluid.is(Content.MANA.get());}
        @Override public int fill(FluidStack fluid,FluidAction action){
            if(fluid.isEmpty() || !fluid.is(Content.MANA.get()))return 0;
            int accepted=Math.min(fluid.getAmount(),Math.max(0,manaCapacity()-mana));
            if(action.execute() && accepted>0){mana+=accepted;constructChanged();}return accepted;
        }
        @Override public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
        @Override public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.EMPTY;}
    };
    public ArcaneAutoCasterBlockEntity(BlockPos pos,BlockState state){super(DeviceContent.AUTO_CASTER_BE.get(),pos,state);}
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours){}
    @Override public void initialize(){super.initialize();ensureCaster();}
    @Override public void tick(){
        super.tick();if(level==null || level.isClientSide || isChunkUnloaded() || isRemoved())return;ensureCaster();long now=level.getGameTime();
        if(now!=lastManaTick){
            lastManaTick=now;int gain=Math.min(Balance.AUTO_REGEN.get(),Math.max(0,manaCapacity()-mana));
            if(gain>0){mana+=gain;recoveredMana+=gain;setChanged();if(now%10==0 || mana==manaCapacity())syncQueued=true;}
        }
        if(caster!=null && !caster.isCasting() && now>=ledger.nextAttempt){
            ledger.nextAttempt=now+Balance.AUTO_INTERVAL.get();setChanged();
            var selected=ScrollTemplate.read(scroll.getStackInSlot(0));
            if(selected.isEmpty())status(CastLedger.Status.NO_SCROLL);
            else {
                var data=selected.get();var spell=data.getSpell();
                if(!ScrollTemplate.supported(spell,data.getLevel()))status(CastLedger.Status.UNSUPPORTED);
                else {
                    int effective=spell.getLevelFor(data.getLevel(),caster);
                    if(!ScrollTemplate.supported(spell,effective))status(CastLedger.Status.UNSUPPORTED);
                    else if(!spell.isEnabled())status(CastLedger.Status.DISABLED);
                    else if(ledger.remaining(spell.getSpellId(),now)>0)status(CastLedger.Status.COOLDOWN);
                    else if(!canPay(spell.getManaCost(effective),false))status(CastLedger.Status.NO_MANA);
                    else {
                        var target=ConstructTargeting.nearest(caster,constructPosition(),Balance.AUTO_RANGE.get(),this::validEnemy);
                        if(target==null)status(CastLedger.Status.INVALID_TARGET);else caster.session.begin(target);
                    }
                }
            }
        }
        if(syncQueued){syncQueued=false;sendData();}
    }
    private void ensureCaster(){
        if(!(level instanceof ServerLevel server) || isRemoved() || isChunkUnloaded() || caster!=null && !caster.isRemoved())return;
        caster=DeviceContent.ARM_CASTER.get().create(server);if(caster!=null){caster.bind(this);server.addFreshEntity(caster);}
    }
    private void releaseCaster(){if(caster!=null){caster.cancelCast();caster.discard();caster=null;}}
    @Override public void onChunkUnloaded(){releaseCaster();super.onChunkUnloaded();}
    @Override public void invalidate(){releaseCaster();super.invalidate();}
    @Override public Direction facing(){return getBlockState().getValue(ArcaneAutoCasterBlock.FACING);}
    /** Emit above the supplied crystal tip; the fixed side lens is its status window. */
    @Override public Vec3 muzzle(){return Vec3.atBottomCenterOf(worldPosition).add(0,1.37,0);}
    private boolean visible(LivingEntity target){
        var point=target.getBoundingBox().getCenter();if(!level.hasChunkAt(BlockPos.containing(point)))return false;
        var result=level.clip(new ClipContext(muzzle(),point,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,caster));
        return result.getType()==HitResult.Type.MISS;
    }
    public boolean validEnemy(LivingEntity target){return ConstructTargeting.hostile(caster,target,constructPosition(),Balance.AUTO_RANGE.get(),this::visible);}
    @Override public boolean validCastTarget(LivingEntity target,AbstractSpell spell,int spellLevel){return validEnemy(target);}
    @Override public SmartBlockEntity blockEntity(){return this;}
    @Override public Level constructLevel(){return level;}
    @Override public Vec3 constructPosition(){return Vec3.atCenterOf(worldPosition);}
    @Override public ScrollTemplate scroll(){return scroll;}
    @Override public CastLedger ledger(){return ledger;}
    @Override public ArcaneArmCaster caster(){return caster!=null && !caster.isRemoved()?caster:null;}
    @Override public UUID ownerId(){return owner;}
    public void setOwner(Player player){owner=player==null?null:player.getUUID();constructChanged();}
    @Override public boolean operating(){return level!=null && !isRemoved() && !isChunkUnloaded();}
    @Override public int storedMana(){return mana;}
    public long recoveredMana(){return recoveredMana;}
    @Override public int manaCapacity(){return Balance.AUTO_CAPACITY.get();}
    public IFluidHandler inputPort(Direction side){return side==null || side==facing().getOpposite()?inlet:null;}
    @Override public boolean canPay(int cost,boolean execute){
        if(cost<0)return false;int amount=liquidCost(cost);if(mana<amount)return false;
        if(execute && amount>0){mana-=amount;constructChanged();}return true;
    }
    @Override public int cooldownMultiplier(){return 1;}
    @Override public void constructChanged(){setChanged();if(level!=null && !level.isClientSide)syncQueued=true;}
    public void restoreLegacyItem(ItemStack item){
        if(item.has(DataComponents.BLOCK_ENTITY_DATA))return;
        var tag=item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(tag.contains("Scroll"))scroll.deserializeNBT(level.registryAccess(),tag.getCompound("Scroll"));
        if(tag.contains("CastingLedger"))ledger.load(tag.getCompound("CastingLedger"));
        constructChanged();
    }
    public void restoreLegacyEntity(CompoundTag tag,UUID legacyId){
        scroll.deserializeNBT(level.registryAccess(),tag.getCompound("ConstructScroll"));ledger.load(tag.getCompound("CastingLedger"));
        owner=tag.hasUUID("ConstructOwner")?tag.getUUID("ConstructOwner"):null;migrationId=legacyId;mana=0;constructChanged();
    }
    public boolean migratedFrom(UUID legacyId){return legacyId.equals(migrationId);}
    @Override protected void write(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.write(tag,lookup,packet);tag.put("Scroll",scroll.serializeNBT(lookup));tag.put("CastingLedger",ledger.save());tag.putInt("InternalMana",mana);tag.putLong("RecoveredMana",recoveredMana);
        if(owner!=null)tag.putUUID("Owner",owner);if(migrationId!=null)tag.putUUID("LegacyReactor",migrationId);
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.read(tag,lookup,packet);scroll.deserializeNBT(lookup,tag.getCompound("Scroll"));ledger.load(tag.getCompound("CastingLedger"));
        mana=Math.clamp(tag.getInt("InternalMana"),0,manaCapacity());owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;
        recoveredMana=Math.max(0,tag.getLong("RecoveredMana"));
        migrationId=tag.hasUUID("LegacyReactor")?tag.getUUID("LegacyReactor"):null;if(!packet)lastManaTick=Long.MIN_VALUE;
    }
}
