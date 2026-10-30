package dev.createspellbook.reactor;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.*;

/** One authoritative 9..289 usable inventory; overflow is retained for recovery. */
public final class ReactorStabilizers extends ItemStackHandler {
    private final Runnable changed;
    public static final int MAX_SLOTS=289;
    private int cursor,activeSlots=49;
    public int activeSlots(){return activeSlots;}
    public void capacity(int n){activeSlots=Math.clamp(n,9,MAX_SLOTS);changed.run();}
    public ReactorStabilizers(Runnable changed){super(MAX_SLOTS);this.changed=changed;}
    public static boolean usable(ItemStack stack){return stack.is(ReactorContent.STABILIZER.get()) && stack.getCount()==1 && stack.getDamageValue()<stack.getMaxDamage();}
    @Override public int getSlotLimit(int slot){return 1;}
    @Override public boolean isItemValid(int slot,ItemStack stack){return slot<activeSlots && usable(stack);}
    @Override protected void onContentsChanged(int slot){changed.run();}
    public int working(){int n=0;for(int i=0;i<activeSlots;i++)if(usable(getStackInSlot(i)))n++;return n;}
    public int reserves(){int n=0;for(int i=activeSlots;i<MAX_SLOTS;i++)if(usable(getStackInSlot(i)))n++;return n;}
    private int count(List<ItemStack> list){int n=0;for(int i=0;i<activeSlots;i++)if(usable(list.get(i)))n++;return n;}
    private boolean replace(List<ItemStack> list,int slot){for(int i=activeSlots;i<MAX_SLOTS;i++)if(usable(list.get(i))){list.set(slot,list.get(i));list.set(i,ItemStack.EMPTY);return true;}return false;}
    private List<ItemStack> copies(){var result=new ArrayList<ItemStack>(MAX_SLOTS);for(int i=0;i<MAX_SLOTS;i++)result.add(getStackInSlot(i).copy());return result;}
    public void replenish(int required){
        if(working()>=required || reserves()==0)return;
        var list=copies();int count=count(list);boolean moved=false;
        for(int i=0;i<activeSlots && count<required;i++)if(list.get(i).isEmpty() && replace(list,i)){count++;moved=true;}
        if(moved)apply(new Wear(list,cursor));
    }
    public record Wear(List<ItemStack> stacks,int nextCursor){}
    public Wear planWear(int points,int required){
        if(working()<required)return null;var list=copies();int next=cursor;
        for(int point=0;point<points;point++){
            int selected=-1;for(int i=0;i<activeSlots;i++){int slot=(next+i)%activeSlots;if(usable(list.get(slot))){selected=slot;break;}}
            if(selected<0)return null;
            var stack=list.get(selected);stack.setDamageValue(stack.getDamageValue()+1);next=(selected+1)%activeSlots;
            if(stack.getDamageValue()>=stack.getMaxDamage()){list.set(selected,ItemStack.EMPTY);replace(list,selected);}
        }
        return new Wear(list,next);
    }
    public void apply(Wear wear){for(int i=0;i<MAX_SLOTS;i++)stacks.set(i,wear.stacks().get(i));cursor=wear.nextCursor();changed.run();}
    @Override public CompoundTag serializeNBT(HolderLookup.Provider lookup){var tag=super.serializeNBT(lookup);tag.putInt("WearCursor",cursor);tag.putInt("ActiveSlots",activeSlots);return tag;}
    @Override public void deserializeNBT(HolderLookup.Provider lookup,CompoundTag source){var tag=source.copy();activeSlots=tag.contains("ActiveSlots")?Math.clamp(tag.getInt("ActiveSlots"),9,MAX_SLOTS):49;tag.putInt("Size",MAX_SLOTS);super.deserializeNBT(lookup,tag);cursor=Math.floorMod(tag.getInt("WearCursor"),activeSlots);}
}
