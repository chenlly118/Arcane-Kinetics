package dev.createspellbook.spell;

import dev.createspellbook.Balance;
import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.entity.MechanicalEntity;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class GearMissileSpell extends AbstractSpell {
    private final DefaultConfig config = new DefaultConfig().setMinRarity(SpellRarity.COMMON).setMaxLevel(10)
            .setCooldownSeconds(1).setSchoolResource(CreateSpellbook.id("mechanical")).setAllowCrafting(true).build();
    public GearMissileSpell() {
        baseManaCost = 8; manaCostPerLevel = 2; baseSpellPower = 12; spellPowerPerLevel = 1; castTime = 0;
    }
    @Override public ResourceLocation getSpellResource() { return CreateSpellbook.id("gear_missile"); }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getCastFinishSound() {
        return io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get().getCastFinishSound();
    }
    @Override public int getManaCost(int level) {
        return (int)((Balance.GEAR_MANA.get()+(double)Balance.GEAR_MANA_GROWTH.get()*Balance.levelSteps(level))*SpellConfigManager.getSpellConfigValue(this,SpellConfigParameter.MANA_MULTIPLIER));
    }
    @Override public float getSpellPower(int level,net.minecraft.world.entity.Entity entity) {
        return (float)((Balance.GEAR_POWER.get()+Balance.GEAR_POWER_GROWTH.get()*Balance.levelSteps(level))*getEntityPowerMultiplier(entity instanceof LivingEntity living?living:null));
    }
    @Override public java.util.List<net.minecraft.network.chat.MutableComponent> getUniqueInfo(int level,LivingEntity caster) {
        return java.util.List.of(net.minecraft.network.chat.Component.translatable("ui.irons_spellbooks.damage",String.format(java.util.Locale.ROOT,"%.1f",getSpellPower(level,caster)*.5*Balance.DAMAGE_SCALE.get())));
    }
    @Override public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (!level.isClientSide) {
            MechanicalEntity.launch(caster, this, spellLevel, 0, (float)(getSpellPower(spellLevel, caster) * 0.5 * Balance.DAMAGE_SCALE.get()), Balance.GEAR_SPEED.get()).processing(null,Balance.GEAR_RANGE.get());
        }
        super.onCast(level, spellLevel, caster, source, data);
    }
}
