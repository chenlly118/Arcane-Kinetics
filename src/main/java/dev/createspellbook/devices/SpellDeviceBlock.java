package dev.createspellbook.devices;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public final class SpellDeviceBlock extends Block implements IBE<SpellDeviceBlockEntity> {
    public enum Kind {BASIN,SENSOR}
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    private final Kind kind;
    public SpellDeviceBlock(Kind kind,Properties properties) {super(properties);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(POWERED,false));}
    public Kind kind() {return kind;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(FACING,POWERED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return switch(kind) {
            case BASIN->AllBlocks.BASIN.getDefaultState().getShape(level,pos,context);
            case SENSOR->Shapes.block();
        };
    }
    @Override public Class<SpellDeviceBlockEntity> getBlockEntityClass() {return SpellDeviceBlockEntity.class;}
    @Override public BlockEntityType<? extends SpellDeviceBlockEntity> getBlockEntityType() {
        return switch(kind) {case BASIN->DeviceContent.BASIN_BE.get();case SENSOR->DeviceContent.SENSOR_BE.get();};
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        var be=getBlockEntity(level,pos);return be==null?InteractionResult.PASS:be.playerUse(player,InteractionHand.MAIN_HAND);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        var be=getBlockEntity(level,pos);
        return be!=null && be.playerUse(player,hand).consumesAction()?ItemInteractionResult.SUCCESS:ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock())) {
            IBE.onRemove(state,level,pos,next);
            if(kind==Kind.SENSOR)level.updateNeighborsAt(pos,this);
        }
        super.onRemove(state,level,pos,next,moving);
    }
    @Override protected java.util.List<ItemStack> getDrops(BlockState state,LootParams.Builder parameters) {
        var drops=super.getDrops(state,parameters);
        if((kind==Kind.BASIN || kind==Kind.SENSOR) && parameters.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof SpellDeviceBlockEntity device) {
            var saved=device.saveWithFullMetadata(parameters.getLevel().registryAccess());
            saved.remove("x");saved.remove("y");saved.remove("z");if(kind==Kind.SENSOR)saved.remove("ExpiresAt");
            for(var stack:drops)if(stack.getItem() instanceof BlockItem item && item.getBlock()==this)stack.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(saved));
        }
        return drops;
    }
    @Override protected boolean isSignalSource(BlockState state) {return kind==Kind.SENSOR;}
    @Override protected int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side) {return level.getBlockEntity(pos) instanceof SpellSensorBlockEntity sensor?sensor.signal():0;}
    @Override protected int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side) {return getSignal(state,level,pos,side);}
}
