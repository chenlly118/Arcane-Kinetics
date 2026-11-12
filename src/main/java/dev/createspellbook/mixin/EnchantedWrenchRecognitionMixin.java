package dev.createspellbook.mixin;

import com.simibubi.create.AllItems;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import dev.createspellbook.item.EnchantedWrenchItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Create 6.0.9 checks its wrench registry entry directly, even before calling useOn. */
@Mixin(value = ItemProviderEntry.class, remap = false)
public abstract class EnchantedWrenchRecognitionMixin {
    @Inject(method = "isIn", at = @At("HEAD"), cancellable = true)
    private void spellbook$recognizeActiveWrench(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == AllItems.WRENCH && EnchantedWrenchItem.isWrenchMode(stack)) cir.setReturnValue(true);
    }
}
