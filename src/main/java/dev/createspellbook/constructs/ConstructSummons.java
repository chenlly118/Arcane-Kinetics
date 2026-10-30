package dev.createspellbook.constructs;

import dev.createspellbook.CreateSpellbook;
import io.redspace.ironsspellbooks.api.events.SpellSummonEvent;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Bounded native spell summons; no AI or ownership changes to unrelated mobs. */
@EventBusSubscriber(modid=CreateSpellbook.ID)
public final class ConstructSummons {
    static final String ROOT="CreateSpellbookMachineSummoner",EXPIRES="CreateSpellbookMachineSummonExpiry";
    public static boolean managed(Entity entity){return entity.getPersistentData().hasUUID(ROOT);}
    @SubscribeEvent public static void summoned(SpellSummonEvent<?> event){
        if(event.getCaster() instanceof ConstructCaster caster){
            var creature=event.getCreature();creature.getPersistentData().putUUID(ROOT,caster.getUUID());
            creature.getPersistentData().putLong(EXPIRES,creature.level().getGameTime()+1200);
            ConstructSafety.mark(creature,caster);
        }
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
        var entity=event.getEntity();if(event.getLevel().isClientSide || !managed(entity))return;
        if(!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel server)
                || !(server.getEntity(entity.getPersistentData().getUUID(ROOT)) instanceof ConstructCaster caster)
                || !(entity instanceof LivingEntity living) || !caster.trackSummon(living))event.setCanceled(true);
    }
    private ConstructSummons(){}
}
