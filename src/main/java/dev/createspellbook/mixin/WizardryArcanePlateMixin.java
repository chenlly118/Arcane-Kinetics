package dev.createspellbook.mixin;

import dev.createspellbook.CreateSpellbook;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

/** An alias cannot override an existing key, so share the holder before Wizardry registers its sheet. */
@Pseudo
@Mixin(targets = "net.ttzplayz.create_wizardry.item.CWItems", remap = false)
public abstract class WizardryArcanePlateMixin {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target =
            "Lnet/neoforged/neoforge/registries/DeferredRegister;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/neoforged/neoforge/registries/DeferredHolder;"))
    private static DeferredHolder<Item, Item> spellbook$shareArcanePlate(
            DeferredRegister<Item> register, String name, Supplier<Item> supplier) {
        if (name.equals("arcane_sheet"))
            return DeferredHolder.create(Registries.ITEM, CreateSpellbook.id("arcane_plate"));
        return register.register(name, supplier);
    }
}
