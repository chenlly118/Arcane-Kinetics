package dev.createspellbook.constructs;

import dev.createspellbook.devices.DeviceContent;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Read-only compatibility for old saves; no health, taunt, attacks or live device functionality. */
public final class LegacyReactorMigration extends Entity {
    private CompoundTag legacy=new CompoundTag();
    public LegacyReactorMigration(EntityType<? extends Entity> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){}
    @Override protected void readAdditionalSaveData(CompoundTag tag){
        legacy=new CompoundTag();for(String key:java.util.List.of("ConstructScroll","CastingLedger","ConstructOwner"))if(tag.contains(key))legacy.put(key,tag.get(key).copy());
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.merge(legacy.copy());}
    @Override public void tick(){
        if(level().isClientSide)return;BlockPos pos=blockPosition();if(!level().hasChunkAt(pos))return;
        if(level().getBlockEntity(pos) instanceof ArcaneAutoCasterBlockEntity present && present.migratedFrom(getUUID())){discard();return;}
        if(level().getBlockState(pos).isAir()){
            var facing=Direction.fromYRot(getYRot());
            if(level().setBlockAndUpdate(pos,DeviceContent.AUTO_CASTER.get().defaultBlockState().setValue(ArcaneAutoCasterBlock.FACING,facing))
                    && level().getBlockEntity(pos) instanceof ArcaneAutoCasterBlockEntity caster){caster.restoreLegacyEntity(legacy,getUUID());discard();return;}
        }
        // A building occupies the old entity's cell. Recover its one scroll and
        // cooldown in one new item instead of replacing a player's block.
        var item=new ItemStack(DeviceContent.REACTOR_ITEM.get());var saved=new CompoundTag();
        saved.putString("id","create_spellbook:arcane_reactor");saved.put("Scroll",legacy.getCompound("ConstructScroll").copy());saved.put("CastingLedger",legacy.getCompound("CastingLedger").copy());
        if(legacy.hasUUID("ConstructOwner"))saved.putUUID("Owner",legacy.getUUID("ConstructOwner"));
        saved.putInt("InternalMana",0);item.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(saved));spawnAtLocation(item);discard();
    }
    @Override public boolean isPickable(){return false;}
}
