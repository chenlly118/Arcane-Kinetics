package dev.createspellbook.devices;

import dev.createspellbook.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.*;
import java.util.List;
import dev.createspellbook.constructs.*;
import dev.createspellbook.printing.*;

public final class DeviceContent {
    public static final DeferredRegister<com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType> ARM_TYPES=DeferredRegister.create(com.simibubi.create.api.registry.CreateRegistries.ARM_INTERACTION_POINT_TYPE,CreateSpellbook.ID);
    public static final DeferredHolder<com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType,EnchantedDepotArmType> DEPOT_ARM=ARM_TYPES.register("enchanted_depot",EnchantedDepotArmType::new);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,CreateSpellbook.ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,CreateSpellbook.ID);
    public static final DeferredHolder<RecipeType<?>,RecipeType<ArcaneProcessingRecipe>> ARCANE_PROCESSING=RECIPE_TYPES.register("arcane_processing",()->new RecipeType<>() {
        @Override public String toString() {return "create_spellbook:arcane_processing";}
    });
    public static final DeferredHolder<RecipeType<?>,RecipeType<dev.createspellbook.processing.SpellAssemblyRecipe>> SPELL_APPLICATION=RECIPE_TYPES.register("spell_application",()->new RecipeType<>(){ @Override public String toString(){return "create_spellbook:spell_application";} });
    public static final DeferredHolder<RecipeSerializer<?>,dev.createspellbook.processing.SpellAssemblyRecipe.Serializer> SPELL_APPLICATION_SERIALIZER=SERIALIZERS.register("spell_application",dev.createspellbook.processing.SpellAssemblyRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>,ArcaneProcessingRecipe.Serializer> ARCANE_SERIALIZER=SERIALIZERS.register("arcane_processing",ArcaneProcessingRecipe.Serializer::new);
    public static final DeferredHolder<Block,SpellDeviceBlock> BASIN=block("focusing_basin",SpellDeviceBlock.Kind.BASIN);
    public static final DeferredHolder<Block,EnchantedDepotBlock> DEPOT=Content.BLOCKS.register("arcane_depot",()->new EnchantedDepotBlock(BlockBehaviour.Properties.of().strength(3,12).noOcclusion()));
    public static final DeferredHolder<Block,SpellDeviceBlock> SENSOR=block("spell_sensor",SpellDeviceBlock.Kind.SENSOR);
    public static final DeferredHolder<Item,BlockItem> BASIN_ITEM=item("focusing_basin",BASIN);
    public static final DeferredHolder<Item,BlockItem> DEPOT_ITEM=item("arcane_depot",DEPOT);
    public static final DeferredHolder<Item,BlockItem> SENSOR_ITEM=item("spell_sensor",SENSOR);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<FocusingBasinBlockEntity>> BASIN_BE=Content.BLOCK_ENTITIES.register("focusing_basin",()->BlockEntityType.Builder.of(FocusingBasinBlockEntity::new,BASIN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ArcaneDepotBlockEntity>> DEPOT_BE=Content.BLOCK_ENTITIES.register("arcane_depot",()->BlockEntityType.Builder.of(ArcaneDepotBlockEntity::new,DEPOT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<SpellSensorBlockEntity>> SENSOR_BE=Content.BLOCK_ENTITIES.register("spell_sensor",()->BlockEntityType.Builder.of(SpellSensorBlockEntity::new,SENSOR.get()).build(null));
    public static final DeferredHolder<EntityType<?>,EntityType<SpellDeviceProbe>> SPELL_PROBE=Content.ENTITIES.register("spell_device_probe",()->EntityType.Builder.<SpellDeviceProbe>of(SpellDeviceProbe::new,MobCategory.MISC).sized(1.08f,1.05f).clientTrackingRange(6).updateInterval(20).noSave().build("spell_device_probe"));
    public static final DeferredHolder<Block,ArcaneCastingArmBlock> CASTING_ARM=Content.BLOCKS.register("arcane_casting_arm",()->new ArcaneCastingArmBlock(BlockBehaviour.Properties.of().strength(3,12).noOcclusion()));
    public static final DeferredHolder<Item,BlockItem> CASTING_ARM_ITEM=Content.ITEMS.register("arcane_casting_arm",()->new BlockItem(CASTING_ARM.get(),new Item.Properties()) {
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {super.appendHoverText(stack,context,lines,flag);lines.add(Component.translatable("tooltip.create_spellbook.arcane_casting_arm").withStyle(net.minecraft.ChatFormatting.GRAY));}
    });
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ArcaneCastingArmBlockEntity>> CASTING_ARM_BE=Content.BLOCK_ENTITIES.register("arcane_casting_arm",()->BlockEntityType.Builder.of(ArcaneCastingArmBlockEntity::new,CASTING_ARM.get()).build(null));
    public static final DeferredHolder<EntityType<?>,EntityType<ArcaneArmCaster>> ARM_CASTER=Content.ENTITIES.register("arm_caster",()->EntityType.Builder.<ArcaneArmCaster>of(ArcaneArmCaster::new,MobCategory.MISC).sized(.25f,.25f).eyeHeight(.125f).clientTrackingRange(6).updateInterval(20).noSave().build("arm_caster"));
    public static final DeferredHolder<Block,ArcaneAutoCasterBlock> AUTO_CASTER=Content.BLOCKS.register("arcane_reactor",()->new ArcaneAutoCasterBlock(BlockBehaviour.Properties.of().strength(3,12).noOcclusion()));
    public static final DeferredHolder<Item,BlockItem> REACTOR_ITEM=Content.ITEMS.register("arcane_reactor",()->new BlockItem(AUTO_CASTER.get(),new Item.Properties().stacksTo(1)) {
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,lines,flag);lines.add(Component.translatable("tooltip.create_spellbook.arcane_reactor").withStyle(net.minecraft.ChatFormatting.GRAY));}
    });
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ArcaneAutoCasterBlockEntity>> AUTO_CASTER_BE=Content.BLOCK_ENTITIES.register("arcane_reactor",()->BlockEntityType.Builder.of(ArcaneAutoCasterBlockEntity::new,AUTO_CASTER.get()).build(null));
    public static final DeferredHolder<Block,ScrollPrinterBlock> SCROLL_PRINTER=Content.BLOCKS.register("scroll_printer",()->new ScrollPrinterBlock(BlockBehaviour.Properties.of().strength(3,12).noOcclusion()));
    public static final DeferredHolder<Item,BlockItem> SCROLL_PRINTER_ITEM=Content.ITEMS.register("scroll_printer",()->new BlockItem(SCROLL_PRINTER.get(),new Item.Properties()) {
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,lines,flag);for(String part:List.of("placement","template","focus","ink"))lines.add(Component.translatable("tooltip.create_spellbook.scroll_printer."+part).withStyle(net.minecraft.ChatFormatting.GRAY));}
    });
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ScrollPrinterBlockEntity>> SCROLL_PRINTER_BE=Content.BLOCK_ENTITIES.register("scroll_printer",()->BlockEntityType.Builder.of(ScrollPrinterBlockEntity::new,SCROLL_PRINTER.get()).build(null));
    public static final DeferredHolder<EntityType<?>,EntityType<LegacyReactorMigration>> LEGACY_REACTOR=Content.ENTITIES.register("arcane_reactor",()->EntityType.Builder.<LegacyReactorMigration>of(LegacyReactorMigration::new,MobCategory.MISC).sized(1,1.5f).clientTrackingRange(4).updateInterval(20).build("arcane_reactor"));
    private static DeferredHolder<Block,SpellDeviceBlock> block(String id,SpellDeviceBlock.Kind kind) {return Content.BLOCKS.register(id,()->new SpellDeviceBlock(kind,BlockBehaviour.Properties.of().strength(3,12).noOcclusion()));}
    private static DeferredHolder<Item,BlockItem> item(String id,DeferredHolder<Block,? extends Block> block) {
        return Content.ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties()) {
            @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
                super.appendHoverText(stack,context,lines,flag);lines.add(Component.translatable("tooltip.create_spellbook."+id).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        });
    }
    public static void register(IEventBus bus) {
        ARM_TYPES.register(bus);RECIPE_TYPES.register(bus);SERIALIZERS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event)->event.put(SPELL_PROBE.get(),LivingEntity.createLivingAttributes().build()));
        bus.addListener((EntityAttributeCreationEvent event)->{
            event.put(ARM_CASTER.get(),PathfinderMob.createMobAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,20).build());
        });
    }
    private DeviceContent() {}
}
