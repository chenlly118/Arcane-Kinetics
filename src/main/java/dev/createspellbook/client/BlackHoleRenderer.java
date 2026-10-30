package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.createspellbook.reactor.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/** Closed, two-sided sphere with the installed Iron's black-hole glow. The dependency's
 * black-hole/beam textures are referenced at runtime, without a combat entity. */
public final class BlackHoleRenderer implements BlockEntityRenderer<ReactorControllerBlockEntity> {
    public static final ResourceLocation CENTER=ResourceLocation.fromNamespaceAndPath("irons_spellbooks","textures/entity/black_hole/black_hole.png");
    public static final ResourceLocation BEAM=ResourceLocation.fromNamespaceAndPath("irons_spellbooks","textures/entity/black_hole/beam.png");
    public static final RenderType CORE_TYPE=ArcaneRenderTypes.REACTOR_CORE;
    public static final int SEGMENTS=32,LATITUDES=16,MAX_MOTES=8,VIEW_DISTANCE=64,ACCIDENT_VIEW_DISTANCE=ReactorAccidents.VISIBILITY_RANGE;
    private static final float[] SPHERE=sphere();
    public BlackHoleRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(ReactorControllerBlockEntity controller,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!controller.isFormed() || controller.lastLayout()==null)return;
        var center=controller.lastLayout().position();var mc=Minecraft.getInstance();
        if(mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(center)>VIEW_DISTANCE*VIEW_DISTANCE)return;
        ReactorFluidRenderer.render(controller,pose,buffers);
        // Contained growth stays on the normal renderer. Only an escaped field
        // transfers ownership to the independent world pass.
        if(controller.operation.accidentPhase()>=2 || ReactorAccidentVisual.escapedAt(controller.lastLayout().center()))return;
        float radius=containedRadius(controller.lastLayout(),controller.operation.sizeUnits());
        if(radius<=0)return;
        ReactorAccidentVisual.containedRendered(controller.lastLayout().center());
        var pos=controller.getBlockPos();float time=controller.getLevel().getGameTime()+partial;
        pose.pushPose();pose.translate(center.x-pos.getX(),center.y-pos.getY(),center.z-pos.getZ());
        draw(pose,buffers,time,radius,controller.operation.accidentPhase()==1,4,mc.gameRenderer.getMainCamera().getPosition().subtract(center));pose.popPose();
    }
    public static float containedRadius(ReactorLayout layout,int units){return (float)(layout.inner()*.5*Math.clamp(units,0,1000)/1000d);}
    /** Shared mesh for normal, growing, escaped and shrinking fields. */
    public static void draw(PoseStack pose,MultiBufferSource buffers,float time,float radius,boolean warning,int motes){
        draw(pose,buffers,time,radius,warning,motes,new net.minecraft.world.phys.Vec3(0,0,radius*8));
    }
    public static void draw(PoseStack pose,MultiBufferSource buffers,float time,float radius,boolean warning,int motes,net.minecraft.world.phys.Vec3 eye){
        if(!Float.isFinite(radius) || radius<=0)return;
        mesh(buffers.getBuffer(CORE_TYPE),pose,radius,0,0,0,255);
        ReactorInteriorVisual.observe(radius,eye,time);
        // A center-facing exterior billboard becomes an edge-on stripe when
        // viewed from inside. The interior vortex owns that view completely.
        if(eye.lengthSqr()<radius*radius)return;
        NativeBlackHoleAura.draw(pose,buffers,time,radius,eye);
    }
    private static float[] sphere(){
        float[] vertices=new float[SEGMENTS*LATITUDES*12];int index=0;
        for(int y=0;y<LATITUDES;y++)for(int x=0;x<SEGMENTS;x++){
            for(int corner=0;corner<4;corner++){
                double latitude=Math.PI*((y+(corner==1 || corner==2?1:0))/(double)LATITUDES-.5);
                double longitude=2*Math.PI*(x+(corner>=2?1:0))/SEGMENTS;
                vertices[index++]=(float)(Math.cos(latitude)*Math.cos(longitude));
                vertices[index++]=(float)Math.sin(latitude);
                vertices[index++]=(float)(Math.cos(latitude)*Math.sin(longitude));
            }
        }return vertices;
    }
    private static void mesh(VertexConsumer v,PoseStack p,float radius,int red,int green,int blue,int alpha){
        // The dependency's center texel is opaque black. Sampling it on real
        // geometry avoids transparent corners and the old camera-facing disk.
        for(int i=0;i<SPHERE.length;i+=3)vertex(v,p,SPHERE[i]*radius,SPHERE[i+1]*radius,SPHERE[i+2]*radius,.5f,.5f,red,green,blue,alpha,SPHERE[i],SPHERE[i+1],SPHERE[i+2]);
    }
    private static void vertex(VertexConsumer v,PoseStack p,float x,float y,float z,float u,float w,int red,int green,int blue,int alpha,float nx,float ny,float nz){v.addVertex(p.last(),x,y,z).setColor(red,green,blue,alpha).setUv(u,w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(p.last(),nx,ny,nz);}
    @Override public boolean shouldRenderOffScreen(ReactorControllerBlockEntity controller){return true;}
    @Override public AABB getRenderBoundingBox(ReactorControllerBlockEntity controller){
        if(controller.lastLayout()==null)return new AABB(controller.getBlockPos());
        return ReactorFluidRenderer.chamber(controller.lastLayout()).inflate(containedRadius(controller.lastLayout(),controller.operation.sizeUnits())*1.1);
    }
    @Override public int getViewDistance(){return VIEW_DISTANCE;}
}
