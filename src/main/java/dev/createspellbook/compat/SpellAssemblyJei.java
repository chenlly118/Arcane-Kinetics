package dev.createspellbook.compat;

import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import dev.createspellbook.devices.DeviceContent;
import dev.createspellbook.processing.SpellAssemblyRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** Native Create assembly subcategory, with searchable nonconsumed scroll and machine slots. */
public final class SpellAssemblyJei extends SequencedAssemblySubCategory {
    public SpellAssemblyJei(){super(42);}
    @Override public void setRecipe(IRecipeLayoutBuilder builder,SequencedRecipe<?> step,IFocusGroup focuses,int x){
        var spell=(SpellAssemblyRecipe)step.getRecipe();
        builder.addSlot(RecipeIngredientRole.CATALYST,x+12,14).setSlotName("spell_template_"+x).addItemStacks(SpellRecipeDisplays.scrolls(spell.requirement()));
        builder.addSlot(RecipeIngredientRole.CATALYST,x+12,49).setSlotName("arcane_spell_emitter_"+x).addItemStack(new ItemStack(DeviceContent.CASTING_ARM_ITEM.get()));
    }
    @Override public void draw(SequencedRecipe<?> step,GuiGraphics graphics,double mouseX,double mouseY,int x){
        graphics.fill(x+18,33,x+22,44,0xffab8ae0);graphics.fill(x+15,40,x+25,42,0xffc8acf4);
    }
}
