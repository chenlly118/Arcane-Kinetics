package dev.createspellbook.utility;

import dev.createspellbook.Content;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import java.util.function.IntSupplier;

/** Shared auto-caster reservoir: loaded-tick regeneration and an input-only port. */
public final class RegeneratingManaTank implements IFluidHandler {
    private final IntSupplier capacity,regeneration;
    private final Runnable dirty,synchronize;
    private int amount;
    private long lastTick=Long.MIN_VALUE,recovered;
    public RegeneratingManaTank(IntSupplier capacity,IntSupplier regeneration,Runnable dirty,Runnable synchronize){
        this.capacity=capacity;this.regeneration=regeneration;this.dirty=dirty;this.synchronize=synchronize;
    }
    public int amount(){return amount;}
    public int capacity(){return capacity.getAsInt();}
    public long recovered(){return recovered;}
    public void tick(long now){
        if(now==lastTick)return;lastTick=now;
        int gain=Math.min(regeneration.getAsInt(),Math.max(0,capacity()-amount));
        if(gain>0){amount+=gain;recovered+=gain;dirty.run();if(now%10==0 || amount==capacity())synchronize.run();}
    }
    public boolean consume(int count,boolean execute){
        if(count<0 || count>amount)return false;
        if(execute && count>0){amount-=count;dirty.run();synchronize.run();}return true;
    }
    /** Explicit stacked-basin transfer, separate from the input-only public capability. */
    public int transferToBasin(dev.createspellbook.devices.FocusingBasinBlockEntity basin,int limit){
        int offered=Math.min(amount,Math.max(0,limit));if(offered==0)return 0;
        int room=basin.tank.fill(new FluidStack(Content.MANA.get(),offered),FluidAction.SIMULATE);if(room<=0)return 0;
        int accepted=basin.tank.fill(new FluidStack(Content.MANA.get(),Math.min(offered,room)),FluidAction.EXECUTE);
        if(accepted>0)consume(accepted,true);return accepted;
    }
    public void save(CompoundTag tag){tag.putInt("InternalMana",amount);tag.putLong("RecoveredMana",recovered);}
    public void load(CompoundTag tag,boolean packet){
        amount=Math.clamp(tag.getInt("InternalMana"),0,capacity());recovered=Math.max(0,tag.getLong("RecoveredMana"));
        if(!packet)lastTick=Long.MIN_VALUE;
    }
    private void check(int tank){if(tank!=0)throw new IndexOutOfBoundsException(tank);}
    @Override public int getTanks(){return 1;}
    @Override public FluidStack getFluidInTank(int tank){check(tank);return amount==0?FluidStack.EMPTY:new FluidStack(Content.MANA.get(),amount);}
    @Override public int getTankCapacity(int tank){check(tank);return capacity();}
    @Override public boolean isFluidValid(int tank,FluidStack fluid){check(tank);return fluid.is(Content.MANA.get());}
    @Override public int fill(FluidStack fluid,FluidAction action){
        if(fluid.isEmpty() || !fluid.is(Content.MANA.get()))return 0;
        int accepted=Math.min(fluid.getAmount(),Math.max(0,capacity()-amount));
        if(action.execute() && accepted>0){amount+=accepted;dirty.run();synchronize.run();}return accepted;
    }
    @Override public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
    @Override public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.EMPTY;}
}
