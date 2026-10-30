package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.createspellbook.Content;
import dev.createspellbook.reactor.*;
import net.createmod.catnip.render.FluidRenderHelper;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

/** Tank-like visual only: no fluid blocks, per-cell scans or extra entities. */
public final class ReactorFluidRenderer {
    private static final FluidRenderHelper<FluidStack> FLUIDS=new FluidRenderHelper<>();
    private static final double INSET=1d/256;
    /** World-aligned bounds work for all four controller orientations. */
    public static AABB chamber(ReactorLayout layout){
        var b=layout.bounds();
        return new AABB(b.minX()+1+INSET,b.minY()+1+INSET,b.minZ()+1+INSET,
                b.maxX()-INSET,b.maxY()-INSET,b.maxZ()-INSET);
    }
    public static AABB volume(ReactorLayout layout,int amount,int capacity){
        if(amount<=0 || capacity<=0)return null;
        var c=chamber(layout);double fraction=Math.clamp(amount/(double)capacity,0,1);
        return new AABB(c.minX,c.minY,c.minZ,c.maxX,c.minY+c.getYsize()*fraction,c.maxZ);
    }
    public static void render(ReactorControllerBlockEntity controller,PoseStack pose,MultiBufferSource buffers){
        var op=controller.operation;var volume=volume(controller.lastLayout(),op.manaStored(),op.manaCapacity());
        if(volume==null)return;
        var pos=controller.getBlockPos();
        // Native Catnip tiling preserves the installed fluid's tint, alpha,
        // animated sprite and luminosity instead of stretching a bucket icon.
        // The empty chamber has no real fluid/light-emitting blocks. Sample the
        // visible exterior as well, like a tank window, so an opaque controller
        // or core does not turn pale mana black in an otherwise sunlit tank.
        int inner=LevelRenderer.getLightColor(controller.getLevel(),controller.lastLayout().center());
        int outer=LevelRenderer.getLightColor(controller.getLevel(),pos.relative(controller.facing()));
        int light=(Math.max((inner>>4)&15,(outer>>4)&15)<<4)|(Math.max((inner>>20)&15,(outer>>20)&15)<<20);
        FLUIDS.renderFluidBox(new FluidStack(Content.MANA.get(),op.manaStored()),
                (float)(volume.minX-pos.getX()),(float)(volume.minY-pos.getY()),(float)(volume.minZ-pos.getZ()),
                (float)(volume.maxX-pos.getX()),(float)(volume.maxY-pos.getY()),(float)(volume.maxZ-pos.getZ()),buffers.getBuffer(ArcaneRenderTypes.REACTOR_FLUID),pose,light,true,false);
    }
    private ReactorFluidRenderer(){}
}
