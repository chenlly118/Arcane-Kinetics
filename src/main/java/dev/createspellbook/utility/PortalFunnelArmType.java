package dev.createspellbook.utility;

import com.simibubi.create.content.kinetics.mechanicalArm.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

public final class PortalFunnelArmType extends ArmInteractionPointType {
    @Override public boolean canCreatePoint(Level level,BlockPos pos,BlockState state){return PortalFunnelBlock.isPortal(state) && level.getBlockEntity(pos) instanceof PortalFunnelBlockEntity funnel && funnel.collecting();}
    @Override public ArmInteractionPoint createPoint(Level level,BlockPos pos,BlockState state){return new Point(this,level,pos,state);}
    private static final class Point extends ArmInteractionPoint {
        Point(ArmInteractionPointType type,Level level,BlockPos pos,BlockState state){super(type,level,pos,state);}
        @Override protected Vec3 getInteractionPositionVector(){return Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(getInteractionDirection().getNormal()).scale(.4));}
        @Override protected Direction getInteractionDirection(){return PortalFunnelBlock.facing(level.getBlockState(pos));}
        @Override protected IItemHandler getHandler(ArmBlockEntity arm){return level.getBlockEntity(pos) instanceof PortalFunnelBlockEntity funnel?funnel.itemPort():null;}
    }
}
