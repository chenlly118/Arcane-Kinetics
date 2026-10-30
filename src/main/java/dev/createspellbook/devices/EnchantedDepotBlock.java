package dev.createspellbook.devices;

import com.simibubi.create.content.logistics.depot.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Native Depot interaction, wrenching, comparator and item landing; original ID and appearance. */
public final class EnchantedDepotBlock extends DepotBlock {
    public EnchantedDepotBlock(Properties properties){super(properties);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        super.createBlockStateDefinition(builder);
        // Preserve old chunk palette properties.
        builder.add(BlockStateProperties.HORIZONTAL_FACING,BlockStateProperties.POWERED);
    }
    @Override public BlockEntityType<? extends DepotBlockEntity> getBlockEntityType(){return DeviceContent.DEPOT_BE.get();}
}
