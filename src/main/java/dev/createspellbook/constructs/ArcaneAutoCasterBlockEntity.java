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
    private boolean syncQueued;
    private final dev.createspellbook.utility.RegeneratingManaTank manaTank=new dev.createspellbook.utility.RegeneratingManaTank(()->Balance.AUTO_CAPACITY.get(),()->Balance.AUTO_REGEN.get(),this::setChanged,()->syncQueued=true);
    public final ScrollTemplate scroll=new ScrollTemplate(this::constructChanged);
    public final CastLedger ledger=new CastLedger();
    public ArcaneAutoCasterBlockEntity(BlockPos pos,BlockState state){super(DeviceContent.AUTO_CASTER_BE.get(),pos,state);}
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours){}
    @Override public void initialize(){super.initialize();ensureCaster();}
    @Override public void tick(){
        super.tick();if(level==null || level.isClientSide || isChunkUnloaded() || isRemoved())return;ensureCaster();long now=level.getGameTime();
        manaTank.tick(now);
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
    @Override public int storedMana(){return manaTank.amount();}
    public int supplyBasin(dev.createspellbook.devices.FocusingBasinBlockEntity basin){
        return level instanceof ServerLevel && !isRemoved() && !isChunkUnloaded() && basin.getLevel()==level && worldPosition.below().equals(basin.getBlockPos())
                ?manaTank.transferToBasin(basin,Balance.BASIN_ABSORPTION.get()):0;
    }
    public long recoveredMana(){return manaTank.recovered();}
    @Override public int manaCapacity(){return Balance.AUTO_CAPACITY.get();}
    public IFluidHandler inputPort(Direction side){return side==null || side==facing().getOpposite()?manaTank:null;}
    @Override public boolean canPay(int cost,boolean execute){
        return cost>=0 && manaTank.consume(liquidCost(cost),execute);
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
        owner=tag.hasUUID("ConstructOwner")?tag.getUUID("ConstructOwner"):null;migrationId=legacyId;manaTank.load(new CompoundTag(),false);constructChanged();
    }
    public boolean migratedFrom(UUID legacyId){return legacyId.equals(migrationId);}
    @Override protected void write(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.write(tag,lookup,packet);tag.put("Scroll",scroll.serializeNBT(lookup));tag.put("CastingLedger",ledger.save());manaTank.save(tag);
        if(owner!=null)tag.putUUID("Owner",owner);if(migrationId!=null)tag.putUUID("LegacyReactor",migrationId);
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.read(tag,lookup,packet);scroll.deserializeNBT(lookup,tag.getCompound("Scroll"));ledger.load(tag.getCompound("CastingLedger"));
        manaTank.load(tag,packet);owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;
        migrationId=tag.hasUUID("LegacyReactor")?tag.getUUID("LegacyReactor"):null;
    }
}
