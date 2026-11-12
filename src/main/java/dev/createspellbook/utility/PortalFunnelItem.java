package dev.createspellbook.utility;

import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelItem;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/** Also support direct top-click placement, since this funnel carries its own inventory. */
public final class PortalFunnelItem extends FunnelItem {
    public PortalFunnelItem(Properties properties){super(UtilityContent.PORTAL.get(),properties);}
    @Override protected BlockState getPlacementState(BlockPlaceContext context){
        var state=super.getPlacementState(context);
        if(state!=null && state.is(UtilityContent.PORTAL.get()) && context.getClickedFace()==Direction.UP
                && state.getValue(FunnelBlock.FACING).getAxis().isVertical()
                && BeltFunnelBlock.isOnValidBelt(UtilityContent.PORTAL_BELT.get().defaultBlockState(),context.getLevel(),context.getClickedPos()))
            return UtilityContent.PORTAL_BELT.get().getStateForPlacement(context);
        return state;
    }
}
