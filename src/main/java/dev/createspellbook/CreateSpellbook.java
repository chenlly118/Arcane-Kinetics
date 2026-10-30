package dev.createspellbook;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

@Mod(CreateSpellbook.ID)
public final class CreateSpellbook {
    public static final String ID = "create_spellbook";
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }

    public CreateSpellbook(IEventBus bus, ModContainer container) {
        Content.register(bus);
        dev.createspellbook.processing.SpellScrollIngredient.register(bus);
        dev.createspellbook.devices.DeviceContent.register(bus);
        dev.createspellbook.reactor.ReactorContent.init(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::capabilities);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->
                com.simibubi.create.api.stress.BlockStressValues.IMPACTS.register(dev.createspellbook.devices.DeviceContent.CASTING_ARM.get(),()->Balance.EMITTER_STRESS.get())));
        container.registerConfig(ModConfig.Type.SERVER, Balance.SPEC);
    }

    private void attributes(EntityAttributeModificationEvent event) {
        for (var type : event.getTypes()) {
            event.add(type, Content.MECHANICAL_POWER);
            event.add(type, Content.MECHANICAL_RESIST);
        }
    }
    private void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                dev.createspellbook.reactor.ReactorContent.MANA_BE.get(),(be,side)->be.fluidPort(side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                dev.createspellbook.reactor.ReactorContent.OUTPUT_BE.get(),(be,side)->be.itemPort(side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                dev.createspellbook.reactor.ReactorContent.PORT_BE.get(),(be,side)->be.itemPort(side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                dev.createspellbook.devices.DeviceContent.SCROLL_PRINTER_BE.get(),(be,side)->be.fluidPort(side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                dev.createspellbook.devices.DeviceContent.SCROLL_PRINTER_BE.get(),(be,side)->be.itemPort(side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                dev.createspellbook.devices.DeviceContent.AUTO_CASTER_BE.get(),(be,side)->be.inputPort(side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                dev.createspellbook.devices.DeviceContent.CASTING_ARM_BE.get(),(be,side)->be.fluidPort(side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                dev.createspellbook.devices.DeviceContent.CASTING_ARM_BE.get(),(be,side)->be.scrollPort(side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                dev.createspellbook.devices.DeviceContent.BASIN_BE.get(),(be,side)->be.tank);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                dev.createspellbook.devices.DeviceContent.DEPOT_BE.get(),(be,side)->be.automation(side));
        event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM,
                (stack,context)->new net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper(stack),Content.MANA_BUCKET.get());
        event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM,
                (stack,context)->new dev.createspellbook.item.BloodBucketItem.Handler(stack),Content.BLOOD_BUCKET.get());
        event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM,
                (stack,context)->new net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper(stack),Content.DIVINE_SOUL_BUCKET.get());
    }
}
