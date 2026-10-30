package dev.createspellbook.devices;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.Optional;

public record ArcaneProcessingRecipe(Ingredient ingredient,int inputCount,SpellRequirement requirement,ItemStack result,int priority) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<ArcaneProcessingRecipe> CODEC=RecordCodecBuilder.<ArcaneProcessingRecipe>mapCodec(instance->instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ArcaneProcessingRecipe::ingredient),
            Codec.intRange(1,64).optionalFieldOf("input_count",1).forGetter(ArcaneProcessingRecipe::inputCount),
            SpellRequirement.CODEC.forGetter(ArcaneProcessingRecipe::requirement),
            ItemStack.CODEC.fieldOf("result").forGetter(ArcaneProcessingRecipe::result),
            Codec.INT.optionalFieldOf("priority",0).forGetter(ArcaneProcessingRecipe::priority)
    ).apply(instance,ArcaneProcessingRecipe::new)).validate(recipe->recipe.result().isEmpty() || recipe.result().getCount()>recipe.result().getMaxStackSize()
            ?DataResult.error(()->"Result must fit in one output stack"):DataResult.success(recipe));
    public ArcaneProcessingRecipe(Ingredient ingredient,int count,Optional<ResourceLocation> spell,Optional<ResourceLocation> school,int minLevel,ItemStack result){
        this(ingredient,count,new SpellRequirement(spell,school,minLevel,Optional.empty(),false),result,0);
    }
    public Optional<ResourceLocation> spell(){return requirement.spell();}
    public Optional<ResourceLocation> school(){return requirement.school();}
    public int minLevel(){return requirement.minLevel();}
    public boolean accepts(ItemStack input,SpellInteraction hit){return accepts(input,hit.spellId(),hit.schoolId(),hit.level());}
    public boolean accepts(ItemStack input,ResourceLocation spellId,ResourceLocation schoolId,int level){
        return input.getCount()>=inputCount && ingredient.test(input) && requirement.accepts(spellId,schoolId,level);
    }
    @Override public boolean matches(SingleRecipeInput input,Level level) {return input.item().getCount()>=inputCount && ingredient.test(input.item());}
    @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider lookup) {return result.copy();}
    @Override public boolean canCraftInDimensions(int width,int height) {return width>=1 && height>=1;}
    @Override public ItemStack getResultItem(HolderLookup.Provider lookup) {return result;}
    @Override public NonNullList<Ingredient> getIngredients() {return NonNullList.of(Ingredient.EMPTY,ingredient);}
    @Override public RecipeSerializer<?> getSerializer() {return DeviceContent.ARCANE_SERIALIZER.get();}
    @Override public RecipeType<?> getType() {return DeviceContent.ARCANE_PROCESSING.get();}
    @Override public boolean isSpecial() {return true;}
    @Override public ItemStack getToastSymbol() {return com.simibubi.create.AllBlocks.DEPOT.asStack();}

    public static final class Serializer implements RecipeSerializer<ArcaneProcessingRecipe> {
        @Override public MapCodec<ArcaneProcessingRecipe> codec() {return CODEC;}
        @Override public StreamCodec<RegistryFriendlyByteBuf,ArcaneProcessingRecipe> streamCodec() {
            return new StreamCodec<>() {
                @Override public ArcaneProcessingRecipe decode(RegistryFriendlyByteBuf buffer) {
                    var ingredient=Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);int count=buffer.readVarInt();
                    var requirement=SpellRequirement.STREAM_CODEC.decode(buffer);var result=ItemStack.STREAM_CODEC.decode(buffer);
                    return new ArcaneProcessingRecipe(ingredient,count,requirement,result,buffer.readInt());
                }
                @Override public void encode(RegistryFriendlyByteBuf buffer,ArcaneProcessingRecipe recipe) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer,recipe.ingredient());buffer.writeVarInt(recipe.inputCount());
                    SpellRequirement.STREAM_CODEC.encode(buffer,recipe.requirement());ItemStack.STREAM_CODEC.encode(buffer,recipe.result());buffer.writeInt(recipe.priority());
                }
            };
        }
    }
}
