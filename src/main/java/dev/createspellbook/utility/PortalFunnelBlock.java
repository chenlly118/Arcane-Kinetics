package dev.createspellbook.utility;

import com.simibubi.create.content.logistics.funnel.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class PortalFunnelBlock extends FunnelBlock {
    public PortalFunnelBlock(Properties properties){super(properties);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
    public static Direction facing(BlockState state){return AbstractFunnelBlock.getFunnelFacing(state);}
    public static boolean isPortal(BlockState state){return state.is(UtilityContent.PORTAL.get()) || state.is(UtilityContent.PORTAL_BELT.get());}
    @Override public BlockState getEquivalentBeltFunnel(BlockGetter level,BlockPos pos,BlockState state){return UtilityContent.PORTAL_BELT.get().defaultBlockState().setValue(BeltFunnelBlock.HORIZONTAL_FACING,state.getValue(FACING)).setValue(POWERED,state.getValue(POWERED)).setValue(WATERLOGGED,state.getValue(WATERLOGGED));}
    @Override public BlockEntityType<? extends FunnelBlockEntity> getBlockEntityType(){return UtilityContent.PORTAL_BE.get();}
    public static void placed(Level level,BlockPos pos,LivingEntity placer){if(!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof PortalFunnelBlockEntity funnel)funnel.setOwner(player);}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){super.setPlacedBy(level,pos,state,placer,stack);placed(level,pos,placer);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!stack.is(Items.ENDER_PEARL))return super.useItemOn(stack,state,level,pos,player,hand,hit);
        return configure(stack,level,pos,player);
    }
    public static ItemInteractionResult configure(ItemStack stack,Level level,BlockPos pos,Player player){
        if(!stack.is(Items.ENDER_PEARL))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof PortalFunnelBlockEntity funnel){
            if(!funnel.canConfigure(player)){player.displayClientMessage(Component.translatable("message.create_spellbook.construct_owner"),true);return ItemInteractionResult.SUCCESS;}
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var selected=data.getCompound("ArcaneKineticsPortal");
            ResourceLocation dimension=ResourceLocation.tryParse(selected.getString("Dimension"));
            if(dimension!=null && selected.hasUUID("Endpoint")){
                var origin=server.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,dimension));BlockPos first=BlockPos.of(selected.getLong("Position"));
                if(origin!=null && origin.hasChunkAt(first) && origin.getBlockEntity(first) instanceof PortalFunnelBlockEntity other && other.identity().equals(selected.getUUID("Endpoint")) && other.canConfigure(player) && funnel.pair(other)){
                    data.remove("ArcaneKineticsPortal");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));player.displayClientMessage(Component.translatable("message.create_spellbook.portal_bound"),true);return ItemInteractionResult.SUCCESS;
                }
                player.displayClientMessage(Component.translatable("message.create_spellbook.portal_reselected"),true);
            }
            var next=new CompoundTag();next.putString("Dimension",level.dimension().location().toString());next.putLong("Position",pos.asLong());next.putUUID("Endpoint",funnel.identity());data.put("ArcaneKineticsPortal",next);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.displayClientMessage(Component.translatable("message.create_spellbook.portal_selected"),true);
        }return ItemInteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        return inspect(level,pos,player);
    }
    public static InteractionResult inspect(Level level,BlockPos pos,Player player){
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof PortalFunnelBlockEntity funnel){
            if(player.isShiftKeyDown() && funnel.canConfigure(player)){funnel.unlink();player.displayClientMessage(Component.translatable("message.create_spellbook.portal_unbound"),true);}
            else player.displayClientMessage(Component.translatable("message.create_spellbook.portal_status",funnel.partner()==null?"—":funnel.partner().dimension().location()+" "+funnel.partner().pos().toShortString(),funnel.pendingCount(),funnel.receivedCount()),true);
        }return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult onWrenched(BlockState state,UseOnContext context){
        return canWrench(context)?super.onWrenched(state,context):InteractionResult.PASS;
    }
    public static boolean canWrench(UseOnContext context){return context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PortalFunnelBlockEntity funnel && context.getPlayer()!=null && funnel.canConfigure(context.getPlayer());}
    public static void removing(BlockState state,Level level,BlockPos pos,BlockState next){
        if(state.getBlock()!=next.getBlock() && !isPortal(next) && !level.isClientSide && level.getBlockEntity(pos) instanceof PortalFunnelBlockEntity funnel)funnel.dropContentsAndUnlink();
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        removing(state,level,pos,next);
        super.onRemove(state,level,pos,next,moving);
    }
}
