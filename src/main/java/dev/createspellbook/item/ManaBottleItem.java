package dev.createspellbook.item;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ManaBottleItem extends Item {
    public ManaBottleItem(Properties properties) { super(properties); }
    @Override public int getUseDuration(ItemStack stack,LivingEntity entity) { return 32; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level,player,hand);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) {
        if(!level.isClientSide && entity instanceof ServerPlayer player) {
            CriteriaTriggers.CONSUME_ITEM.trigger(player,stack);
            var data=MagicData.getPlayerMagicData(player);
            // Iron's public API applies max mana, its change event and cancellation.
            data.addMana(100);
            PacketDistributor.sendToPlayer(player,new SyncManaPacket(data));
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        if(entity instanceof Player player && player.getAbilities().instabuild) return stack;
        stack.shrink(1);
        if(stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
        if(!level.isClientSide && entity instanceof Player player) {
            var empty=new ItemStack(Items.GLASS_BOTTLE);
            if(!player.getInventory().add(empty)) player.drop(empty,false);
        }
        return stack;
    }
}
