package dev.createspellbook.devices;

import dev.createspellbook.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class FocusingBasinBlockEntity extends SpellDeviceBlockEntity {
    private double fractionalMana;
    public final FluidTank tank=new FluidTank(Balance.BASIN_CAPACITY.get(),fluid->fluid.is(Content.MANA.get())) {
        @Override protected void onContentsChanged() {
            // Lowering capacity never destroys already stored mana.
            setCapacity(Math.max(getFluidAmount(),Balance.BASIN_CAPACITY.get()));changed();
        }
    };
    public FocusingBasinBlockEntity(BlockPos pos,BlockState state) {super(DeviceContent.BASIN_BE.get(),pos,state);}
    @Override protected void handleSpell(SpellInteraction hit) {
        if(hit.damage()<=0)return;
        int space=tank.getSpace();
        if(space<=0){if(fractionalMana!=0){fractionalMana=0;changed();}return;}
        double amount=hit.damage()*Balance.MANA_PER_DAMAGE.get()+fractionalMana;
        int whole=(int)Math.min(space,Math.floor(amount));
        fractionalMana=amount>=space?0:amount-whole;
        if(whole>0)tank.fill(new FluidStack(Content.MANA.get(),whole),FluidAction.EXECUTE);
        changed();
    }
    @Override public InteractionResult playerUse(Player player,InteractionHand hand) {
        if(level.isClientSide)return player.getItemInHand(hand).isEmpty() || FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()?InteractionResult.SUCCESS:InteractionResult.PASS;
        if(FluidUtil.interactWithFluidHandler(player,hand,tank))return InteractionResult.SUCCESS;
        if(player.getItemInHand(hand).isEmpty()) {
            if(!player.level().isClientSide)player.displayClientMessage(Component.translatable("message.create_spellbook.basin_contents",tank.getFluidAmount(),tank.getCapacity()),true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override protected void write(CompoundTag tag,HolderLookup.Provider registries,boolean packet) {
        super.write(tag,registries,packet);tag.put("Mana",tank.writeToNBT(registries,new CompoundTag()));
        tag.putDouble("ManaFraction",fractionalMana);tag.putInt("TankCapacity",tank.getCapacity());
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider registries,boolean packet) {
        super.read(tag,registries,packet);tank.readFromNBT(registries,tag.getCompound("Mana"));
        tank.setCapacity(Math.max(tank.getFluidAmount(),packet?tag.getInt("TankCapacity"):Balance.BASIN_CAPACITY.get()));
        double fraction=tag.getDouble("ManaFraction");fractionalMana=Double.isFinite(fraction)?Math.clamp(fraction,0,1):0;
    }
}
