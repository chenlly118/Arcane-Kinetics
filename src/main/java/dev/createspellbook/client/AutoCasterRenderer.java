package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.constructs.ArcaneAutoCasterBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;

/** The supplied crystal is the emission point; the frame stays baked in its chunk. */
public final class AutoCasterRenderer implements BlockEntityRenderer<ArcaneAutoCasterBlockEntity> {
    public static final ResourceLocation CRYSTAL=CreateSpellbook.id("block/casting_construct/auto_crystal");
    public AutoCasterRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(ArcaneAutoCasterBlockEntity caster,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        double time=caster.getLevel().getGameTime()+partial;
        float pulse=ConstructModels.recoil(caster.getLevel().getGameTime(),caster.ledger.lastRelease,partial);
        pose.pushPose();pose.translate(.5,.9375+Math.sin(time*.06)*.018+pulse*.25,.5);
        pose.mulPose(Axis.YP.rotationDegrees((float)(time*.8)));pose.translate(-.5,-.9375,-.5);
        ArcaneModels.render(CRYSTAL,pose,buffers,0x00f000f0,1,false);pose.popPose();
        var scroll=caster.scroll.getStackInSlot(0);
        if(!scroll.isEmpty()){
            // Recessed square at the rear of the supplied mesh, centered in its
            // six-pixel frame. This is the real template item, not another texture.
            pose.pushPose();pose.translate(.5,7d/16,.5);CrankRenderer.orient(pose,caster.facing());pose.translate(0,0,.508);
            pose.scale(.29f,.29f,.29f);
            net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(scroll,net.minecraft.world.item.ItemDisplayContext.FIXED,light,overlay,pose,buffers,caster.getLevel(),0);
            pose.popPose();
        }
    }
    @Override public boolean shouldRenderOffScreen(ArcaneAutoCasterBlockEntity caster){return true;}
}
