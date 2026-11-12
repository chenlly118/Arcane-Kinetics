package dev.createspellbook.spell;

import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * Project Create's blasting and haunting particle descriptions along the spell beam.
 *
 * {@code AllFanProcessingTypes.BLASTING.morphAirFlow} supplies the exact lava
 * gradient (0xFF4400..0xFF8855), its 0.5 alpha and the rare FLAME / lava block
 * extra particles.  Create's own {@code create:air_flow} particle cannot be sent
 * from a server stand-alone: it resolves a live fan source at the encoded block
 * position and discards itself without one.  The access implementation below is
 * therefore a thin adapter onto vanilla particle packets; the visual parameters
 * stay owned by the installed Create version.
 */
public final class FanAirflow {
    public static void spawn(ServerLevel level, Vec3 pos) {
        spawn(level,pos,AllFanProcessingTypes.BLASTING,9);
        spawn(level,pos,AllFanProcessingTypes.HAUNTING,5);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,pos.x,pos.y,pos.z,1,.28,.28,.28,.01);
    }

    private static void spawn(ServerLevel level, Vec3 pos,FanProcessingType type,int count) {
        List<ParticleOptions> extras = new ArrayList<>();
        int[] color = {0xFF6600};
        type.morphAirFlow(new FanProcessingType.AirFlowParticleAccess() {
            @Override public void setColor(int rgb) { color[0] = rgb; }
            @Override public void setAlpha(float alpha) { /* dust particles carry no alpha; scale is softened below */ }
            @Override public void spawnExtraParticle(ParticleOptions options, float size) { extras.add(options); }
        }, level.random);
        int rgb = color[0];
        // Slightly smaller dust approximates the translucent air-flow puffs.
        Vector3f tint = new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
        level.sendParticles(new DustParticleOptions(tint, .8f), pos.x, pos.y, pos.z, count, .45, .45, .45, 0);
        for (ParticleOptions extra : extras)
            level.sendParticles(extra, pos.x, pos.y, pos.z, 3, .35, .35, .35, .01);
    }

    public static AABB bounds(Vec3 origin,Vec3 forward,double range) {
        return new AABB(origin,origin.add(forward.scale(range))).inflate(range*.52);
    }

    public static boolean reaches(ServerLevel level,LivingEntity caster,Entity target,Vec3 origin,Vec3 forward,double range) {
        if(target==caster || target.noPhysics || target.isSpectator() || !target.isAlive())return false;
        var center=target.getBoundingBox().getCenter();
        var delta=center.subtract(origin);
        if(delta.lengthSqr()>range*range || delta.normalize().dot(forward)<=.86)return false;
        if(target instanceof LivingEntity living)return IndustrialSpell.movableTarget(caster,living);
        return level.clip(new ClipContext(origin,center,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,caster)).getType()==HitResult.Type.MISS;
    }

    public static void move(ServerLevel level,LivingEntity caster,Vec3 origin,Vec3 forward,double range) {
        boolean pull=caster.isShiftKeyDown();
        Vec3 direction=pull?forward.scale(-1):forward;
        for(var target:level.getEntities(caster,bounds(origin,forward,range),entity->entity instanceof LivingEntity || entity instanceof ItemEntity
                || entity.isPushable() && entity.getPassengers().isEmpty())) {
            if(!reaches(level,caster,target,origin,forward,range))continue;
            double resistance=target instanceof LivingEntity living?living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE):0;
            double speed=.55*(1-Math.clamp(resistance,0,1));
            if(pull) {
                double distance=target.getBoundingBox().getCenter().subtract(origin).dot(forward);
                speed=Math.min(speed,Math.max(0,(distance-.7)*.22));
            }
            var motion=target.getDeltaMovement();
            double adjustment=Math.clamp((speed-motion.dot(direction))*.32,0,.12);
            if(adjustment<=0 || speed<=0)continue;
            target.setDeltaMovement(motion.add(direction.scale(adjustment)));
            target.hasImpulse=true;
            target.hurtMarked=true;
        }
    }

    public static void haunt(LivingEntity target) {
        // Create's soul-fire exposure applies these effects for 30 and 20 ticks.
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,30,0,false,false));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,20,1,false,false));
    }
    private FanAirflow() {}
}
