package dev.createspellbook.spell;

import dev.createspellbook.CreateSpellbook;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class BrassExoskeletonEffect extends MobEffect {
    public BrassExoskeletonEffect() {
        super(MobEffectCategory.BENEFICIAL,0xcba562);
        addAttributeModifier(Attributes.ARMOR,CreateSpellbook.id("exoskeleton_armor"),2,AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,CreateSpellbook.id("exoskeleton_attack"),2,AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS,CreateSpellbook.id("exoskeleton_toughness"),1,AttributeModifier.Operation.ADD_VALUE);
    }
}
