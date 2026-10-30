package dev.createspellbook.spell;

import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;

public enum SpellKind {
    ARCANE_PRESS("arcane_press",10,SpellRarity.COMMON,CastType.LONG,20,18,3,8,1,5,8),
    POWER_SAW("power_saw",6,SpellRarity.RARE,CastType.INSTANT,0,30,5,5,1,12,24),
    STRESS_DRIVE("stress_drive",10,SpellRarity.COMMON,CastType.CONTINUOUS,600,3,1,1,0,5,12),
    BLAZE_INFUSION("blaze_infusion",8,SpellRarity.RARE,CastType.INSTANT,0,20,3,7,1,8,10),
    FAN_SMELTING("fan_smelting",10,SpellRarity.COMMON,CastType.CONTINUOUS,100,5,1,2,1,12,8),
    CRUSHING_WHEEL("crushing_wheel",6,SpellRarity.RARE,CastType.LONG,40,60,8,7,1,20,20),
    PISTON_LAUNCH("piston_launch",8,SpellRarity.UNCOMMON,CastType.INSTANT,0,20,2,1,0,10,8),
    ARCANE_DRILL("arcane_drill",8,SpellRarity.UNCOMMON,CastType.LONG,20,25,4,9,1,10,8),
    BRASS_EXOSKELETON("brass_exoskeleton",3,SpellRarity.EPIC,CastType.INSTANT,0,300,50,1,0,330,1),
    PRESS_BARRAGE("press_barrage",6,SpellRarity.RARE,CastType.LONG,30,65,10,10,2,25,14),
    EJECTION_OVERLOAD("ejection_overload",6,SpellRarity.UNCOMMON,CastType.INSTANT,0,40,8,1,0,16,16),
    MECHANIC("mechanic",3,SpellRarity.EPIC,CastType.INSTANT,0,160,60,1,0,60,1),
    MECHANICAL_MASTER("mechanical_master",1,SpellRarity.LEGENDARY,CastType.INSTANT,0,550,0,1,0,120,16);

    public final String id;
    public final int maxLevel, castTicks, mana, manaPerLevel, power, powerPerLevel;
    public final double cooldown, range;
    public final SpellRarity rarity;
    public final CastType castType;
    SpellKind(String id,int max,SpellRarity rarity,CastType type,int ticks,int mana,int manaGrowth,int power,int growth,double cooldown,double range) {
        this.id=id; maxLevel=max; this.rarity=rarity; castType=type; castTicks=ticks;
        this.mana=mana; manaPerLevel=manaGrowth; this.power=power; powerPerLevel=growth; this.cooldown=cooldown; this.range=range;
    }
}
