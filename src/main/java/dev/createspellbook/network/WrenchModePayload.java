package dev.createspellbook.network;

import dev.createspellbook.CreateSpellbook;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record WrenchModePayload(int slot, boolean wrenchMode) implements CustomPacketPayload {
    public static final Type<WrenchModePayload> TYPE = new Type<>(CreateSpellbook.id("wrench_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WrenchModePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WrenchModePayload::slot,
            ByteBufCodecs.BOOL, WrenchModePayload::wrenchMode, WrenchModePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
