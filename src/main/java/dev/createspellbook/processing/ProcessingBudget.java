package dev.createspellbook.processing;

import dev.createspellbook.Balance;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

/** Shared by all projectiles/recasts belonging to one paid cast. Server thread only. */
public final class ProcessingBudget implements ICastDataSerializable {
    public int remaining;
    public UUID token = UUID.randomUUID();
    /** Server-only audio cadence; does not affect items and is not sent over the network. */
    public long nextSoundTick;
    /** Fractional per-tick channel work; unused whole credits are never banked. */
    public double fanCredit;
    public ProcessingBudget() {}
    public ProcessingBudget(int remaining) { this.remaining=Math.clamp(remaining,0,Balance.MAX_PROCESSING_BUDGET); }
    @Override public void reset() { /* A recast / in-flight construct can still own this budget. */ }
    @Override public void writeToBuffer(FriendlyByteBuf buf) { buf.writeVarInt(remaining); buf.writeUUID(token); }
    @Override public void readFromBuffer(FriendlyByteBuf buf) { remaining=Math.clamp(buf.readVarInt(),0,Balance.MAX_PROCESSING_BUDGET); token=buf.readUUID(); }
    @Override public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag=new CompoundTag(); tag.putInt("Remaining",remaining); tag.putUUID("Token",token); return tag;
    }
    @Override public void deserializeNBT(HolderLookup.Provider provider,CompoundTag tag) {
        remaining=Math.clamp(tag.getInt("Remaining"),0,Balance.MAX_PROCESSING_BUDGET); if(tag.hasUUID("Token")) token=tag.getUUID("Token");
    }
}
