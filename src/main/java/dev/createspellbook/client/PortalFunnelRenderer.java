package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.FlapStuffs;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.utility.*;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Native filter item rendering and the original Create funnel flap geometry. */
public final class PortalFunnelRenderer extends SmartBlockEntityRenderer<PortalFunnelBlockEntity> {
    public static final net.minecraft.resources.ResourceLocation FLAP=CreateSpellbook.id("block/portal_funnel_flap");
    public static final net.minecraft.resources.ResourceLocation BELT_FLAP=CreateSpellbook.id("block/portal_belt_funnel_flap");
    private static final PartialModel FLAP_MODEL=PartialModel.of(FLAP);
    private static final PartialModel BELT_FLAP_MODEL=PartialModel.of(BELT_FLAP);
    public PortalFunnelRenderer(BlockEntityRendererProvider.Context context){super(context);}
    @Override protected void renderSafe(PortalFunnelBlockEntity funnel,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        super.renderSafe(funnel,partial,pose,buffers,light,overlay);
        var state=funnel.getBlockState();var facing=PortalFunnelBlock.facing(state);if(facing.getAxis().isVertical())return;
        var model=state.getBlock() instanceof com.simibubi.create.content.logistics.funnel.BeltFunnelBlock?BELT_FLAP_MODEL:FLAP_MODEL;
        float flap=((dev.createspellbook.mixin.FunnelBehaviourAccess)(Object)funnel).spellbook$flap().getValue(partial);
        FlapStuffs.renderFlaps(pose,buffers.getBuffer(RenderType.cutoutMipped()),CachedBuffers.partial(model,state),FlapStuffs.FUNNEL_PIVOT,facing,flap,-funnel.getFlapOffset(),light);
    }
}
