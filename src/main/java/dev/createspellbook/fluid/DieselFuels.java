package dev.createspellbook.fluid;

import dev.createspellbook.Content;
import dev.createspellbook.CreateSpellbook;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class DieselFuels {
    public static final String MOD_ID = "createdieselgenerators";
    public static final List<Fuel> FUELS = ModList.get().isLoaded(MOD_ID)
            ? List.of(new Fuel("arcane_fuel", 1000, 8, false), new Fuel("refined_arcane_fuel", 1500, 12, true))
            : List.of();

    private DieselFuels() {}

    public static void registerLegacyAliases() {
        for (var fuel : FUELS) {
            String name = fuel.name;
            Content.FLUIDS.addAlias(oldId(name), CreateSpellbook.id(name));
            Content.FLUIDS.addAlias(oldId("flowing_" + name), CreateSpellbook.id("flowing_" + name));
            Content.FLUID_TYPES.addAlias(oldId(name), CreateSpellbook.id(name));
            Content.BLOCKS.addAlias(oldId(name), CreateSpellbook.id(name));
            Content.ITEMS.addAlias(oldId(name + "_bucket"), CreateSpellbook.id(name + "_bucket"));
        }
    }

    private static ResourceLocation oldId(String name) {
        return ResourceLocation.fromNamespaceAndPath("kubejs", name);
    }

    public static final class Fuel {
        private final String name;
        private final boolean translucent;
        private final DeferredHolder<FluidType, FluidType> type;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        private final DeferredHolder<Block, LiquidBlock> block;
        private final DeferredHolder<Item, BucketItem> bucket;

        private Fuel(String name, int temperature, int light, boolean translucent) {
            this.name = name;
            this.translucent = translucent;
            type = Content.FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.create_spellbook." + name).temperature(temperature).lightLevel(light)
                    .canConvertToSource(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
            source = Content.FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties()));
            flowing = Content.FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties()));
            block = Content.BLOCKS.register(name, () -> new LiquidBlock(source.get(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).lightLevel(state -> light)));
            bucket = Content.ITEMS.register(name + "_bucket", () -> new BucketItem(source.get(),
                    new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
        }

        private BaseFlowingFluid.Properties properties() {
            return new BaseFlowingFluid.Properties(type, source, flowing).block(block).bucket(bucket)
                    .slopeFindDistance(4).levelDecreasePerBlock(1).explosionResistance(1).tickRate(5);
        }

        public FluidType type() { return type.get(); }
        public Fluid source() { return source.get(); }
        public Fluid flowing() { return flowing.get(); }
        public BucketItem bucket() { return bucket.get(); }
        public boolean translucent() { return translucent; }
        public ResourceLocation stillTexture() { return CreateSpellbook.id("fluid/" + name + "_still"); }
        public ResourceLocation flowingTexture() { return CreateSpellbook.id("fluid/" + name + "_flow"); }
    }
}
