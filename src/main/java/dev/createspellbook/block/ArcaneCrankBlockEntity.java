package dev.createspellbook.block;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.createspellbook.Balance;
import dev.createspellbook.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public final class ArcaneCrankBlockEntity extends GeneratingKineticBlockEntity {
    private long expiresAt;
    private float rpm;
    private float totalCapacity;
    private boolean suspended;
    /** Client-side visual angle in degrees; never used for kinetic simulation. */
    private float independentAngle;
    private float chasingVelocity;
    public ArcaneCrankBlockEntity(BlockPos pos, BlockState state) { super(Content.CRANK_BE.get(), pos, state); }

    public void activate(int spellLevel, double power) {
        if (level == null || level.isClientSide) return;
        var output=output(spellLevel,power);
        rpm=output.rpm(); totalCapacity=output.capacity();
        expiresAt=level.getGameTime()+output.leaseTicks();
        updateGeneratedRotation();
        setChanged();
    }
    /** Used by both the authoritative network source and the spell tooltip. */
    public record Output(float rpm,float capacity,int leaseTicks) {}
    public static Output output(int spellLevel,double power) {
        long steps=Balance.levelSteps(spellLevel);
        float rpm=(float)Math.clamp(Balance.CRANK_MIN_RPM.get()
                +(Balance.CRANK_MAX_RPM.get()-Balance.CRANK_MIN_RPM.get())*steps/9d,1,AllConfigs.server().kinetics.maxRotationSpeed.get());
        // Steam engines generate at 16 RPM; Create capacity is expressed per RPM.
        float capacity=(float)Math.min(Float.MAX_VALUE,BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get())*16d
                *(1+steps/3d)*Math.max(0,power)*Balance.BOILER_SCALE.get());
        int lease=(int)Math.clamp(Balance.CRANK_MIN_LEASE.get()
                +(Balance.CRANK_MAX_LEASE.get()-Balance.CRANK_MIN_LEASE.get())*steps/9,10,1200);
        return new Output(rpm,capacity,lease);
    }
    public boolean isActive() { return !suspended && level != null && level.getGameTime() < expiresAt && rpm > 0; }
    public float totalCapacity() { return isActive() ? totalCapacity : 0; }
    public long expiresAt() { return expiresAt; }
    /** Smoothed handle angle in degrees, mirroring Create's HandCrankBlockEntity. */
    public float renderedAngle(float partial) { return independentAngle + partial * chasingVelocity; }
    @Override public float getGeneratedSpeed() {
        return isActive() ? convertToDirection(rpm, getBlockState().getValue(ArcaneCrankBlock.FACING)) : 0;
    }
    @Override public float calculateAddedStressCapacity() {
        lastCapacityProvided = isActive() ? totalCapacity / Math.abs(rpm) : 0;
        return lastCapacityProvided;
    }
    @Override public void initialize() { suspended=false; super.initialize(); if (!level.isClientSide) updateGeneratedRotation(); }
    @Override public void onChunkUnloaded() {
        // Create deliberately retains unloaded kinetic members. A temporary source must withdraw its lease.
        suspended=true;
        if(level!=null && !level.isClientSide) updateGeneratedRotation();
        super.onChunkUnloaded();
    }
    @Override public void tick() {
        super.tick();
        // 1 RPM = 360 / 20 / 60 = 0.3 degrees per tick; smooth like Create's crank.
        float actualSpeed = getSpeed();
        chasingVelocity += (actualSpeed * .3f - chasingVelocity) * .25f;
        independentAngle += chasingVelocity;
        if (!level.isClientSide && expiresAt != 0 && level.getGameTime()>=expiresAt) {
            expiresAt = 0; rpm = 0; totalCapacity = 0;
            updateGeneratedRotation(); setChanged();
        }
    }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putLong("ArcaneExpiresAt", expiresAt); tag.putFloat("ArcaneRPM", rpm); tag.putFloat("ArcaneCapacity", totalCapacity);
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        expiresAt = tag.getLong("ArcaneExpiresAt"); rpm = tag.getFloat("ArcaneRPM"); totalCapacity = tag.getFloat("ArcaneCapacity");
        super.read(tag, registries, clientPacket);
    }
}
