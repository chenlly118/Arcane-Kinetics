package dev.createspellbook.processing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.*;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import dev.createspellbook.*;
import dev.createspellbook.devices.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import java.util.*;
import java.util.function.Supplier;

/** A genuine Create ProcessingRecipe: Create owns the assembly progress and final result. */
public final class SpellAssemblyRecipe extends ProcessingRecipe<SingleRecipeInput,SpellAssemblyRecipe.Params> implements IAssemblyRecipe {
    private static final IRecipeTypeInfo INFO=new IRecipeTypeInfo(){
        @Override public ResourceLocation getId(){return CreateSpellbook.id("spell_application");}
        @SuppressWarnings("unchecked") @Override public <T extends RecipeSerializer<?>> T getSerializer(){return (T)DeviceContent.SPELL_APPLICATION_SERIALIZER.get();}
        @SuppressWarnings("unchecked") @Override public <I extends RecipeInput,R extends Recipe<I>> RecipeType<R> getType(){return (RecipeType<R>)DeviceContent.SPELL_APPLICATION.get();}
    };
    public SpellAssemblyRecipe(Params params){super(INFO,params);}
    public SpellRequirement requirement(){return params.requirement;}
    @Override protected int getMaxInputCount(){return 1;}
    @Override protected int getMaxOutputCount(){return 1;}
    @Override public boolean matches(SingleRecipeInput input,Level level){return !input.item().isEmpty() && !ingredients.isEmpty() && ingredients.getFirst().test(input.item());}
    @Override public Component getDescriptionForAssembly(){return Component.translatable("recipe.create_spellbook.spell_application");}
    @Override public void addRequiredMachines(Set<ItemLike> machines){machines.add(DeviceContent.CASTING_ARM.get());}
    // The scroll is a nonconsumed template, displayed as a CATALYST by the JEI subcategory.
    @Override public void addAssemblyIngredients(List<Ingredient> ingredients){}
    @Override public Supplier<Supplier<com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory>> getJEISubCategory(){return ()->dev.createspellbook.compat.SpellAssemblyJei::new;}
    public static final class Params extends ProcessingRecipeParams {
        private SpellRequirement requirement;
        public static final MapCodec<Params> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
                ProcessingRecipeParams.codec(Params::new).forGetter(p->p),
                SpellRequirement.CODEC.forGetter(p->p.requirement)
        ).apply(i,(params,requirement)->{params.requirement=requirement;return params;}));
        public static final StreamCodec<RegistryFriendlyByteBuf,Params> STREAM_CODEC=new StreamCodec<>(){
            private final StreamCodec<RegistryFriendlyByteBuf,Params> base=ProcessingRecipeParams.streamCodec(Params::new);
            @Override public Params decode(RegistryFriendlyByteBuf b){var p=base.decode(b);p.requirement=SpellRequirement.STREAM_CODEC.decode(b);return p;}
            @Override public void encode(RegistryFriendlyByteBuf b,Params p){base.encode(b,p);SpellRequirement.STREAM_CODEC.encode(b,p.requirement);}
        };
    }
    public static final class Serializer implements RecipeSerializer<SpellAssemblyRecipe>{
        @Override public MapCodec<SpellAssemblyRecipe> codec(){return ProcessingRecipe.codec(SpellAssemblyRecipe::new,Params.CODEC);}
        @Override public StreamCodec<RegistryFriendlyByteBuf,SpellAssemblyRecipe> streamCodec(){return ProcessingRecipe.streamCodec(SpellAssemblyRecipe::new,Params.STREAM_CODEC);}
    }
}
