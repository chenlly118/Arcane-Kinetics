package dev.createspellbook.constructs;

import dev.createspellbook.devices.DeviceContent;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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
import net.neoforged.neoforge.fluids.FluidUtil;

public final class ArcaneCastingArmBlock extends com.simibubi.create.content.kinetics.base.KineticBlock implements IBE<ArcaneCastingArmBlockEntity> {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public ArcaneCastingArmBlock(Properties properties) {super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {return defaultBlockState().setValue(FACING,context.getHorizontalDirection());}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {super.setPlacedBy(level,pos,state,placer,stack);if(placer instanceof Player player && level.getBlockEntity(pos) instanceof ArcaneCastingArmBlockEntity arm)arm.setOwner(player);}
    @Override public Direction.Axis getRotationAxis(BlockState state) {return Direction.Axis.Y;}
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction face) {return face==Direction.DOWN;}
    @Override public Class<ArcaneCastingArmBlockEntity> getBlockEntityClass() {return ArcaneCastingArmBlockEntity.class;}
    @Override public BlockEntityType<? extends ArcaneCastingArmBlockEntity> getBlockEntityType() {return DeviceContent.CASTING_ARM_BE.get();}
    private InteractionResult use(Level level,BlockPos pos,Player player,InteractionHand hand,Direction face) {
        if(!(level.getBlockEntity(pos) instanceof ArcaneCastingArmBlockEntity arm))return InteractionResult.PASS;
        if(!arm.canConfigure(player)){if(!level.isClientSide)player.displayClientMessage(Component.translatable("message.create_spellbook.construct_owner"),true);return InteractionResult.SUCCESS;}
        if(ConstructInteraction.isWrench(player.getItemInHand(hand)))return ConstructInteraction.cycle(arm,player);
        if(level.isClientSide)return player.getItemInHand(hand).isEmpty() || ScrollTemplate.read(player.getItemInHand(hand)).isPresent()
                || arm.fluidPort(face)!=null && FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()?InteractionResult.SUCCESS:InteractionResult.PASS;
        if(arm.fluidPort(face)!=null && FluidUtil.interactWithFluidHandler(player,hand,arm.tank))return InteractionResult.SUCCESS;
        return ConstructInteraction.template(arm,player,hand);
    }
    @Override public InteractionResult onWrenched(BlockState state,net.minecraft.world.item.context.UseOnContext context){
        return context.getPlayer()!=null && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ArcaneCastingArmBlockEntity arm
                ?ConstructInteraction.cycle(arm,context.getPlayer()):InteractionResult.PASS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {return use(level,pos,player,InteractionHand.MAIN_HAND,hit.getDirection());}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {return use(level,pos,player,hand,hit.getDirection()).consumesAction()?ItemInteractionResult.SUCCESS:ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving) {super.onPlace(state,level,pos,old,moving);if(state.getBlock()==old.getBlock() && state.getValue(FACING)!=old.getValue(FACING)){level.invalidateCapabilities(pos);if(level.getBlockEntity(pos) instanceof ArcaneCastingArmBlockEntity arm)arm.directionChanged();}}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {if(!state.is(next.getBlock()))super.onRemove(state,level,pos,next,moving);}
    @Override protected java.util.List<ItemStack> getDrops(BlockState state,LootParams.Builder parameters) {
        var drops=super.getDrops(state,parameters);
        if(parameters.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ArcaneCastingArmBlockEntity arm) {
            var saved=arm.saveWithFullMetadata(parameters.getLevel().registryAccess());saved.remove("x");saved.remove("y");saved.remove("z");
            for(String key:java.util.List.of("Speed","Source","Network","Sequence","NeedsSpeedUpdate"))saved.remove(key);
            for(var stack:drops)if(stack.is(DeviceContent.CASTING_ARM_ITEM.get()))stack.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(saved));
        }
        return drops;
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation) {return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror) {return state.rotate(mirror.getRotation(state.getValue(FACING)));}
}
