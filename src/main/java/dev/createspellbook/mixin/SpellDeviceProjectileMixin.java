package dev.createspellbook.mixin;

import dev.createspellbook.devices.SpellDeviceBridge;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

/** The installed Iron version posts no public event for this block impact path. */
@Mixin(value=AbstractMagicProjectile.class,remap=false)
public abstract class SpellDeviceProjectileMixin {
    @Shadow protected abstract void onHit(HitResult hit);
    @Redirect(method="handleHitDetection",at=@At(value="INVOKE",target="Lio/redspace/ironsspellbooks/entity/spells/AbstractMagicProjectile;onHit(Lnet/minecraft/world/phys/HitResult;)V"))
    private void createSpellbook$deviceContact(AbstractMagicProjectile projectile,HitResult hit) {
        if(!SpellDeviceBridge.blockImpact(projectile,hit))onHit(hit);
    }
}
