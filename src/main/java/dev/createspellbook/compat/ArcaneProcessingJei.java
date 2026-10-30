package dev.createspellbook.compat;

import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.devices.*;
import io.redspace.ironsspellbooks.api.registry.*;
import mezz.jei.api.*;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Optional integration for the JEI version actually installed in the target instance. */
@JeiPlugin
public final class ArcaneProcessingJei implements IModPlugin {
    public static final mezz.jei.api.recipe.RecipeType<ArcaneProcessingRecipe> TYPE=mezz.jei.api.recipe.RecipeType.create(CreateSpellbook.ID,"arcane_processing",ArcaneProcessingRecipe.class);
    public static IJeiRuntime runtime;
    @Override public ResourceLocation getPluginUid() {return CreateSpellbook.id("arcane_processing");}
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));}
    @Override public void registerRecipes(IRecipeRegistration registration) {registration.addRecipes(TYPE,recipes());}
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {registration.addRecipeCatalyst(new ItemStack(DeviceContent.DEPOT_ITEM.get()),TYPE);registration.addRecipeCatalyst(com.simibubi.create.AllBlocks.DEPOT.asStack(),TYPE);registration.addRecipeCatalyst(new ItemStack(DeviceContent.CASTING_ARM_ITEM.get()),TYPE);registration.addRecipeCatalyst(com.simibubi.create.AllItems.BELT_CONNECTOR.asStack(),TYPE);}
    @Override public void onRuntimeAvailable(IJeiRuntime available) {
        runtime=available;var manager=available.getRecipeManager();var current=manager.createRecipeLookup(TYPE).includeHidden().get().toList();
        var missing=recipes().stream().filter(recipe->!current.contains(recipe)).toList();if(!missing.isEmpty())manager.addRecipes(TYPE,missing);
    }
    @Override public void onRuntimeUnavailable() {runtime=null;}
    private static List<ArcaneProcessingRecipe> recipes() {
        var level=Minecraft.getInstance().level;
        return level==null?List.of():level.getRecipeManager().getAllRecipesFor(DeviceContent.ARCANE_PROCESSING.get()).stream().map(holder->holder.value()).toList();
    }
    public static Component requirement(ArcaneProcessingRecipe recipe){return SpellRecipeDisplays.title(recipe.requirement());}
    /** Called after a real client recipe sync, including datapack reload. */
    public static void refreshRecipes(){
        if(runtime==null || Minecraft.getInstance().level==null)return;
        var manager=runtime.getRecipeManager();var old=manager.createRecipeLookup(TYPE).includeHidden().get().toList();
        manager.hideRecipes(TYPE,old);var current=recipes();manager.addRecipes(TYPE,current);manager.unhideRecipes(TYPE,current);
    }
    private static final class Category implements IRecipeCategory<ArcaneProcessingRecipe> {
        private final IDrawable background,icon,arrow;
        Category(IGuiHelper gui) {background=gui.createBlankDrawable(180,80);icon=gui.createDrawableItemStack(new ItemStack(DeviceContent.DEPOT_ITEM.get()));arrow=gui.getRecipeArrow();}
        @Override public mezz.jei.api.recipe.RecipeType<ArcaneProcessingRecipe> getRecipeType() {return TYPE;}
        @Override public Component getTitle() {return Component.translatable("jei.create_spellbook.arcane_processing");}
        @Override public IDrawable getBackground() {return background;}
        @Override public IDrawable getIcon() {return icon;}
        @Override public void setRecipe(IRecipeLayoutBuilder builder,ArcaneProcessingRecipe recipe,IFocusGroup focuses) {
            var inputs=Arrays.stream(recipe.ingredient().getItems()).map(stack->stack.copyWithCount(recipe.inputCount())).toList();
            builder.addSlot(RecipeIngredientRole.INPUT,10,30).addItemStacks(inputs);
            builder.addSlot(RecipeIngredientRole.OUTPUT,148,30).addItemStack(recipe.result());
            builder.addSlot(RecipeIngredientRole.CATALYST,73,16).setSlotName("spell_template").addItemStacks(SpellRecipeDisplays.scrolls(recipe.requirement()));
            builder.addSlot(RecipeIngredientRole.CATALYST,73,44).setSlotName("arcane_spell_emitter").addItemStack(new ItemStack(DeviceContent.CASTING_ARM_ITEM.get()));
        }
        @Override public void draw(ArcaneProcessingRecipe recipe,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
            arrow.draw(graphics,112,30);var font=Minecraft.getInstance().font;
            graphics.drawString(font,requirement(recipe),4,3,0xff303030,false);
            graphics.drawString(font,SpellRecipeDisplays.condition(recipe.requirement()),4,68,0xff505050,false);
        }
    }
}
