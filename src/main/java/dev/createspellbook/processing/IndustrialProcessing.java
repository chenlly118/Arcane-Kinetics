package dev.createspellbook.processing;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import java.util.*;

public final class IndustrialProcessing {
    public enum Mode { PRESS, SAW, CRUSH, BLAST }
    private static final String PRODUCT_TAG="CreateSpellbookProcessed";

    public static int around(ServerLevel level, LivingEntity caster, Vec3 center,double radius,Mode mode,ProcessingBudget budget) {
        if (budget==null || budget.remaining<=0) return 0;
        int processed=0;
        for(var entity: level.getEntitiesOfClass(ItemEntity.class,new AABB(center,center).inflate(radius))) {
            if (budget.remaining<=0) break;
            if (entity.getPersistentData().hasUUID(PRODUCT_TAG) && entity.getPersistentData().getUUID(PRODUCT_TAG).equals(budget.token)) continue;
            var visible=level.clip(new net.minecraft.world.level.ClipContext(center,entity.position(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,caster));
            if (visible.getType()!=net.minecraft.world.phys.HitResult.Type.MISS && visible.getLocation().distanceToSqr(entity.position())>.5) continue;
            if (caster instanceof Player player && (!player.mayBuild() || !level.mayInteract(player,entity.blockPosition()))) continue;
            processed+=process(level, entity, mode, budget);
        }
        return processed;
    }

    /** Plan all outputs, verify the input snapshot, spawn outputs, then commit consumption once. */
    public static int process(ServerLevel level, ItemEntity input,Mode mode,ProcessingBudget budget) {
        if (!input.isAlive() || budget.remaining<=0) return 0;
        ItemStack snapshot=input.getItem().copy();
        int amount=Math.min(snapshot.getCount(), budget.remaining);
        List<ItemStack> products=new ArrayList<>();
        SingleRecipeInput recipeInput=new SingleRecipeInput(snapshot.copyWithCount(1));
        if(mode==Mode.BLAST) {
            var recipe=blastRecipe(level,recipeInput);
            if(recipe==null) return 0;
            ItemStack result=recipe.assemble(recipeInput,level.registryAccess());
            if(result.isEmpty()) return 0;
            for(int n=0;n<amount;n++) products.add(result.copy());
        } else {
            AllRecipeTypes type=switch(mode) { case PRESS->AllRecipeTypes.PRESSING; case SAW->AllRecipeTypes.CUTTING; case CRUSH->AllRecipeTypes.CRUSHING; default->throw new IllegalStateException(); };
            // CuttingRecipe in Create 6.0.9 accepts RecipeWrapper, unlike pressing/crushing.
            // AllRecipeTypes.find is generic but does not enforce each enum entry's input type.
            Optional<? extends RecipeHolder<?>> optional;
            if(mode==Mode.SAW) {
                var handler=new ItemStackHandler(1);
                handler.setStackInSlot(0,snapshot.copyWithCount(1));
                optional=AllRecipeTypes.CUTTING.find(new RecipeWrapper(handler),level).filter(AllRecipeTypes.CAN_BE_AUTOMATED);
            } else optional=type.find(recipeInput,level).filter(AllRecipeTypes.CAN_BE_AUTOMATED);
            if(optional.isEmpty() && mode==Mode.CRUSH) optional=AllRecipeTypes.MILLING.find(recipeInput,level).filter(AllRecipeTypes.CAN_BE_AUTOMATED);
            if(optional.isEmpty()) return 0;
            if(!(optional.get().value() instanceof ProcessingRecipe<?,?> recipe)) return 0;
            if(recipe.getIngredients().size()!=1 || !recipe.getFluidIngredients().isEmpty() || !recipe.getFluidResults().isEmpty()) return 0;
            for(int n=0;n<amount;n++) products.addAll(recipe.rollResults(level.random));
        }
        ItemStack single=snapshot.copyWithCount(1);
        if(single.hasCraftingRemainingItem()) for(int n=0;n<amount;n++) products.add(single.getCraftingRemainingItem());
        if(!ItemStack.matches(snapshot,input.getItem())) return 0;
        List<ItemEntity> spawned=new ArrayList<>();
        for(ItemStack product: products) {
            if(product.isEmpty()) continue;
            int left=product.getCount();
            while(left>0) {
                int count=Math.min(left,product.getMaxStackSize());
                ItemEntity output=new ItemEntity(level,input.getX(),input.getY(),input.getZ(),product.copyWithCount(count));
                output.setDeltaMovement(input.getDeltaMovement()); output.setDefaultPickUpDelay();
                output.getPersistentData().putUUID(PRODUCT_TAG,budget.token);
                if(!level.addFreshEntity(output)) { spawned.forEach(ItemEntity::discard); return 0; }
                spawned.add(output); left-=count;
            }
        }
        if(!input.isAlive() || !ItemStack.matches(snapshot,input.getItem())) { spawned.forEach(ItemEntity::discard); return 0; }
        ItemStack remainder=snapshot.copy(); remainder.shrink(amount);
        if(remainder.isEmpty()) input.discard(); else input.setItem(remainder);
        budget.remaining-=amount;
        if(mode==Mode.SAW) {
            var sound=(snapshot.is(net.minecraft.tags.ItemTags.LOGS) || snapshot.is(net.minecraft.tags.ItemTags.PLANKS))
                    ? com.simibubi.create.AllSoundEvents.SAW_ACTIVATE_WOOD : com.simibubi.create.AllSoundEvents.SAW_ACTIVATE_STONE;
            sound.playOnServer(level,input.blockPosition(),.8f,1f);
        }
        return amount;
    }

    private static AbstractCookingRecipe blastRecipe(ServerLevel level, SingleRecipeInput input) {
        Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> selected=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level).filter(AllRecipeTypes.CAN_BE_AUTOMATED);
        if(selected.isEmpty()) selected=level.getRecipeManager().getRecipeFor(RecipeType.BLASTING,input,level).filter(AllRecipeTypes.CAN_BE_AUTOMATED);
        if(selected.isEmpty()) return null;
        var smoking=level.getRecipeManager().getRecipeFor(RecipeType.SMOKING,input,level).filter(AllRecipeTypes.CAN_BE_AUTOMATED);
        if(smoking.isPresent() && ItemStack.isSameItem(smoking.get().value().getResultItem(level.registryAccess()),selected.get().value().getResultItem(level.registryAccess()))) return null;
        return selected.get().value();
    }
    private IndustrialProcessing() {}
}
