package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import dev.createspellbook.reactor.ReactorShaftBlockEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;

/** Only the native shaft turns. The housing remains part of the static chunk mesh.
 * This BE has no Flywheel visual, so its fallback renderer must run on either backend. */
public final class ReactorShaftRenderer implements BlockEntityRenderer<ReactorShaftBlockEntity> {
    public ReactorShaftRenderer(BlockEntityRendererProvider.Context context){}
    /** A full opaque casing has no interior skylight; sample its exposed socket
     * too, just as Create lights a metal fitting on the outside of a machine. */
    public static int shaftLight(ReactorShaftBlockEntity shaft,int light){
        if(shaft.getLevel()==null)return light;
        var front=shaft.getBlockPos().relative(shaft.getBlockState().getValue(dev.createspellbook.reactor.ReactorShaftBlock.FACING));
        int outer=LevelRenderer.getLightColor(shaft.getLevel(),front);
        return LightTexture.pack(Math.max(LightTexture.block(light),LightTexture.block(outer)),Math.max(LightTexture.sky(light),LightTexture.sky(outer)));
    }
    @Override public void render(ReactorShaftBlockEntity shaft,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var state=shaft.getBlockState();var facing=state.getValue(dev.createspellbook.reactor.ReactorShaftBlock.FACING);
        // The installed half-shaft extends from the center towards its facing.
        // A complete shaft puts a coplanar end cap on the closed rear casing.
        var rod=CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF,state,facing);
        KineticBlockEntityRenderer.renderRotatingBuffer(shaft,rod,pose,buffers.getBuffer(RenderType.cutoutMipped()),shaftLight(shaft,light));
    }
}
