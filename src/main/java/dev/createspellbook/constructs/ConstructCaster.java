package dev.createspellbook.constructs;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Real non-player caster; no fake player, pathfinding goals or player-only cast API. */
public abstract class ConstructCaster extends PathfinderMob implements IMagicEntity,OwnableEntity {
    private static final EntityDataAccessor<Long> LAST_RELEASE=SynchedEntityData.defineId(ConstructCaster.class,EntityDataSerializers.LONG);
    private final MagicData magic=new MagicData(true);
    private final java.util.Set<UUID> summons=new java.util.HashSet<>();
    public final CastingSession session=new CastingSession(this);
    protected ConstructCaster(EntityType<? extends PathfinderMob> type,Level level) {super(type,level);blocksBuilding=false;setNoAi(true);magic.setSyncedData(new SyncedSpellData(this));}
    public abstract ConstructHost host();
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {super.defineSynchedData(builder);builder.define(LAST_RELEASE,Long.MIN_VALUE);}
    public long lastRelease() {return entityData.get(LAST_RELEASE);}
    public void released() {entityData.set(LAST_RELEASE,level().getGameTime());}
    @Override public UUID getOwnerUUID() {var host=host();return host==null?null:host.ownerId();}
    @Override public boolean isAlliedTo(Entity entity) {
        if(entity instanceof ConstructCaster || entity==this || ConstructSummons.managed(entity))return true;
        var owner=getOwner();return entity==owner || owner!=null && owner.isAlliedTo(entity) || super.isAlliedTo(entity);
    }
    public void aim() {
        var target=session.target();if(target==null)return;
        Vec3 direction=target.getBoundingBox().getCenter().subtract(getEyePosition()).normalize();point(direction);
    }
    public void point(Vec3 direction) {
        setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));setXRot((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
        yHeadRot=getYRot();yBodyRot=getYRot();
    }
    @Override public void tick() {
        super.tick();if(!level().isClientSide && isAlive()){
            session.tick();
            if(tickCount%10==0 && level() instanceof net.minecraft.server.level.ServerLevel server && host()!=null && !summons.isEmpty()){
                int range=host() instanceof ArcaneAutoCasterBlockEntity?dev.createspellbook.Balance.AUTO_RANGE.get():dev.createspellbook.Balance.EMITTER_RANGE.get();
                java.util.function.Predicate<LivingEntity> visible=target->host() instanceof ArcaneCastingArmBlockEntity emitter
                        ?emitter.visible(target.getBoundingBox().getCenter(),null):((ArcaneAutoCasterBlockEntity)host()).validEnemy(target);
                var target=ConstructTargeting.nearest(this,host().constructPosition(),range,entity->ConstructTargeting.hostile(this,entity,host().constructPosition(),range,visible));
                setTarget(target);
                var iterator=summons.iterator();while(iterator.hasNext()){
                    var summoned=server.getEntity(iterator.next());
                    if(summoned==null || !summoned.isAlive()){iterator.remove();continue;}
                    if(level().getGameTime()>=summoned.getPersistentData().getLong(ConstructSummons.EXPIRES)){summoned.discard();iterator.remove();continue;}
                    if(summoned instanceof Mob mob)mob.setTarget(target);
                    ConstructSafety.mark(summoned,this);
                }
            }
        }
    }
    boolean trackSummon(LivingEntity entity){
        if(!summons.contains(entity.getUUID()) && summons.size()>=16)return false;
        summons.add(entity.getUUID());if(entity instanceof Mob mob)mob.setTarget(session.target());return true;
    }
    @Override public void remove(RemovalReason reason){
        if(!level().isClientSide && level() instanceof net.minecraft.server.level.ServerLevel server && summons!=null){
            for(var id:java.util.List.copyOf(summons)){var summoned=server.getEntity(id);if(summoned!=null)summoned.discard();}summons.clear();
        }
        super.remove(reason);
    }
    @Override public MagicData getMagicData() {return magic;}
    @Override public void setSyncedSpellData(SyncedSpellData data) {if(level().isClientSide)magic.setSyncedData(data);}
    @Override public boolean isCasting() {return session.active();}
    @Override public void initiateCastSpell(AbstractSpell spell,int level) {
        var selected=host()==null?java.util.Optional.<io.redspace.ironsspellbooks.api.spells.SpellData>empty():ScrollTemplate.read(host().scroll().getStackInSlot(0));
        if(selected.isPresent() && selected.get().getSpell()==spell && selected.get().getLevel()==level)session.begin(getTarget());
    }
    @Override public void cancelCast() {session.finish(true);}
    @Override public void castComplete() {session.finish(false);}
    // Stationary constructs intentionally do not implement movement, evasion or potions;
    // spells requiring these abilities are rejected by ScrollTemplate.supported.
    @Override public void notifyDangerousProjectile(Projectile projectile) {}
    @Override public boolean setTeleportLocationBehindTarget(int distance) {return false;}
    @Override public void setBurningDashDirectionData() {}
    @Override public boolean isDrinkingPotion() {return false;}
    @Override public boolean getHasUsedSingleAttack() {return false;}
    @Override public void setHasUsedSingleAttack(boolean used) {}
    @Override public void startDrinkingPotion() {}
    @Override public boolean isPushable() {return false;}
    @Override public void push(double x,double y,double z) {}
    @Override public void knockback(double strength,double x,double z) {}
}
