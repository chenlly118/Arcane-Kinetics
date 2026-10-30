package dev.createspellbook.reactor;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ReactorPartBlock extends Block implements IBE<ReactorPartBlockEntity> {
    public final ReactorRole role;
    public ReactorPartBlock(ReactorRole role,Properties properties){super(properties);this.role=role;}
    @Override public Class<ReactorPartBlockEntity> getBlockEntityClass(){return ReactorPartBlockEntity.class;}
    @Override public BlockEntityType<? extends ReactorPartBlockEntity> getBlockEntityType(){return ReactorContent.partType(role);}
    @Override protected void onRemove(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState next,boolean moving){if(!state.is(next.getBlock()))IBE.onRemove(state,level,pos,next);super.onRemove(state,level,pos,next,moving);}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        if(level.getBlockEntity(pos) instanceof ReactorPartBlockEntity part){
            if(role==ReactorRole.MANA && net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(stack).isPresent()){
                if(!level.isClientSide && part.fluidPort(hit.getDirection())!=null)net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,part.fluidPort(hit.getDirection()));return net.minecraft.world.ItemInteractionResult.SUCCESS;
            }
            if(role==ReactorRole.STABILIZER && ReactorStabilizers.usable(stack)){
                if(!level.isClientSide && part.itemPort(hit.getDirection())!=null){var one=stack.copyWithCount(1);var remaining=net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(part.itemPort(hit.getDirection()),one,false);if(remaining.isEmpty() && !player.isCreative())stack.shrink(1);}return net.minecraft.world.ItemInteractionResult.SUCCESS;
            }
        }return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof ReactorPartBlockEntity part){
            if(role==ReactorRole.STABILIZER && part.reactorBinding().valid()){var controller=part.reactorBinding().owner();player.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,p)->new ReactorMenu(id,inventory,controller,pos),net.minecraft.network.chat.Component.translatable("block.create_spellbook.reactor_controller")));}
            else if(role==ReactorRole.OUTPUT && part.itemPort(hit.getDirection())!=null){var output=part.itemPort(hit.getDirection());for(int i=0;i<output.getSlots();i++)if(!output.getStackInSlot(i).isEmpty()){player.getInventory().placeItemBackInInventory(output.extractItem(i,64,false));break;}}
            else if(role==ReactorRole.MANA && part.fluidPort(hit.getDirection())!=null){var tank=part.fluidPort(hit.getDirection());player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.create_spellbook.reactor.mana",tank.getFluidInTank(0).getAmount(),tank.getTankCapacity(0)),true);}
        }return net.minecraft.world.InteractionResult.SUCCESS;
    }
    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){
        var drops=super.getDrops(state,params);
        if((role==ReactorRole.MANA || role==ReactorRole.OUTPUT) && params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof ReactorPartBlockEntity part){
            var tag=part.saveWithFullMetadata(params.getLevel().registryAccess());for(var key:java.util.List.of("x","y","z","ReactorController","ReactorIdentity"))tag.remove(key);
            for(var item:drops)if(item.is(asItem()))item.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        }return drops;
    }
}
