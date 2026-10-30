package dev.createspellbook.printing;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.createspellbook.devices.DeviceContent;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class ScrollPrinterBlock extends Block implements IBE<ScrollPrinterBlockEntity>,IWrenchable {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape[] SHAPES=new VoxelShape[4];
    static {
        // The hanging head is a visual in the required empty processing gap.
        // Collision follows the reservoir, lower gland, side pocket and inlet.
        double[][] boxes={{2,3,2,14,14,14},{5,0,5,11,3,11},{13,4.5,3.5,16,12.5,12.5},{5,14,5,11,16,11}};
        for(int turn=0;turn<4;turn++){
            var shape=Shapes.empty();
            for(var box:boxes){
                double x=box[0],z=box[2],u=box[3],w=box[5];
                for(int i=0;i<turn;i++){double nx=16-w,nz=x,nu=16-z,nw=u;x=nx;z=nz;u=nu;w=nw;}
                shape=Shapes.or(shape,Block.box(x,box[1],z,u,box[4],w));
            }SHAPES[turn]=shape;
        }
    }
    public ScrollPrinterBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public Class<ScrollPrinterBlockEntity> getBlockEntityClass(){return ScrollPrinterBlockEntity.class;}
    @Override public BlockEntityType<? extends ScrollPrinterBlockEntity> getBlockEntityType(){return DeviceContent.SCROLL_PRINTER_BE.get();}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPES[switch(state.getValue(FACING)){case NORTH->0;case EAST->1;case SOUTH->2;case WEST->3;default->0;}];}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return use(level,pos,player,InteractionHand.MAIN_HAND,hit);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return use(level,pos,player,hand,hit).consumesAction()?ItemInteractionResult.SUCCESS:ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
    private InteractionResult use(Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof ScrollPrinterBlockEntity printer))return InteractionResult.PASS;
        return printer.playerUse(player,hand,hit);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock()))IBE.onRemove(state,level,pos,next);super.onRemove(state,level,pos,next,moving);}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){super.onPlace(state,level,pos,old,moving);if(state.is(old.getBlock()) && state.getValue(FACING)!=old.getValue(FACING))level.invalidateCapabilities(pos);}
    @Override protected java.util.List<ItemStack> getDrops(BlockState state,LootParams.Builder params){
        var drops=super.getDrops(state,params);
        if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ScrollPrinterBlockEntity printer){
            var tag=printer.saveWithFullMetadata(params.getLevel().registryAccess());tag.remove("x");tag.remove("y");tag.remove("z");
            for(var item:drops)if(item.is(DeviceContent.SCROLL_PRINTER_ITEM.get()))item.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tag));
        }return drops;
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
}
