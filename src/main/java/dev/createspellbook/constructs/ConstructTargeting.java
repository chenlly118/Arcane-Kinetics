package dev.createspellbook.constructs;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import java.util.Comparator;
import java.util.function.Predicate;

/** Shared hostile selection; never modifies a target's AI or forces chunk loading. */
public final class ConstructTargeting {
    public static boolean hostile(ConstructCaster caster,LivingEntity target,Vec3 origin,int range,Predicate<LivingEntity> visible) {
        return caster!=null && target!=null && target.isAlive() && !target.isInvulnerable() && target.level()==caster.level()
                && !(target instanceof Player) && !(target instanceof OwnableEntity) && !(target instanceof ConstructCaster)
                && (target instanceof Enemy || target.getType().getCategory()==MobCategory.MONSTER)
                && !ConstructSafety.protectedFrom(caster,target) && target.distanceToSqr(origin)<=range*(double)range && visible.test(target);
    }
    public static LivingEntity nearest(ConstructCaster caster,Vec3 origin,int range,Predicate<LivingEntity> eligible) {
        if(caster==null)return null;
        return caster.level().getEntitiesOfClass(LivingEntity.class,new AABB(origin,origin).inflate(range),eligible).stream()
                .min(Comparator.<LivingEntity>comparingDouble(entity->entity.distanceToSqr(origin)).thenComparingInt(Entity::getId)).orElse(null);
    }
    private ConstructTargeting() {}
}
