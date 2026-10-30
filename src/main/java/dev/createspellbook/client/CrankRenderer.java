package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.createspellbook.block.ArcaneCrankBlock;
import dev.createspellbook.block.ArcaneCrankBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/**
 * The installed Create handle partial is authored for a NORTH facing crank: its
 * hub sits on the +Z side and the grip protrudes towards -Z.  Create orients it with
 * {@code CachedBuffers.partialFacing(handle, state, facing.getOpposite())}, i.e. the
 * rotateY/rotateX pair for the opposite direction only, then spins it around the
 * facing axis.  The previous renderer applied a different rotation to every facing,
 * which detached the handle from the shaft.  Keep the same pivot (block centre) and
 * spin the handle in its own XY plane before orienting it.
 */
public final class CrankRenderer implements BlockEntityRenderer<ArcaneCrankBlockEntity> {
    public CrankRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ArcaneCrankBlockEntity entity,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
        Direction facing=entity.getBlockState().getValue(ArcaneCrankBlock.FACING);
        poses.pushPose();
        poses.translate(.5,.5,.5);
        orient(poses,facing);
        // Spin in the authored XY plane; the orientation above maps that plane's
        // normal onto the kinetic axis, so every facing rotates around its own shaft.
        poses.mulPose(Axis.ZP.rotationDegrees(entity.renderedAngle(partial)));
        poses.translate(-.5,-.5,-.5);
        ArcaneModels.render(ArcaneModels.CRANK_HANDLE,poses,buffers,light,1f,false);
        poses.popPose();
    }
    /** Same orientation Create produces for create:block/hand_crank/handle. */
    public static void orient(PoseStack poses,Direction facing) {
        switch(facing) {
            case UP -> poses.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> poses.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> poses.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> poses.mulPose(Axis.YP.rotationDegrees(-90));
            case WEST -> poses.mulPose(Axis.YP.rotationDegrees(90));
            case NORTH -> { }
        }
    }
}
