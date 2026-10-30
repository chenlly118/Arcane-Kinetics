package dev.createspellbook.devices;

import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Reuse Create's actual DepotPoint, with our original block ID as the additional target. */
public final class EnchantedDepotArmType extends AllArmInteractionPointTypes.DepotType {
    @Override public boolean canCreatePoint(Level level,BlockPos pos,BlockState state){return state.is(DeviceContent.DEPOT.get());}
}
