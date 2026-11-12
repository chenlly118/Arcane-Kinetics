package dev.createspellbook.mixin;

import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Create exposes no inventory-behaviour factory. Only our subclass sets these references. */
@Mixin(value=FunnelBlockEntity.class,remap=false)
public interface FunnelBehaviourAccess {
    @Accessor("invManipulation") void spellbook$inventory(InvManipulationBehaviour inventory);
    @Accessor("invVersionTracker") void spellbook$tracker(VersionedInventoryTrackerBehaviour tracker);
    @Accessor("filtering") void spellbook$filter(FilteringBehaviour filter);
    @Accessor("flap") LerpedFloat spellbook$flap();
}
