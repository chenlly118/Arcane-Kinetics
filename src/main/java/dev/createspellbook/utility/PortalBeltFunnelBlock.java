package dev.createspellbook.utility;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.logistics.funnel.*;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Native belt-funnel identity is required by Create's actual moving-belt transport handler. */
public final class PortalBeltFunnelBlock extends BeltFunnelBlock implements SpecialBlockItemRequirement {
    // This project uses DeferredRegister, not a Registrate BlockEntry. Every parent-dependent
    // method is implemented below, while native belt placement, shapes and wrenching are reused.
    public PortalBeltFunnelBlock(Properties properties){super(null,properties);}
    @Override public boolean isOfSameType(FunnelBlock other){return other==UtilityContent.PORTAL.get();}
    @Override public BlockEntityType<? extends FunnelBlockEntity> getBlockEntityType(){return UtilityContent.PORTAL_BE.get();}
    @Override public ItemStack getCloneItemStack(BlockState state,HitResult target,LevelReader level,BlockPos pos,Player player){return new ItemStack(UtilityContent.PORTAL_ITEM.get());}
    @Override public ItemRequirement getRequiredItems(BlockState state,BlockEntity entity){return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME,UtilityContent.PORTAL_ITEM.get());}
    @Override public BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos other){
        if(isOnValidBelt(state,level,pos))return super.updateShape(state,direction,neighbor,level,pos,other);
        updateWater(level,state,pos);
        return UtilityContent.PORTAL.get().defaultBlockState().setValue(FunnelBlock.FACING,state.getValue(HORIZONTAL_FACING))
                .setValue(FunnelBlock.EXTRACTING,state.getValue(SHAPE)==Shape.PUSHING).setValue(POWERED,state.getValue(POWERED)).setValue(WATERLOGGED,state.getValue(WATERLOGGED));
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){super.setPlacedBy(level,pos,state,placer,stack);PortalFunnelBlock.placed(level,pos,placer);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return PortalFunnelBlock.configure(stack,level,pos,player);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return PortalFunnelBlock.inspect(level,pos,player);}
    @Override public InteractionResult onWrenched(BlockState state,UseOnContext context){return PortalFunnelBlock.canWrench(context)?super.onWrenched(state,context):InteractionResult.PASS;}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){PortalFunnelBlock.removing(state,level,pos,next);super.onRemove(state,level,pos,next,moving);}
}
