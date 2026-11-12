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
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot==0?ScrollPrinting.validTemplate(stack):ScrollPrinting.validFocus(stack);}
        @Override protected void onContentsChanged(int slot){changed();}
    };
    public final FluidTank tank=new FluidTank(Balance.PRINTER_CAPACITY.get(),fluid->ScrollPrinting.inkRarity(fluid).isPresent()){
        @Override protected void onContentsChanged(){setCapacity(Math.max(getFluidAmount(),Balance.PRINTER_CAPACITY.get()));changed();}
    };
    private final IItemHandler templatePort=new RangedWrapper(inventory,0,1),focusPort=new RangedWrapper(inventory,1,2);
    private int remaining=-1,strokeTicks=20,cursor;
    private boolean committed,upgradeMode;
    private TransportedItemStack workingItem;
    private long lastTick=Long.MIN_VALUE,lastCommit=Long.MIN_VALUE;
    public long printed;
    public ScrollPrinterBlockEntity(BlockPos pos,BlockState state){super(DeviceContent.SCROLL_PRINTER_BE.get(),pos,state);}
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours){
        behaviours.add(new BeltProcessingBehaviour(this).whenItemEnters(this::received).whileItemHeld(this::held));
    }
    @Override public void initialize(){
        super.initialize();
        if(level==null || level.isClientSide)return;
        var focus=inventory.getStackInSlot(1);
        if(focus.getCount()>1){
            inventory.setStackInSlot(1,focus.copyWithCount(1));
            Containers.dropItemStack(level,worldPosition.getX(),worldPosition.getY()+.5,worldPosition.getZ(),focus.copyWithCount(focus.getCount()-1));
        }
    }
    public Direction facing(){return getBlockState().getValue(ScrollPrinterBlock.FACING);}
    public IFluidHandler fluidPort(Direction side){return side==null || side==Direction.UP?tank:null;}
    public IItemHandler itemPort(Direction side){return side==null?inventory:side==Direction.UP?null:side==facing() || side==facing().getClockWise()?templatePort:focusPort;}
    public int remainingTicks(){return remaining;}
    public int strokeTicks(){return strokeTicks;}
    public boolean upgradeMode(){return upgradeMode;}
    private Component modeName(){return Component.translatable(upgradeMode?"message.create_spellbook.printer_mode_upgrade":"message.create_spellbook.printer_mode_print");}
    public InteractionResult switchMode(Player player){
        if(!player.mayBuild())return InteractionResult.PASS;
        if(!level.isClientSide){
            upgradeMode=!upgradeMode;
            remaining=-1;workingItem=null;committed=false;
            changed();player.displayClientMessage(modeName(),true);
        }
        return InteractionResult.SUCCESS;
    }
    private void changed(){setChanged();if(level!=null && !level.isClientSide)sendData();}
    private int inkCost(){return upgradeMode?ScrollPrinting.ANVIL_INK:Balance.PRINTER_INK.get();}
    private boolean eligible(TransportedItemStack item){
        if(!upgradeMode)return item.stack.is(Items.PAPER) && ScrollPrinting.validTemplate(inventory.getStackInSlot(0));
        return ScrollPrinting.upgradeTarget(item.stack)
                .filter(next->tank.isEmpty() || ScrollPrinting.inkRarity(tank.getFluid()).orElse(null)==next.getRarity()).isPresent();
    }
    private Optional<ScrollPrinting.Selection> selection(ItemStack input){
        if(tank.getFluidAmount()<inkCost())return Optional.empty();
        return upgradeMode?ScrollPrinting.upgrade(input,tank.getFluid(),cursor)
                :ScrollPrinting.select(inventory.getStackInSlot(0),inventory.getStackInSlot(1),tank.getFluid(),cursor);
    }
    private ProcessingResult received(TransportedItemStack item,TransportedItemStackHandlerBehaviour handler){return eligible(item)?ProcessingResult.HOLD:ProcessingResult.PASS;}
    private ProcessingResult held(TransportedItemStack item,TransportedItemStackHandlerBehaviour handler){
        if(level==null || level.isClientSide || isRemoved() || isChunkUnloaded())return ProcessingResult.PASS;
        if(!eligible(item))return ProcessingResult.PASS;
        // The native processor sits two blocks above the belt/depot. Obstructions
        // must neither be printed through nor replaced by temporary head blocks.
        if(!level.getBlockState(worldPosition.below()).getCollisionShape(level,worldPosition.below()).isEmpty())return ProcessingResult.PASS;
        if(remaining>=0 && workingItem!=null && workingItem!=item && !committed)return ProcessingResult.HOLD;
        var selected=selection(item.stack);if(selected.isEmpty())return ProcessingResult.HOLD;
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
        tank.drain(inkCost(),FluidAction.EXECUTE);
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
        var held=player.getItemInHand(hand);
        boolean focus=ScrollPrinting.validFocus(held);
        boolean focusFace=face!=Direction.UP && face!=facing() && face!=facing().getClockWise();
        int slot=focus || head || held.isEmpty() && focusFace?1:0;
        if(!focus && face==Direction.UP && FluidUtil.getFluidHandler(held).isPresent()){
            if(!level.isClientSide)FluidUtil.interactWithFluidHandler(player,hand,tank);return InteractionResult.SUCCESS;
        }
        if(!held.isEmpty() && !inventory.isItemValid(slot,held)){
            if(!level.isClientSide)player.displayClientMessage(Component.translatable(slot==0?"message.create_spellbook.printer_template":"message.create_spellbook.printer_focus"),true);
            return held.getItem() instanceof com.simibubi.create.content.logistics.filter.FilterItem?InteractionResult.SUCCESS:InteractionResult.PASS;
        }
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(held.isEmpty()){
            var result=inventory.extractItem(slot,64,false);if(!result.isEmpty())ItemHandlerHelper.giveItemToPlayer(player,result);
            else player.displayClientMessage(Component.translatable("message.create_spellbook.printer_contents",tank.getFluidAmount(),tank.getCapacity(),inkCost()),true);
        }else{
            var previous=inventory.getStackInSlot(slot).copy();
            if(slot==1 && ItemStack.isSameItemSameComponents(previous,held))return InteractionResult.SUCCESS;
            inventory.setStackInSlot(slot,held.copyWithCount(1));
            if(!player.isCreative())held.shrink(1);
            if(!previous.isEmpty())ItemHandlerHelper.giveItemToPlayer(player,previous);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public boolean addToGoggleTooltip(List<Component> lines,boolean sneaking){
        lines.add(Component.translatable("block.create_spellbook.scroll_printer"));
        lines.add(modeName());
        lines.add(Component.translatable("message.create_spellbook.printer_contents",tank.getFluidAmount(),tank.getCapacity(),inkCost()));
        lines.add(Component.translatable(upgradeMode?"message.create_spellbook.printer_upgrade_levels":"message.create_spellbook.printer_native_levels"));
        if(!upgradeMode)selection(ItemStack.EMPTY).ifPresent(selected->lines.add(selected.output().getHoverName()));
        return true;
    }
    @Override protected void write(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.write(tag,lookup,packet);tag.put("Inventory",inventory.serializeNBT(lookup));tag.put("Ink",tank.writeToNBT(lookup,new CompoundTag()));
        tag.putInt("Cursor",cursor);tag.putLong("Printed",printed);tag.putBoolean("UpgradeMode",upgradeMode);
        if(packet){tag.putInt("Remaining",remaining);tag.putInt("StrokeTicks",strokeTicks);}
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.read(tag,lookup,packet);inventory.deserializeNBT(lookup,tag.getCompound("Inventory"));tank.readFromNBT(lookup,tag.getCompound("Ink"));
        tank.setCapacity(Math.max(tank.getFluidAmount(),Balance.PRINTER_CAPACITY.get()));cursor=Math.max(0,tag.getInt("Cursor"));printed=Math.max(0,tag.getLong("Printed"));
        upgradeMode=tag.getBoolean("UpgradeMode");remaining=packet?tag.getInt("Remaining"):-1;strokeTicks=packet?Math.clamp(tag.getInt("StrokeTicks"),8,1200):20;
        workingItem=null;committed=false;lastTick=lastCommit=Long.MIN_VALUE;
    }
}
