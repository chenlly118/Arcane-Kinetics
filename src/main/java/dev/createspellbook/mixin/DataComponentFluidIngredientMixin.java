package dev.createspellbook.mixin;

import dev.createspellbook.compat.WizardryFluids;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DataComponentFluidIngredient.class, remap = false)
abstract class DataComponentFluidIngredientMixin {
    @Redirect(method = "test(Lnet/neoforged/neoforge/fluids/FluidStack;)Z", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/HolderSet;contains(Lnet/minecraft/core/Holder;)Z"))
    private boolean createSpellbook$matchLegacyFluid(HolderSet<Fluid> fluids, Holder<Fluid> fluid) {
        return fluids.contains(fluid) || WizardryFluids.matchesLegacy(fluid.value(), fluids::contains);
    }
}
