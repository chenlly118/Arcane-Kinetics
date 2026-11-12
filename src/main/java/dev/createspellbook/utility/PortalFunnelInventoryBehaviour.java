package dev.createspellbook.utility;

import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import net.createmod.catnip.math.BlockFace;

/** Native belt/funnel operations read our queues instead of an adjacent physical inventory. */
final class PortalFunnelInventoryBehaviour extends InvManipulationBehaviour {
    private final PortalFunnelBlockEntity funnel;
    PortalFunnelInventoryBehaviour(PortalFunnelBlockEntity funnel){
        super(funnel,(level,pos,state)->new BlockFace(pos.relative(PortalFunnelBlock.facing(state)),PortalFunnelBlock.facing(state).getOpposite()));
        this.funnel=funnel;
    }
    @Override public void findNewCapability(){targetCapability=funnel.nativeInventory();}
    @Override public void initialize(){super.initialize();findNewCapability();}
}
