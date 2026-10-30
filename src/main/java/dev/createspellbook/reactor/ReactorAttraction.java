package dev.createspellbook.reactor;

import dev.createspellbook.Balance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.*;
import java.lang.ref.WeakReference;
import java.util.*;

/** Bounded target discovery plus per-tick steering. Sector scans alone used to
 * apply force only once every eight ticks, making attraction slow and jerky. */
public final class ReactorAttraction {
    private record Target(UUID field,WeakReference<Entity> entity){}
    private final ServerLevel level;
    private final Map<Integer,Target> targets=new LinkedHashMap<>();
    private final Map<UUID,ReactorAccident> fields=new HashMap<>();
    public ReactorAttraction(ServerLevel level){this.level=level;}
    public int cached(){return targets.size();}
    public void remove(UUID field){fields.remove(field);targets.values().removeIf(t->t.field.equals(field));}
    public void begin(Collection<ReactorAccident> active,long now){
        fields.clear();for(var a:active)fields.put(a.id,a);
        targets.values().removeIf(t->{var e=t.entity.get();var a=fields.get(t.field);return a==null || !valid(e) || e.position().distanceToSqr(a.position())>Math.pow(a.gravityRadius(now),2);});
        while(targets.size()>Balance.REACTOR_ENTITY_BUDGET.get())targets.remove(targets.keySet().iterator().next());
    }
    private boolean valid(Entity e){return e!=null && e.isAlive() && e.level()==level && !(e instanceof Player p && (p.isCreative() || p.isSpectator()));}
    private static boolean absorbProp(Entity e,ReactorAccident a,long now){
        if((e instanceof net.minecraft.world.entity.item.FallingBlockEntity || e instanceof net.minecraft.world.entity.decoration.ArmorStand)
                && e.getBoundingBox().getCenter().distanceToSqr(a.position())<=Math.pow(Math.max(.8,a.visualRadius(now)),2)){e.discard();return true;}return false;
    }
    /** Native network motion supports +/-3.9 per component. Steer below that
     * bound, damp tangential momentum, and brake before overshooting the center. */
    public static Vec3 motion(Vec3 current,Vec3 delta,double reach,boolean lift){
        double distance=delta.length();if(distance<.02)return Vec3.ZERO;
        double speed=Math.min(Balance.REACTOR_PULL_SPEED.get()*(.6+.4*(1-Math.clamp(distance/Math.max(.01,reach),0,1))),distance*.55);
        var desired=delta.scale(speed/distance);
        if(lift && distance>2)desired=desired.add(0,.35,0);
        var motion=current.lerp(desired,.85);double cap=Math.min(Balance.REACTOR_PULL_SPEED.get(),distance*.65);
        return motion.lengthSqr()>cap*cap?motion.normalize().scale(cap):motion;
    }
    public int apply(ReactorAccident a,long now,int limit){
        double range=a.gravityRadius(now);if(limit<=0 || range<=0)return 0;var center=a.position();
        int sector=(int)(now+a.id.getLeastSignificantBits())&7;
        var box=new AABB(center.x+((sector&1)==0?-range:0),center.y+((sector&2)==0?-range:0),center.z+((sector&4)==0?-range:0),center.x+((sector&1)==0?0:range),center.y+((sector&2)==0?0:range),center.z+((sector&4)==0?0:range));
        if(now%20==0)box=new AABB(center.x-range,center.y-range,center.z-range,center.x+range,center.y+range,center.z+range);
        var found=new ArrayList<Entity>();int discovery=Math.min(limit,now%20==0?4096:512);
        level.getEntities(EntityTypeTest.forClass(Entity.class),box,e->valid(e) && e.position().distanceToSqr(center)<=range*range,found,discovery);
        int absorbed=0;
        for(var e:found){
            // New center props must not be excluded by a cache full of distant
            // targets. Discovery plus steering still shares the same budget.
            if(absorbProp(e,a,now)){absorbed++;a.interactions++;continue;}
            var prior=targets.get(e.getId());
            if(prior==null){if(targets.size()>=Balance.REACTOR_ENTITY_BUDGET.get())continue;}
            else if(!prior.field.equals(a.id)){
                var old=fields.get(prior.field);
                if(old!=null && e.position().distanceToSqr(old.position())<=e.position().distanceToSqr(center))continue;
            }
            targets.put(e.getId(),new Target(a.id,new WeakReference<>(e)));
        }
        int count=absorbed;double damageRange=a.destructionRadius(now);
        for(var t:targets.values()){
            if(count>=limit)break;if(!t.field.equals(a.id))continue;var e=t.entity.get();if(!valid(e))continue;
            var delta=center.subtract(e.getBoundingBox().getCenter());double distance=delta.length();if(distance>range)continue;
            count++;a.interactions++;
            // Falling blocks and armor stands do not reliably die to magic
            // damage. Swallow props at the visible horizon, without drops or
            // replacing falling blocks back into the world at the center.
            if(absorbProp(e,a,now))continue;
            e.setDeltaMovement(motion(e.getDeltaMovement(),delta,range,e.onGround() || e.horizontalCollision));e.hurtMarked=true;
            // Items visibly travel to the center instead of vanishing at the
            // outer boundary. Neither blocks nor living entities are teleported.
            if(e instanceof ItemEntity){if(distance<.8)e.discard();continue;}
            if(distance<=damageRange && now%20==0){
                var tag=e.getPersistentData();long last=tag.getLong("CSBReactorDamageTick");
                if(!tag.contains("CSBReactorDamageTick") || now-last>=20){tag.putLong("CSBReactorDamageTick",now);e.hurt(level.damageSources().magic(),Balance.REACTOR_ACCIDENT_DAMAGE.get().floatValue());}
            }
        }
        return count;
    }
}
