package dev.createspellbook.spell;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marks a target whose next valid fall damage is multiplied once. */
public final class EjectionOverloadEffect extends MobEffect {
    public EjectionOverloadEffect() { super(MobEffectCategory.HARMFUL,0x7fb7e8); }
}
