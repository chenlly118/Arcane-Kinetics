package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.entity.spells.black_hole.BlackHole;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/** Calls the installed Iron's renderer, retaining only its additive beam pass.
 * The reusable client projection is never added to a level or ticked, so none
 * of the combat entity's damage, terrain, sound or lifetime logic can run. */
public final class NativeBlackHoleAura {
    private static BlackHole projection;
    private static final VertexConsumer DISCARD = new VertexConsumer() {
        public VertexConsumer addVertex(float x,float y,float z){return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float u,float v){return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
    };
    public static void draw(PoseStack pose,MultiBufferSource buffers,float time,float radius,Vec3 eye){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(projection==null || projection.level()!=mc.level){
            projection=new BlackHole(EntityRegistry.BLACK_HOLE.get(),mc.level);
            projection.refreshDimensions();
        }
        // The native default radius is 5. Keep that visual parameter unchanged
        // and scale the complete native animation, including its rotation.
        float scale=radius*.5f;
        var center=mc.gameRenderer.getMainCamera().getPosition().subtract(eye);
        projection.setPos(center.x,center.y-projection.getBbHeight()*.5,center.z);
        projection.tickCount=80+Math.floorMod((int)time,24000);
        float partial=time-(float)Math.floor(time);
        MultiBufferSource beamOnly=type->type.toString().contains("textures/entity/black_hole/beam.png")
                ?buffers.getBuffer(ArcaneRenderTypes.REACTOR_GLOW):DISCARD;
        pose.pushPose();pose.scale(scale,scale,scale);pose.translate(0,-projection.getBbHeight()*.5,0);
        mc.getEntityRenderDispatcher().getRenderer(projection).render(projection,0,partial,pose,beamOnly,15728880);
        pose.popPose();
    }
    public static void clear(){projection=null;}
    private NativeBlackHoleAura(){}
}
