package dev.createspellbook.spell;

import dev.createspellbook.Content;
import dev.createspellbook.CreateSpellbook;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Applies the stored overload multiplier to the next fall damage that actually
 * lands.  Zero, canceled, immune or otherwise absorbed hits leave the state intact;
 * only a positive post-damage result consumes it.
 */
@EventBusSubscriber(modid=CreateSpellbook.ID)
public final class EjectionEvents {
    public static final String MULTIPLIER_KEY="CSB_EjectionMultiplier";

    @SubscribeEvent public static void incoming(LivingIncomingDamageEvent event) {
        if(!event.getSource().is(DamageTypeTags.IS_FALL)) return;
        LivingEntity target=event.getEntity();
        if(target.getEffect(Content.EJECTION_OVERLOAD_EFFECT)==null) {
            // Drop stale data left by an expired state without changing this hit.
            target.getPersistentData().remove(MULTIPLIER_KEY);
            return;
        }
        float multiplier=target.getPersistentData().getFloat(MULTIPLIER_KEY);
        if(multiplier>1 && event.getAmount()>0) event.setAmount(event.getAmount()*multiplier);
    }

    @SubscribeEvent public static void post(LivingDamageEvent.Post event) {
        if(!event.getSource().is(DamageTypeTags.IS_FALL)) return;
        LivingEntity target=event.getEntity();
        if(event.getNewDamage()<=0 || target.getEffect(Content.EJECTION_OVERLOAD_EFFECT)==null) return;
        target.removeEffect(Content.EJECTION_OVERLOAD_EFFECT);
        target.getPersistentData().remove(MULTIPLIER_KEY);
    }
    private EjectionEvents() {}
}
