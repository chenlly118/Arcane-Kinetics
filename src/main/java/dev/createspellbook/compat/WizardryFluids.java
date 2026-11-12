package dev.createspellbook.compat;

import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import dev.createspellbook.Content;
import io.redspace.ironsspellbooks.registries.FluidRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class WizardryFluids {
    public static final ResourceLocation BLOOD = id("blood");
    public static final ResourceLocation FLOWING_BLOOD = id("blood_flowing");
    private static volatile Map<Fluid, Fluid> replacements = Map.of();

    private WizardryFluids() {}

    public static void init() {
        if (!Content.HAS_WIZARDRY) return;
        var legacyBlood = FluidRegistry.BLOOD.get();
        var blood = BuiltInRegistries.FLUID.getOptional(BLOOD).orElse(legacyBlood);
        var aliases = new HashMap<Fluid, Fluid>();
        if (blood != legacyBlood) aliases.put(legacyBlood, blood);
        BuiltInRegistries.FLUID.getOptional(FLOWING_BLOOD).ifPresent(fluid -> aliases.put(fluid, blood));
        BuiltInRegistries.FLUID.getOptional(id("mana_flowing")).ifPresent(fluid -> aliases.put(fluid, Content.MANA.get()));
        replacements = Map.copyOf(aliases);

        // Wizardry registers the pipe effect against Iron's virtual blood.
        var effects = OpenPipeEffectHandler.REGISTRY;
        var effect = effects.get(legacyBlood);
        if (effect != null && effects.get(blood) == null) effects.register(blood, effect);
    }

    public static Fluid canonical(Fluid fluid) {
        return fluid == null ? null : replacements.getOrDefault(fluid, fluid);
    }

    public static Holder<Fluid> canonical(Holder<Fluid> holder) {
        if (replacements.isEmpty()) return holder;
        var fluid = holder.value();
        var canonical = canonical(fluid);
        return fluid == canonical ? holder : canonical.builtInRegistryHolder();
    }

    public static boolean matchesLegacy(Fluid fluid, Predicate<Holder<Fluid>> matcher) {
        for (var entry : replacements.entrySet()) {
            if (entry.getValue() == fluid && matcher.test(entry.getKey().builtInRegistryHolder())) return true;
        }
        return false;
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath("create_wizardry", name);
    }
}
