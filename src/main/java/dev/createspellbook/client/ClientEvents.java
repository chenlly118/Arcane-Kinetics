package dev.createspellbook.client;

import dev.createspellbook.Content;
import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.reactor.ReactorContent;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;

@EventBusSubscriber(modid=CreateSpellbook.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientEvents {
    private static final int SINGULARITY_TINT=0xff2b2833;
    static { dev.createspellbook.client.ponder.PonderClientSetup.init(); }
    @SubscribeEvent public static void menus(RegisterMenuScreensEvent event){event.register(dev.createspellbook.reactor.ReactorContent.CONTROL_MENU.get(),ReactorScreen::new);}
    public static final int MANA_COLOR=0x887fcfff;
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) {
        e.enqueueWork(()->{
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(dev.createspellbook.utility.UtilityContent.PORTAL.get(),net.minecraft.client.renderer.RenderType.cutoutMipped());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(dev.createspellbook.utility.UtilityContent.PORTAL_BELT.get(),net.minecraft.client.renderer.RenderType.cutoutMipped());
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(Content.ENGINEERS_HANDBOOK.get(),io.redspace.ironsspellbooks.render.SpellBookCurioRenderer::new);
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(dev.createspellbook.reactor.ReactorContent.GLASS.get(),net.minecraft.client.renderer.RenderType.translucent());
            // The basin has its own open cuboid shell and hand-drawn atlas.
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(dev.createspellbook.devices.DeviceContent.BASIN.get(),net.minecraft.client.renderer.RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(Content.DIVINE_SOUL.get(),net.minecraft.client.renderer.RenderType.translucent());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(Content.FLOWING_DIVINE_SOUL.get(),net.minecraft.client.renderer.RenderType.translucent());
            for (var fuel : dev.createspellbook.fluid.DieselFuels.FUELS) {
                var layer = fuel.translucent() ? net.minecraft.client.renderer.RenderType.translucent() : net.minecraft.client.renderer.RenderType.solid();
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(fuel.source(), layer);
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(fuel.flowing(), layer);
            }
        });
    }
    @SubscribeEvent public static void fluids(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e) {
        e.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
            @Override public net.minecraft.resources.ResourceLocation getStillTexture() { return CreateSpellbook.id("fluid/liquid_divine_soul_still"); }
            @Override public net.minecraft.resources.ResourceLocation getFlowingTexture() { return CreateSpellbook.id("fluid/liquid_divine_soul_flow"); }
        },Content.DIVINE_SOUL_TYPE.get());
        for (var fuel : dev.createspellbook.fluid.DieselFuels.FUELS) {
            e.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
                @Override public net.minecraft.resources.ResourceLocation getStillTexture() { return fuel.stillTexture(); }
                @Override public net.minecraft.resources.ResourceLocation getFlowingTexture() { return fuel.flowingTexture(); }
            }, fuel.type());
        }
        if(Content.USE_WIZARDRY_MANA) return; // Preserve Wizardry's native fluid textures and behavior.
        e.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
            @Override public net.minecraft.resources.ResourceLocation getStillTexture() { return net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_still"); }
            @Override public net.minecraft.resources.ResourceLocation getFlowingTexture() { return net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_flow"); }
            @Override public int getTintColor() { return MANA_COLOR; }
        },Content.MANA_TYPE.get());
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item e) {
        e.register(new net.neoforged.neoforge.client.model.DynamicFluidContainerModel.Colors(),Content.MANA_BUCKET.get(),Content.BLOOD_BUCKET.get(),Content.DIVINE_SOUL_BUCKET.get());
        for (var fuel : dev.createspellbook.fluid.DieselFuels.FUELS) {
            e.register(new net.neoforged.neoforge.client.model.DynamicFluidContainerModel.Colors(), fuel.bucket());
        }
        e.register((stack,index)->index==0 ? 0xff7fcfff:0xffffffff,Content.MANA_BOTTLE.get());
        e.register((stack,index)->index==0?SINGULARITY_TINT:-1,ReactorContent.SINGULARITY_INGOT.get(),ReactorContent.SINGULARITY_BLOCK_ITEM.get());
    }
    @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block e) {
        e.register((state,level,pos,index)->index==0?SINGULARITY_TINT:-1,ReactorContent.SINGULARITY_BLOCK.get());
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(dev.createspellbook.utility.UtilityContent.PORTAL_BE.get(),PortalFunnelRenderer::new);
        e.registerBlockEntityRenderer(dev.createspellbook.utility.UtilityContent.FIREWORK_BE.get(),FireworkReactorRenderer::new);
        e.registerEntityRenderer(dev.createspellbook.reactor.ReactorContent.MATTER_ENTITY.get(),net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        e.registerBlockEntityRenderer(dev.createspellbook.reactor.ReactorContent.CONTROLLER_BE.get(),BlackHoleRenderer::new);
        e.registerBlockEntityRenderer(dev.createspellbook.reactor.ReactorContent.SHAFT_BE.get(),ReactorShaftRenderer::new);
        e.registerBlockEntityRenderer(dev.createspellbook.devices.DeviceContent.SCROLL_PRINTER_BE.get(),ScrollPrinterRenderer::new);
        e.registerEntityRenderer(dev.createspellbook.devices.DeviceContent.ARM_CASTER.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);
        e.registerEntityRenderer(dev.createspellbook.devices.DeviceContent.LEGACY_REACTOR.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);
        e.registerBlockEntityRenderer(dev.createspellbook.devices.DeviceContent.AUTO_CASTER_BE.get(),AutoCasterRenderer::new);
        e.registerBlockEntityRenderer(dev.createspellbook.devices.DeviceContent.CASTING_ARM_BE.get(),CastingArmRenderer::new);
        e.registerEntityRenderer(dev.createspellbook.devices.DeviceContent.SPELL_PROBE.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);
        e.registerBlockEntityRenderer(dev.createspellbook.devices.DeviceContent.BASIN_BE.get(),SpellDeviceRenderer::new);
        e.registerBlockEntityRenderer(dev.createspellbook.devices.DeviceContent.DEPOT_BE.get(),com.simibubi.create.content.logistics.depot.DepotRenderer::new);
        e.registerEntityRenderer(Content.MECHANICAL_ENTITY.get(),MechanicalRenderer::new);
        e.registerBlockEntityRenderer(Content.CRANK_BE.get(),CrankRenderer::new);
    }
    @SubscribeEvent public static void shaders(RegisterShadersEvent e) throws java.io.IOException {
        e.registerShader(new ShaderInstance(e.getResourceProvider(),CreateSpellbook.id("arcane_blue"),DefaultVertexFormat.NEW_ENTITY),shader->ArcaneRenderTypes.shader=shader);
    }
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional e) {
        e.register(ModelResourceLocation.standalone(FireworkReactorRenderer.CRYSTAL));
        e.register(ModelResourceLocation.standalone(PortalFunnelRenderer.FLAP));
        e.register(ModelResourceLocation.standalone(PortalFunnelRenderer.BELT_FLAP));
        ReactorConnectedTextures.init();
        e.register(ModelResourceLocation.standalone(ScrollPrinterRenderer.HEAD));
        for(var location:ConstructModels.MODELS)e.register(ModelResourceLocation.standalone(location));
        for(var location:ArcaneModels.CONSTRUCTS) e.register(ModelResourceLocation.standalone(location));
        e.register(ModelResourceLocation.standalone(ArcaneModels.CRANK_HANDLE));
        e.register(ModelResourceLocation.standalone(ArcaneModels.PISTON_BODY));
        e.register(ModelResourceLocation.standalone(ArcaneModels.PRESS_BODY));
        for(var location:ArcaneModels.BATCH2_MODELS) e.register(ModelResourceLocation.standalone(location));
        e.register(ModelResourceLocation.standalone(CreateSpellbook.id("item/scroll_mechanical")));
    }
    @SubscribeEvent public static void connectedModels(ModelEvent.ModifyBakingResult e){ReactorConnectedTextures.bake(e);}
    @SubscribeEvent public static void spriteSources(RegisterSpriteSourceTypesEvent e){e.register(PortalFunnelSprites.ID,PortalFunnelSprites.TYPE);}
}
