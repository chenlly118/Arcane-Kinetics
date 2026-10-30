package dev.createspellbook.spell;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker buff: while active, the mechanical master scheduler attacks every interval. */
public final class MechanicalMasterEffect extends MobEffect {
    public MechanicalMasterEffect() { super(MobEffectCategory.BENEFICIAL,0x6398de); }
}
