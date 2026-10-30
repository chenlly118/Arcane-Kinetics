package dev.createspellbook.reactor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/** Persistent dropped waste, not a black-hole entity. Administrative /kill and
 * explicit mod/command removal remain possible; no accident or retaliation. */
public final class CollapsedMatterEntity extends ItemEntity {
    public CollapsedMatterEntity(EntityType<? extends ItemEntity> type,Level level){super(type,level);protectLifetime();}
    // Vanilla's unlimited-lifetime sentinel disables merging. Renew ordinary age
    // instead, so persistent waste can still merge into normal-sized stacks.
    public void protectLifetime(){lifespan=Integer.MAX_VALUE;setExtendedLifetime();}
    @Override public void tick(){if(getAge()>=1000 || getAge()==Short.MIN_VALUE)protectLifetime();super.tick();}
    @Override public boolean hurt(DamageSource source,float amount){return source.is(DamageTypes.GENERIC_KILL) && super.hurt(source,amount);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);protectLifetime();}
    @Override protected void onBelowWorld(){
        // Keep it in the already-loaded column. No new chunk tickets or searches.
        setPos(getX(),level().getMinBuildHeight()+1,getZ());setDeltaMovement(0,0,0);protectLifetime();
    }
}
