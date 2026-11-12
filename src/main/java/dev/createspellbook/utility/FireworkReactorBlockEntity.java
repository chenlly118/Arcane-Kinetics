package dev.createspellbook.utility;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.createspellbook.Balance;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import java.util.List;

public final class FireworkReactorBlockEntity extends SmartBlockEntity {
    private boolean syncQueued;
    private ItemStack template=ItemStack.EMPTY;
    private int cooldown;
    private long launches;
    private long lastActionTick=Long.MIN_VALUE;
    public final RegeneratingManaTank mana=new RegeneratingManaTank(()->Balance.AUTO_CAPACITY.get(),()->Balance.AUTO_REGEN.get(),this::setChanged,()->syncQueued=true);
    public FireworkReactorBlockEntity(BlockPos pos,BlockState state){super(UtilityContent.FIREWORK_BE.get(),pos,state);}
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours){}
    public void configure(ItemStack stack){template=stack.is(Items.FIREWORK_ROCKET)?stack.copyWithCount(1):ItemStack.EMPTY;setChanged();syncQueued=true;}
    public ItemStack template(){return template.copy();}
    public long launches(){return launches;}
    public int cooldown(){return cooldown;}
    public int supplyBasin(dev.createspellbook.devices.FocusingBasinBlockEntity basin){
        return level instanceof net.minecraft.server.level.ServerLevel && !isRemoved() && !isChunkUnloaded() && basin.getLevel()==level && worldPosition.below().equals(basin.getBlockPos())
                ?mana.transferToBasin(basin,Balance.BASIN_ABSORPTION.get()):0;
    }
    public IFluidHandler inputPort(Direction side){return side==null || side==getBlockState().getValue(FireworkReactorBlock.FACING).getOpposite()?mana:null;}
    @Override public void tick(){
        super.tick();if(level==null || level.isClientSide || isChunkUnloaded() || isRemoved())return;
        if(lastActionTick==level.getGameTime())return;lastActionTick=level.getGameTime();
        mana.tick(level.getGameTime());
        if(cooldown>0){cooldown--;setChanged();}
        if(cooldown==0 && !template.isEmpty() && !level.hasNeighborSignal(worldPosition) && mana.consume(Balance.FIREWORK_MANA.get(),false)){
            var rocket=new FireworkRocketEntity(level,worldPosition.getX()+.5,worldPosition.getY()+1.4,worldPosition.getZ()+.5,template.copyWithCount(1));
            if(level.addFreshEntity(rocket)){
                mana.consume(Balance.FIREWORK_MANA.get(),true);launches++;syncQueued=true;
            }
            cooldown=Balance.FIREWORK_INTERVAL.get();setChanged();
        }
        if(syncQueued){syncQueued=false;sendData();}
    }
    @Override protected void write(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.write(tag,lookup,packet);mana.save(tag);tag.putInt("FireworkCooldown",cooldown);tag.putLong("Launches",launches);
        if(!template.isEmpty())tag.put("FireworkTemplate",template.save(lookup));
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.read(tag,lookup,packet);mana.load(tag,packet);cooldown=Math.clamp(tag.getInt("FireworkCooldown"),0,Balance.FIREWORK_INTERVAL.get());
        launches=Math.max(0,tag.getLong("Launches"));
        var loaded=ItemStack.parseOptional(lookup,tag.getCompound("FireworkTemplate"));template=loaded.is(Items.FIREWORK_ROCKET)?loaded.copyWithCount(1):ItemStack.EMPTY;
    }
}
