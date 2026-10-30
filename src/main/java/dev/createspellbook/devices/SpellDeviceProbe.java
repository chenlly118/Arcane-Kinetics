package dev.createspellbook.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;

/** A lightweight, non-AI hit receiver bound to a loaded device, never a saved mob. */
public final class SpellDeviceProbe extends LivingEntity {
    private static final EntityDataAccessor<BlockPos> DEVICE=SynchedEntityData.defineId(SpellDeviceProbe.class,EntityDataSerializers.BLOCK_POS);
    public SpellDeviceProbe(EntityType<? extends LivingEntity> type,Level level) {super(type,level);blocksBuilding=false;noPhysics=true;setNoGravity(true);setSilent(true);}
    public void bind(BlockPos pos) {entityData.set(DEVICE,pos.immutable());setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);}
    public BlockPos devicePos() {return entityData.get(DEVICE);}
    @Override public boolean canBeSeenAsEnemy() {return false;}
    // Hide client overlays/debug hitboxes too. Server invisibility can change
    // native spell targeting, so leave the authoritative receiver unchanged.
    @Override public boolean isInvisible() {return level().isClientSide || super.isInvisible();}
    public SpellTarget device() {return SpellTarget.at(level(),devicePos());}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {super.defineSynchedData(builder);builder.define(DEVICE,BlockPos.ZERO);}
    @Override public void tick() {
        baseTick();
        if(!level().isClientSide) {
            var be=device();if(be==null || be.isRemoved() || !be.ownsProbe(getUUID())){discard();return;}
            // Keep the receiver center above the device's rim/top so native
            // area-spell line-of-sight checks do not hit its own casing.
            var pos=devicePos();setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        }
        setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }
    @Override public boolean hurt(DamageSource source,float amount) {if(!level().isClientSide)SpellDeviceBridge.directDamage(this,source,amount);return false;}
    @Override public InteractionResult interact(Player player,InteractionHand hand) {var be=device();return be==null?InteractionResult.PASS:be.playerUse(player,hand);}
    @Override public boolean isPushable() {return false;}
    @Override public boolean isOnFire() {return false;}
    @Override public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {return false;}
    // Vanilla client mining/use must select the real block, not an invisible mob.
    // Server spell queries still see the receiver for rays/area effects.
    @Override public boolean isPickable() {return !level().isClientSide;}
    @Override public boolean canBeCollidedWith() {return false;}
    @Override public void push(double x,double y,double z) {}
    @Override public Iterable<ItemStack> getArmorSlots() {return List.of();}
    @Override public ItemStack getItemBySlot(EquipmentSlot slot) {return ItemStack.EMPTY;}
    @Override public void setItemSlot(EquipmentSlot slot,ItemStack stack) {}
    @Override public HumanoidArm getMainArm() {return HumanoidArm.RIGHT;}
    @Override public void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);tag.putLong("Device",devicePos().asLong());}
    @Override public void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);bind(BlockPos.of(tag.getLong("Device")));}
}
