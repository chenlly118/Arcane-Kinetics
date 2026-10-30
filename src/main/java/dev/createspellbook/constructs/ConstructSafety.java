package dev.createspellbook.constructs;

import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.devices.SpellDeviceProbe;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import java.util.UUID;

@EventBusSubscriber(modid=CreateSpellbook.ID)
public final class ConstructSafety {
    private static final ThreadLocal<ConstructCaster> ACTIVE=new ThreadLocal<>();
    private static final String TARGET="CreateSpellbookRetaliationTarget";
    private static final String SEALED="CreateSpellbookSealedManaShot";
    private static final String ORIGIN="CreateSpellbookMachineOrigin";
    private static final String CONSTRUCT="CreateSpellbookConstructShot",OWNER="CreateSpellbookConstructOwner";
    public static boolean retaliation(Entity effect) {return effect!=null && (effect.getPersistentData().getBoolean(SEALED) || effect.getPersistentData().hasUUID(TARGET));}
    public static boolean sealedCaster(ConstructCaster caster) {return caster.host() instanceof ArcaneAutoCasterBlockEntity;}
    public static void duringCast(ConstructCaster caster,Runnable action){
        var previous=ACTIVE.get();ACTIVE.set(caster);try{action.run();}finally{if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);}
    }
    private static boolean orphanAllowed(Entity direct,Entity target) {
        if(target instanceof Player || target instanceof ConstructCaster || target instanceof OwnableEntity || ConstructSummons.managed(target))return false;
        if(direct.getPersistentData().hasUUID(OWNER)) {
            var owner=direct.level().getPlayerByUUID(direct.getPersistentData().getUUID(OWNER));if(owner!=null && owner.isAlliedTo(target))return false;
        }
        return target instanceof SpellDeviceProbe || target instanceof LivingEntity living && (living instanceof Enemy || living.getType().getCategory()==MobCategory.MONSTER);
    }
    public static boolean protectedFrom(ConstructCaster caster,Entity target) {
        if(target==caster || target instanceof Player || target instanceof ConstructCaster || target instanceof OwnableEntity || ConstructSummons.managed(target))return true;
        var owner=caster.getOwner();return caster.isAlliedTo(target) || owner!=null && owner.isAlliedTo(target);
    }
    private static boolean canHit(ConstructCaster caster,Entity direct,Entity target) {
        if(protectedFrom(caster,target))return false;
        if(sealedCaster(caster)) {
            return target instanceof LivingEntity living && ((ArcaneAutoCasterBlockEntity)caster.host()).validEnemy(living);
        }
        return target instanceof SpellDeviceProbe || target instanceof LivingEntity living && (living instanceof Enemy || living.getType().getCategory()==MobCategory.MONSTER);
    }
    @SubscribeEvent public static void spawned(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide)return;
        var entity=event.getEntity();Entity owner=entity instanceof TraceableEntity trace?trace.getOwner():entity instanceof OwnableEntity own?own.getOwner():null;
        var root=owner instanceof ConstructCaster caster?caster:ACTIVE.get();
        if(root!=null){
            mark(entity,root);
            if(entity instanceof LivingEntity living && !(living instanceof ConstructCaster) && !(living instanceof SpellDeviceProbe)){
                entity.getPersistentData().putUUID(ConstructSummons.ROOT,root.getUUID());entity.getPersistentData().putLong(ConstructSummons.EXPIRES,entity.level().getGameTime()+1200);
                if(!root.trackSummon(living))event.setCanceled(true);
            }
        }
        else if(owner!=null && owner.getPersistentData().getBoolean(CONSTRUCT)){
            for(String key:java.util.List.of(CONSTRUCT,OWNER,SEALED,TARGET,ORIGIN))
                if(owner.getPersistentData().contains(key))entity.getPersistentData().put(key,owner.getPersistentData().get(key).copy());
        }
    }
    public static void mark(Entity entity,ConstructCaster caster){
        var tag=entity.getPersistentData();tag.putBoolean(CONSTRUCT,true);
        if(caster.getOwnerUUID()!=null)tag.putUUID(OWNER,caster.getOwnerUUID());
        if(sealedCaster(caster)){
            tag.putBoolean(SEALED,true);tag.putLong(ORIGIN,net.minecraft.core.BlockPos.containing(caster.host().constructPosition()).asLong());
            if(caster.session.target()!=null)tag.putUUID(TARGET,caster.session.target().getUUID());
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void impact(ProjectileImpactEvent event) {
        if(event.getRayTraceResult() instanceof EntityHitResult hit &&
                (event.getProjectile().getOwner() instanceof ConstructCaster caster && !canHit(caster,event.getProjectile(),hit.getEntity())
                || event.getProjectile().getPersistentData().getBoolean(CONSTRUCT) && !orphanAllowed(event.getProjectile(),hit.getEntity())
                || retaliation(event.getProjectile()) && !orphanSafe(event.getProjectile(),hit.getEntity())))event.setCanceled(true);
    }
    private static boolean orphanSafe(Entity direct,Entity target) {
        if(!(target instanceof LivingEntity living) || !(living instanceof Enemy || living.getType().getCategory()==MobCategory.MONSTER)
                || target instanceof Player || target instanceof OwnableEntity || target instanceof ConstructCaster || ConstructSummons.managed(target))return false;
        var tag=direct.getPersistentData();
        if(tag.getBoolean(SEALED) && tag.contains(ORIGIN)){
            var origin=net.minecraft.world.phys.Vec3.atCenterOf(net.minecraft.core.BlockPos.of(tag.getLong(ORIGIN)));
            int range=dev.createspellbook.Balance.AUTO_RANGE.get();return target.distanceToSqr(origin)<=range*(double)range;
        }
        return tag.hasUUID(TARGET) && target.getUUID().equals(tag.getUUID(TARGET));
    }
    private static boolean cancel(DamageSource source,LivingEntity target) {
        return source.getEntity() instanceof ConstructCaster caster && !canHit(caster,source.getDirectEntity(),target)
                || source.getDirectEntity()!=null && source.getDirectEntity().getPersistentData().getBoolean(CONSTRUCT) && !orphanAllowed(source.getDirectEntity(),target)
                || retaliation(source.getDirectEntity()) && !orphanSafe(source.getDirectEntity(),target);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void damage(SpellDamageEvent event) {if(cancel(event.getSpellDamageSource(),event.getEntity()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void incoming(LivingIncomingDamageEvent event) {if(cancel(event.getSource(),event.getEntity()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void lightning(EntityStruckByLightningEvent event){
        var bolt=event.getLightning();
        if(bolt.getPersistentData().getBoolean(CONSTRUCT) && (!orphanAllowed(bolt,event.getEntity())
                || retaliation(bolt) && !orphanSafe(bolt,event.getEntity())))event.setCanceled(true);
    }
    private ConstructSafety() {}
}
