package dev.createspellbook.spell;

import dev.createspellbook.Balance;
import dev.createspellbook.Content;
import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.entity.MechanicalEntity;
import dev.createspellbook.processing.ServerSpellTasks;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;

/**
 * Server-authoritative random mechanical attack scheduler.  One buff per player,
 * one event per configured interval, one primary target per event.  All visuals
 * are short-lived client constructs; no AI, power network or real fluid is created.
 */
@EventBusSubscriber(modid=CreateSpellbook.ID)
public final class MechanicalMasterHandler {
    public enum Attack { PRESS, SWORD, DRILL, LAVA, PISTON, SAW }
    private static final Map<UUID,ServerLevel> TICKING=new HashMap<>();
    private static final String ATTACK_COUNT="CSB_MasterAttacks";
    private static final String LAST_ATTACK="CSB_MasterLastAttack";

    public static void cast(ServerLevel level,LivingEntity caster,int spellLevel) {
        caster.removeEffect(Content.MECHANICAL_MASTER_EFFECT);
        caster.addEffect(new MobEffectInstance(Content.MECHANICAL_MASTER_EFFECT,
                Balance.MASTER_DURATION_SECONDS.get()*20,Balance.effectAmplifier(spellLevel),false,false,true));
        startTicker(level,caster);
    }

    /** One repeating server task per buffed player; it stops on death, expiry or unload. */
    private static void startTicker(ServerLevel level,LivingEntity caster) {
        if(!(caster instanceof ServerPlayer player)) return;
        UUID id=player.getUUID();
        if(TICKING.containsKey(id)) return;
        TICKING.put(id,level);
        int interval=Balance.MASTER_INTERVAL.get();
        ServerSpellTasks.every(level,interval,()->{
            boolean keep=player.isAlive() && !player.isRemoved() && player.level()==level
                    && player.getEffect(Content.MECHANICAL_MASTER_EFFECT)!=null;
            if(!keep) TICKING.remove(id);
            return keep;
        },()->{
            if(!player.isAlive() || player.level()!=level) return;
            LivingEntity target=selectTarget(player);
            if(target==null) return;
            Attack attack=rollAttack(level.random);
            player.getPersistentData().putString(LAST_ATTACK,attack.name());
            player.getPersistentData().putInt(ATTACK_COUNT,player.getPersistentData().getInt(ATTACK_COUNT)+1);
            runAttack(level,player,target,attack);
        });
    }

    @SubscribeEvent public static void login(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player && player.getEffect(Content.MECHANICAL_MASTER_EFFECT)!=null
                && player.level() instanceof ServerLevel level) startTicker(level,player);
    }
    @SubscribeEvent public static void changedDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if(event.getEntity() instanceof ServerPlayer player && player.getEffect(Content.MECHANICAL_MASTER_EFFECT)!=null
                && player.level() instanceof ServerLevel level) startTicker(level,player);
    }
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if(event.getLevel() instanceof ServerLevel level) TICKING.entrySet().removeIf(entry->entry.getValue()==level);
    }

    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            player.removeEffect(Content.MECHANICAL_MASTER_EFFECT);
            TICKING.remove(player.getUUID());
        }
    }

    public static Attack rollAttack(net.minecraft.util.RandomSource random) {
        double total=Balance.masterTotalWeight();
        if(total<=0) return Attack.PISTON;
        double roll=random.nextDouble()*total;
        double[] weights={Balance.MASTER_PRESS_WEIGHT.get(),Balance.MASTER_SWORD_WEIGHT.get(),Balance.MASTER_DRILL_WEIGHT.get(),
                Balance.MASTER_LAVA_WEIGHT.get(),Balance.MASTER_PISTON_WEIGHT.get(),Balance.MASTER_SAW_WEIGHT.get()};
        Attack[] values=Attack.values();
        for(int i=0;i<weights.length;i++) { roll-=weights[i]; if(roll<0) return values[i]; }
        return Attack.SAW;
    }

    /** Only hostile, living, visible targets inside the configured radius; never players or pets. */
    public static LivingEntity selectTarget(ServerPlayer player) {
        double radius=Balance.MASTER_RADIUS.get();
        List<LivingEntity> candidates=new ArrayList<>();
        for(LivingEntity target:player.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(radius))) {
            if(target==player || !target.isAlive() || target.isSpectator()) continue;
            if(target instanceof Player) continue;
            if(!(target instanceof Enemy) && target.getType().getCategory()!=net.minecraft.world.entity.MobCategory.MONSTER) continue;
            if(target instanceof TamableAnimal tame && tame.isTame()) continue;
            if(target instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID())) continue;
            if(DamageSources.isFriendlyFireBetween(player,target)) continue;
            if(target.distanceToSqr(player)>radius*radius || !player.hasLineOfSight(target)) continue;
            candidates.add(target);
        }
        if(candidates.isEmpty()) return null;
        return candidates.get(player.level().random.nextInt(candidates.size()));
    }

    public static int attackCount(ServerPlayer player) { return player.getPersistentData().getInt(ATTACK_COUNT); }
    public static String lastAttack(ServerPlayer player) { return player.getPersistentData().getString(LAST_ATTACK); }

    /** Resolve once per attack event. Delayed segments share the same scaled total. */
    public static double damageMultiplier(ServerPlayer player) {
        var effect=player.getEffect(Content.MECHANICAL_MASTER_EFFECT);
        int level=effect==null?1:effect.getAmplifier()+1;
        return Content.MECHANICAL_MASTER.get().getSpellPower(level,player)*Balance.DAMAGE_SCALE.get();
    }

    /** Scheduler and tests share the same implementation. */
    public static void runAttack(ServerLevel level,ServerPlayer player,LivingEntity target,Attack attack) {
        Vec3 aim=target.getBoundingBox().getCenter();
        Vec3 flat=new Vec3(target.getX()-player.getX(),0,target.getZ()-player.getZ());
        Vec3 forward=flat.lengthSqr()<1.0E-4?new Vec3(0,0,1):flat.normalize();
        Vec3 side=new Vec3(-forward.z,0,forward.x);
        double power=damageMultiplier(player);
        switch(attack) {
            case PRESS -> {
                var visual=visual(level,player,MechanicalEntity.MASTER_PRESS,target.position(),forward);
                hitLater(level,6,player,target,Balance.MASTER_PRESS_DAMAGE.get()*power,visual,true,success->{
                    if(!success) return;
                    int ticks=Balance.MASTER_PRESS_DEBUFF_TICKS.get();
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,ticks,0));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,ticks,4));
                    level.sendParticles(ParticleTypes.CLOUD,aim.x,aim.y,aim.z,18,.4,.4,.4,.08);
                });
            }
            case SWORD -> {
                Vec3 base=target.position().add(side.scale(1.25)).add(0,1.0,0);
                var visual=visual(level,player,MechanicalEntity.MASTER_SWORD,base,aim.subtract(base));
                hitLater(level,4,player,target,Balance.MASTER_SWORD_DAMAGE.get()*power,visual,true,success->{
                    if(success) level.sendParticles(ParticleTypes.CRIT,aim.x,aim.y,aim.z,14,.3,.3,.3,.12);
                });
            }
            case DRILL -> {
                Vec3 base=aim.subtract(forward.scale(3.2)).add(0,.65,0);
                Vec3 route=aim.subtract(base);
                var visual=visual(level,player,MechanicalEntity.MASTER_DRILL,base,route);
                int segments=Balance.MASTER_DRILL_SEGMENTS.get();
                float per=(float)(Balance.MASTER_DRILL_DAMAGE.get()*power/segments);
                for(int i=0;i<segments;i++) flightHitLater(level,MasterFlight.IMPACT_TICKS+i*3,player,target,per,visual,aim,success->{
                    if(success) {
                        Vec3 push=route.normalize().scale(.42);
                        target.push(push.x,push.y,push.z);
                        target.hurtMarked=true;
                        Vec3 hit=target.getBoundingBox().getCenter();
                        level.sendParticles(ParticleTypes.CRIT,hit.x,hit.y,hit.z,6,.25,.25,.25,.1);
                    }
                });
            }
            case LAVA -> {
                Vec3 base=target.position().add(0,3.0,0);
                var visual=visual(level,player,MechanicalEntity.MASTER_LAVA,base,new Vec3(0,-1,0));
                hitLater(level,6,player,target,Balance.MASTER_LAVA_DAMAGE.get()*power,visual,true,success->{
                    if(!success) return;
                    target.igniteForSeconds(Balance.MASTER_LAVA_FIRE_TICKS.get()/20f);
                    for(int i=0;i<16;i++) level.sendParticles(ParticleTypes.FLAME,
                            aim.x+(level.random.nextDouble()-.5)*.7,aim.y+.3,aim.z+(level.random.nextDouble()-.5)*.7,
                            1,0,.08,0,.01);
                    for(int i=0;i<6;i++) level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(1f,.35f,.05f),1.1f),aim.x,aim.y+.4,aim.z,1,.3,.1,.3,0);
                });
            }
            case PISTON -> {
                Vec3 base=target.position().add(side.scale(1.6)).add(0,.7,0);
                var visual=visual(level,player,MechanicalEntity.MASTER_PISTON,base,aim.subtract(base));
                hitLater(level,3,player,target,Balance.MASTER_PISTON_DAMAGE.get()*power,visual,true,success->{
                    if(!success) return;
                    target.push(forward.x*.55,1.2,forward.z*.55);
                    target.hurtMarked=true;
                    level.sendParticles(ParticleTypes.CLOUD,aim.x,aim.y,aim.z,10,.2,.2,.2,.05);
                });
            }
            case SAW -> {
                Vec3 base=aim.add(side.scale(3.2));
                var visual=visual(level,player,MechanicalEntity.MASTER_SAW,base,aim.subtract(base));
                int segments=Balance.MASTER_SAW_SEGMENTS.get();
                float per=(float)(Balance.MASTER_SAW_DAMAGE.get()*power/segments);
                for(int i=0;i<segments;i++) flightHitLater(level,MasterFlight.IMPACT_TICKS+i*4,player,target,per,visual,aim,success->{
                    if(success) level.sendParticles(ParticleTypes.CRIT,aim.x,aim.y,aim.z,8,.3,.25,.3,.08);
                });
            }
        }
    }

    /** A short committed flight, not unlimited homing; dodging the contact area avoids the hit. */
    private static void flightHitLater(ServerLevel level,int delay,ServerPlayer player,LivingEntity target,double damage,
                                       MechanicalEntity visual,Vec3 contact,java.util.function.Consumer<Boolean> after) {
        ServerSpellTasks.runLater(level,delay,()->{
            if(!validFlightTarget(level,player,target,contact) || damage<=0) return;
            boolean success=ServerSpellTasks.authorizedHit(target,(float)damage,
                    Content.MECHANICAL_MASTER.get().getDamageSource(visual!=null?visual:player,player));
            if(after!=null) after.accept(success);
        });
    }
    private static boolean validFlightTarget(ServerLevel level,ServerPlayer player,LivingEntity target,Vec3 contact) {
        double radius=Balance.MASTER_RADIUS.get();
        return player.isAlive() && !player.isRemoved() && player.level()==level
                && target.isAlive() && !target.isRemoved() && target.level()==level
                && target.getBoundingBox().inflate(1.6).contains(contact)
                && target.distanceToSqr(player)<=radius*radius && player.hasLineOfSight(target)
                && !DamageSources.isFriendlyFireBetween(player,target);
    }

    private static MechanicalEntity visual(ServerLevel level,ServerPlayer player,int kind,Vec3 position,Vec3 direction) {
        int active=level.getEntitiesOfClass(MechanicalEntity.class,player.getBoundingBox().inflate(64),
                e->e.kind()>=MechanicalEntity.MASTER_PRESS && e.kind()<=MechanicalEntity.MASTER_SAW).size();
        if(active>=Balance.MASTER_VISUAL_CAP.get()) return null;
        return MechanicalEntity.visual(player,kind,position,24,direction);
    }

    private static void hitLater(ServerLevel level,int delay,ServerPlayer player,LivingEntity target,double damage,
                                 MechanicalEntity visual,boolean authorized,java.util.function.Consumer<Boolean> after) {
        ServerSpellTasks.runLater(level,delay,()->{
            if(!player.isAlive() || player.level()!=level || !target.isAlive() || target.level()!=level) { if(after!=null) after.accept(false); return; }
            if(damage<=0) { if(after!=null) after.accept(false); return; }
            var source=Content.MECHANICAL_MASTER.get().getDamageSource(visual!=null?visual:player,player);
            boolean success=authorized
                    ? ServerSpellTasks.authorizedHit(target,(float)damage,source)
                    : DamageSources.applyDamage(target,(float)damage,source);
            if(after!=null) after.accept(success);
        });
    }
    private MechanicalMasterHandler() {}
}
