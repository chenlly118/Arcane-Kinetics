package dev.createspellbook.utility;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelFilterSlotPositioning;
import net.minecraft.core.*;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The original positioning code tests FunnelBlock identity; adapt only its state input. */
public final class PortalFunnelFilterSlot extends FunnelFilterSlotPositioning {
    public static BlockState nativeState(BlockState state){
        if(state.getBlock() instanceof BeltFunnelBlock)return AllBlocks.BRASS_BELT_FUNNEL.getDefaultState().setValue(BeltFunnelBlock.HORIZONTAL_FACING,PortalFunnelBlock.facing(state)).setValue(BeltFunnelBlock.SHAPE,state.getValue(BeltFunnelBlock.SHAPE));
        return AllBlocks.BRASS_FUNNEL.getDefaultState().setValue(FunnelBlock.FACING,PortalFunnelBlock.facing(state)).setValue(FunnelBlock.EXTRACTING,state.getValue(FunnelBlock.EXTRACTING));
    }
    @Override public Vec3 getLocalOffset(LevelAccessor level,BlockPos pos,BlockState state){return super.getLocalOffset(level,pos,nativeState(state));}
    @Override public void rotate(LevelAccessor level,BlockPos pos,BlockState state,PoseStack pose){super.rotate(level,pos,nativeState(state),pose);}
    @Override protected boolean isSideActive(BlockState state,Direction side){return super.isSideActive(nativeState(state),side);}
}
