package dev.createspellbook.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class Targeting {
    public static BlockHitResult block(LivingEntity caster,double range) {
        return caster.level().clip(new ClipContext(caster.getEyePosition(),caster.getEyePosition().add(caster.getLookAngle().scale(range)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,caster));
    }
    public static boolean canInteract(ServerLevel level, LivingEntity caster,BlockHitResult hit) {
        BlockPos pos=hit.getBlockPos();
        if(hit.getType()!=HitResult.Type.BLOCK || !level.hasChunkAt(pos)) return false;
        if(!(caster instanceof Player player)) return false;
        if(!player.mayBuild() || !level.mayInteract(player,pos)) return false;
        var event=new PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,pos,hit);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled() && event.getUseBlock()!=net.neoforged.neoforge.common.util.TriState.FALSE;
    }
    private Targeting() {}
}
