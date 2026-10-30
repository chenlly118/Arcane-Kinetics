package dev.createspellbook.devices;

import com.simibubi.create.content.logistics.depot.*;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.*;

/** A skin of Create's Depot. No private processing inventory or exclusive recipes. */
public final class ArcaneDepotBlockEntity extends DepotBlockEntity {
    public ArcaneDepotBlockEntity(BlockPos pos,BlockState state){super(DeviceContent.DEPOT_BE.get(),pos,state);}
    public IItemHandler automation(Direction side){return getBehaviour(DepotBehaviour.TYPE).itemHandler;}
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        if(tag.contains("Inventory") && !tag.contains("HeldItem") && !tag.contains("OutputBuffer")){
            // Convert both legacy slots once, before Create reads its native data.
            var old=new ItemStackHandler(2);old.deserializeNBT(lookup,tag.getCompound("Inventory"));
            tag=tag.copy();
            if(!old.getStackInSlot(0).isEmpty())tag.put("HeldItem",new TransportedItemStack(old.getStackInSlot(0).copy()).serializeNBT(lookup));
            var output=new ItemStackHandler(8);output.setStackInSlot(0,old.getStackInSlot(1).copy());
            tag.put("OutputBuffer",output.serializeNBT(lookup));tag.remove("Inventory");
        }
        super.read(tag,lookup,packet);
    }
}
