package dev.createspellbook;

import dev.createspellbook.block.ArcaneCrankBlock;
import dev.createspellbook.block.ArcaneCrankBlockEntity;
import dev.createspellbook.entity.MechanicalEntity;
import dev.createspellbook.fluid.ManaFluid;
import dev.createspellbook.item.ManaBottleItem;
import dev.createspellbook.item.ManaBucketItem;
import dev.createspellbook.item.BloodBucketItem;
import dev.createspellbook.spell.GearMissileSpell;
import dev.createspellbook.spell.IndustrialSpell;
import dev.createspellbook.spell.SpellKind;
import dev.createspellbook.spell.BrassExoskeletonEffect;
import dev.createspellbook.spell.EjectionOverloadEffect;
import dev.createspellbook.spell.MechanicEffect;
import dev.createspellbook.spell.MechanicalMasterEffect;
import io.redspace.ironsspellbooks.api.attribute.MagicPercentAttribute;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.fluids.FluidType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public final class Content {
    /** One actual fluid when Wizardry is installed; old addon IDs remain readable aliases. */
    public static final boolean HAS_WIZARDRY=net.neoforged.fml.ModList.get().isLoaded("create_wizardry");
    public static final boolean USE_WIZARDRY_MANA=HAS_WIZARDRY;
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, CreateSpellbook.ID);
    public static final DeferredRegister<SchoolType> SCHOOLS = DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, CreateSpellbook.ID);
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, CreateSpellbook.ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, CreateSpellbook.ID);
    public static final DeferredRegister<FluidType> FLUID_TYPES=DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES,CreateSpellbook.ID);
    public static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(Registries.FLUID,CreateSpellbook.ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, CreateSpellbook.ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateSpellbook.ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, CreateSpellbook.ID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT,CreateSpellbook.ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,CreateSpellbook.ID);
    public static final DeferredHolder<Item,com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem> INCOMPLETE_CINDER_ESSENCE=ITEMS.register("incomplete_cinder_essence",()->new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()));
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> TAB=TABS.register("mechanical",()->CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create_spellbook")).icon(()->new ItemStack(Content.ENCHANTED_WRENCH.get()))
            .displayItems((parameters,output)->{
                output.accept(Content.ENCHANTED_WRENCH.get()); output.accept(Content.ENGINEERS_HANDBOOK.get()); output.accept(Content.ARCANE_CRANK_ITEM.get());
                output.accept(Content.ARCANE_PLATE.get());
                output.accept(Content.ARCANE_MECHANISM.get());
                output.accept(Content.MANA_BUCKET.get()); output.accept(Content.MANA_BOTTLE.get());
                output.accept(Content.BLOOD_BUCKET.get());
                output.accept(Content.DIVINE_SOUL_BUCKET.get());
                output.accept(dev.createspellbook.devices.DeviceContent.BASIN_ITEM.get());
                output.accept(dev.createspellbook.devices.DeviceContent.DEPOT_ITEM.get());
                output.accept(dev.createspellbook.devices.DeviceContent.SENSOR_ITEM.get());
                output.accept(dev.createspellbook.devices.DeviceContent.CASTING_ARM_ITEM.get());
                output.accept(dev.createspellbook.devices.DeviceContent.REACTOR_ITEM.get());
                output.accept(dev.createspellbook.devices.DeviceContent.SCROLL_PRINTER_ITEM.get());
                for(var item:dev.createspellbook.reactor.ReactorContent.ITEMS)output.accept(item.get());
                output.accept(dev.createspellbook.reactor.ReactorContent.STABILIZER.get());
                output.accept(dev.createspellbook.reactor.ReactorContent.MATTER.get());
                for(var spell:SPELLS.getEntries()) {
                    var scroll=new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
                    ISpellContainer.createScrollContainer(spell.get(),1,scroll); output.accept(scroll);
                }
            }).build());
    public static final DeferredHolder<MobEffect,BrassExoskeletonEffect> EXOSKELETON = EFFECTS.register("brass_exoskeleton",BrassExoskeletonEffect::new);
    public static final DeferredHolder<MobEffect,EjectionOverloadEffect> EJECTION_OVERLOAD_EFFECT = EFFECTS.register("ejection_overload",EjectionOverloadEffect::new);
    public static final DeferredHolder<MobEffect,MechanicEffect> MECHANIC_EFFECT = EFFECTS.register("mechanic",MechanicEffect::new);
    public static final DeferredHolder<MobEffect,MechanicalMasterEffect> MECHANICAL_MASTER_EFFECT = EFFECTS.register("mechanical_master",MechanicalMasterEffect::new);
    public static final DeferredHolder<Attribute, Attribute> MECHANICAL_POWER = ATTRIBUTES.register("mechanical_spell_power",
            () -> new MagicPercentAttribute("attribute.create_spellbook.mechanical_spell_power", 1, 0, 100).setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> MECHANICAL_RESIST = ATTRIBUTES.register("mechanical_magic_resist",
            () -> new MagicPercentAttribute("attribute.create_spellbook.mechanical_magic_resist", 1, 0, 100).setSyncable(true));
    public static final TagKey<Item> FOCUS = TagKey.create(Registries.ITEM, CreateSpellbook.id("mechanical_focus"));
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, CreateSpellbook.id("mechanical_magic"));
    public static final DeferredHolder<SchoolType, SchoolType> MECHANICAL = SCHOOLS.register("mechanical", () -> new SchoolType(
            CreateSpellbook.id("mechanical"), FOCUS, Component.translatable("school.create_spellbook.mechanical").withColor(0xcba562),
            MECHANICAL_POWER, MECHANICAL_RESIST, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME), DAMAGE));
    public static final DeferredHolder<Item, StaffItem> ENCHANTED_WRENCH = ITEMS.register("enchanted_wrench", () -> new StaffItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, dev.createspellbook.item.EngineersHandbookItem> ENGINEERS_HANDBOOK = ITEMS.register("engineers_handbook", dev.createspellbook.item.EngineersHandbookItem::new);
    public static final DeferredHolder<Item,Item> ARCANE_PLATE=HAS_WIZARDRY
            ? DeferredHolder.create(Registries.ITEM,net.minecraft.resources.ResourceLocation.parse("create_wizardry:arcane_sheet"))
            : ITEMS.register("arcane_plate",()->new Item(new Item.Properties()));
    public static final DeferredHolder<Item,BloodBucketItem> BLOOD_BUCKET=ITEMS.register("blood_bucket",
            ()->new BloodBucketItem(new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)));
    public static final DeferredHolder<Item,Item> ARCANE_MECHANISM=ITEMS.register("arcane_mechanism",()->new Item(new Item.Properties()));
    /** Pack-origin materials are registered here; their production recipes remain pack-local. */
    public static final DeferredHolder<FluidType,FluidType> DIVINE_SOUL_TYPE=FLUID_TYPES.register("liquid_divine_soul",
            ()->new FluidType(FluidType.Properties.create().descriptionId("fluid.create_spellbook.liquid_divine_soul").temperature(300).lightLevel(13)
                    .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL,SoundEvents.BUCKET_FILL)
                    .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY,SoundEvents.BUCKET_EMPTY)
                    .sound(net.neoforged.neoforge.common.SoundActions.FLUID_VAPORIZE,SoundEvents.FIRE_EXTINGUISH)));
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Source> DIVINE_SOUL=FLUIDS.register("liquid_divine_soul",
            ()->new BaseFlowingFluid.Source(divineSoulProperties()));
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Flowing> FLOWING_DIVINE_SOUL=FLUIDS.register("flowing_liquid_divine_soul",
            ()->new BaseFlowingFluid.Flowing(divineSoulProperties()));
    public static final DeferredHolder<Block,LiquidBlock> DIVINE_SOUL_BLOCK=BLOCKS.register("liquid_divine_soul",
            ()->new LiquidBlock(DIVINE_SOUL.get(),BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).lightLevel(state->13)));
    public static final DeferredHolder<Item,BucketItem> DIVINE_SOUL_BUCKET=ITEMS.register("liquid_divine_soul_bucket",
            ()->new BucketItem(DIVINE_SOUL.get(),new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    private static BaseFlowingFluid.Properties divineSoulProperties() {
        // Exact defaults read from the installed KubeJS 2101.7.2-build.368 FluidBuilder.
        return new BaseFlowingFluid.Properties(DIVINE_SOUL_TYPE,DIVINE_SOUL,FLOWING_DIVINE_SOUL)
                .bucket(DIVINE_SOUL_BUCKET).block(DIVINE_SOUL_BLOCK)
                .slopeFindDistance(4).levelDecreasePerBlock(1).explosionResistance(1).tickRate(5);
    }
    public static final DeferredHolder<FluidType,FluidType> MANA_TYPE=USE_WIZARDRY_MANA
            ? DeferredHolder.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES,
                    net.minecraft.resources.ResourceLocation.parse("create_wizardry:mana_type"))
            : FLUID_TYPES.register("mana",()->new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.create_spellbook.mana").density(1000).viscosity(1000).canHydrate(false).canConvertToSource(false)));
    public static final DeferredHolder<Fluid,Fluid> MANA=USE_WIZARDRY_MANA
            ? DeferredHolder.create(Registries.FLUID,net.minecraft.resources.ResourceLocation.parse("create_wizardry:mana"))
            : FLUIDS.register("mana",ManaFluid::new);
    public static final DeferredHolder<Item,ManaBucketItem> MANA_BUCKET=ITEMS.register("mana_bucket",()->new ManaBucketItem(new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)));
    public static final DeferredHolder<Item,ManaBottleItem> MANA_BOTTLE=ITEMS.register("mana_bottle",()->new ManaBottleItem(new Item.Properties().stacksTo(16).craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE)));
    public static final DeferredHolder<Block, ArcaneCrankBlock> ARCANE_CRANK = BLOCKS.register("arcane_crank", () -> new ArcaneCrankBlock(BlockBehaviour.Properties.of().strength(3).noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> ARCANE_CRANK_ITEM = ITEMS.register("arcane_crank", () -> new BlockItem(ARCANE_CRANK.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneCrankBlockEntity>> CRANK_BE = BLOCK_ENTITIES.register("arcane_crank",
            () -> BlockEntityType.Builder.of(ArcaneCrankBlockEntity::new, ARCANE_CRANK.get()).build(null));
    public static final DeferredHolder<EntityType<?>, EntityType<MechanicalEntity>> MECHANICAL_ENTITY = ENTITIES.register("mechanical_construct",
            () -> EntityType.Builder.<MechanicalEntity>of(MechanicalEntity::new, MobCategory.MISC).sized(0.4f, 0.4f).clientTrackingRange(10).updateInterval(1).noSave().build("mechanical_construct"));
    public static final DeferredHolder<AbstractSpell, GearMissileSpell> GEAR_MISSILE = SPELLS.register("gear_missile", GearMissileSpell::new);
    public static final DeferredHolder<AbstractSpell, IndustrialSpell> ARCANE_PRESS = spell(SpellKind.ARCANE_PRESS);
    public static final DeferredHolder<AbstractSpell, IndustrialSpell> POWER_SAW = spell(SpellKind.POWER_SAW);
    public static final DeferredHolder<AbstractSpell, IndustrialSpell> STRESS_DRIVE = spell(SpellKind.STRESS_DRIVE);
    public static final DeferredHolder<AbstractSpell, IndustrialSpell> BLAZE_INFUSION = spell(SpellKind.BLAZE_INFUSION);
    public static final DeferredHolder<AbstractSpell, IndustrialSpell> FAN_SMELTING = spell(SpellKind.FAN_SMELTING);
    public static final DeferredHolder<AbstractSpell, IndustrialSpell> CRUSHING_WHEEL = spell(SpellKind.CRUSHING_WHEEL);
    public static final DeferredHolder<AbstractSpell, IndustrialSpell> PISTON_LAUNCH = spell(SpellKind.PISTON_LAUNCH);
    public static final DeferredHolder<AbstractSpell, IndustrialSpell> ARCANE_DRILL = spell(SpellKind.ARCANE_DRILL);
    public static final DeferredHolder<AbstractSpell, IndustrialSpell> BRASS_EXOSKELETON = spell(SpellKind.BRASS_EXOSKELETON);
    public static final DeferredHolder<AbstractSpell, IndustrialSpell> PRESS_BARRAGE = spell(SpellKind.PRESS_BARRAGE);
    public static final DeferredHolder<AbstractSpell, IndustrialSpell> EJECTION_OVERLOAD = spell(SpellKind.EJECTION_OVERLOAD);
    public static final DeferredHolder<AbstractSpell, IndustrialSpell> MECHANIC = spell(SpellKind.MECHANIC);
    public static final DeferredHolder<AbstractSpell, IndustrialSpell> MECHANICAL_MASTER = spell(SpellKind.MECHANICAL_MASTER);
    private static DeferredHolder<AbstractSpell, IndustrialSpell> spell(SpellKind kind) { return SPELLS.register(kind.id,()->new IndustrialSpell(kind)); }

    public static void register(IEventBus bus) {
        // Resolve existing items, tanks, recipes, tags and world block states to
        // the migrated materials. KubeJS no longer registers a second copy.
        FLUIDS.addAlias(net.minecraft.resources.ResourceLocation.parse("kubejs:liquid_divine_soul"),CreateSpellbook.id("liquid_divine_soul"));
        FLUIDS.addAlias(net.minecraft.resources.ResourceLocation.parse("kubejs:flowing_liquid_divine_soul"),CreateSpellbook.id("flowing_liquid_divine_soul"));
        FLUID_TYPES.addAlias(net.minecraft.resources.ResourceLocation.parse("kubejs:liquid_divine_soul"),CreateSpellbook.id("liquid_divine_soul"));
        BLOCKS.addAlias(net.minecraft.resources.ResourceLocation.parse("kubejs:liquid_divine_soul"),CreateSpellbook.id("liquid_divine_soul"));
        ITEMS.addAlias(net.minecraft.resources.ResourceLocation.parse("kubejs:liquid_divine_soul_bucket"),CreateSpellbook.id("liquid_divine_soul_bucket"));
        ITEMS.addAlias(net.minecraft.resources.ResourceLocation.parse("kubejs:arcane_mechanism"),CreateSpellbook.id("arcane_mechanism"));
        if(USE_WIZARDRY_MANA) {
            // NeoForge's registry alias also resolves old saved FluidStacks and
            // recipe holders to the canonical object, without scanning or draining tanks.
            FLUIDS.addAlias(CreateSpellbook.id("mana"),net.minecraft.resources.ResourceLocation.parse("create_wizardry:mana"));
            FLUID_TYPES.addAlias(CreateSpellbook.id("mana"),net.minecraft.resources.ResourceLocation.parse("create_wizardry:mana_type"));
            // Keep existing Wizardry factories using their canonical sheet. Its
            // appearance is supplied by our authored runtime parent model.
            ITEMS.addAlias(CreateSpellbook.id("arcane_plate"),net.minecraft.resources.ResourceLocation.parse("create_wizardry:arcane_sheet"));
        } else {
            // Uninstalling the optional addon must not strand its saved mana,
            // bucket or sheet IDs. Aliases preserve counts and normalize on save.
            FLUIDS.addAlias(net.minecraft.resources.ResourceLocation.parse("create_wizardry:mana"),CreateSpellbook.id("mana"));
            FLUIDS.addAlias(net.minecraft.resources.ResourceLocation.parse("create_wizardry:mana_flowing"),CreateSpellbook.id("mana"));
            FLUID_TYPES.addAlias(net.minecraft.resources.ResourceLocation.parse("create_wizardry:mana_type"),CreateSpellbook.id("mana"));
            ITEMS.addAlias(net.minecraft.resources.ResourceLocation.parse("create_wizardry:mana_bucket"),CreateSpellbook.id("mana_bucket"));
            ITEMS.addAlias(net.minecraft.resources.ResourceLocation.parse("create_wizardry:arcane_sheet"),CreateSpellbook.id("arcane_plate"));
        }
        ATTRIBUTES.register(bus); SCHOOLS.register(bus); SPELLS.register(bus);
        FLUID_TYPES.register(bus); FLUIDS.register(bus); ITEMS.register(bus); BLOCKS.register(bus); BLOCK_ENTITIES.register(bus); ENTITIES.register(bus); EFFECTS.register(bus); TABS.register(bus);
    }
    private Content() {}
}
