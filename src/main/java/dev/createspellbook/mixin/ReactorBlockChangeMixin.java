package dev.createspellbook.mixin;

import dev.createspellbook.reactor.ReactorManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe the verified vanilla state notification, including setBlock(...,2).
 * No replacement, cancellation, neighbour changes or additional world writes. */
@Mixin(value=ServerLevel.class,remap=false)
public abstract class ReactorBlockChangeMixin {
    @Inject(method="onBlockStateChange",at=@At("TAIL"))
    private void createSpellbook$reactorGeometryChanged(BlockPos pos,BlockState before,BlockState after,CallbackInfo callback){ReactorManager.changed((ServerLevel)(Object)this,pos,before,after);}
    // The public LevelEvent.Save fires AFTER chunks are written in NeoForge 21.1.251.
    // Bracket the verified native save call instead; all source/network changes still
    // go through GeneratingKineticBlockEntity.updateGeneratedRotation().
    @Redirect(method="save",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerChunkCache;save(Z)V"))
    private void createSpellbook$snapshotValidatedSources(ServerChunkCache chunks,boolean flush){ReactorManager.saveKinetics((ServerLevel)(Object)this,()->chunks.save(flush));}
}
