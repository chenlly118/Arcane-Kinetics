package dev.createspellbook.compat;

import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import dev.createspellbook.devices.DeviceContent;
import dev.createspellbook.processing.SpellAssemblyRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

/** Native Create assembly subcategory, with searchable nonconsumed scroll and machine slots. */
public final class SpellAssemblyJei extends SequencedAssemblySubCategory {
    private static final IIngredientRenderer<ItemStack> EMITTER_RENDERER=new IIngredientRenderer<>() {
        @Override public int getWidth(){return 36;}
        @Override public int getHeight(){return 30;}
        @Override public void render(GuiGraphics graphics,ItemStack stack){
            var pose=graphics.pose();pose.pushPose();
            // The supplied item model has a 0.42 GUI scale. Compensate only in this
            // recipe slot so its casing matches Create's 14-15 px/block previews.
            pose.translate(0,-6,0);pose.scale(2.25f,2.25f,1);
            graphics.renderItem(stack,0,0);
            pose.popPose();
        }
        @Override public List<Component> getTooltip(ItemStack stack,TooltipFlag flag){return List.of(stack.getHoverName());}
    };
    public SpellAssemblyJei(){super(42);}
    @Override public void setRecipe(IRecipeLayoutBuilder builder,SequencedRecipe<?> step,IFocusGroup focuses,int x){
        var spell=(SpellAssemblyRecipe)step.getRecipe();
        builder.addSlot(RecipeIngredientRole.CATALYST,x+12,14).setSlotName("spell_template_"+x).addItemStacks(SpellRecipeDisplays.scrolls(spell.requirement()));
        builder.addSlot(RecipeIngredientRole.CATALYST,x+2,30).setSlotName("arcane_spell_emitter_"+x)
                .setCustomRenderer(VanillaTypes.ITEM_STACK,EMITTER_RENDERER)
                .addItemStack(new ItemStack(DeviceContent.CASTING_ARM_ITEM.get()));
    }
    @Override public void draw(SequencedRecipe<?> step,GuiGraphics graphics,double mouseX,double mouseY,int x){
        // JEI draws the searchable scroll and the full-size machine catalyst slots.
    }
}
