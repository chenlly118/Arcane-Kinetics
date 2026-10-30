package dev.createspellbook.spell;

import dev.createspellbook.Balance;
import dev.createspellbook.Content;
import dev.createspellbook.entity.MechanicalEntity;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One-target vertical ejector: a transient Create weighted-ejector projection
 * launches the target with normal gravity/collision, then only its next valid
 * fall damage is multiplied once.
 */
public final class EjectionOverload {
    public static boolean cast(ServerLevel level,LivingEntity caster,int spellLevel,IndustrialSpell spell) {
        var hit=RaycastBuilder.begin(level,caster).range((float)spell.range()).checkForBlocks(true).bbInflation(.3f).build();
        if(!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)) return false;
        if(!IndustrialSpell.validTarget(caster,target)) return false;
        // Respect knockback immunity rather than dragging an anchor/boss upward.
        if(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)>=.99) return false;
        double height=Balance.ejectionHeight(spellLevel);
        double multiplier=Balance.ejectionMultiplier(spellLevel,spell.getEntityPowerMultiplier(caster));
        MechanicalEntity.visual(caster,MechanicalEntity.EJECTOR,target.position(),24,new Vec3(0,1,0));
        target.setDeltaMovement(0,launchVelocity(height),0);
        target.hasImpulse=true;target.hurtMarked=true;
        if(target instanceof ServerPlayer player) player.connection.send(new ClientboundSetEntityMotionPacket(target));
        target.removeEffect(Content.EJECTION_OVERLOAD_EFFECT);
        target.addEffect(new MobEffectInstance(Content.EJECTION_OVERLOAD_EFFECT,
                Balance.EJECTION_STATE_SECONDS.get()*20,Balance.effectAmplifier(spellLevel),false,true,true));
        target.getPersistentData().putFloat(EjectionEvents.MULTIPLIER_KEY,(float)multiplier);
        level.playSound(null,target.getX(),target.getY(),target.getZ(),SoundEvents.PISTON_EXTEND,SoundSource.PLAYERS,.8f,1.35f);
        level.sendParticles(ParticleTypes.CLOUD,target.getX(),target.getY()+.2,target.getZ(),8,.35,.15,.35,.08);
        return true;
    }

    /**
     * Solves the discrete living-entity trajectory (gravity 0.08, drag 0.98) so the
     * real apex matches the configured height instead of using an ideal parabola.
     */
    public static double launchVelocity(double height) {
        double low=0,high=4;
        for(int iteration=0;iteration<50;iteration++) {
            double velocity=(low+high)/2,dy=velocity,apex=0;
            for(int tick=0;tick<240;tick++) {
                dy=(dy-.08)*.98;
                if(dy<=0) break;
                apex+=dy;
            }
            if(apex<height) low=velocity; else high=velocity;
        }
        return (low+high)/2;
    }
    private EjectionOverload() {}
}
