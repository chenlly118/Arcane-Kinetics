package dev.createspellbook.reactor;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class ReactorControllerBlock extends Block implements IBE<ReactorControllerBlockEntity>,IWrenchable {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED=BooleanProperty.create("formed");
    public ReactorControllerBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(FORMED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,FORMED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public Class<ReactorControllerBlockEntity> getBlockEntityClass(){return ReactorControllerBlockEntity.class;}
    @Override public BlockEntityType<? extends ReactorControllerBlockEntity> getBlockEntityType(){return ReactorContent.CONTROLLER_BE.get();}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof ReactorControllerBlockEntity controller){
            if(player.isShiftKeyDown()){controller.checkManually();player.displayClientMessage(controller.statusText(),true);}
            else player.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,p)->new ReactorMenu(id,inventory,controller),net.minecraft.network.chat.Component.translatable("block.create_spellbook.reactor_controller")));
        }return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult onWrenched(BlockState state,UseOnContext context){if(!context.getLevel().isClientSide)context.getLevel().setBlockAndUpdate(context.getClickedPos(),state.setValue(FACING,state.getValue(FACING).getClockWise()));return InteractionResult.SUCCESS;}
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())){if(!level.isClientSide && level.getBlockEntity(pos) instanceof ReactorControllerBlockEntity c)c.operation.structureBroken();IBE.onRemove(state,level,pos,next);}super.onRemove(state,level,pos,next,moving);}
    @Override protected java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){
        var drops=super.getDrops(state,params);
        if(params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof ReactorControllerBlockEntity controller){
            var tag=controller.saveWithFullMetadata(params.getLevel().registryAccess());for(var key:java.util.List.of("x","y","z","ReactorOrigin","ReactorFacing","ReactorIdentity"))tag.remove(key);
            tag.getCompound("Operation").putBoolean("Requested",false);tag.getCompound("Operation").putInt("FieldSize",0);
            for(var item:drops)if(item.is(ReactorContent.CONTROLLER.get().asItem()))item.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        }return drops;
    }
}
