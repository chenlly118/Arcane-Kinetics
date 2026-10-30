package dev.createspellbook.constructs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** One unsaved, unrendered native caster per loaded casting block. */
public final class ArcaneArmCaster extends ConstructCaster {
    private BlockConstructHost arm;
    public ArcaneArmCaster(EntityType<? extends PathfinderMob> type,Level level) {super(type,level);noPhysics=true;setNoGravity(true);setSilent(true);}
    public void bind(BlockConstructHost arm) {this.arm=arm;aim();}
    @Override public BlockConstructHost host() {return arm;}
    @Override public void aim() {
        if(arm==null)return;
        var mouth=arm.muzzle();
        var target=session.target();
        var direction=target==null?Vec3.atLowerCornerOf(arm.facing().getNormal()):
                (target instanceof dev.createspellbook.devices.SpellDeviceProbe probe?Vec3.atCenterOf(probe.devicePos()).add(0,.15,0):target.getBoundingBox().getCenter()).subtract(mouth).normalize();
        float yaw=getYRot(),pitch=getXRot();
        setPos(mouth.add(0,-getEyeHeight(),0));point(direction);setDeltaMovement(Vec3.ZERO);
        if(arm.constructLevel()!=null && !arm.constructLevel().isClientSide && (Math.abs(net.minecraft.util.Mth.wrapDegrees(getYRot()-yaw))>1 || Math.abs(getXRot()-pitch)>1))arm.constructChanged();
    }
    @Override public void tick() {
        var be=arm==null?null:arm.blockEntity();
        if(!level().isClientSide && (be==null || be.isRemoved() || be.isChunkUnloaded() || !level().hasChunkAt(be.getBlockPos())
                || level().getBlockEntity(be.getBlockPos())!=arm || arm.caster()!=this)){cancelCast();discard();return;}
        aim();super.tick();
    }
    @Override public boolean isPickable() {return false;}
    @Override public boolean canBeSeenAsEnemy() {return false;}
    @Override public boolean hurt(DamageSource source,float amount) {return false;}
    @Override public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {return false;}
}
