package dev.createspellbook.compat;

import dev.createspellbook.Content;
import dev.createspellbook.CreateSpellbook;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.jei.ScrollForgeRecipe;
import io.redspace.ironsspellbooks.jei.ScrollForgeRecipeCategory;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.*;

/** Optional JEI integration: fill absent native scribing entries after config sync. */
@JeiPlugin
public final class MechanicalScribingJei implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return CreateSpellbook.id("mechanical_scribing"); }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        var type=ScrollForgeRecipeCategory.SCROLL_FORGE_RECIPE_RECIPE_TYPE;
        var manager=runtime.getRecipeManager();
        var registered=keys(manager.createRecipeLookup(type).includeHidden().get().toList());
        List<ScrollForgeRecipe> additions=new ArrayList<>();
        for(var recipe:expectedRecipes()) {
            List<ItemStack> inks=new ArrayList<>(),outputs=new ArrayList<>();
            for(int i=0;i<recipe.scrollOutputs().size();i++) {
                if(!registered.contains(key(recipe.inkInputs().get(i),recipe.scrollOutputs().get(i)))) {
                    inks.add(recipe.inkInputs().get(i));outputs.add(recipe.scrollOutputs().get(i));
                }
            }
            if(!outputs.isEmpty()) additions.add(new ScrollForgeRecipe(inks,recipe.paperInput(),recipe.focusInput(),outputs));
        }
        if(!additions.isEmpty()) manager.addRecipes(type,additions);
    }
    /** Only genuine native table outputs: the minimum level for each supported ink. */
    public static List<ScrollForgeRecipe> expectedRecipes() {
        List<ScrollForgeRecipe> recipes=new ArrayList<>();
        var inks=BuiltInRegistries.ITEM.stream().filter(i->i instanceof InkItem).map(i->(InkItem)i)
                .sorted(Comparator.comparingInt(i->i.getRarity().getValue())).toList();
        for(var entry:Content.SPELLS.getEntries()) {
            var spell=entry.get();
            if(!spell.isEnabled() || !spell.allowCrafting()) continue;
            List<ItemStack> inputs=new ArrayList<>(),outputs=new ArrayList<>();
            for(var ink:inks) {
                int level=spell.getMinLevelForRarity(ink.getRarity());
                if(level<1 || level>spell.getMaxLevel()) continue;
                var scroll=new ItemStack(ItemRegistry.SCROLL.get());
                ISpellContainer.createScrollContainer(spell,level,scroll);
                inputs.add(new ItemStack(ink));outputs.add(scroll);
            }
            if(!outputs.isEmpty()) recipes.add(new ScrollForgeRecipe(inputs,Ingredient.of(Items.PAPER),
                    Ingredient.of(Content.FOCUS),outputs));
        }
        return recipes;
    }
    public static Set<String> keys(List<ScrollForgeRecipe> recipes) {
        Set<String> keys=new HashSet<>();
        for(var recipe:recipes) for(int i=0;i<Math.min(recipe.scrollOutputs().size(),recipe.inkInputs().size());i++)
            keys.add(key(recipe.inkInputs().get(i),recipe.scrollOutputs().get(i)));
        return keys;
    }
    private static String key(ItemStack ink,ItemStack scroll) {
        if(!ISpellContainer.isSpellContainer(scroll) || ISpellContainer.get(scroll).isEmpty()) return "";
        var data=ISpellContainer.get(scroll).getSpellAtIndex(0);
        return BuiltInRegistries.ITEM.getKey(ink.getItem())+"|"+data.getSpell().getSpellId()+"|"+data.getLevel();
    }
}
