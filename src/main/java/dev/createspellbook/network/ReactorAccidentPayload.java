package dev.createspellbook.network;

import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.reactor.ReactorAccident;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/** Server-only authority. Stage 0 withdraws an existing visual; no C2S handler. */
public record ReactorAccidentPayload(UUID id,ResourceLocation dimension,BlockPos center,net.minecraft.world.phys.Vec3 position,long serverTick,long started,long warningEnd,long runawayEnd,long end,int phase,int radius,int causes,float visibleFraction){
    public ReactorAccidentPayload(UUID id,ResourceLocation dimension,BlockPos center,net.minecraft.world.phys.Vec3 position,long serverTick,long started,long warningEnd,long runawayEnd,long end,int phase,int radius,int causes){this(id,dimension,center,position,serverTick,started,warningEnd,runawayEnd,end,phase,radius,causes,.82f);}
    public ReactorAccidentPayload(UUID id,ResourceLocation dimension,BlockPos center,long serverTick,long started,long warningEnd,long runawayEnd,long end,int phase,int radius,int causes){this(id,dimension,center,center.getCenter(),serverTick,started,warningEnd,runawayEnd,end,phase,radius,causes);}
    public static final CustomPacketPayload.Type<Message> TYPE=new CustomPacketPayload.Type<>(CreateSpellbook.id("reactor_accident"));
    public record Message(ReactorAccidentPayload state) implements CustomPacketPayload{
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static final StreamCodec<RegistryFriendlyByteBuf,Message> CODEC=new StreamCodec<>(){
        @Override public void encode(RegistryFriendlyByteBuf b,Message m){var s=m.state();b.writeUUID(s.id);b.writeResourceLocation(s.dimension);b.writeBlockPos(s.center);b.writeDouble(s.position.x);b.writeDouble(s.position.y);b.writeDouble(s.position.z);b.writeVarLong(s.serverTick);b.writeVarLong(s.started);b.writeVarLong(s.warningEnd);b.writeVarLong(s.runawayEnd);b.writeVarLong(s.end);b.writeVarInt(s.phase);b.writeVarInt(s.radius);b.writeVarInt(s.causes);b.writeFloat(s.visibleFraction);}
        @Override public Message decode(RegistryFriendlyByteBuf b){return new Message(new ReactorAccidentPayload(b.readUUID(),b.readResourceLocation(),b.readBlockPos(),new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readFloat()));}
    };
    public static Message of(ReactorAccident a,ResourceLocation dimension,long now,boolean remove){return new Message(new ReactorAccidentPayload(a.id,dimension,a.center,a.position(),now,a.started,a.warningEnd,a.runawayEnd,a.end,remove?0:a.phase(now).ordinal()+1,a.radius,a.causes,dev.createspellbook.Balance.REACTOR_VISUAL_FRACTION.get().floatValue()));}
}
