package dev.createspellbook.printing;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.belt.behaviour.*;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.createspellbook.*;
import dev.createspellbook.devices.DeviceContent;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import java.util.*;

/** A native belt/depot processor. No dropped-item scanning or private inventories. */
public final class ScrollPrinterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public final ItemStackHandler inventory=new ItemStackHandler(2){
        @Override public int getSlotLimit(int slot){return slot==0?1:64;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot==0?ScrollPrinting.validTemplate(stack):ScrollPrinting.validFocus(stack);}
        @Override protected void onContentsChanged(int slot){changed();}
    };
    public final FluidTank tank=new FluidTank(Balance.PRINTER_CAPACITY.get(),fluid->ScrollPrinting.inkRarity(fluid).isPresent()){
        @Override protected void onContentsChanged(){setCapacity(Math.max(getFluidAmount(),Balance.PRINTER_CAPACITY.get()));changed();}
    };
    private final IItemHandler templatePort=new RangedWrapper(inventory,0,1),focusPort=new RangedWrapper(inventory,1,2);
    private BeltProcessingBehaviour processing;
    private int remaining=-1,strokeTicks=20,cursor;
    private boolean committed;
    private TransportedItemStack workingItem;
    private long lastTick=Long.MIN_VALUE,lastCommit=Long.MIN_VALUE;
    public long printed;
    public ScrollPrinterBlockEntity(BlockPos pos,BlockState state){super(DeviceContent.SCROLL_PRINTER_BE.get(),pos,state);}
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours){
        processing=new BeltProcessingBehaviour(this).whenItemEnters(this::received).whileItemHeld(this::held);behaviours.add(processing);
    }
    public Direction facing(){return getBlockState().getValue(ScrollPrinterBlock.FACING);}
    public IFluidHandler fluidPort(Direction side){return side==null || side==Direction.UP?tank:null;}
    public IItemHandler itemPort(Direction side){return side==null?inventory:side==Direction.UP?null:side==facing() || side==facing().getClockWise()?templatePort:focusPort;}
    public int remainingTicks(){return remaining;}
    public int strokeTicks(){return strokeTicks;}
    private void changed(){setChanged();if(level!=null && !level.isClientSide)sendData();}
    private boolean eligible(TransportedItemStack item){return item.stack.is(Items.PAPER) && ScrollPrinting.validTemplate(inventory.getStackInSlot(0));}
    private Optional<ScrollPrinting.Selection> selection(){return tank.getFluidAmount()<Balance.PRINTER_INK.get()?Optional.empty():ScrollPrinting.select(inventory.getStackInSlot(0),inventory.getStackInSlot(1),tank.getFluid(),cursor);}
    private ProcessingResult received(TransportedItemStack item,TransportedItemStackHandlerBehaviour handler){return eligible(item)?ProcessingResult.HOLD:ProcessingResult.PASS;}
    private ProcessingResult held(TransportedItemStack item,TransportedItemStackHandlerBehaviour handler){
        if(level==null || level.isClientSide || isRemoved() || isChunkUnloaded())return ProcessingResult.PASS;
        if(!eligible(item))return ProcessingResult.PASS;
        // The native processor sits two blocks above the belt/depot. Obstructions
        // must neither be printed through nor replaced by temporary head blocks.
        if(!level.getBlockState(worldPosition.below()).getCollisionShape(level,worldPosition.below()).isEmpty())return ProcessingResult.PASS;
        if(remaining>=0 && workingItem!=null && workingItem!=item && !committed)return ProcessingResult.HOLD;
        var selected=selection();if(selected.isEmpty())return ProcessingResult.HOLD;
        if(!outputFits(item,selected.get().output()))return ProcessingResult.HOLD;
        if(remaining<0){
            workingItem=item;committed=false;strokeTicks=Balance.PRINTER_TICKS.get();remaining=strokeTicks;
            AllSoundEvents.SPOUTING.playOnServer(level,worldPosition,.45f,.8f);changed();return ProcessingResult.HOLD;
        }
        if(committed || remaining>Math.max(2,strokeTicks/4) || lastCommit==level.getGameTime())return ProcessingResult.HOLD;
        // Prepare both outputs before changing anything. Create owns all transport
        // bookkeeping, output buffering and the retained remainder of the input.
        var output=item.copy();output.stack=selected.get().output().copy();output.clearFanProcessingData();
        TransportedItemStack left=null;
        if(item.stack.getCount()>1){left=item.copy();left.stack.shrink(1);}
        handler.handleProcessingOnItem(item,TransportedResult.convertToAndLeaveHeld(List.of(output),left));
        tank.drain(Balance.PRINTER_INK.get(),FluidAction.EXECUTE);inventory.extractItem(1,1,false);
        cursor=selected.get().nextIndex();committed=true;lastCommit=level.getGameTime();printed++;
        AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.playOnServer(level,worldPosition,.45f,1.35f);changed();
        return ProcessingResult.HOLD;
    }
    private boolean outputFits(TransportedItemStack item,ItemStack output){
        if(item.stack.getCount()==1 || !(level.getBlockEntity(worldPosition.below(2)) instanceof com.simibubi.create.content.logistics.depot.DepotBlockEntity))return true;
        // Native depots have one held slot followed by eight public output-buffer
        // slots. Avoid Create's overflow-to-world path when filters cycle through
        // many non-stackable component variants and automation has not collected them.
        var handler=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,worldPosition.below(2),Direction.UP);
        if(handler==null)return false;
        for(int slot=1;slot<handler.getSlots();slot++){
            var stack=handler.getStackInSlot(slot);int max=Math.min(handler.getSlotLimit(slot),output.getMaxStackSize());
            if(stack.isEmpty() || ItemStack.isSameItemSameComponents(stack,output) && stack.getCount()<max)return true;
        }return false;
    }
    @Override public void tick(){
        super.tick();if(level==null || isRemoved() || isChunkUnloaded())return;
        if(level.isClientSide){if(remaining>=0)remaining--;return;}
        if(lastTick==level.getGameTime())return;lastTick=level.getGameTime();
        if(remaining>=0 && --remaining<0){workingItem=null;committed=false;changed();}
    }
    @Override protected net.minecraft.world.phys.AABB createRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition).expandTowards(0,-1.2,0);}
    public InteractionResult playerUse(Player player,InteractionHand hand,Direction face){
        return playerUse(player,hand,face,face==Direction.DOWN);
    }
    public InteractionResult playerUse(Player player,InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        return playerUse(player,hand,hit.getDirection(),hit.getLocation().y<worldPosition.getY()+3d/16);
    }
    private InteractionResult playerUse(Player player,InteractionHand hand,Direction face,boolean head){
        if(!player.mayBuild())return InteractionResult.PASS;
        var held=player.getItemInHand(hand);int slot=head || ScrollPrinting.validFocus(held)?1:0;
        if(face==Direction.UP && FluidUtil.getFluidHandler(held).isPresent()){
            if(!level.isClientSide)FluidUtil.interactWithFluidHandler(player,hand,tank);return InteractionResult.SUCCESS;
        }
        if(!held.isEmpty() && !inventory.isItemValid(slot,held)){
            if(!level.isClientSide)player.displayClientMessage(Component.translatable(slot==0?"message.create_spellbook.printer_template":"message.create_spellbook.printer_focus"),true);
            return held.getItem() instanceof com.simibubi.create.content.logistics.filter.FilterItem?InteractionResult.SUCCESS:InteractionResult.PASS;
        }
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(held.isEmpty()){
            var result=inventory.extractItem(slot,64,false);if(!result.isEmpty())ItemHandlerHelper.giveItemToPlayer(player,result);
            else player.displayClientMessage(Component.translatable("message.create_spellbook.printer_contents",tank.getFluidAmount(),tank.getCapacity(),Balance.PRINTER_INK.get()),true);
        }else if(slot==0){
            var previous=inventory.getStackInSlot(0).copy();inventory.setStackInSlot(0,held.copyWithCount(1));
            if(!player.isCreative())held.shrink(1);
            if(!previous.isEmpty())ItemHandlerHelper.giveItemToPlayer(player,previous);
        }else{
            var remainder=inventory.insertItem(slot,held.copy(),false);if(!player.isCreative())player.setItemInHand(hand,remainder);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public boolean addToGoggleTooltip(List<Component> lines,boolean sneaking){
        lines.add(Component.translatable("block.create_spellbook.scroll_printer"));
        lines.add(Component.translatable("message.create_spellbook.printer_contents",tank.getFluidAmount(),tank.getCapacity(),Balance.PRINTER_INK.get()));
        lines.add(Component.translatable("message.create_spellbook.printer_native_levels"));
        var selected=selection();if(selected.isPresent())lines.add(selected.get().output().getHoverName());
        return true;
    }
    @Override protected void write(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.write(tag,lookup,packet);tag.put("Inventory",inventory.serializeNBT(lookup));tag.put("Ink",tank.writeToNBT(lookup,new CompoundTag()));
        tag.putInt("Cursor",cursor);tag.putLong("Printed",printed);
        if(packet){tag.putInt("Remaining",remaining);tag.putInt("StrokeTicks",strokeTicks);}
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.read(tag,lookup,packet);inventory.deserializeNBT(lookup,tag.getCompound("Inventory"));tank.readFromNBT(lookup,tag.getCompound("Ink"));
        tank.setCapacity(Math.max(tank.getFluidAmount(),Balance.PRINTER_CAPACITY.get()));cursor=Math.max(0,tag.getInt("Cursor"));printed=Math.max(0,tag.getLong("Printed"));
        remaining=packet?tag.getInt("Remaining"):-1;strokeTicks=packet?Math.clamp(tag.getInt("StrokeTicks"),8,1200):20;
        workingItem=null;committed=false;lastTick=lastCommit=Long.MIN_VALUE;
    }
}
