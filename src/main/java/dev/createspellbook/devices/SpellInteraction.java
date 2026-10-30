package dev.createspellbook.devices;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import javax.annotation.Nullable;
import java.util.UUID;

/** Server-authoritative contact. Level 0 means unknown, never an assumed level. */
public record SpellInteraction(ResourceLocation spellId,ResourceLocation schoolId,int level,float damage,
        @Nullable LivingEntity caster,@Nullable UUID casterId,UUID hitId,@Nullable UUID projectileId,@Nullable UUID castId) {
    public SpellInteraction(ResourceLocation spellId,ResourceLocation schoolId,int level,float damage,LivingEntity caster,UUID casterId,UUID hitId,UUID projectileId){this(spellId,schoolId,level,damage,caster,casterId,hitId,projectileId,null);}
    public SpellInteraction {
        if(spellId==null || schoolId==null || hitId==null || !Float.isFinite(damage) || damage<0 || level<0)
            throw new IllegalArgumentException("Invalid spell contact");
    }
}
