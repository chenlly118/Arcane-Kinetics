package dev.createspellbook.reactor;

import dev.createspellbook.Balance;
import dev.createspellbook.Content;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.*;

/** One controller-owned tank and 1,024-item buffer. Ports never save copies. */
public final class ReactorResources {
    public static final int MATTER_CAPACITY=1024,MATTER_SLOTS=16;
    private final Runnable changed;
    private final java.util.function.Supplier<ReactorLayout> geometry;
    public final FluidTank mana=new FluidTank(16000,fluid->fluid.is(Content.MANA.get())){
        @Override protected void onContentsChanged(){setCapacity(Math.max(nominalManaCapacity(),getFluidAmount()));changed.run();}
    };
    public final ItemStackHandler matter=new ItemStackHandler(MATTER_SLOTS){
        @Override public boolean isItemValid(int slot,ItemStack stack){return stack.is(ReactorContent.MATTER.get());}
        @Override protected void onContentsChanged(int slot){changed.run();}
    };
    public ReactorResources(Runnable changed){this(changed,()->null);}
    public ReactorResources(Runnable changed,java.util.function.Supplier<ReactorLayout> geometry){this.changed=changed;this.geometry=geometry;}
    public static int volumeCapacity(ReactorLayout layout,int perCell){int inner=layout.inner();return (int)Math.min(Integer.MAX_VALUE,(long)inner*inner*inner*Math.max(1,perCell));}
    public int nominalManaCapacity(){var layout=geometry.get();return layout==null?(Balance.SPEC.isLoaded()?Balance.REACTOR_TANK.get():16000):volumeCapacity(layout,Balance.SPEC.isLoaded()?Balance.REACTOR_MANA_PER_CELL.get():4000);}
    public int manaCapacity(){return Math.max(nominalManaCapacity(),mana.getFluidAmount());}
    public void refreshCapacity(){mana.setCapacity(manaCapacity());}
    public int fill(FluidStack fluid,FluidAction action){
        if(!fluid.is(Content.MANA.get()))return 0;
        int room=Math.max(0,nominalManaCapacity()-mana.getFluidAmount());if(room==0)return 0;
        mana.setCapacity(manaCapacity());return mana.fill(fluid.copyWithAmount(Math.min(room,fluid.getAmount())),action);
    }
    /** Keep old per-port mana even above the new capacity, without accepting extra. */
    int absorbLegacyMana(FluidStack fluid){
        if(!fluid.is(Content.MANA.get()))return 0;
        int amount=(int)Math.min(fluid.getAmount(),(long)Integer.MAX_VALUE-mana.getFluidAmount());if(amount<=0)return 0;
        mana.setCapacity(Math.max(nominalManaCapacity(),mana.getFluidAmount()+amount));return mana.fill(fluid.copyWithAmount(amount),FluidAction.EXECUTE);
    }
    public int count(){int count=0;for(int i=0;i<MATTER_SLOTS;i++)count+=matter.getStackInSlot(i).getCount();return count;}
    public boolean full(){return count()>=MATTER_CAPACITY;}
    public boolean acceptsMatter(){return ItemHandlerHelper.insertItemStacked(matter,new ItemStack(ReactorContent.MATTER.get()),true).isEmpty();}
    public void emit(){if(!ItemHandlerHelper.insertItemStacked(matter,new ItemStack(ReactorContent.MATTER.get()),false).isEmpty())throw new IllegalStateException("Shared reactor buffer changed inside a server transaction");}
    public void write(CompoundTag tag,HolderLookup.Provider lookup){tag.put("Mana",mana.writeToNBT(lookup,new CompoundTag()));tag.put("Matter",matter.serializeNBT(lookup));}
    public void read(CompoundTag tag,HolderLookup.Provider lookup){
        mana.readFromNBT(lookup,tag.getCompound("Mana"));mana.setCapacity(manaCapacity());
        var inventory=tag.getCompound("Matter").copy();inventory.putInt("Size",MATTER_SLOTS);matter.deserializeNBT(lookup,inventory);
    }
}
