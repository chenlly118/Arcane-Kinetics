package dev.createspellbook.spell;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.AllSoundEvents;
import dev.createspellbook.*;
import dev.createspellbook.block.ArcaneCrankBlockEntity;
import dev.createspellbook.entity.MechanicalEntity;
import dev.createspellbook.processing.*;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.List;

public class IndustrialSpell extends AbstractSpell {
    public final SpellKind kind;
    private final DefaultConfig defaults;
    public IndustrialSpell(SpellKind kind) {
        this.kind=kind; baseManaCost=kind.mana; manaCostPerLevel=kind.manaPerLevel;
        baseSpellPower=kind.power; spellPowerPerLevel=kind.powerPerLevel; castTime=kind.castTicks;
        defaults=new DefaultConfig().setMaxLevel(kind.maxLevel).setMinRarity(kind.rarity)
                .setSchoolResource(CreateSpellbook.id("mechanical")).setAllowCrafting(true).setCooldownSeconds(kind.cooldown).build();
    }
    @Override public ResourceLocation getSpellResource() { return CreateSpellbook.id(kind.id); }
    @Override public DefaultConfig getDefaultConfig() { return defaults; }
    @Override public CastType getCastType() { return kind.castType; }
    @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getCastStartSound() {
        return switch(kind) {
            case ARCANE_PRESS, PRESS_BARRAGE, CRUSHING_WHEEL, ARCANE_DRILL -> java.util.Optional.of(AllSoundEvents.COGS.getMainEvent());
            default -> java.util.Optional.empty();
        };
    }
    @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getCastFinishSound() {
        if(kind.castType==CastType.CONTINUOUS) return java.util.Optional.empty();
        return java.util.Optional.of(switch(kind) {
            case ARCANE_PRESS, PRESS_BARRAGE -> AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.getMainEvent();
            case POWER_SAW -> AllSoundEvents.SAW_ACTIVATE_STONE.getMainEvent();
            case CRUSHING_WHEEL -> AllSoundEvents.CRUSHING_3.getMainEvent();
            case STRESS_DRIVE -> AllSoundEvents.CRANKING.getMainEvent();
            case BLAZE_INFUSION, FAN_SMELTING -> AllSoundEvents.STEAM.getMainEvent();
            case ARCANE_DRILL -> AllSoundEvents.CRUSHING_1.getMainEvent();
            case PISTON_LAUNCH -> net.minecraft.sounds.SoundEvents.PISTON_EXTEND;
            case BRASS_EXOSKELETON -> AllSoundEvents.COPPER_ARMOR_EQUIP.getMainEvent();
            case EJECTION_OVERLOAD -> AllSoundEvents.FWOOMP.getMainEvent();
            case MECHANIC -> AllSoundEvents.COPPER_ARMOR_EQUIP.getMainEvent();
            case MECHANICAL_MASTER -> AllSoundEvents.STEAM.getMainEvent();
        });
    }
    @Override public ICastDataSerializable getEmptyCastData() { return new ProcessingBudget(); }
    @Override public int getManaCost(int level) {
        var t=Balance.SPELLS.get(kind);
        return (int)((t.mana().get()+(double)t.manaGrowth().get()*Balance.levelSteps(level))*SpellConfigManager.getSpellConfigValue(this,SpellConfigParameter.MANA_MULTIPLIER));
    }
    @Override public float getSpellPower(int level,Entity entity) {
        var t=Balance.SPELLS.get(kind);
        double base=t.power().get()+t.powerGrowth().get()*Balance.levelSteps(level);
        if(kind==SpellKind.MECHANICAL_MASTER) base*=Balance.masterLevelMultiplier(level);
        return (float)(base*getEntityPowerMultiplier(entity instanceof LivingEntity living ? living:null));
    }
    public double range() { return Balance.SPELLS.get(kind).range().get(); }
    @Override public int getRecastCount(int level,LivingEntity caster) { return kind==SpellKind.POWER_SAW ? 3+level/2:0; }
    @Override public boolean checkPreCastConditions(Level level,int spellLevel,LivingEntity caster,MagicData data) {
        if(kind==SpellKind.EJECTION_OVERLOAD) {
            var hit=RaycastBuilder.begin(level,caster).range((float)range()).checkForBlocks(true).bbInflation(.3f).build();
            return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target
                    && validTarget(caster,target)
                    && target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)<.99;
        }
        if(kind!=SpellKind.STRESS_DRIVE) return true;
        var hit=Targeting.block(caster,range());
        return level instanceof ServerLevel server && Targeting.canInteract(server,caster,hit) && level.getBlockEntity(hit.getBlockPos()) instanceof ArcaneCrankBlockEntity;
    }
    @Override public void onServerPreCast(Level level,int spellLevel,LivingEntity caster,MagicData data) {
        if(data!=null && kind.castType==CastType.CONTINUOUS) data.setAdditionalCastData(new ProcessingBudget(Balance.inputLimit(spellLevel,kind.maxLevel)));
        super.onServerPreCast(level,spellLevel,caster,data);
    }
    @Override public void onServerCastTick(Level level,int spellLevel,LivingEntity caster,MagicData data) {
        if(kind!=SpellKind.FAN_SMELTING || !(level instanceof ServerLevel server) || data==null) return;
        // Iron's calls this hook every channel tick, while onCast only runs every ten ticks.
        var budget=budget(spellLevel,caster,CastSource.SPELLBOOK,data);
        budget.fanCredit+=Balance.fanRate(spellLevel)/20d;
        int available=(int)Math.floor(budget.fanCredit+1e-9);
        budget.fanCredit-=available;
        budget.remaining=available;
        if(available==0) return;
        Vec3 origin=caster.getEyePosition();
        double distance=Math.min(range(),origin.distanceTo(Targeting.block(caster,range()).getLocation()));
        for(double d=1;d<=distance && budget.remaining>0;d+=1.5)
            IndustrialProcessing.around(server,caster,origin.add(caster.getLookAngle().scale(d)),1.4,IndustrialProcessing.Mode.BLAST,budget);
        // Empty windows cannot accumulate credits for a later burst.
        budget.remaining=0;
    }
    public int heatTicks(int spellLevel,LivingEntity caster) {
        return (int)Math.clamp(Math.ceil((Balance.HEAT_SECONDS.get()+3d*(spellLevel-1))*20*getEntityPowerMultiplier(caster)),1,1728000);
    }
    public int pressCount(int spellLevel) { return (int)Math.min(Balance.MAX_PRESS_HEADS.get(),3+Balance.levelSteps(spellLevel)); }
    /** Horizontal cast yaw, retained even while the player looks straight up or down. */
    public static Vec3 horizontalForward(LivingEntity caster) {
        float yaw=caster.getYRot()*(float)(Math.PI/180d);
        var direction=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
        return direction.lengthSqr()<1.0E-6?new Vec3(0,0,1):direction.normalize();
    }
    private void barrage(ServerLevel level,LivingEntity caster,int spellLevel,float damage,ProcessingBudget budget) {
        Vec3 forward=horizontalForward(caster);
        int count=pressCount(spellLevel);
        double maxDistance=range();
        // The configured 3-block spacing would push eight heads to 23 blocks, beyond
        // the 14-block range. Compress only when the level/range needs it and keep the
        // near-to-far order; never silently extend past the configured range.
        double start=Math.min(2d,maxDistance*.25d);
        double span=Math.max(0d,maxDistance-start);
        double spacing=count>1?Math.min(Balance.PRESS_SPACING.get(),span/(count-1)):Balance.PRESS_SPACING.get();
        for(int i=0;i<count;i++) {
            Vec3 at=caster.position().add(forward.scale(start+i*spacing));
            if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(at))) continue;
            var ground=level.clip(new net.minecraft.world.level.ClipContext(at.add(0,3,0),at.add(0,-6,0),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,caster));
            Vec3 center=ground.getType()==HitResult.Type.BLOCK?ground.getLocation():at.add(0,-1.5,0);
            MechanicalEntity.press(caster,this,spellLevel,center,i*Balance.PRESS_INTERVAL.get(),damage,forward).pressBudget(budget);
        }
    }
    private ProcessingBudget budget(int level,LivingEntity caster,CastSource source,MagicData data) {
        if(kind==SpellKind.POWER_SAW && source==CastSource.MOB)return new ProcessingBudget(Balance.inputLimit(level,kind.maxLevel));
        if(kind==SpellKind.POWER_SAW) {
            var existing=data.getPlayerRecasts().getRecastInstance(getSpellId());
            if(existing!=null && data.getPlayerRecasts().hasRecastForSpell(getSpellId()) && existing.getCastData() instanceof ProcessingBudget b) return b;
            var b=new ProcessingBudget(Balance.inputLimit(level,kind.maxLevel));
            data.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(),level,getRecastCount(level,caster),80,source,b),data);
            return b;
        }
        if(kind.castType==CastType.CONTINUOUS && data.getAdditionalCastData() instanceof ProcessingBudget b) return b;
        var b=new ProcessingBudget(Balance.inputLimit(level,kind.maxLevel));
        if(kind.castType==CastType.CONTINUOUS) data.setAdditionalCastData(b);
        return b;
    }
    @Override public void onCast(Level level,int spellLevel,LivingEntity caster,CastSource source,MagicData data) {
        if(level instanceof ServerLevel server) {
            ProcessingBudget budget=budget(spellLevel,caster,source,data);
            if(kind.castType==CastType.CONTINUOUS && server.getGameTime()>=budget.nextSoundTick) {
                var sound=kind==SpellKind.STRESS_DRIVE ? AllSoundEvents.CRANKING : AllSoundEvents.STEAM;
                sound.playOnServer(server,caster.blockPosition(),.65f,1f);
                budget.nextSoundTick=server.getGameTime()+(kind==SpellKind.STRESS_DRIVE ? 20:40);
            }
            float damage=(float)(getSpellPower(spellLevel,caster)*Balance.DAMAGE_SCALE.get());
            switch(kind) {
                case ARCANE_PRESS -> {
                    var hit=RaycastBuilder.begin(level,caster).range((float)range()).checkForBlocks(true).bbInflation(.3f).build();
                    Vec3 center=hit.getLocation();
                    MechanicalEntity.visual(caster,3,center,14);
                    IndustrialProcessing.around(server,caster,center,1.8,IndustrialProcessing.Mode.PRESS,budget);
                    for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(1.5)))
                        if(validTarget(caster,target)) DamageSources.applyDamage(target,damage,getDamageSource(caster));
                }
                case PRESS_BARRAGE -> barrage(server,caster,spellLevel,damage,budget);
                case POWER_SAW -> MechanicalEntity.launch(caster,this,spellLevel,1,damage,Balance.SAW_SPEED.get()).processing(budget,range());
                case STRESS_DRIVE -> {
                    var hit=Targeting.block(caster,range());
                    if(Targeting.canInteract(server,caster,hit) && level.getBlockEntity(hit.getBlockPos()) instanceof ArcaneCrankBlockEntity crank)
                        crank.activate(spellLevel,getEntityPowerMultiplier(caster));
                }
                case BLAZE_INFUSION -> {
                    var hit=Targeting.block(caster,range());
                    if(Targeting.canInteract(server,caster,hit) && level.getBlockEntity(hit.getBlockPos()) instanceof BlazeBurnerBlockEntity burner
                            && burner.getBlockState().getValue(BlazeBurnerBlock.HEAT_LEVEL)!=BlazeBurnerBlock.HeatLevel.NONE) {
                        ((ArcaneHeat)burner).createSpellbook$heatFor(heatTicks(spellLevel,caster));
                    } else {
                        var combat=RaycastBuilder.begin(level,caster).range((float)range()).checkForBlocks(true).bbInflation(.3f).build();
                        if(combat instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target && validTarget(caster,target)
                                && DamageSources.applyDamage(target,damage,getDamageSource(caster)))
                            ServerSpellTasks.burn(caster,target,this,(float)(damage*Balance.BLAZE_DOT_SHARE.get()));
                    }
                }
                case FAN_SMELTING -> {
                    Vec3 origin=caster.getEyePosition();
                    var hit=Targeting.block(caster,range());
                    double distance=Math.min(range(),origin.distanceTo(hit.getLocation()));
                    for(double d=1;d<=distance;d+=1.5) {
                        Vec3 center=origin.add(caster.getLookAngle().scale(d));
                        FanAirflow.spawnBlasting(server,center);
                    }
                    for(var target:level.getEntitiesOfClass(LivingEntity.class,caster.getBoundingBox().inflate(range()))) {
                        Vec3 delta=target.getBoundingBox().getCenter().subtract(origin);
                        if(validTarget(caster,target) && delta.length()<=range() && delta.normalize().dot(caster.getLookAngle())>.86 && caster.hasLineOfSight(target))
                            DamageSources.applyDamage(target,damage,getDamageSource(caster));
                    }
                }
                case CRUSHING_WHEEL -> MechanicalEntity.launch(caster,this,spellLevel,2,damage,Balance.WHEEL_SPEED.get()).processing(budget,range());
                case PISTON_LAUNCH -> {
                    double impulse=Balance.pistonImpulse(spellLevel,getEntityPowerMultiplier(caster));
                    Vec3 movement=caster.getLookAngle().scale(impulse).add(0,.15,0);
                    caster.setDeltaMovement(caster.getDeltaMovement().add(movement)); caster.hasImpulse=true; caster.hurtMarked=true;
                    if(caster instanceof ServerPlayer player) player.connection.send(new ClientboundSetEntityMotionPacket(caster));
                    MechanicalEntity.visual(caster,5,caster.position().subtract(caster.getLookAngle().scale(1.1)),20);
                }
                case ARCANE_DRILL -> MechanicalEntity.launch(caster,this,spellLevel,4,damage,Balance.DRILL_SPEED.get()).lifetime(range());
                case BRASS_EXOSKELETON -> caster.addEffect(new MobEffectInstance(Content.EXOSKELETON,
                        Balance.EXOSKELETON_SECONDS.get()*20,Balance.effectAmplifier(spellLevel),false,false,true));
                case EJECTION_OVERLOAD -> EjectionOverload.cast(server,caster,spellLevel,this);
                case MECHANIC -> MechanicHandler.cast(server,caster,spellLevel);
                case MECHANICAL_MASTER -> MechanicalMasterHandler.cast(server,caster,spellLevel);
            }
        }
        super.onCast(level,spellLevel,caster,source,data);
    }
    public static boolean validTarget(LivingEntity caster,LivingEntity target) { return target!=caster && target.isAlive() && !DamageSources.isFriendlyFireBetween(caster,target) && caster.hasLineOfSight(target); }
    @Override public SpellDamageSource getDamageSource(Entity projectile,Entity attacker) {
        if(kind==SpellKind.BLAZE_INFUSION) return new ThermalDamage(projectile,attacker,this);
        if(kind==SpellKind.FAN_SMELTING) return new ThermalDamage(projectile,attacker,this).setFireTicks(60);
        return super.getDamageSource(projectile,attacker);
    }
    @Override public List<MutableComponent> getUniqueInfo(int level,LivingEntity caster) {
        if(kind==SpellKind.EJECTION_OVERLOAD) return List.of(
                Component.translatable("tooltip.create_spellbook.ejection_height",fmt(Balance.ejectionHeight(level))),
                Component.translatable("tooltip.create_spellbook.ejection_multiplier",
                        fmt(Balance.ejectionMultiplier(level,getEntityPowerMultiplier(caster)))));
        if(kind==SpellKind.MECHANIC) return List.of(
                Component.translatable("tooltip.create_spellbook.mechanic_damage",
                        fmt(Balance.mechanicDamage(Balance.effectAmplifier(level)+1)*getEntityPowerMultiplier(caster)*Balance.DAMAGE_SCALE.get())),
                Component.translatable("tooltip.create_spellbook.duration_seconds",Balance.MECHANIC_SECONDS.get()));
        if(kind==SpellKind.MECHANICAL_MASTER) return List.of(
                Component.translatable("tooltip.create_spellbook.duration_seconds",Balance.MASTER_DURATION_SECONDS.get()),
                Component.translatable("tooltip.create_spellbook.master_interval",Balance.MASTER_INTERVAL.get()),
                Component.translatable("tooltip.create_spellbook.master_radius",Balance.MASTER_RADIUS.get()),
                Component.translatable("tooltip.create_spellbook.master_direct_dps",fmt(Balance.masterDirectAverage()
                        *getSpellPower(Balance.effectAmplifier(level)+1,caster)*Balance.DAMAGE_SCALE.get()*20/Balance.MASTER_INTERVAL.get())));
        if(kind==SpellKind.BRASS_EXOSKELETON) return List.of(Component.translatable("tooltip.create_spellbook.exoskeleton_stats",2*(Balance.effectAmplifier(level)+1),2*(Balance.effectAmplifier(level)+1),Balance.effectAmplifier(level)+1),
                Component.translatable("tooltip.create_spellbook.duration_seconds",Balance.EXOSKELETON_SECONDS.get()));
        if(kind==SpellKind.STRESS_DRIVE) {
            var output=ArcaneCrankBlockEntity.output(level,getEntityPowerMultiplier(caster));
            return List.of(Component.translatable("tooltip.create_spellbook.stress_capacity",fmt(output.capacity())),
                    Component.translatable("tooltip.create_spellbook.crank_rpm",fmt(output.rpm())),
                    Component.translatable("tooltip.create_spellbook.channel_seconds",30));
        }
        if(kind==SpellKind.PISTON_LAUNCH) return List.of(Component.translatable("tooltip.create_spellbook.collision_dash"),
                Component.translatable("tooltip.create_spellbook.dash_impulse",fmt(Balance.pistonImpulse(level,getEntityPowerMultiplier(caster)))));
        if(kind==SpellKind.FAN_SMELTING) return List.of(Component.translatable("tooltip.create_spellbook.inputs_per_second",String.format(java.util.Locale.ROOT,"%.1f",Balance.fanRate(level))),
                Component.translatable("ui.irons_spellbooks.damage",String.format(java.util.Locale.ROOT,"%.1f",getSpellPower(level,caster)*Balance.DAMAGE_SCALE.get())));
        if(kind==SpellKind.PRESS_BARRAGE) return List.of(Component.translatable("tooltip.create_spellbook.press_count",pressCount(level)),
                Component.translatable("ui.irons_spellbooks.damage",String.format(java.util.Locale.ROOT,"%.1f",getSpellPower(level,caster)*Balance.DAMAGE_SCALE.get())),
                Component.translatable("tooltip.create_spellbook.press_debuffs"));
        if(kind==SpellKind.BLAZE_INFUSION) return List.of(Component.translatable("tooltip.create_spellbook.burner_seconds",String.format(java.util.Locale.ROOT,"%.1f",heatTicks(level,caster)/20d)),
                Component.translatable("tooltip.create_spellbook.burn_pulses",3),
                Component.translatable("ui.irons_spellbooks.damage",String.format(java.util.Locale.ROOT,"%.1f",getSpellPower(level,caster)*Balance.DAMAGE_SCALE.get())));
        return List.of(Component.translatable("ui.irons_spellbooks.damage",String.format(java.util.Locale.ROOT,"%.1f",getSpellPower(level,caster)*Balance.DAMAGE_SCALE.get())),
                Component.translatable("tooltip.create_spellbook.input_limit",Balance.inputLimit(level,kind.maxLevel)),
                Component.translatable("ui.irons_spellbooks.distance",range()));
    }
    private static String fmt(double value) { return String.format(java.util.Locale.ROOT,"%.1f",value); }
    private static final class ThermalDamage extends SpellDamageSource {
        ThermalDamage(Entity projectile,Entity attacker,AbstractSpell spell) { super(projectile,attacker,null,spell); }
        @Override public boolean is(TagKey<DamageType> tag) { return tag==DamageTypeTags.IS_FIRE || super.is(tag); }
    }
}
