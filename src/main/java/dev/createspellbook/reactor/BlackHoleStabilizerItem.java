package dev.createspellbook.reactor;

import dev.createspellbook.Balance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import java.util.List;

/** Reactor fuel, not a tool: direct accounted wear cannot be avoided with Unbreaking. */
@net.neoforged.fml.common.EventBusSubscriber(modid=dev.createspellbook.CreateSpellbook.ID)
public final class BlackHoleStabilizerItem extends Item {
    public BlackHoleStabilizerItem(){super(new Item.Properties().durability(1800).setNoRepair().rarity(Rarity.RARE));}
    @Override public int getMaxDamage(ItemStack stack){return Balance.SPEC.isLoaded()?Balance.REACTOR_DURABILITY.get():1800;}
    @Override public boolean isEnchantable(ItemStack stack){return false;}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return false;}
    @Override public boolean isPrimaryItemFor(ItemStack stack,Holder<Enchantment> enchantment){return false;}
    @Override public boolean supportsEnchantment(ItemStack stack,Holder<Enchantment> enchantment){return false;}
    @Override public boolean canGrindstoneRepair(ItemStack stack){return false;}
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack repair){return false;}
    @net.neoforged.bus.api.SubscribeEvent public static void anvil(net.neoforged.neoforge.event.AnvilUpdateEvent event){
        if(event.getLeft().is(ReactorContent.STABILIZER.get()) && !event.getRight().isEmpty())event.setCanceled(true);
    }
}
