package dev.createspellbook.mixin;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.createspellbook.processing.ArcaneHeat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds a lease to the effective heat result. Never changes real fuel or burn time. */
@Mixin(value=BlazeBurnerBlockEntity.class, remap=false)
public abstract class BlazeBurnerMixin extends SmartBlockEntity implements ArcaneHeat {
    @Unique private long createSpellbook$until;
    protected BlazeBurnerMixin(BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); }
    @Override public void createSpellbook$heatFor(int ticks) {
        if(level==null || level.isClientSide) return;
        if(((BlazeBurnerBlockEntity)(Object)this).isCreative()) return;
        createSpellbook$until=Math.max(createSpellbook$until,level.getGameTime()+ticks);
        ((BlazeBurnerBlockEntity)(Object)this).updateBlockState(); setChanged(); sendData();
    }
    @Override public long createSpellbook$heatExpiry() { return createSpellbook$until; }
    @Inject(method="getHeatLevel",at=@At("RETURN"),cancellable=true)
    private void createSpellbook$effectiveHeat(CallbackInfoReturnable<BlazeBurnerBlock.HeatLevel> cir) {
        if(level!=null && level.getGameTime()<createSpellbook$until && !cir.getReturnValue().isAtLeast(BlazeBurnerBlock.HeatLevel.KINDLED))
            cir.setReturnValue(BlazeBurnerBlock.HeatLevel.KINDLED);
    }
    @Inject(method="tick",at=@At("TAIL"))
    private void createSpellbook$expire(CallbackInfo ci) {
        if(level!=null && !level.isClientSide && createSpellbook$until!=0 && level.getGameTime()>=createSpellbook$until) {
            createSpellbook$until=0; ((BlazeBurnerBlockEntity)(Object)this).updateBlockState(); setChanged(); sendData();
        }
    }
    @Inject(method="write",at=@At("TAIL"))
    private void createSpellbook$save(CompoundTag tag,HolderLookup.Provider registries,boolean packet,CallbackInfo ci) { tag.putLong("CreateSpellbookHeatExpiry",createSpellbook$until); }
    @Inject(method="read",at=@At("TAIL"))
    private void createSpellbook$load(CompoundTag tag,HolderLookup.Provider registries,boolean packet,CallbackInfo ci) { createSpellbook$until=tag.getLong("CreateSpellbookHeatExpiry"); }
}
