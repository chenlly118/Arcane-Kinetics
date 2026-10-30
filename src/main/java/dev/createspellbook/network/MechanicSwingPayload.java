package dev.createspellbook.network;

import dev.createspellbook.CreateSpellbook;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** One small S2C message per accepted melee follow-up, never a following entity. */
public record MechanicSwingPayload(int playerId,int targetId,long started) implements CustomPacketPayload {
    public static final Type<MechanicSwingPayload> TYPE=new Type<>(CreateSpellbook.id("mechanic_swing"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MechanicSwingPayload> CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_INT,MechanicSwingPayload::playerId,
            ByteBufCodecs.VAR_INT,MechanicSwingPayload::targetId,
            ByteBufCodecs.VAR_LONG,MechanicSwingPayload::started,MechanicSwingPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
