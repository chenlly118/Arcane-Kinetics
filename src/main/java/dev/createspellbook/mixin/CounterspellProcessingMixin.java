package dev.createspellbook.mixin;

import dev.createspellbook.devices.CounterspellProcessing;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.spells.ender.CounterspellSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=CounterspellSpell.class,remap=false)
public abstract class CounterspellProcessingMixin {
    @Inject(method="onCast",at=@At("RETURN"),remap=false)
    private void createSpellbook$processing(Level level,int spellLevel,LivingEntity caster,CastSource source,MagicData magic,CallbackInfo ci){
        CounterspellProcessing.complete(level,spellLevel,caster);
    }
}
