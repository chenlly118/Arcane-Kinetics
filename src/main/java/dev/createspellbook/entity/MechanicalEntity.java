package dev.createspellbook.entity;

import com.simibubi.create.AllSoundEvents;
import dev.createspellbook.Content;
import dev.createspellbook.Balance;
import dev.createspellbook.devices.*;
import dev.createspellbook.processing.DrillMining;
import dev.createspellbook.processing.IndustrialProcessing;
import dev.createspellbook.processing.ProcessingBudget;
import dev.createspellbook.processing.ServerSpellTasks;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Server-authoritative travelling constructs and finite animated spell visuals. */
public final class MechanicalEntity extends Projectile {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(MechanicalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<org.joml.Vector3f> DIRECTION=SynchedEntityData.defineId(MechanicalEntity.class,EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Long> START=SynchedEntityData.defineId(MechanicalEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DELAY=SynchedEntityData.defineId(MechanicalEntity.class,EntityDataSerializers.INT);
    public static final int DRILL = 4;
    public static final int BARRAGE = 6;
    /** Batch 2 visual-only kinds; they never apply damage from the entity tick. */
    public static final int EJECTOR = 7;
    public static final int MECHANIC_ARM = 8;
    public static final int MASTER_PRESS = 9;
    public static final int MASTER_SWORD = 10;
    public static final int MASTER_DRILL = 11;
    public static final int MASTER_LAVA = 12;
    public static final int MASTER_PISTON = 13;
    public static final int MASTER_SAW = 14;
    private float damage;
    private String spell = "create_spellbook:gear_missile";
    private int life = 40;
    private int spellLevel = 1;
    private ProcessingBudget budget;
    private final Map<UUID, Integer> lastHits = new HashMap<>();
    private final Set<BlockPos> drilled=new HashSet<>();
    private boolean slammed;
    public MechanicalEntity(EntityType<? extends MechanicalEntity> type, Level level) { super(type, level); }
    public static MechanicalEntity launch(LivingEntity caster, AbstractSpell spell, int spellLevel, int kind, float damage, double speed) {
        var entity = new MechanicalEntity(Content.MECHANICAL_ENTITY.get(), caster.level());
        entity.setOwner(caster); entity.spell = spell.getSpellId(); entity.spellLevel = spellLevel;
        entity.entityData.set(KIND, kind); entity.damage = damage;
        entity.setPos(caster.getEyePosition().add(caster.getLookAngle().scale(0.5)));
        entity.setDeltaMovement(caster.getLookAngle().scale(speed));
        if(kind==2) {
            Vec3 horizontal=new Vec3(caster.getLookAngle().x,0,caster.getLookAngle().z).normalize();
            entity.setPos(caster.position().add(horizontal.scale(1.2)).add(0,.05,0));
            entity.setDeltaMovement(horizontal.scale(speed));
        }
        entity.refreshDimensions();
        caster.level().addFreshEntity(entity);
        return entity;
    }
    public int kind() { return entityData.get(KIND); }
    /** Independent of Projectile.lerpMotion: a stationary spawn's zero motion resets vanilla rotation. */
    public Vec3 castDirection() { var d=entityData.get(DIRECTION);return new Vec3(d.x,d.y,d.z); }
    public float visualAge(float partial) { return Math.max(0,level().getGameTime()-entityData.get(START)+partial); }
    public int pressDelay() { return entityData.get(DELAY); }
    @Override public EntityDimensions getDimensions(Pose pose) { return kind()==2 ? EntityDimensions.scalable(1.4f,1.4f):kind()==BARRAGE?EntityDimensions.scalable(4,7):super.getDimensions(pose); }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) { super.onSyncedDataUpdated(key); if(KIND.equals(key)) refreshDimensions(); }
    public MechanicalEntity processing(ProcessingBudget budget,double range) {
        this.budget=budget; return lifetime(range);
    }
    /** Flight lifetime derived from the configured range and the actual launch speed. */
    public MechanicalEntity lifetime(double range) {
        life=(int)Math.ceil(range/Math.max(.1,getDeltaMovement().length())); return this;
    }
    public static MechanicalEntity visual(LivingEntity caster,int kind,Vec3 position,int ticks) {
        var entity=new MechanicalEntity(Content.MECHANICAL_ENTITY.get(),caster.level());
        entity.setOwner(caster); entity.entityData.set(KIND,kind); entity.setPos(position); entity.life=ticks;
        // Stationary visuals still need the cast orientation in their spawn packet.
        entity.setYRot(caster.getYRot()); entity.setXRot(caster.getXRot());
        Vec3 direction=caster.getLookAngle();
        entity.entityData.set(DIRECTION,new org.joml.Vector3f((float)direction.x,(float)direction.y,(float)direction.z));
        entity.entityData.set(START,caster.level().getGameTime());
        caster.level().addFreshEntity(entity);
        return entity;
    }
    /** Visual with an explicit direction so looking straight up/down cannot erase the cast yaw. */
    public static MechanicalEntity visual(LivingEntity caster,int kind,Vec3 position,int ticks,Vec3 direction) {
        var entity=visual(caster,kind,position,ticks);
        entity.entityData.set(DIRECTION,new org.joml.Vector3f((float)direction.x,(float)direction.y,(float)direction.z));
        return entity;
    }
    public static MechanicalEntity press(LivingEntity caster,AbstractSpell spell,int spellLevel,Vec3 center,int delay,float damage) {
        return press(caster,spell,spellLevel,center,delay,damage,horizontal(caster));
    }
    public static MechanicalEntity press(LivingEntity caster,AbstractSpell spell,int spellLevel,Vec3 center,int delay,float damage,Vec3 direction) {
        var entity=visual(caster,BARRAGE,center,delay+28,direction);
        entity.spell=spell.getSpellId();entity.spellLevel=spellLevel;entity.damage=damage;entity.entityData.set(DELAY,delay);
        entity.refreshDimensions();return entity;
    }
    /** Shared processing budget for one paid press-barrage cast; every head uses the same object. */
    public MechanicalEntity pressBudget(ProcessingBudget budget) { this.budget=budget; return this; }
    private static Vec3 horizontal(LivingEntity caster) {
        float yaw=caster.getYRot()*(float)(Math.PI/180d);
        var direction=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
        return direction.lengthSqr()<1.0E-6?new Vec3(0,0,1):direction.normalize();
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND,0);builder.define(DIRECTION,new org.joml.Vector3f(0,0,1));builder.define(START,0L);builder.define(DELAY,0);
    }
    @Override public void tick() {
        super.tick();
        Vec3 start = position(), end = start.add(getDeltaMovement());
        if (!level().isClientSide) {
            if (--life <= 0 || getOwner() == null || !getOwner().isAlive() || !level().hasChunkAt(blockPosition())) { discard(); return; }
            if(kind()==3 || kind()==5 || kind()>=EJECTOR) return;
            if(kind()==BARRAGE) { tickPress();return; }
            if(!level().hasChunkAt(net.minecraft.core.BlockPos.containing(end))) { discard(); return; }
            if(kind()==DRILL) { tickDrill(start,end); return; }
            if(kind()==2) {
                Vec3 velocity=getDeltaMovement();
                move(MoverType.SELF,velocity);
                if(horizontalCollision) { discard(); return; }
                end=position();
                setDeltaMovement(velocity.x,onGround()?0:Math.max(-.6,velocity.y-.08),velocity.z);
                if(budget!=null && getOwner() instanceof LivingEntity caster)
                    IndustrialProcessing.around((net.minecraft.server.level.ServerLevel)level(),caster,end,1.4,IndustrialProcessing.Mode.CRUSH,budget);
                hitTargets(start,end,1.05); return;
            }
            HitResult block = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            boolean deviceContact=block instanceof BlockHitResult contact && dev.createspellbook.devices.SpellTarget.at(level(),contact.getBlockPos())!=null;
            if(deviceContact && block instanceof BlockHitResult contact) {
                var registered=SpellRegistry.REGISTRY.get(ResourceLocation.parse(spell));
                if(registered!=null)SpellDeviceBridge.mechanicalContact(this,dev.createspellbook.devices.SpellTarget.at(level(),contact.getBlockPos()),registered,spellLevel,damage,true);
            } else end = block.getLocation();
            if(kind()==1 && budget!=null && getOwner() instanceof LivingEntity caster) {
                for(double step=0;step<=1;step+=.5) IndustrialProcessing.around((net.minecraft.server.level.ServerLevel)level(),caster,start.lerp(end,step),.75,IndustrialProcessing.Mode.SAW,budget);
            }
            hitTargets(start,end,.4);
            if (!deviceContact && block.getType() != HitResult.Type.MISS) {
                if(kind()==1 && getOwner() instanceof ServerPlayer player && block instanceof BlockHitResult hit)
                    ServerSpellTasks.fell(player,hit.getBlockPos());
                impactSound(end); discard();
            }
        }
        setPos(end);
    }
    /** Pierce entities once each and sweep the full flight segment without stopping at obstacles. */
    private void tickDrill(Vec3 start,Vec3 end) {
        hitTargets(start,end,.35);
        boolean mined=false;
        if(getOwner() instanceof ServerPlayer player) {
            var box=new AABB(start,end).inflate(.22);
            for(var mutable:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))) {
                var pos=mutable.immutable();
                if(drilled.contains(pos) || !level().hasChunkAt(pos)) continue;
                var blockBox=new AABB(pos).inflate(.22);
                if(!blockBox.contains(start) && blockBox.clip(start,end).isEmpty()) continue;
                drilled.add(pos);
                mined|=DrillMining.mine(player,pos,spellLevel);
            }
        }
        if(mined) drillImpact(end);
        setPos(end);
    }
    private void tickPress() {
        if(slammed || visualAge(0)<pressDelay()+8) return;
        slammed=true;
        if(!(getOwner() instanceof LivingEntity caster)) return;
        var registered=SpellRegistry.REGISTRY.get(ResourceLocation.parse(spell));
        if(registered==null) return;
        double radius=Balance.PRESS_RADIUS.get();
        for(var target:level().getEntitiesOfClass(LivingEntity.class,new AABB(position(),position()).inflate(radius,3,radius))) {
            if(target==caster || !target.isAlive() || DamageSources.isFriendlyFireBetween(caster,target)) continue;
            // Impacts do not damage through walls between the head and its target.
            Vec3 from=position().add(0,1,0),to=target.getBoundingBox().getCenter();
            var hit=level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
            if(hit.getType()!=HitResult.Type.MISS && hit.getLocation().distanceToSqr(to)>.1) continue;
            if(DamageSources.applyDamage(target,damage,registered.getDamageSource(this,caster))) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,40,0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,4));
            }
        }
        // Each head processes at its real impact point, but all heads share the cast's
        // single budget and product token so overlapping areas cannot duplicate work.
        if(budget!=null)
            IndustrialProcessing.around((net.minecraft.server.level.ServerLevel)level(),caster,position(),
                    Balance.PRESS_RADIUS.get(),IndustrialProcessing.Mode.PRESS,budget);
        AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.playOnServer(level(),blockPosition(),1.3f,.8f);
    }
    private void hitTargets(Vec3 start,Vec3 end,double radius) {
            List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(radius+.2),
                    target -> target != getOwner() && target.isAlive() && !DamageSources.isFriendlyFireBetween(getOwner(),target));
            targets.sort(Comparator.comparingDouble(target -> target.distanceToSqr(start)));
            for (LivingEntity target : targets) {
                if (!target.getBoundingBox().inflate(radius).contains(start) && target.getBoundingBox().inflate(radius).clip(start, end).isEmpty()) continue;
                if (kind()==DRILL ? lastHits.containsKey(target.getUUID()) : tickCount-lastHits.getOrDefault(target.getUUID(),-100)<10) continue;
                var registered = SpellRegistry.REGISTRY.get(ResourceLocation.parse(spell));
                if(registered!=null && target instanceof SpellDeviceProbe probe) {
                    SpellDeviceBridge.mechanicalContact(this,probe.device(),registered,spellLevel,damage,kind()!=2);
                    lastHits.put(target.getUUID(),tickCount);continue;
                }
                if (registered != null && DamageSources.applyDamage(target, damage, registered.getDamageSource(this, getOwner()))) {
                    lastHits.put(target.getUUID(), tickCount);
                    if(kind()==2) target.knockback(1.25,-getDeltaMovement().x,-getDeltaMovement().z);
                    if(kind()==DRILL) drillImpact(target.position());
                }
                if(kind()==0) { impactSound(target.position()); discard(); break; }
            }
    }
    private void impactSound(Vec3 at) {
        if(kind()==0) level().playSound(null,at.x,at.y,at.z,
                io.redspace.ironsspellbooks.registries.SoundRegistry.ARCANE_IMPACT.get(),SoundSource.PLAYERS,.8f,1f);
    }
    private void drillImpact(Vec3 at) {
        level().playSound(null,at.x,at.y,at.z,AllSoundEvents.CRUSHING_1.getMainEvent(),SoundSource.PLAYERS,.65f,1.25f);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putInt("Kind", kind()); tag.putFloat("Damage", damage);
        tag.putString("Spell", spell); tag.putInt("Life", life); tag.putInt("SpellLevel", spellLevel);
        Vec3 d=castDirection();tag.putDouble("DirectionX",d.x);tag.putDouble("DirectionY",d.y);tag.putDouble("DirectionZ",d.z);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); entityData.set(KIND, tag.getInt("Kind")); damage = tag.getFloat("Damage");
        spell = tag.getString("Spell"); life = Math.clamp(tag.getInt("Life"), 0, 100); spellLevel = tag.getInt("SpellLevel");
    }
}
