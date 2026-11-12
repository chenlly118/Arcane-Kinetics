package dev.createspellbook.item;

import com.simibubi.create.AllItems;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;

/** Keep native casting and wrench interactions exclusive, including Create's identity checks. */
public final class EnchantedWrenchItem extends StaffItem {
    private static final String MODE_KEY = "create_spellbook_wrench_mode";

    public EnchantedWrenchItem(Properties properties) {
        super(properties);
    }

    public static boolean isWrenchMode(ItemStack stack) {
        return stack.getItem() instanceof EnchantedWrenchItem
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(MODE_KEY);
    }

    public static void setWrenchMode(ItemStack stack, boolean wrenchMode) {
        if (!(stack.getItem() instanceof EnchantedWrenchItem)) return;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (wrenchMode) {
            data.putBoolean(MODE_KEY, true);
            stack.remove(ComponentRegistry.CASTING_IMPLEMENT.get());
        } else {
            data.remove(MODE_KEY);
            stack.set(ComponentRegistry.CASTING_IMPLEMENT.get(), Unit.INSTANCE);
        }
        if (data.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    /** Slot validation keeps a delayed scroll packet from changing another held item. */
    public static void selectMode(ServerPlayer player, int slot, boolean wrenchMode) {
        if (!player.isAlive() || player.isSpectator() || !player.isShiftKeyDown()
                || player.isUsingItem() || MagicData.getPlayerMagicData(player).isCasting()) return;
        if (slot != player.getInventory().selected && slot != 40) return;
        var stack = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof EnchantedWrenchItem)) return;
        setWrenchMode(stack, wrenchMode);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable(wrenchMode
                ? "message.create_spellbook.wrench_mode" : "message.create_spellbook.staff_mode"), true);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        return isWrenchMode(context.getItemInHand()) ? AllItems.WRENCH.get().useOn(context) : super.useOn(context);
    }

    @Override public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return isWrenchMode(stack) || super.doesSneakBypassUse(stack, level, pos, player);
    }
}
