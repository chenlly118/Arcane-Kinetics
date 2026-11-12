package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.utility.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.resources.ResourceLocation;

/** Uses the original machine's recessed configuration square for the real template. */
public final class FireworkReactorRenderer implements BlockEntityRenderer<FireworkReactorBlockEntity> {
    public static final ResourceLocation CRYSTAL=CreateSpellbook.id("block/utility/firework_crystal");
    public FireworkReactorRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(FireworkReactorBlockEntity reactor,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(reactor.getLevel()==null)return;
        double time=ConstructModels.renderTime(reactor.getLevel(),partial);
        pose.pushPose();pose.translate(.5,.9375+Math.sin(time*.06)*.018,.5);
        pose.mulPose(Axis.YP.rotationDegrees((float)(time*.8)));pose.translate(-.5,-.9375,-.5);
        ArcaneModels.render(CRYSTAL,pose,buffers,0x00f000f0,1,false);pose.popPose();
        var template=reactor.template();if(template.isEmpty())return;
        pose.pushPose();pose.translate(.5,7d/16,.5);CrankRenderer.orient(pose,reactor.getBlockState().getValue(FireworkReactorBlock.FACING));pose.translate(0,0,.508);pose.scale(.29f,.29f,.29f);
        Minecraft.getInstance().getItemRenderer().renderStatic(template,ItemDisplayContext.FIXED,light,overlay,pose,buffers,reactor.getLevel(),0);pose.popPose();
    }
}
