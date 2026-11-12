package dev.createspellbook.mixin;

import dev.createspellbook.compat.WizardryFluids;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidStack.class, remap = false)
abstract class FluidStackMixin {
    @Shadow public abstract Fluid getFluid();

    @ModifyVariable(method = "<init>(Lnet/minecraft/world/level/material/Fluid;ILnet/minecraft/core/component/PatchedDataComponentMap;)V",
            at = @At("HEAD"), argsOnly = true)
    private static Fluid createSpellbook$unifyFluid(Fluid fluid) {
        return WizardryFluids.canonical(fluid);
    }

    @ModifyVariable(method = "is(Lnet/minecraft/world/level/material/Fluid;)Z", at = @At("HEAD"), argsOnly = true)
    private Fluid createSpellbook$matchLegacyFluid(Fluid fluid) {
        return WizardryFluids.canonical(fluid);
    }

    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"), cancellable = true)
    private void createSpellbook$matchLegacyTag(TagKey<Fluid> tag, CallbackInfoReturnable<Boolean> result) {
        if (!result.getReturnValue() && WizardryFluids.matchesLegacy(getFluid(), holder -> holder.is(tag))) {
            result.setReturnValue(true);
        }
    }
}
