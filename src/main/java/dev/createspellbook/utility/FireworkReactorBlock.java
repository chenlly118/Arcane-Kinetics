package dev.createspellbook.utility;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
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
import net.neoforged.neoforge.fluids.FluidUtil;

public final class FireworkReactorBlock extends Block implements IBE<FireworkReactorBlockEntity> {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public FireworkReactorBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection());}
    @Override public Class<FireworkReactorBlockEntity> getBlockEntityClass(){return FireworkReactorBlockEntity.class;}
    @Override public BlockEntityType<? extends FireworkReactorBlockEntity> getBlockEntityType(){return UtilityContent.FIREWORK_BE.get();}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof FireworkReactorBlockEntity reactor))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(stack.is(Items.FIREWORK_ROCKET) && hit.getDirection().getAxis().isHorizontal()){
            if(!level.isClientSide){reactor.configure(stack);player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.create_spellbook.firework_configured"),true);}
            return ItemInteractionResult.SUCCESS;
        }
        var port=reactor.inputPort(hit.getDirection());
        if(port!=null && FluidUtil.getFluidHandler(stack).isPresent()){
            if(!level.isClientSide)FluidUtil.interactWithFluidHandler(player,hand,port);return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,BlockHitResult hit){
        if(!hit.getDirection().getAxis().isHorizontal())return InteractionResult.PASS;
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof FireworkReactorBlockEntity reactor){
            reactor.configure(ItemStack.EMPTY);player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.create_spellbook.firework_cleared"),true);
        }return InteractionResult.SUCCESS;
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock()))IBE.onRemove(state,level,pos,next);super.onRemove(state,level,pos,next,moving);}
    @Override protected java.util.List<ItemStack> getDrops(BlockState state,LootParams.Builder params){
        var drops=super.getDrops(state,params);
        if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof FireworkReactorBlockEntity reactor){
            var tag=reactor.saveWithFullMetadata(params.getLevel().registryAccess());tag.remove("x");tag.remove("y");tag.remove("z");
            for(var stack:drops)if(stack.is(UtilityContent.FIREWORK_ITEM.get()))stack.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tag));
        }return drops;
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
}
