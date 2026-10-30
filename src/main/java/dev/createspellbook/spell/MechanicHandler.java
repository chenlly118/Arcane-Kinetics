package dev.createspellbook.spell;

import dev.createspellbook.Balance;
import dev.createspellbook.Content;
import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.network.MechanicSwingPayload;
import dev.createspellbook.processing.ServerSpellTasks;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The mechanic buff is a single marker effect, never one listener per cast.
 * Only a successful direct melee hit by the buffed player schedules one arm
 * follow-up; arrows, spells, summons, zero/canceled hits and the arm's own damage
 * cannot re-enter.
 */
@EventBusSubscriber(modid=CreateSpellbook.ID)
public final class MechanicHandler {
    public static final int HIT_TICKS=3;
    public static final int SWING_TICKS=12;
    private static final String NEXT_INTERVAL="CSB_MechanicNext";
    public static void cast(ServerLevel level,LivingEntity caster,int spellLevel) {
        caster.removeEffect(Content.MECHANIC_EFFECT);
        caster.addEffect(new MobEffectInstance(Content.MECHANIC_EFFECT,Balance.MECHANIC_SECONDS.get()*20,
                Balance.effectAmplifier(spellLevel),false,true,true));
    }

    @SubscribeEvent public static void melee(LivingDamageEvent.Post event) {
        LivingEntity target=event.getEntity();
        var source=event.getSource();
        if(event.getNewDamage()<=0 || !(source.getDirectEntity() instanceof ServerPlayer player) || source.getEntity()!=player) return;
        if(target==player || !target.isAlive() || source instanceof SpellDamageSource) return;
        if(source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_FALL)) return;
        var effect=player.getEffect(Content.MECHANIC_EFFECT);
        if(effect==null || DamageSources.isFriendlyFireBetween(player,target) || !(player.level() instanceof ServerLevel level)) return;
        long now=level.getGameTime();
        if(now<player.getPersistentData().getLong(NEXT_INTERVAL)) return;
        player.getPersistentData().putLong(NEXT_INTERVAL,now+Balance.MECHANIC_MIN_INTERVAL.get());
        int spellLevel=effect.getAmplifier()+1;
        float damage=(float)(Balance.mechanicDamage(spellLevel)*Content.MECHANIC.get().getEntityPowerMultiplier(player)*Balance.DAMAGE_SCALE.get());
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new MechanicSwingPayload(player.getId(),target.getId(),now));
        ServerSpellTasks.runLater(level,HIT_TICKS,()->followUp(level,player,target,damage));
    }

    /** Minecraft +Z is forward at yaw 0; right is -X, rear is -Z. */
    public static Vec3 armOrigin(Vec3 position,float yaw) {
        double angle=Math.toRadians(yaw);
        Vec3 forward=new Vec3(-Math.sin(angle),0,Math.cos(angle));
        Vec3 right=new Vec3(-forward.z,0,forward.x);
        return position.add(right.scale(.85)).subtract(forward.scale(.65)).add(0,.9,0);
    }

    private static void followUp(ServerLevel level,ServerPlayer player,LivingEntity target,float damage) {
        if(!player.isAlive() || player.level()!=level || !target.isAlive() || target.level()!=level
                || player.isRemoved() || target.isRemoved() || target.distanceToSqr(player)>36
                || !player.hasLineOfSight(target) || DamageSources.isFriendlyFireBetween(player,target)
                || player.getEffect(Content.MECHANIC_EFFECT)==null) return;
        Vec3 aim=target.getBoundingBox().getCenter();
        var spell=Content.MECHANIC.get();
        ServerSpellTasks.authorizedHit(target,damage,spell.getDamageSource(player,player));
        level.sendParticles(ParticleTypes.CRIT,aim.x,aim.y,aim.z,7,.25,.25,.25,.05);
    }
    private MechanicHandler() {}
}
