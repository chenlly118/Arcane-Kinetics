package dev.createspellbook.spell;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker buff: the mechanic arm follows every successful direct melee hit. */
public final class MechanicEffect extends MobEffect {
    public MechanicEffect() { super(MobEffectCategory.BENEFICIAL,0xcba562); }
}
