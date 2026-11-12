package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.constructs.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** PDF housing is baked; only its shaft, swivelling nozzle and actual scroll render here. */
public final class CastingArmRenderer implements BlockEntityRenderer<ArcaneCastingArmBlockEntity> {
    public static final ResourceLocation NOZZLE=CreateSpellbook.id("block/casting_construct/emitter_nozzle");
    public static final ResourceLocation SHAFT=CreateSpellbook.id("block/casting_construct/emitter_shaft");
    public CastingArmRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ArcaneCastingArmBlockEntity arm,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var facing=arm.facing();var ring=arm.ringSide();
        pose.pushPose();pose.translate(.5,.5,.5);CrankRenderer.orient(pose,facing);pose.translate(0,-.5,0);
        float angle=com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer.getAngleForBe(arm,arm.getBlockPos(),ring.getAxis());
        pose.mulPose(Axis.YP.rotation(angle*ring.getAxisDirection().getStep()));
        pose.translate(-.5,0,-.5);ArcaneModels.render(SHAFT,pose,buffers,light,1,false);pose.popPose();
        pose.pushPose();pose.translate(.5+ring.getStepX()*3d/16,.5+ring.getStepY()*3d/16,.5+ring.getStepZ()*3d/16);
        if(facing.getAxis().isVertical()){
            // Vertical aiming has no stable yaw. Turn from the mounted direction
            // so the emitter head keeps the same roll as its socket.
            var aim=Vec3.directionFromRotation(arm.aimPitch(),arm.aimYaw());
            pose.mulPose(new Quaternionf().rotationTo(facing.getStepX(),facing.getStepY(),facing.getStepZ(),(float)aim.x,(float)aim.y,(float)aim.z));
            CrankRenderer.orient(pose,facing);
        }else{
            pose.mulPose(Axis.YP.rotationDegrees(-arm.aimYaw()-180));pose.mulPose(Axis.XP.rotationDegrees(-arm.aimPitch()));
        }
        pose.translate(-.5,-11d/16,ConstructModels.recoil(arm.getLevel().getGameTime(),arm.ledger.lastRelease,partial)-.5);
        ArcaneModels.render(NOZZLE,pose,buffers,light,1,false);pose.popPose();
        var scroll=arm.scroll.getStackInSlot(0);if(scroll.isEmpty())return;
        pose.pushPose();pose.translate(.5,.5,.5);CrankRenderer.orient(pose,facing);pose.translate(0,3d/16,7d/16);
        pose.mulPose(Axis.XP.rotationDegrees(22.5f));pose.translate(0,.045,3d/16);
        pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(.32f,.32f,.32f);
        Minecraft.getInstance().getItemRenderer().renderStatic(scroll,ItemDisplayContext.FIXED,light,overlay,pose,buffers,arm.getLevel(),0);pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(ArcaneCastingArmBlockEntity entity) {return true;}
}
