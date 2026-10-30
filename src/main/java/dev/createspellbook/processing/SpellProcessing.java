package dev.createspellbook.processing;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import dev.createspellbook.devices.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;

/** Recipe lookup has no side effects on inventories. No cached recipe objects survive /reload. */
public final class SpellProcessing {
    public record Plan(ResourceLocation id,int inputCount,ItemStack preview,ArcaneProcessingRecipe direct,SpellAssemblyRecipe assembly){
        public ItemStack craft(Level level,ItemStack input){
            if(direct!=null)return direct.result().copy();
            // Rebind Create's native forced-result supplier to this exact current input.
            var nativeStep=SequencedAssemblyRecipe.getRecipes(level,input,DeviceContent.SPELL_APPLICATION.get(),SpellAssemblyRecipe.class,
                    holder->holder.id().equals(id)).stream().findFirst();
            if(nativeStep.isEmpty())return ItemStack.EMPTY;
            var output=nativeStep.get().value().rollResults(level.random);
            return output.size()==1?output.getFirst():ItemStack.EMPTY;
        }
    }
    public static Optional<Plan> find(Level level,ItemStack input,ResourceLocation spell,ResourceLocation school,int spellLevel){
        if(input.isEmpty())return Optional.empty();
        var sequence=SequencedAssemblyRecipe.getRecipes(level,input,DeviceContent.SPELL_APPLICATION.get(),SpellAssemblyRecipe.class,
                holder->holder.value().requirement().accepts(spell,school,spellLevel)).stream()
                .sorted(Comparator.comparing(h->h.id().toString())).findFirst();
        if(sequence.isPresent()){
            var h=sequence.get();return Optional.of(new Plan(h.id(),1,h.value().getResultItem(level.registryAccess()),null,h.value()));
        }
        // An existing native assembly must never fall through to an unrelated recipe.
        if(input.has(AllDataComponents.SEQUENCED_ASSEMBLY))return Optional.empty();
        return level.getRecipeManager().getAllRecipesFor(DeviceContent.ARCANE_PROCESSING.get()).stream()
                .filter(h->h.value().accepts(input,spell,school,spellLevel))
                .sorted(Comparator.<RecipeHolder<ArcaneProcessingRecipe>>comparingInt(h->h.value().priority()).reversed().thenComparing(h->h.id().toString()))
                .map(h->new Plan(h.id(),h.value().inputCount(),h.value().result(),h.value(),null)).findFirst();
    }
    private SpellProcessing(){}
}
