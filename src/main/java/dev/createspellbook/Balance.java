package dev.createspellbook;

import net.neoforged.neoforge.common.ModConfigSpec;
import dev.createspellbook.spell.SpellKind;
import java.util.EnumMap;
import java.util.Map;

public final class Balance {
    /** Bound serialized work, rather than clamping a spell to its scribing level. */
    public static final int MAX_PROCESSING_BUDGET = 4096;
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue WRENCH_POWER;
    public static final ModConfigSpec.DoubleValue DAMAGE_SCALE;
    public static final ModConfigSpec.DoubleValue BOILER_SCALE;
    public static final Map<SpellKind, Tuning> SPELLS = new EnumMap<>(SpellKind.class);
    public static final ModConfigSpec.IntValue HEAT_SECONDS;
    public static final ModConfigSpec.IntValue EXOSKELETON_SECONDS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> EQUIPMENT;
    public static final ModConfigSpec.IntValue GEAR_MANA, GEAR_MANA_GROWTH;
    public static final ModConfigSpec.DoubleValue GEAR_POWER, GEAR_POWER_GROWTH;
    public static final ModConfigSpec.DoubleValue GEAR_RANGE;
    public static final ModConfigSpec.DoubleValue GEAR_SPEED, SAW_SPEED, WHEEL_SPEED, DRILL_SPEED;
    public static final ModConfigSpec.IntValue CRANK_MIN_RPM,CRANK_MAX_RPM,CRANK_MIN_LEASE,CRANK_MAX_LEASE;
    public static final ModConfigSpec.DoubleValue FAN_MIN_RATE,FAN_MAX_RATE,BLAZE_DOT_SHARE,PRESS_SPACING,PRESS_RADIUS;
    public static final ModConfigSpec.IntValue TREE_MAX_LOGS,TREE_LOGS_PER_TICK,TREE_RADIUS,TREE_HEIGHT,BLAZE_DOT_INTERVAL,PRESS_INTERVAL;
    public static final ModConfigSpec.IntValue EJECTION_STATE_SECONDS;
    public static final ModConfigSpec.DoubleValue EJECTION_HEIGHT_BASE,EJECTION_HEIGHT_PER_LEVEL,EJECTION_MULT_BASE,EJECTION_MULT_PER_LEVEL,EJECTION_POWER_BONUS;
    public static final ModConfigSpec.IntValue MECHANIC_SECONDS,MECHANIC_MIN_INTERVAL;
    public static final ModConfigSpec.DoubleValue MECHANIC_DAMAGE_BASE,MECHANIC_DAMAGE_PER_LEVEL;
    public static final ModConfigSpec.IntValue MASTER_DURATION_SECONDS,MASTER_INTERVAL,MASTER_RADIUS,MASTER_DRILL_SEGMENTS,
            MASTER_SAW_SEGMENTS,MASTER_LAVA_FIRE_TICKS,MASTER_PRESS_DEBUFF_TICKS,MASTER_VISUAL_CAP;
    public static final ModConfigSpec.DoubleValue MASTER_PRESS_WEIGHT,MASTER_SWORD_WEIGHT,MASTER_DRILL_WEIGHT,
            MASTER_LAVA_WEIGHT,MASTER_PISTON_WEIGHT,MASTER_SAW_WEIGHT;
    public static final ModConfigSpec.DoubleValue MASTER_PRESS_DAMAGE,MASTER_SWORD_DAMAGE,MASTER_DRILL_DAMAGE,
            MASTER_LAVA_DAMAGE,MASTER_PISTON_DAMAGE,MASTER_SAW_DAMAGE;
    public static final ModConfigSpec.DoubleValue MASTER_OVERLEVEL_GROWTH,MAX_FAN_RATE,MAX_EJECTION_HEIGHT,MAX_PISTON_IMPULSE;
    public static final ModConfigSpec.IntValue MAX_INSTANT_INPUTS,MAX_PRESS_HEADS;
    public static final ModConfigSpec.IntValue BASIN_CAPACITY,SENSOR_TICKS,SENSOR_STRENGTH;
    public static final ModConfigSpec.DoubleValue MANA_PER_DAMAGE;
    public static final ModConfigSpec.IntValue ARM_CAPACITY,ARM_INTERVAL,CONSTRUCT_MAX_LEVEL,AUTO_CAPACITY,AUTO_REGEN,AUTO_RANGE,AUTO_INTERVAL;
    public static final ModConfigSpec.DoubleValue ARM_MB_PER_MANA,EMITTER_STRESS;
    public static final ModConfigSpec.IntValue EMITTER_RANGE;
    public static final ModConfigSpec.IntValue PRINTER_CAPACITY,PRINTER_INK,PRINTER_TICKS;
    public static final ModConfigSpec.IntValue REACTOR_SIZE_STEP,REACTOR_DURABILITY,REACTOR_MATTER_RATE,REACTOR_RPM,REACTOR_TANK,REACTOR_MANA_PER_CELL;
    public static final ModConfigSpec.IntValue REACTOR_BLOCK_BUDGET,REACTOR_PROBE_BUDGET,REACTOR_ENTITY_BUDGET,REACTOR_WORK_MILLIS;
    public static final ModConfigSpec.IntValue REACTOR_LOAD_WORKERS;
    public static final ModConfigSpec.DoubleValue REACTOR_VISUAL_FRACTION,REACTOR_GRAVITY_FACTOR,REACTOR_PULL_SPEED,REACTOR_ACCIDENT_DAMAGE,REACTOR_CHAMBER_DAMAGE;
    static {
        var b = new ModConfigSpec.Builder();
        WRENCH_POWER = b.comment("Additive mechanical school power while held. Other schools are unchanged.")
                .defineInRange("wrenchMechanicalPower", 0.15, 0, 5);
        DAMAGE_SCALE = b.defineInRange("combatDamageMultiplier", 1.0, 0, 100);
        BOILER_SCALE = b.comment("Scale relative to current Create steam engine stress configuration.")
                .defineInRange("crankCapacityMultiplier", 1.0, 0, 100);
        CRANK_MIN_RPM=b.defineInRange("crankMinRPM",32,1,256); CRANK_MAX_RPM=b.defineInRange("crankMaxRPM",128,1,256);
        CRANK_MIN_LEASE=b.defineInRange("crankMinLeaseTicks",22,10,1200); CRANK_MAX_LEASE=b.defineInRange("crankMaxLeaseTicks",40,10,1200);
        HEAT_SECONDS = b.comment("Level-one burner duration, scaled by the caster's spell power.").defineInRange("baseBurnerHeatSeconds", 120, 1, 3600);
        EXOSKELETON_SECONDS = b.defineInRange("baseExoskeletonSeconds", 210, 1, 3600);
        b.push("overlevel");
        MAX_INSTANT_INPUTS=b.comment("Work limit for extremely overleveled casts; normal max-level input count remains 64.")
                .defineInRange("maxInstantInputs",MAX_PROCESSING_BUDGET,64,MAX_PROCESSING_BUDGET);
        MAX_FAN_RATE=b.comment("Safety ceiling in inputs/second; growth continues beyond level ten.")
                .defineInRange("maxFanInputsPerSecond",1280d,64,1280);
        MAX_PRESS_HEADS=b.comment("Per-cast construct limit; per-head damage continues growing beyond this limit.")
                .defineInRange("maxPressHeads",32,8,64);
        MAX_EJECTION_HEIGHT=b.comment("Keep launch velocity below Minecraft's entity motion packet limit; fall multiplier still grows.")
                .defineInRange("maxEjectionHeight",48d,16,48);
        MAX_PISTON_IMPULSE=b.comment("Maximum dash impulse, below Minecraft's 3.9 blocks/tick motion packet limit.")
                .defineInRange("maxPistonImpulse",3.8d,2.4,3.8);
        b.pop();
        b.push("fanProcessing");
        FAN_MIN_RATE=b.defineInRange("minInputsPerSecond",4d,1,64);
        FAN_MAX_RATE=b.defineInRange("maxInputsPerSecond",16d,1,64);
        b.pop();
        b.push("treeFelling");
        TREE_MAX_LOGS=b.comment("Maximum connected logs per hit; work is spread over server ticks.").defineInRange("maxLogs",1024,1,4096);
        TREE_LOGS_PER_TICK=b.defineInRange("logsPerTick",16,1,64);
        TREE_RADIUS=b.defineInRange("horizontalRadius",16,1,32);
        TREE_HEIGHT=b.defineInRange("verticalRange",64,1,256);
        b.pop();
        b.push("blazeDamage");
        BLAZE_DOT_SHARE=b.comment("Each of the three delayed hits deals this fraction of the initial damage.").defineInRange("damagePerPulse",1d/3,0,10);
        BLAZE_DOT_INTERVAL=b.defineInRange("pulseIntervalTicks",20,10,200);
        b.pop();
        b.push("pressBarrage");
        PRESS_INTERVAL=b.defineInRange("ticksBetweenHeads",5,1,40);
        PRESS_SPACING=b.defineInRange("headSpacing",3d,1,6);
        PRESS_RADIUS=b.defineInRange("impactRadius",2d,.5,5);
        b.pop();
        b.push("ejectionOverload");
        EJECTION_STATE_SECONDS=b.comment("How long the next valid fall damage keeps the overload multiplier.")
                .defineInRange("stateSeconds",10,1,60);
        EJECTION_HEIGHT_BASE=b.defineInRange("baseLaunchHeight",6d,1,32);
        EJECTION_HEIGHT_PER_LEVEL=b.defineInRange("launchHeightPerLevel",2d,0,8);
        EJECTION_MULT_BASE=b.defineInRange("baseFallMultiplier",1.5,1,10);
        EJECTION_MULT_PER_LEVEL=b.defineInRange("fallMultiplierPerLevel",.2,0,2);
        EJECTION_POWER_BONUS=b.comment("Extra fall multiplier per +100% mechanical spell power; keeps the level curve recognizable.")
                .defineInRange("powerFallMultiplierBonus",.5,0,5);
        b.pop();
        b.push("mechanic");
        MECHANIC_SECONDS=b.defineInRange("durationSeconds",20,1,300);
        MECHANIC_MIN_INTERVAL=b.comment("Minimum ticks between arm follow-ups; prevents spam and respects the target's hurt frames.")
                .defineInRange("minIntervalTicks",10,1,100);
        MECHANIC_DAMAGE_BASE=b.defineInRange("baseArmDamage",5d,0,100);
        MECHANIC_DAMAGE_PER_LEVEL=b.defineInRange("armDamagePerLevel",2d,0,50);
        b.pop();
        b.push("mechanicalMaster");
        MASTER_OVERLEVEL_GROWTH=b.comment("Extra direct damage factor per level above one, before spell power. 0.2 means +20% per level.")
                .defineInRange("overlevelDamagePerLevel",.2,0,10);
        MASTER_DURATION_SECONDS=b.defineInRange("durationSeconds",30,1,300);
        MASTER_INTERVAL=b.defineInRange("ticksBetweenAttacks",10,2,100);
        MASTER_RADIUS=b.defineInRange("targetRadius",16,2,48);
        MASTER_DRILL_SEGMENTS=b.defineInRange("drillSegments",3,1,6);
        MASTER_SAW_SEGMENTS=b.defineInRange("sawSegments",2,1,4);
        MASTER_LAVA_FIRE_TICKS=b.defineInRange("lavaFireTicks",60,0,400);
        MASTER_PRESS_DEBUFF_TICKS=b.defineInRange("pressDebuffTicks",20,0,200);
        MASTER_VISUAL_CAP=b.comment("Maximum simultaneous master/mechanic visuals in a level; damage is unaffected.")
                .defineInRange("visualCapPerLevel",64,1,256);
        MASTER_PRESS_WEIGHT=b.defineInRange("pressWeight",10d,0,100);
        MASTER_SWORD_WEIGHT=b.defineInRange("swordWeight",10d,0,100);
        MASTER_DRILL_WEIGHT=b.defineInRange("drillWeight",20d,0,100);
        MASTER_LAVA_WEIGHT=b.defineInRange("lavaWeight",20d,0,100);
        MASTER_PISTON_WEIGHT=b.defineInRange("pistonWeight",20d,0,100);
        MASTER_SAW_WEIGHT=b.defineInRange("sawWeight",20d,0,100);
        MASTER_PRESS_DAMAGE=b.defineInRange("pressDamage",8d,0,100);
        MASTER_SWORD_DAMAGE=b.defineInRange("swordDamage",8d,0,100);
        MASTER_DRILL_DAMAGE=b.comment("Total direct damage for one drill attack event, split across its segments.")
                .defineInRange("drillDamage",5d,0,100);
        MASTER_LAVA_DAMAGE=b.defineInRange("lavaDamage",3d,0,100);
        MASTER_PISTON_DAMAGE=b.defineInRange("pistonDamage",3d,0,100);
        MASTER_SAW_DAMAGE=b.comment("Total direct damage for one saw attack event, split across two passes.")
                .defineInRange("sawDamage",5d,0,100);
        b.pop();
        EQUIPMENT = b.comment("item id|equipment slot|additive mechanical power; optional items apply only if installed.",
                "Valid slots: head, chest, legs, feet. Goggles also work in Curios with the same modifier ID.")
                .defineListAllowEmpty("equipmentBonuses", java.util.List.of("create:goggles|head|0.05",
                        "create:copper_backtank|chest|0.08", "create:copper_diving_helmet|head|0.04", "create:copper_diving_boots|feet|0.04",
                        "create:netherite_backtank|chest|0.12", "create:netherite_diving_helmet|head|0.08", "create:netherite_diving_boots|feet|0.08"),
                        value->value instanceof String s && s.split("\\|").length==3);
        b.push("gear_missile");
        GEAR_MANA=b.defineInRange("baseMana",8,0,10000); GEAR_MANA_GROWTH=b.defineInRange("manaPerLevel",2,0,1000);
        GEAR_POWER=b.defineInRange("basePower",12d,0,10000); GEAR_POWER_GROWTH=b.defineInRange("powerPerLevel",1d,0,1000);
        GEAR_RANGE=b.defineInRange("range",32d,1,64);
        b.pop();
        b.push("projectiles");
        GEAR_SPEED=b.comment("Blocks per game tick; lifetime follows configured range.").defineInRange("gearSpeed",1.6d,.1,4);
        SAW_SPEED=b.defineInRange("sawSpeed",1.1d,.1,4);
        WHEEL_SPEED=b.defineInRange("wheelSpeed",.22d,.1,1);
        DRILL_SPEED=b.comment("Blocks per game tick for the travelling arcane drill; lifetime follows configured range.")
                .defineInRange("drillSpeed",.9d,.1,4);
        b.pop();
        for (var kind : SpellKind.values()) {
            b.push(kind.id);
            SPELLS.put(kind, new Tuning(b.defineInRange("baseMana",kind.mana,0,10000),
                    b.defineInRange("manaPerLevel",kind.manaPerLevel,0,1000),
                    b.defineInRange("basePower",(double)kind.power,0,10000),
                    b.defineInRange("powerPerLevel",(double)kind.powerPerLevel,0,1000),
                    b.defineInRange("range",kind.range,1,64)));
            b.pop();
        }
        b.push("spell_devices");
        BASIN_CAPACITY=b.comment("Focusing Basin fluid capacity in mB.").defineInRange("basinCapacity",2000,1,64000);
        MANA_PER_DAMAGE=b.defineInRange("manaPerDamage",10d,0,10000);
        SENSOR_TICKS=b.defineInRange("sensorPulseTicks",15,1,1200);
        SENSOR_STRENGTH=b.defineInRange("sensorSignalStrength",15,0,15);
        b.pop();
        b.push("casting_constructs");
        ARM_CAPACITY=b.defineInRange("armManaCapacity",1000,100,64000);
        ARM_MB_PER_MANA=b.comment("Liquid mana mB spent per point of the spell's configured mana cost, rounded up at release.").defineInRange("mbPerMana",1d,.01,100);
        ARM_INTERVAL=b.defineInRange("defaultIntervalTicks",20,1,12000);
        EMITTER_STRESS=b.comment("Spell emitter stress impact in SU per RPM; a rotating, non-overstressed bottom shaft is required in every mode.").defineInRange("emitterStressImpact",4d,0,256);
        EMITTER_RANGE=b.comment("Turret target radius and maximum forward processing distance. Unloaded chunks are never searched.").defineInRange("emitterRange",16,2,32);
        CONSTRUCT_MAX_LEVEL=b.comment("Safety ceiling for scroll/modified levels; overlevels below this limit remain supported.").defineInRange("maxSpellLevel",100,1,1000);
        b.pop();
        b.push("auto_caster");
        AUTO_CAPACITY=b.comment("Internal mana capacity in mB. The rear inlet is fill-only; no extraction interface is exposed.").defineInRange("manaCapacity",1000,1,64000);
        AUTO_REGEN=b.comment("Mana recovered per loaded server tick. No offline catch-up.").defineInRange("manaPerTick",1,0,1000);
        AUTO_RANGE=b.defineInRange("targetRadius",16,1,32);
        AUTO_INTERVAL=b.comment("Target scan/attempt interval; never bypasses native spell cooldown.").defineInRange("scanIntervalTicks",10,1,200);
        b.pop();
        b.push("scroll_printer");
        PRINTER_CAPACITY=b.comment("Ink capacity in mB. Only the top face accepts fluid pipes.").defineInRange("inkCapacity",4000,250,64000);
        PRINTER_INK=b.comment("Ink consumed per printed scroll; one native ink bottle is 250 mB.").defineInRange("inkPerScroll",250,1,4000);
        PRINTER_TICKS=b.comment("Ticks per printing stroke; no kinetic power is required.").defineInRange("printingTicks",20,8,1200);
        b.pop();
        b.push("black_hole_reactor");
        REACTOR_CHAMBER_DAMAGE=b.comment("Base magic damage per 20 ticks to living entities inside a formed reactor's chamber while switched on and its field exists. Exterior entities, creative and spectator players are excluded. Zero disables this hazard.").defineInRange("activeChamberDamagePerSecond",10d,0,100);
        REACTOR_SIZE_STEP=b.comment("Healthy convergence and safe shutdown in tenths of a percent per tick. Fault growth is fixed at +0.1% every 5 ticks.").defineInRange("fieldConvergenceTenthsPerTick",5,1,100);
        REACTOR_DURABILITY=b.comment("Maximum stabilizer durability; existing damage is retained. Restart after changing.").defineInRange("stabilizerDurability",10000,1,1000000);
        REACTOR_MATTER_RATE=b.defineInRange("matterPerMillionPerSecond",4,1,64);
        REACTOR_RPM=b.comment("Native Create output RPM, also bounded by Create's maximum rotation speed.").defineInRange("outputRPM",256,1,256);
        REACTOR_TANK=b.comment("Legacy unformed-controller fallback only. Certified reactors now use interior volume times manaCapacityPerInteriorBlock.").defineInRange("manaCapacityPerInlet",16000,1000,1000000);
        REACTOR_MANA_PER_CELL=b.comment("Shared mana capacity per interior cell in mB: (side - 2)^3 times this value. Interior core placement does not change geometric volume. More inlets do not add storage.").defineInRange("manaCapacityPerInteriorBlock",4000,1,400000);
        REACTOR_WORK_MILLIS=b.comment("Soft shared accident CPU time budget per dimension tick. Size rates use game ticks, not wall time.").defineInRange("sizeAccidentWorkMillis",25,1,200);
        REACTOR_BLOCK_BUDGET=b.comment("Maximum swallowed blocks per dimension per tick, shared fairly by all accidents. No block/item drops.").defineInRange("sizeAccidentBlocksPerTick",8192,1,131072);
        REACTOR_PROBE_BUDGET=b.comment("Maximum terrain probes per dimension per tick, including air and unloaded positions.").defineInRange("sizeAccidentProbesPerTick",65536,1,1048576);
        REACTOR_ENTITY_BUDGET=b.comment("Maximum entity interactions per dimension per tick. Queries use bounded spatial sectors.").defineInRange("sizeAccidentEntitiesPerTick",4096,1,32768);
        REACTOR_LOAD_WORKERS=b.comment("Concurrent forced-loading workers per dimension. Each holds a center and moving 5x5 full-chunk window, at most 50 full chunks plus native generation dependencies. Saved progress resumes after restart; tickets are released on completion.").defineInRange("accidentChunkWorkers",2,1,8);
        REACTOR_VISUAL_FRACTION=b.comment("Visible sphere radius at the escaped peak, relative to the unchanged destruction radius. Containment and the escape seed stay continuous.").defineInRange("accidentVisibleRadiusFraction",.82,.5,.95);
        REACTOR_GRAVITY_FACTOR=b.comment("Gravity reach relative to the terrain-destruction envelope. Does not increase terrain destruction or forced-chunk footprint.").defineInRange("accidentGravityRadiusFactor",1.2,1,1.5);
        REACTOR_PULL_SPEED=b.comment("Maximum attraction speed in blocks per tick. Below the native entity-motion packet's 3.9 component limit. Normal collision remains enabled.").defineInRange("accidentPullSpeed",3.3,.25,3.5);
        REACTOR_ACCIDENT_DAMAGE=b.comment("Base magic damage per 20 ticks inside the destruction envelope. Overlapping reactor fields share the per-entity damage cooldown.").defineInRange("accidentDamagePerSecond",10d,0,100);
        b.pop();
        SPEC = b.build();
    }
    public record Tuning(ModConfigSpec.IntValue mana, ModConfigSpec.IntValue manaGrowth,
                         ModConfigSpec.DoubleValue power, ModConfigSpec.DoubleValue powerGrowth, ModConfigSpec.DoubleValue range) {}
    public static int inputLimit(int level, int max) {
        return (int)Math.min(MAX_INSTANT_INPUTS.get(),8+56*levelSteps(level)/Math.max(1,max-1));
    }
    public static long levelSteps(int level) { return Math.max(0L,(long)level-1); }
    /** Vanilla MobEffectInstance's verified constructor clamps its amplifier to 0..255. */
    public static int effectAmplifier(int level) { return (int)Math.min(255,levelSteps(level)); }
    public static double fanRate(int level) {
        return Math.clamp(FAN_MIN_RATE.get()+(FAN_MAX_RATE.get()-FAN_MIN_RATE.get())*levelSteps(level)/9d,1,MAX_FAN_RATE.get());
    }
    public static double ejectionHeight(int level) {
        return Math.min(MAX_EJECTION_HEIGHT.get(),EJECTION_HEIGHT_BASE.get()+EJECTION_HEIGHT_PER_LEVEL.get()*levelSteps(level));
    }
    public static double ejectionMultiplier(int level,double powerMultiplier) {
        double base=EJECTION_MULT_BASE.get()+EJECTION_MULT_PER_LEVEL.get()*levelSteps(level);
        // Spell power affects only the extra fall multiplier, once; the launch height
        // is not multiplied again, so the level curve stays recognizable.
        base+=(Math.max(0,powerMultiplier)-1)*EJECTION_POWER_BONUS.get();
        return Math.max(base,1);
    }
    public static double mechanicDamage(int level) {
        return MECHANIC_DAMAGE_BASE.get()+MECHANIC_DAMAGE_PER_LEVEL.get()*levelSteps(level);
    }
    public static double masterLevelMultiplier(int level) { return 1+MASTER_OVERLEVEL_GROWTH.get()*levelSteps(level); }
    public static double pistonImpulse(int level,double power) {
        return Math.min(MAX_PISTON_IMPULSE.get(),(1.325+.075*levelSteps(level))*Math.clamp(power,0,1.4));
    }
    public static double masterTotalWeight() {
        return MASTER_PRESS_WEIGHT.get()+MASTER_SWORD_WEIGHT.get()+MASTER_DRILL_WEIGHT.get()
                +MASTER_LAVA_WEIGHT.get()+MASTER_PISTON_WEIGHT.get()+MASTER_SAW_WEIGHT.get();
    }
    public static double masterDirectAverage() {
        double total=masterTotalWeight();
        if(total<=0) return 0;
        return (MASTER_PRESS_WEIGHT.get()*MASTER_PRESS_DAMAGE.get()+MASTER_SWORD_WEIGHT.get()*MASTER_SWORD_DAMAGE.get()
                +MASTER_DRILL_WEIGHT.get()*MASTER_DRILL_DAMAGE.get()+MASTER_LAVA_WEIGHT.get()*MASTER_LAVA_DAMAGE.get()
                +MASTER_PISTON_WEIGHT.get()*MASTER_PISTON_DAMAGE.get()+MASTER_SAW_WEIGHT.get()*MASTER_SAW_DAMAGE.get())/total;
    }
    private Balance() {}
}
