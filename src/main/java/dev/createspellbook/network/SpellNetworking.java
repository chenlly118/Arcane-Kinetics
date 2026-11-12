package dev.createspellbook.network;

import dev.createspellbook.CreateSpellbook;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid=CreateSpellbook.ID,bus=EventBusSubscriber.Bus.MOD)
public final class SpellNetworking {
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        // Persistent reactor accident snapshots and authenticated menu telemetry.
        var registrar=event.registrar("14");
        registrar.playToServer(WrenchModePayload.TYPE,WrenchModePayload.CODEC,(message,context)->context.enqueueWork(()->{
            if(context.player() instanceof net.minecraft.server.level.ServerPlayer player)
                dev.createspellbook.item.EnchantedWrenchItem.selectMode(player,message.slot(),message.wrenchMode());
        }));
        registrar.playToClient(MechanicSwingPayload.TYPE,MechanicSwingPayload.CODEC,
                (message,context)->context.enqueueWork(()->dev.createspellbook.client.MechanicProjection.swing(message)));
        registrar.playToClient(ReactorAccidentPayload.TYPE,ReactorAccidentPayload.CODEC,(message,context)->context.enqueueWork(()->dev.createspellbook.client.ReactorAccidentVisual.accept(message.state())));
        registrar.playToClient(ReactorDebrisPayload.TYPE,ReactorDebrisPayload.CODEC,(message,context)->context.enqueueWork(()->dev.createspellbook.client.ReactorDebrisVisual.accept(message)));
    }
    private SpellNetworking() {}
}
