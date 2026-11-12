package dev.createspellbook.client;

import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.item.EnchantedWrenchItem;
import dev.createspellbook.network.WrenchModePayload;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CreateSpellbook.ID, value = Dist.CLIENT)
public final class WrenchModeInput {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void scroll(InputEvent.MouseScrollingEvent event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (mc.screen != null || player == null || !player.isShiftKeyDown() || event.getScrollDeltaY() == 0) return;
        int slot = player.getInventory().selected;
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof EnchantedWrenchItem)) {
            slot = 40;
            stack = player.getOffhandItem();
        }
        if (!(stack.getItem() instanceof EnchantedWrenchItem)) return;
        event.setCanceled(true);
        if (player.isUsingItem() || ClientMagicData.isCasting()) return;
        boolean wrenchMode = event.getScrollDeltaY() > 0;
        if (wrenchMode != EnchantedWrenchItem.isWrenchMode(stack))
            PacketDistributor.sendToServer(new WrenchModePayload(slot, wrenchMode));
    }
}
