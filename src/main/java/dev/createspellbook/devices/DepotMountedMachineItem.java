package dev.createspellbook.devices;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.logistics.depot.DepotBlock;
import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;
import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.constructs.ArcaneCastingArmBlockEntity;
import dev.createspellbook.constructs.EmitterMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid=CreateSpellbook.ID)
public final class DepotMountedMachineItem extends AssemblyOperatorBlockItem {
    public DepotMountedMachineItem(Block block,Properties properties) {super(block,properties);}

    private static boolean mountingSurface(BlockState state) {
        return state.getBlock() instanceof DepotBlock
                || state.getBlock() instanceof BeltBlock && state.getValue(BeltBlock.SLOPE)==BeltSlope.HORIZONTAL;
    }

    @Override protected boolean operatesOn(LevelReader level,BlockPos pos,BlockState state) {
        return mountingSurface(state);
    }

    @Override public InteractionResult place(BlockPlaceContext context) {
        var level=context.getLevel();
        var gap=context.getClickedPos();
        if(context.getClickedFace()!=Direction.UP || !mountingSurface(level.getBlockState(gap.below())))return super.place(context);
        var mounted=gap.above();
        if(!level.getBlockState(gap).getCollisionShape(level,gap).isEmpty()
                || context.getPlayer()!=null && !level.mayInteract(context.getPlayer(),mounted))return InteractionResult.FAIL;
        var result=super.place(context);
        if(result.consumesAction() && !level.isClientSide && level.getBlockEntity(mounted) instanceof ArcaneCastingArmBlockEntity arm)
            arm.setMode(EmitterMode.PROCESSING);
        return result;
    }

    @SubscribeEvent public static void useOnSurface(PlayerInteractEvent.RightClickBlock event) {
        // A regular top click normally puts the held stack on the depot.
        // Let the normal item placement and protection checks handle these machines.
        if(event.getFace()==Direction.UP && event.getEntity().mayBuild()
                && event.getItemStack().getItem() instanceof DepotMountedMachineItem
                && event.getUseBlock()==TriState.DEFAULT && event.getUseItem()!=TriState.FALSE
                && mountingSurface(event.getLevel().getBlockState(event.getPos())))event.setUseBlock(TriState.FALSE);
    }
}
