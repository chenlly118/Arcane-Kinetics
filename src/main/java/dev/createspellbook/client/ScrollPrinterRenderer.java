package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.printing.ScrollPrinterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

/** Compact ink reservoir with a hanging, vertically actuated printing die. */
public final class ScrollPrinterRenderer implements BlockEntityRenderer<ScrollPrinterBlockEntity> {
    public static final ResourceLocation HEAD=CreateSpellbook.id("block/scroll_printer/head");
    private static final net.createmod.catnip.render.FluidRenderHelper<net.neoforged.neoforge.fluids.FluidStack> FLUIDS=new net.createmod.catnip.render.FluidRenderHelper<>();
    public ScrollPrinterRenderer(BlockEntityRendererProvider.Context context){}
    public static float press(float remaining,int total){
        if(remaining<0)return 0;
        float progress=1-Math.clamp(remaining/Math.max(1,total),0,1);
        // Contact matches the server's 75% payment point; then retract.
        float travel=progress<.15f?0:progress<.7f?(progress-.15f)/.55f:progress<.8f?1:(1-progress)/.2f;
        travel=Math.clamp(travel,0,1);
        return travel*travel*(3-2*travel)*9/16;
    }
    @Override public void render(ScrollPrinterBlockEntity printer,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        float remaining=printer.remainingTicks()-partial;
        pose.pushPose();pose.translate(.5,.5,.5);CrankRenderer.orient(pose,printer.facing());pose.translate(-.5,-.5,-.5);
        if(!printer.tank.isEmpty()){
            float height=4.1f+8.7f*Math.clamp(printer.tank.getFluidAmount()/(float)Math.max(1,printer.tank.getCapacity()),0,1);
            FLUIDS.renderFluidBox(printer.tank.getFluid(),3.1f/16,4.1f/16,3.1f/16,12.9f/16,height/16,12.9f/16,
                    buffers.getBuffer(net.minecraft.client.renderer.RenderType.translucent()),pose,light,true,false);
        }
        pose.pushPose();pose.translate(0,-press(remaining,printer.strokeTicks()),0);ArcaneModels.render(HEAD,pose,buffers,light,1,false);
        var focus=printer.inventory.getStackInSlot(1);
        if(!focus.isEmpty()){
            pose.pushPose();pose.translate(.5,-4.8/16,2.98/16);pose.mulPose(Axis.YP.rotationDegrees(180));pose.scale(.13f,.13f,.13f);
            Minecraft.getInstance().getItemRenderer().renderStatic(focus,ItemDisplayContext.FIXED,light,overlay,pose,buffers,printer.getLevel(),1);pose.popPose();
        }
        pose.popPose();
        var template=printer.inventory.getStackInSlot(0);
        if(!template.isEmpty()){
            pose.pushPose();pose.translate(14.4/16,8.5/16,8d/16);pose.mulPose(Axis.YP.rotationDegrees(90));pose.scale(.30f,.30f,.30f);
            Minecraft.getInstance().getItemRenderer().renderStatic(template,ItemDisplayContext.FIXED,light,overlay,pose,buffers,printer.getLevel(),0);pose.popPose();
        }
        pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(ScrollPrinterBlockEntity printer){return true;}
}
