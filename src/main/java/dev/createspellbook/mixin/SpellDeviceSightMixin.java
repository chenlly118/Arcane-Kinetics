package dev.createspellbook.mixin;

import dev.createspellbook.devices.SpellDeviceProbe;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A spell hitting a machine's receiving surface must not see its casing as a wall. */
@Mixin(value=Utils.class,remap=false)
public abstract class SpellDeviceSightMixin {
    @Inject(method="hasLineOfSight(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Z)Z",at=@At("HEAD"),cancellable=true)
    private static void createSpellbook$receivingSurface(Level level,Entity from,Entity target,boolean shields,CallbackInfoReturnable<Boolean> callback) {
        if(!(target instanceof SpellDeviceProbe))return;
        var start=from.getEyePosition();var box=target.getBoundingBox();
        var surface=box.contains(start)?start:box.clip(start,box.getCenter()).orElse(box.getCenter());
        // The public vector overload still tests intervening blocks and shields.
        callback.setReturnValue(Utils.hasLineOfSight(level,start,surface,shields));
    }
}
