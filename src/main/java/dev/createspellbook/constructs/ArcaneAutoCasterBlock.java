package dev.createspellbook.constructs;

import dev.createspellbook.devices.DeviceContent;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
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
import net.neoforged.neoforge.fluids.FluidUtil;

public final class ArcaneAutoCasterBlock extends Block implements IBE<ArcaneAutoCasterBlockEntity> {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE=Shapes.or(Block.box(0,0,0,16,14,16),Block.box(5,14,5,11,21,11));
    public ArcaneAutoCasterBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection());}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof ArcaneAutoCasterBlockEntity caster){caster.restoreLegacyItem(stack);if(placer instanceof Player player)caster.setOwner(player);}
    }
    @Override public Class<ArcaneAutoCasterBlockEntity> getBlockEntityClass(){return ArcaneAutoCasterBlockEntity.class;}
    @Override public BlockEntityType<? extends ArcaneAutoCasterBlockEntity> getBlockEntityType(){return DeviceContent.AUTO_CASTER_BE.get();}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    private InteractionResult use(Level level,BlockPos pos,Player player,InteractionHand hand,Direction face){
        if(!(level.getBlockEntity(pos) instanceof ArcaneAutoCasterBlockEntity caster))return InteractionResult.PASS;
        if(!caster.canConfigure(player)){if(!level.isClientSide)player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.create_spellbook.construct_owner"),true);return InteractionResult.SUCCESS;}
        var held=player.getItemInHand(hand);
        if(level.isClientSide)return held.isEmpty() || ScrollTemplate.read(held).isPresent() || caster.inputPort(face)!=null && FluidUtil.getFluidHandler(held).isPresent()?InteractionResult.SUCCESS:InteractionResult.PASS;
        if(caster.inputPort(face)!=null && FluidUtil.interactWithFluidHandler(player,hand,caster.inputPort(face)))return InteractionResult.SUCCESS;
        return ConstructInteraction.template(caster,player,hand);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return use(level,pos,player,InteractionHand.MAIN_HAND,hit.getDirection());}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return use(level,pos,player,hand,hit.getDirection()).consumesAction()?ItemInteractionResult.SUCCESS:ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock()))IBE.onRemove(state,level,pos,next);super.onRemove(state,level,pos,next,moving);}
    @Override protected java.util.List<ItemStack> getDrops(BlockState state,LootParams.Builder params){
        var drops=super.getDrops(state,params);
        if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ArcaneAutoCasterBlockEntity caster){
            var tag=caster.saveWithFullMetadata(params.getLevel().registryAccess());tag.remove("x");tag.remove("y");tag.remove("z");
            for(var item:drops)if(item.is(DeviceContent.REACTOR_ITEM.get()))item.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tag));
        }return drops;
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
}
