package dev.createspellbook.mixin;

import dev.createspellbook.compat.WizardryFluids;
import net.minecraft.core.Holder;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.crafting.SingleFluidIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = SingleFluidIngredient.class, remap = false)
abstract class SingleFluidIngredientMixin {
    @ModifyVariable(method = "<init>(Lnet/minecraft/core/Holder;)V", at = @At("HEAD"), argsOnly = true)
    private static Holder<Fluid> createSpellbook$unifyIngredient(Holder<Fluid> fluid) {
        return WizardryFluids.canonical(fluid);
    }
}
