package dev.createspellbook.client;

import com.mojang.math.Axis;
import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.network.ReactorDebrisPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.util.*;

/** Lightweight render objects only: no FallingBlockEntity, items, block writes,
 * collision, drops or inventories. Only actual server removal samples enter. */
@EventBusSubscriber(modid=CreateSpellbook.ID,value=Dist.CLIENT)
public final class ReactorDebrisVisual {
    public static final int MAX_TOTAL=128,MAX_PER_FIELD=32;
    private record Key(UUID field,long tick,long pos){}
    private record Fragment(Key key,net.minecraft.world.level.block.state.BlockState state,Vec3 start,Vec3 end,long received,int duration,int light){}
    private static final Map<Key,Fragment> FRAGMENTS=new LinkedHashMap<>();
    private static Object lastLevel;
    private static long accepted,rendered;
    private static com.mojang.blaze3d.vertex.ByteBufferBuilder scratch;
    private static net.minecraft.client.renderer.MultiBufferSource.BufferSource batch;
    private static void closeBatch(){if(scratch!=null)scratch.close();scratch=null;batch=null;}
    private static void scope(){var l=Minecraft.getInstance().level;if(lastLevel!=l){FRAGMENTS.clear();closeBatch();lastLevel=l;accepted=rendered=0;}}
    public static int count(){scope();return FRAGMENTS.size();}
    public static long accepted(){scope();return accepted;}
    public static long rendered(){scope();return rendered;}
    public static void withdraw(UUID field){FRAGMENTS.values().removeIf(f->f.key.field.equals(field));}
    public static void accept(ReactorDebrisPayload message){
        scope();var mc=Minecraft.getInstance();var l=mc.level;
        if(l==null || !l.dimension().location().equals(message.dimension()) || !ReactorAccidentVisual.activeField(message.field()))return;
        for(var sample:message.samples()){
            var key=new Key(message.field(),message.tick(),sample.pos().asLong());if(FRAGMENTS.containsKey(key))continue;
            var state=Block.stateById(sample.state());var start=sample.pos().getCenter();
            if(state.isAir() || state.hasBlockEntity() || state.getRenderShape()!=RenderShape.MODEL || start.distanceToSqr(message.center())>600*600)continue;
            if(FRAGMENTS.values().stream().filter(f->f.key.field.equals(message.field())).count()>=MAX_PER_FIELD){
                var old=FRAGMENTS.values().stream().filter(f->f.key.field.equals(message.field())).findFirst().orElseThrow();FRAGMENTS.remove(old.key);
            }
            while(FRAGMENTS.size()>=MAX_TOTAL)FRAGMENTS.remove(FRAGMENTS.keySet().iterator().next());
            int light=l.hasChunkAt(sample.pos())?LevelRenderer.getLightColor(l,sample.pos()):15728880;
            // The client may still have the just-removed opaque source cell.
            // Sample its exposed neighbors, as for the reactor's enclosed shaft.
            for(var d:net.minecraft.core.Direction.values()){
                var pos=sample.pos().relative(d);if(!l.hasChunkAt(pos))continue;int other=LevelRenderer.getLightColor(l,pos);
                light=(Math.max((light>>4)&15,(other>>4)&15)<<4)|(Math.max((light>>20)&15,(other>>20)&15)<<20);
            }
            FRAGMENTS.put(key,new Fragment(key,state,start,message.center(),l.getGameTime(),Math.clamp((int)(start.distanceTo(message.center())*.12)+20,20,48),light));accepted++;
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){FRAGMENTS.clear();closeBatch();lastLevel=null;}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        scope();var l=Minecraft.getInstance().level;if(l==null)return;
        FRAGMENTS.values().removeIf(f->l.getGameTime()-f.received>=f.duration || !ReactorAccidentVisual.activeField(f.key.field));
    }
    /** Called by the escaped-field pass while its extended projection is active.
     * Using the ordinary world projection would depth-test nearby fragments
     * behind a farther sphere whose depth was encoded with a different far plane. */
    public static void render(RenderLevelStageEvent event){
        scope();var mc=Minecraft.getInstance();if(mc.level==null || FRAGMENTS.isEmpty())return;
        var camera=event.getCamera().getPosition();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if(scratch==null){scratch=new com.mojang.blaze3d.vertex.ByteBufferBuilder(65536);batch=net.minecraft.client.renderer.MultiBufferSource.immediate(scratch);}
        var pose=event.getPoseStack();var buffers=batch;
        for(var f:FRAGMENTS.values()){
            float t=Math.clamp((mc.level.getGameTime()-f.received+partial)/f.duration,0,1);
            // A short visible detachment before acceleration keeps thin outer
            // shell samples from disappearing behind the horizon immediately.
            float travel=Math.clamp((t-.18f)/.82f,0,1),ease=travel*travel*(2-travel);
            var pos=f.start.lerp(f.end,ease);double swirl=Math.min(2,f.start.distanceTo(f.end)*.06)*Math.sin(t*Math.PI);
            double phase=(f.key.pos&65535)*.01;
            pos=pos.add(Math.sin(t*8+phase)*swirl,Math.cos(t*6)*swirl*.3,Math.cos(t*8+phase)*swirl);
            if(camera.distanceToSqr(pos)>192*192)continue;float scale=Math.max(.025f,1-ease);
            pose.pushPose();pose.translate(pos.x-camera.x,pos.y-camera.y,pos.z-camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(t*180+(f.key.pos&31)));pose.mulPose(Axis.XP.rotationDegrees(t*110));pose.scale(scale,scale,scale);pose.translate(-.5,-.5,-.5);
            mc.getBlockRenderer().renderSingleBlock(f.state,pose,buffers,f.light,OverlayTexture.NO_OVERLAY);pose.popPose();rendered++;
        }
        // Submit native block sheets before the field pass restores its far
        // plane, without flushing unrelated world / block-entity buffers.
        buffers.endBatch();
    }
    private ReactorDebrisVisual(){}
}
