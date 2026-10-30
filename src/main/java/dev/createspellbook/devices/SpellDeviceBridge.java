package dev.createspellbook.devices;

import dev.createspellbook.CreateSpellbook;
import io.redspace.ironsspellbooks.api.events.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

/** Public Iron/NeoForge adapters, plus one narrowly scoped block-impact adapter. */
@EventBusSubscriber(modid=CreateSpellbook.ID)
public final class SpellDeviceBridge {
    private static final String STAMP="CreateSpellbookDeviceCast";
    private static final LinkedHashMap<UUID,LinkedHashMap<String,Cast>> HISTORY=new LinkedHashMap<>();
    private static final StackWalker CAST_STACK=StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private record Cast(String spell,int level,long tick,UUID caster,UUID token) {}
    private static void remember(LivingEntity caster,String spell,int level,boolean starting) {
        if(caster.level().isClientSide)return;
        var spells=HISTORY.computeIfAbsent(caster.getUUID(),key->new LinkedHashMap<>());
        var prior=spells.get(spell);
        var token=!starting && prior!=null && casterData(caster).isCasting()?prior.token():UUID.randomUUID();
        spells.remove(spell);spells.put(spell,new Cast(spell,Math.max(0,level),caster.level().getGameTime(),caster.getUUID(),token));
        while(spells.size()>64)spells.remove(spells.keySet().iterator().next());
        while(HISTORY.size()>512)HISTORY.remove(HISTORY.keySet().iterator().next());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void preCast(SpellPreCastEvent event) {remember(event.getEntity(),event.getSpellId(),event.getSpellLevel(),true);}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void cast(SpellOnCastEvent event) {remember(event.getEntity(),event.getSpellId(),event.getSpellLevel(),false);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {HISTORY.remove(event.getEntity().getUUID());}

    public static void beginMachineCast(LivingEntity caster,AbstractSpell spell,int level){remember(caster,spell.getSpellId(),level,true);}
    /** An external integration can supply exact metadata for its own delayed effects. */
    public static void stamp(Entity effect,AbstractSpell spell,int level,LivingEntity caster) {
        var tag=new CompoundTag();tag.putString("Spell",spell.getSpellId());tag.putInt("Level",Math.max(0,level));
        tag.putLong("Tick",effect.level().getGameTime());tag.putUUID("Caster",caster.getUUID());
        var recent=HISTORY.get(caster.getUUID());var cast=recent==null?null:recent.get(spell.getSpellId());
        tag.putUUID("Cast",cast==null?UUID.randomUUID():cast.token());
        effect.getPersistentData().put(STAMP,tag);
    }
    private static LivingEntity owner(Entity effect) {
        Entity entity=effect instanceof Projectile projectile?projectile.getOwner():effect instanceof OwnableEntity owned?owned.getOwner():null;
        return entity instanceof LivingEntity living?living:null;
    }
    private static boolean executing(AbstractSpell spell) {
        // Delayed entity ticks have no original spell frame. Refuse to label a
        // delayed effect using whichever unrelated spell its owner is now casting.
        return CAST_STACK.walk(frames->frames.anyMatch(frame->frame.getDeclaringClass()==spell.getClass()));
    }
    private static MagicData casterData(LivingEntity caster) {
        return caster instanceof io.redspace.ironsspellbooks.api.entity.IMagicEntity magic?magic.getMagicData():MagicData.getPlayerMagicData(caster);
    }
    @SubscribeEvent public static void spawned(EntityJoinLevelEvent event) {
        var effect=event.getEntity();if(event.getLevel().isClientSide || effect.getPersistentData().contains(STAMP))return;
        var caster=owner(effect);if(caster==null)return;
        Cast found=null;var data=casterData(caster);
        if(data.isCasting())found=new Cast(data.getCastingSpellId(),data.getCastingSpellLevel(),event.getLevel().getGameTime(),caster.getUUID(),UUID.randomUUID());
        var recent=HISTORY.get(caster.getUUID());
        if(recent!=null) for(var cast:recent.values()) {
            long age=event.getLevel().getGameTime()-cast.tick();
            if(age>=0 && age<=1 && (found==null || cast.tick()>=found.tick()))found=cast;
        }
        if(found!=null) {
            var spell=SpellRegistry.getSpell(found.spell());
            if(spell.getSpellId().equals(found.spell()) && executing(spell))stamp(effect,spell,found.level(),caster);
        }
    }
    private static int levelFor(SpellDamageSource source) {
        var direct=source.getDirectEntity();String id=source.spell().getSpellId();
        if(direct!=null && direct.getPersistentData().contains(STAMP)) {
            var tag=direct.getPersistentData().getCompound(STAMP);
            if(tag.getString("Spell").equals(id))return Math.max(0,tag.getInt("Level"));
        }
        if(source.getEntity() instanceof LivingEntity caster) {
            if(!executing(source.spell()))return 0;
            var data=casterData(caster);
            if(data.isCasting() && data.getCastingSpellId().equals(id))return Math.max(0,data.getCastingSpellLevel());
            var history=HISTORY.get(caster.getUUID());var recent=history==null?null:history.get(id);
            if(recent!=null) {long age=caster.level().getGameTime()-recent.tick();if(age>=0 && age<=2)return recent.level();}
        }
        return 0; // Never infer an old effect's level from a different current spell.
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void damage(SpellDamageEvent event) {
        if(!(event.getEntity() instanceof SpellDeviceProbe probe) || probe.level().isClientSide)return;
        applyDamage(probe,event.getSpellDamageSource(),event.getAmount());
        // Probe is a receiver, never a target granting lifesteal, hurt frames or XP.
        event.setCanceled(true);
    }
    private static void applyDamage(SpellDeviceProbe probe,SpellDamageSource source,float amount) {
        if(!Float.isFinite(amount) || amount<0)return;
        if(dev.createspellbook.constructs.ConstructSafety.retaliation(source.getDirectEntity()))return;
        var device=probe.device();if(device==null)return;
        var caster=source.getEntity() instanceof LivingEntity living?living:null;
        device.acceptSpell(new SpellInteraction(source.spell().getSpellResource(),source.spell().getSchoolType().getId(),levelFor(source),amount,
                caster,source.getEntity()==null?null:source.getEntity().getUUID(),UUID.randomUUID(),
                source.getDirectEntity() instanceof AbstractMagicProjectile projectile?projectile.getUUID():null,processingCast(source)));
    }
    private static UUID processingCast(SpellDamageSource source){
        var direct=source.getDirectEntity();
        if(direct!=null && direct.getPersistentData().contains(STAMP)){
            var tag=direct.getPersistentData().getCompound(STAMP);if(tag.hasUUID("Cast"))return tag.getUUID("Cast");
        }
        if(direct!=null && direct!=source.getEntity())return direct.getUUID();
        if(source.getEntity() instanceof LivingEntity caster && executing(source.spell())){
            var history=HISTORY.get(caster.getUUID());var cast=history==null?null:history.get(source.spell().getSpellId());
            if(cast!=null)return cast.token();
        }return null;
    }
    /** Exact metadata owned by this addon's travelling mechanical spells. */
    public static void mechanicalContact(Entity effect,SpellTarget device,AbstractSpell spell,int level,float damage,boolean oncePerFlight) {
        if(device==null || effect.level().isClientSide || dev.createspellbook.constructs.ConstructSafety.retaliation(effect))return;
        var caster=owner(effect);
        device.acceptSpell(new SpellInteraction(spell.getSpellResource(),spell.getSchoolType().getId(),Math.max(0,level),damage,
                caster,caster==null?null:caster.getUUID(),UUID.randomUUID(),oncePerFlight?effect.getUUID():null,effect.getUUID()));
    }
    public static void directDamage(SpellDeviceProbe probe,DamageSource source,float amount) {
        if(source instanceof SpellDamageSource spell)applyDamage(probe,spell,amount);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void impact(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event) {
        if(!(event.getProjectile() instanceof AbstractMagicProjectile projectile) || projectile.level().isClientSide)return;
        if(event.getRayTraceResult() instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof SpellDeviceProbe probe) {
            projectileContact(projectile,probe.device());event.setCanceled(true);
        }
    }
    private static void projectileContact(AbstractMagicProjectile projectile,SpellTarget device) {
        if(device==null || dev.createspellbook.constructs.ConstructSafety.retaliation(projectile)
                || device.hasSeenProjectile(projectile.getUUID()) || !projectile.getPersistentData().contains(STAMP))return;
        var tag=projectile.getPersistentData().getCompound(STAMP);var spell=SpellRegistry.getSpell(tag.getString("Spell"));
        if(!spell.getSpellId().equals(tag.getString("Spell")))return;
        float damage=projectile.getDamage();if(!Float.isFinite(damage) || damage<0)return;
        var caster=owner(projectile);UUID casterId=tag.hasUUID("Caster")?tag.getUUID("Caster"):caster==null?null:caster.getUUID();
        var probe=device.probe();
        if(probe!=null) {
            // Preserve other mods' public SpellDamageEvent adjustments/cancellation.
            // Our receiver cancels the final health hit, so no lifesteal or iFrames.
            io.redspace.ironsspellbooks.damage.DamageSources.applyDamage(probe,damage,spell.getDamageSource(projectile,caster));return;
        }
        device.acceptSpell(new SpellInteraction(spell.getSpellResource(),spell.getSchoolType().getId(),Math.max(0,tag.getInt("Level")),damage,
                caster,casterId,UUID.randomUUID(),projectile.getUUID(),tag.hasUUID("Cast")?tag.getUUID("Cast"):projectile.getUUID()));
    }
    /** Iron 3.16.3 omits ProjectileImpactEvent for blocks; called only by its scoped Mixin. */
    public static boolean blockImpact(AbstractMagicProjectile projectile,HitResult hit) {
        if(!(hit instanceof BlockHitResult blockHit) || !projectile.level().hasChunkAt(blockHit.getBlockPos())
                || SpellTarget.at(projectile.level(),blockHit.getBlockPos())==null)return false;
        var device=SpellTarget.at(projectile.level(),blockHit.getBlockPos());
        if(!projectile.level().isClientSide)projectileContact(projectile,device);
        return true; // Suppress only this device's consuming/ricocheting impact; travel remains vanilla.
    }
    private SpellDeviceBridge() {}
}
