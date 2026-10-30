package dev.createspellbook.client;

import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.network.ReactorAccidentPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.util.*;

/** Bounded, level-scoped projection cache. UUID updates replace, never append.
 * No entities or client-side block/health changes. Stale snapshots self-expire. */
@EventBusSubscriber(modid=CreateSpellbook.ID,value=Dist.CLIENT)
public final class ReactorAccidentVisual {
    private record View(ReactorAccidentPayload state,long received){}
    private static final Map<UUID,View> VIEWS=new LinkedHashMap<>();
    private static final Set<BlockPos> CONTAINED_FRAME=new HashSet<>();
    private static Object lastLevel;
    private static long particleTick=Long.MIN_VALUE;
    private static void scope(){var level=Minecraft.getInstance().level;if(lastLevel!=level){VIEWS.clear();CONTAINED_FRAME.clear();lastLevel=level;particleTick=Long.MIN_VALUE;}}
    public static void accept(ReactorAccidentPayload state){
        scope();var level=Minecraft.getInstance().level;if(level==null || !level.dimension().location().equals(state.dimension()))return;
        if(state.phase()==0){VIEWS.remove(state.id());ReactorDebrisVisual.withdraw(state.id());return;}
        if(state.phase()<1 || state.phase()>3 || state.radius()<1 || state.radius()>475 || !Float.isFinite(state.visibleFraction()) || state.visibleFraction()<.5 || state.visibleFraction()>.95 || state.end()<=state.runawayEnd() || state.runawayEnd()<=state.warningEnd() || state.warningEnd()<=state.started())return;
        if(!VIEWS.containsKey(state.id()) && VIEWS.size()>=16)VIEWS.remove(VIEWS.keySet().iterator().next());
        VIEWS.put(state.id(),new View(state,level.getGameTime()));
    }
    public static int count(){scope();return VIEWS.size();}
    public static boolean activeField(UUID id){scope();var view=VIEWS.get(id);return view!=null && phase(view)>=2;}
    public static java.util.List<ReactorAccidentPayload> snapshots(){scope();return VIEWS.values().stream().map(View::state).toList();}
    public static boolean contains(BlockPos center){scope();return VIEWS.values().stream().anyMatch(v->v.state.center().equals(center));}
    public static boolean escapedAt(BlockPos center){scope();return VIEWS.values().stream().anyMatch(v->phase(v)>=2 && v.state.center().equals(center));}
    public static void containedRendered(BlockPos center){scope();CONTAINED_FRAME.add(center);}
    private static long now(View view){return view.state.serverTick()+Math.max(0,Minecraft.getInstance().level.getGameTime()-view.received);}
    private static int phase(View view){long now=now(view);return now>=view.state.end()?0:now>=view.state.runawayEnd()?3:now>=view.state.warningEnd()?2:1;}
    private static float radius(View view){var s=view.state;return dev.createspellbook.reactor.ReactorAccident.visibleRadius(phase(view),now(view),s.started(),s.warningEnd(),s.runawayEnd(),s.end(),s.radius(),s.visibleFraction());}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){VIEWS.clear();CONTAINED_FRAME.clear();lastLevel=null;}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        scope();var mc=Minecraft.getInstance();var level=mc.level;if(level==null)return;
        VIEWS.values().removeIf(v->phase(v)==0 || level.getGameTime()-v.received>60);
        if(level.getGameTime()%4!=0 || mc.player==null || particleTick==level.getGameTime())return;particleTick=level.getGameTime();int count=0;
        for(var v:VIEWS.values()){
            var center=v.state.position();if(mc.player.position().distanceToSqr(center)>128*128)continue;
            for(int i=0;i<2 && count++<24;i++){double angle=level.random.nextDouble()*Math.PI*2,r=radius(v)+1;
                var point=center.add(Math.cos(angle)*r,(level.random.nextDouble()-.5)*r,Math.sin(angle)*r);
                level.addParticle(ParticleTypes.REVERSE_PORTAL,point.x,point.y,point.z,(center.x-point.x)*.04,(center.y-point.y)*.04,(center.z-point.z)*.04);
            }
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        // Before translucent terrain: Create glass writes depth as it blends.
        // Drawing a contained field after particles puts it behind that glass
        // depth and makes the entire 50..100% field disappear.
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
        scope();var mc=Minecraft.getInstance();if(mc.level==null)return;var camera=event.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var contained=Set.copyOf(CONTAINED_FRAME);CONTAINED_FRAME.clear();
        if(VIEWS.isEmpty())return;
        // Contained fallbacks use the world's exact projection/depth mapping.
        // Extending its far plane would also change the established depth map.
        renderPass(event,false,contained);
        renderPass(event,true,contained);
    }
    private static void renderPass(RenderLevelStageEvent event,boolean escaped,Set<BlockPos> contained){
        var mc=Minecraft.getInstance();var camera=event.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var oldProjection=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());
        var sorting=com.mojang.blaze3d.systems.RenderSystem.getVertexSorting();
        float fogStart=com.mojang.blaze3d.systems.RenderSystem.getShaderFogStart(),fogEnd=com.mojang.blaze3d.systems.RenderSystem.getShaderFogEnd();
        try{
            var projection=new org.joml.Matrix4f(oldProjection);float near=projection.m32()/(projection.m22()-1),far=projection.m32()/(projection.m22()+1);
            if(escaped && Float.isFinite(near) && Float.isFinite(far) && near>0 && far>near && far<4096){far=4096;projection.m22(-(far+near)/(far-near));projection.m32(-2*far*near/(far-near));com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection,sorting);}
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(1000000);com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(1001024);
        for(var v:VIEWS.values()){
            int phase=phase(v);var center=v.state.position();
            if(phase==0 || (phase>=2)!=escaped || !escaped && contained.contains(v.state.center()) || camera.distanceToSqr(center)>Math.pow(BlackHoleRenderer.ACCIDENT_VIEW_DISTANCE+v.state.radius(),2))continue;
            var pose=event.getPoseStack();pose.pushPose();pose.translate(center.x-camera.x,center.y-camera.y,center.z-camera.z);
            float r=radius(v);
            BlackHoleRenderer.draw(pose,buffers,now(v)+partial,r,phase!=2,BlackHoleRenderer.MAX_MOTES,camera.subtract(center));pose.popPose();
        }
        buffers.endBatch(BlackHoleRenderer.CORE_TYPE);buffers.endBatch(ArcaneRenderTypes.REACTOR_GLOW);
        if(escaped)ReactorDebrisVisual.render(event);
        }finally{
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(oldProjection,sorting);
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(fogStart);com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(fogEnd);
        }
    }
    private ReactorAccidentVisual(){}
}
