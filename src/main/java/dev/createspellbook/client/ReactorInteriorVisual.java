package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.*;
import dev.createspellbook.CreateSpellbook;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Camera-local atmosphere inside an actual 3D horizon. A fresh observation is
 * required each frame, so leaving a field or world cannot leave a dark overlay.
 * This world pass also works with F1; inventory and HUD remain separate. */
@EventBusSubscriber(modid=CreateSpellbook.ID,value=Dist.CLIENT)
public final class ReactorInteriorVisual {
    private static float strength,time;
    private static float lastRendered;
    public static void observe(float radius,Vec3 eye,float tick){
        if(radius<=0 || !Double.isFinite(eye.lengthSqr()))return;
        float value=(float)Math.clamp((radius-eye.length())/Math.max(.1,radius*.2),0,1);
        if(value>strength){strength=value;time=tick;}
    }
    public static float lastIntensity(){return lastRendered;}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY){strength=0;lastRendered=0;return;}
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL || strength<=0)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;lastRendered=strength;
        // The event stack's camera transform differs from BE-local stacks.
        // Cancel the shader's actual model-view matrix, not a second guessed
        // camera quaternion; this stays screen-aligned at every yaw and pitch.
        var pose=new PoseStack();var buffers=mc.renderBuffers().bufferSource();pose.pushPose();
        pose.mulPose(new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix()).invert());pose.translate(0,0,-.22);
        var v=buffers.getBuffer(ArcaneRenderTypes.REACTOR_INTERIOR);
        point(v,pose,-2,-2,7,2,18,(int)(65*strength));point(v,pose,2,-2,7,2,18,(int)(65*strength));
        point(v,pose,2,2,7,2,18,(int)(65*strength));point(v,pose,-2,2,7,2,18,(int)(65*strength));
        // Slow spiral wisps provide movement and depth without rapid flashes,
        // camera shake, blindness effects or a fully opaque black screen.
        for(int arm=0;arm<4;arm++)for(int step=0;step<32;step++){
            double t=step/32d,u=(step+1)/32d;
            double a=time*.012+arm*Math.PI/2-t*Math.PI*3,b=time*.012+arm*Math.PI/2-u*Math.PI*3;
            float r=(float)(.02+.34*t),s=(float)(.02+.34*u),width=(float)(.0015+.009*t);
            int alpha=(int)((40+105*t)*strength);
            point(v,pose,(float)Math.cos(a)*(r-width),(float)Math.sin(a)*(r-width),165,65,235,alpha);
            point(v,pose,(float)Math.cos(b)*(s-width),(float)Math.sin(b)*(s-width),165,65,235,alpha);
            point(v,pose,(float)Math.cos(b)*(s+width),(float)Math.sin(b)*(s+width),74,25,135,0);
            point(v,pose,(float)Math.cos(a)*(r+width),(float)Math.sin(a)*(r+width),74,25,135,0);
        }
        pose.popPose();buffers.endBatch(ArcaneRenderTypes.REACTOR_INTERIOR);
    }
    private static void point(VertexConsumer v,PoseStack p,float x,float y,int r,int g,int b,int a){v.addVertex(p.last(),x,y,0).setColor(r,g,b,a);}
    private ReactorInteriorVisual(){}
}
