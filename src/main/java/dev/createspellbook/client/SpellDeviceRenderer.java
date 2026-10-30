package dev.createspellbook.client;

import dev.createspellbook.devices.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.createmod.catnip.render.FluidRenderHelper;
import net.neoforged.neoforge.fluids.FluidStack;

public final class SpellDeviceRenderer<T extends SpellDeviceBlockEntity> implements BlockEntityRenderer<T> {
    private static final FluidRenderHelper<FluidStack> FLUIDS=new FluidRenderHelper<>();
    public SpellDeviceRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(T device,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(device instanceof FocusingBasinBlockEntity basin && !basin.tank.isEmpty()) {
            float fraction=(float)basin.tank.getFluidAmount()/Math.max(1,basin.tank.getCapacity());
            FLUIDS.renderFluidBox(basin.tank.getFluid(),.125f,.15f,.125f,.875f,.15f+.5f*fraction,.875f,buffers,pose,light,false,true);
        }
    }
}
