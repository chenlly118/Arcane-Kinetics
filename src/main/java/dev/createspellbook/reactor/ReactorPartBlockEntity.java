package dev.createspellbook.reactor;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.createspellbook.Content;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.*;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.*;
import java.util.*;

/** Exterior-only, revocable controller views. A port saves only unmigrated
 * pre-0.1.20 contents, never a copy of controller-owned resources. */
public final class ReactorPartBlockEntity extends SmartBlockEntity implements ReactorLinked {
    public final ReactorRole role;
    private final ReactorBinding binding=new ReactorBinding(this::changed);
    private final FluidTank legacyMana=new FluidTank(Integer.MAX_VALUE,fluid->fluid.is(Content.MANA.get()));
    private final ItemStackHandler legacyOutput=new ItemStackHandler(9);
    private final EnumMap<Direction,IFluidHandler> fluids=new EnumMap<>(Direction.class);
    private final EnumMap<Direction,IItemHandler> items=new EnumMap<>(Direction.class);
    private int energyCapacity,reactionPower;
    private Direction knownExterior;
    public ReactorPartBlockEntity(ReactorRole role,BlockPos pos,BlockState state){super(ReactorContent.partType(role),pos,state);this.role=role;}
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours){}
    @Override public ReactorBinding reactorBinding(){return binding;}
    public Direction exterior(){
        if(!binding.valid())return knownExterior;var box=binding.owner().lastLayout().bounds();var p=worldPosition;
        if(p.getX()==box.minX())return Direction.WEST;if(p.getX()==box.maxX())return Direction.EAST;
        if(p.getY()==box.minY())return Direction.DOWN;if(p.getY()==box.maxY())return Direction.UP;
        if(p.getZ()==box.minZ())return Direction.NORTH;if(p.getZ()==box.maxZ())return Direction.SOUTH;return null;
    }
    private boolean accessible(Direction side){return side!=null && level!=null && !level.isClientSide && !isRemoved() && !isChunkUnloaded() && binding.valid() && side==exterior() && level.getBlockEntity(worldPosition)==this;}
    public IFluidHandler fluidPort(){return fluidPort(exterior());}
    public IItemHandler itemPort(){return itemPort(exterior());}
    public IFluidHandler fluidPort(Direction side){
        // Keep the physical pipe endpoint stable while certification is absent.
        // Its type/capacity remain discoverable, but all resource access is revoked.
        // Otherwise Create disconnects the pipe arm before the reactor can re-form.
        if(role!=ReactorRole.MANA || side==null || isRemoved() || knownExterior!=null && side!=knownExterior)return null;
        return fluids.computeIfAbsent(side,face->new IFluidHandler(){
            private ReactorResources storage(){return accessible(face)?binding.owner().operation.resources:null;}
            @Override public int getTanks(){return 1;}
            @Override public FluidStack getFluidInTank(int tank){var s=storage();return s==null?FluidStack.EMPTY:s.mana.getFluid().copy();}
            @Override public int getTankCapacity(int tank){var s=storage();return s==null?binding.owner()!=null?binding.owner().operation.resources.manaCapacity():16000:s.manaCapacity();}
            @Override public boolean isFluidValid(int tank,FluidStack fluid){return fluid.is(Content.MANA.get());}
            @Override public int fill(FluidStack fluid,FluidAction action){var s=storage();return s==null?0:s.fill(fluid,action);}
            @Override public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
            @Override public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.EMPTY;}
        });
    }
    public IItemHandler itemPort(Direction side){
        if((role!=ReactorRole.OUTPUT && role!=ReactorRole.STABILIZER) || !accessible(side))return null;
        return items.computeIfAbsent(side,face->new IItemHandler(){
            private IItemHandler storage(){return !accessible(face)?null:role==ReactorRole.OUTPUT?binding.owner().operation.resources.matter:binding.owner().operation.stabilizers;}
            @Override public int getSlots(){return role==ReactorRole.OUTPUT?ReactorResources.MATTER_SLOTS:ReactorStabilizers.MAX_SLOTS;}
            @Override public ItemStack getStackInSlot(int slot){var s=storage();return s==null?ItemStack.EMPTY:s.getStackInSlot(slot).copy();}
            @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){var s=storage();return s==null || role==ReactorRole.OUTPUT?stack:s.insertItem(slot,stack,simulate);}
            @Override public ItemStack extractItem(int slot,int amount,boolean simulate){var s=storage();if(s==null || role!=ReactorRole.OUTPUT)return ItemStack.EMPTY;var stack=s.getStackInSlot(slot);return stack.is(ReactorContent.MATTER.get())?s.extractItem(slot,amount,simulate):ItemStack.EMPTY;}
            @Override public int getSlotLimit(int slot){return role==ReactorRole.OUTPUT?64:1;}
            @Override public boolean isItemValid(int slot,ItemStack stack){return storage()!=null && role==ReactorRole.STABILIZER && ReactorStabilizers.usable(stack);}
        });
    }
    int legacyMatter(){int count=0;for(int i=0;i<9;i++)count+=legacyOutput.getStackInSlot(i).getCount();return count;}
    void migrate(ReactorResources storage){
        if(!binding.valid())return;boolean moved=false;
        if(role==ReactorRole.MANA && !legacyMana.isEmpty()){int amount=storage.absorbLegacyMana(legacyMana.getFluid());if(amount>0){legacyMana.drain(amount,IFluidHandler.FluidAction.EXECUTE);moved=true;}}
        if(role==ReactorRole.OUTPUT)for(int i=0;i<9;i++){var stack=legacyOutput.getStackInSlot(i);if(stack.isEmpty() || !stack.is(ReactorContent.MATTER.get()))continue;var rest=ItemHandlerHelper.insertItemStacked(storage.matter,stack.copy(),false);if(rest.getCount()!=stack.getCount()){legacyOutput.setStackInSlot(i,rest);moved=true;}}
        if(moved)setChanged();
    }
    public void setEnergyCapacity(int su){energyCapacity=su;}
    public int energyCapacity(){return binding.valid()?energyCapacity:0;}
    public void maintainReaction(int paidSU){if(role!=ReactorRole.REACTION_CORE)throw new IllegalStateException("Not a reaction core");reactionPower=binding.valid()?paidSU:0;}
    public int convertEnergy(ReactorPartBlockEntity core,int su){if(role!=ReactorRole.ENERGY_CORE)throw new IllegalStateException("Not an energy core");energyCapacity=binding.valid() && core.binding.ownedBy(binding.owner())?Math.min(su,core.reactionPower):0;return energyCapacity;}
    private void changed(){
        if(binding.valid())knownExterior=exterior();else{energyCapacity=0;reactionPower=0;}
        if(level!=null && !isRemoved() && !isChunkUnloaded() && level.hasChunkAt(worldPosition)){
            setChanged();if(!level.isClientSide){level.invalidateCapabilities(worldPosition);
                if(role==ReactorRole.MANA)level.updateNeighborsAt(worldPosition,getBlockState().getBlock());
                sendData();
            }
        }
    }
    @Override public void onChunkUnloaded(){binding.suspended();super.onChunkUnloaded();}
    @Override public void remove(){binding.suspended();super.remove();}
    @Override protected void write(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){super.write(tag,lookup,packet);binding.write(tag);if(knownExterior!=null)tag.putInt("PortExterior",knownExterior.get3DDataValue());if(!legacyMana.isEmpty())tag.put("ReactorMana",legacyMana.writeToNBT(lookup,new CompoundTag()));if(legacyMatter()>0)tag.put("ReactorOutput",legacyOutput.serializeNBT(lookup));}
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.read(tag,lookup,packet);binding.read(tag);energyCapacity=0;reactionPower=0;knownExterior=tag.contains("PortExterior")?Direction.from3DDataValue(tag.getInt("PortExterior")):null;
        if(role==ReactorRole.MANA)legacyMana.readFromNBT(lookup,tag.getCompound("ReactorMana"));
        if(role==ReactorRole.OUTPUT){var contents=tag.getCompound("ReactorOutput").copy();contents.putInt("Size",9);legacyOutput.deserializeNBT(lookup,contents);}
    }
}
