package dev.createspellbook.network;

import dev.createspellbook.CreateSpellbook;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded server-authored samples of blocks that were actually swallowed.
 * This packet cannot create, move or drop a real block on either side. */
public record ReactorDebrisPayload(UUID field,ResourceLocation dimension,Vec3 center,long tick,List<Sample> samples) implements CustomPacketPayload {
    public static final int MAX_SAMPLES=16;
    public record Sample(BlockPos pos,int state){}
    public ReactorDebrisPayload{samples=List.copyOf(samples);if(samples.size()>MAX_SAMPLES)throw new IllegalArgumentException("Debris batch too large");}
    public static final Type<ReactorDebrisPayload> TYPE=new Type<>(CreateSpellbook.id("reactor_debris"));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static final StreamCodec<RegistryFriendlyByteBuf,ReactorDebrisPayload> CODEC=new StreamCodec<>(){
        @Override public void encode(RegistryFriendlyByteBuf b,ReactorDebrisPayload m){
            b.writeUUID(m.field);b.writeResourceLocation(m.dimension);b.writeDouble(m.center.x);b.writeDouble(m.center.y);b.writeDouble(m.center.z);b.writeVarLong(m.tick);b.writeVarInt(m.samples.size());
            for(var s:m.samples){b.writeBlockPos(s.pos);b.writeVarInt(s.state);}
        }
        @Override public ReactorDebrisPayload decode(RegistryFriendlyByteBuf b){
            var id=b.readUUID();var dim=b.readResourceLocation();var c=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());long tick=b.readVarLong();int n=b.readVarInt();
            if(n<0 || n>MAX_SAMPLES)throw new io.netty.handler.codec.DecoderException("Debris batch outside bounds");
            var list=new ArrayList<Sample>(n);for(int i=0;i<n;i++)list.add(new Sample(b.readBlockPos(),b.readVarInt()));return new ReactorDebrisPayload(id,dim,c,tick,list);
        }
    };
}
