package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.createspellbook.entity.MechanicalEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Visual poses only.  Sizes never feed collision, damage or processing ranges.
 *
 * The installed Create partials are authored in different local frames:
 * cogwheel/saw/roller are horizontal discs, the drill head points +Z, the press
 * head points down and the vanilla piston head points -Z. Derive the wheel and drill
 * orientation from the actual velocity so every heading (and diagonals) looks
 * correct instead of sliding sideways or flying face-first.
 *
 * The pose helpers are public so the client regression test can assert the
 * resulting axes (horizontal saw, rolling wheel axis, drill alignment) without
 * duplicating the maths.
 */
public final class MechanicalRenderer extends EntityRenderer<MechanicalEntity> {
    public static final float GEAR_SIZE=.42f;
    public static final float SAW_SIZE=1.5f;
    public static final float WHEEL_SIZE=1.45f;
    /** Create's crushing_wheel.obj disc radius in blocks (x/z -0.570..1.570). */
    public static final float WHEEL_MODEL_RADIUS=1.07f;
    public static final float PRESS_SIZE=1.5f;
    public static final float DRILL_SIZE=1.3f;
    public static final float PISTON_SIZE=1.35f;
    public static final float BARRAGE_SIZE=2.5f;
    public MechanicalRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public void render(MechanicalEntity entity,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light) {
        int kind=Math.clamp(entity.kind(),0,14);
        float ticks=(kind==5 || kind==6 || kind>=7)?entity.visualAge(partial):entity.tickCount+partial;
        poses.pushPose();
        if(kind==MechanicalEntity.MASTER_DRILL || kind==MechanicalEntity.MASTER_SAW) {
            Vec3 offset=dev.createspellbook.spell.MasterFlight.offset(entity.castDirection(),ticks,kind==MechanicalEntity.MASTER_SAW);
            poses.translate(offset.x,offset.y,offset.z);
        }
        switch(kind) {
            case 1 -> poseSaw(poses,ticks);
            case 2 -> poseWheel(poses,entity.getDeltaMovement(),ticks);
            case 3 -> posePress(poses,yaw,ticks);
            case 4 -> poseDrill(poses,entity.getDeltaMovement(),ticks);
            case 5 -> posePiston(poses,entity.castDirection());
            case 6 -> poseBarrage(poses,entity.castDirection(),ticks-entity.pressDelay());
            case 7 -> poseEjector(poses);
            case 8 -> poseArm(poses,entity.castDirection());
            case 9 -> poseMasterPress(poses);
            case 10 -> poseArm(poses,entity.castDirection());
            case 11 -> poseDrill(poses,entity.castDirection(),ticks);
            case 12 -> poseSpout(poses);
            case 13 -> posePiston(poses,entity.castDirection());
            case 14 -> poseSaw(poses,ticks);
            default -> poseGear(poses,yaw,ticks);
        }
        if(kind==5) ArcaneModels.renderPiston(poses,buffers,0xf000f0,.62f,ticks);
        else if(kind==6) renderBarrage(poses,buffers,ticks-entity.pressDelay());
        else if(kind==7) ArcaneModels.renderEjector(poses,buffers,0xf000f0,.72f,ejectorLift(ticks));
        else if(kind==8) ArcaneModels.renderArm(poses,buffers,0xf000f0,.72f,armExtension(ticks));
        else if(kind==9) renderBarrage(poses,buffers,ticks);
        else if(kind==10) renderSwordArm(poses,buffers,light,ticks);
        else if(kind==11) ArcaneModels.renderConstruct(4,poses,buffers,0xf000f0,.72f);
        else if(kind==12) ArcaneModels.renderSpout(poses,buffers,0xf000f0,.72f);
        else if(kind==13) ArcaneModels.renderPiston(poses,buffers,0xf000f0,.72f,ticks);
        else if(kind==14) ArcaneModels.renderConstruct(1,poses,buffers,0xf000f0,.62f);
        else ArcaneModels.renderConstruct(kind,poses,buffers,0xf000f0,.62f);
        poses.popPose();
        super.render(entity,yaw,partial,poses,buffers,light);
    }
    /** Weighted ejector on the target's feet; the top plate pops while the target leaves. */
    public static void poseEjector(PoseStack poses) {
        poses.translate(-.5,0,-.5);
    }
    public static float ejectorLift(float ticks) { return .34f*Math.clamp(ticks/4f,0,1); }
    /** Create arm parts extend along local -Z; point them at the target. */
    public static void poseArm(PoseStack poses,Vec3 direction) {
        Vec3 forward=direction.lengthSqr()<1.0E-6?new Vec3(0,0,-1):direction.normalize();
        poses.translate(0,.55,0);
        poses.mulPose(new org.joml.Quaternionf().rotationTo(0,0,-1,(float)forward.x,(float)forward.y,(float)forward.z));
        poses.scale(1.05f,1.05f,1.05f);
    }
    public static float armExtension(float ticks) {
        if(ticks<2) return Math.clamp(ticks/2f,0,1);
        if(ticks<6) return 1;
        return Math.clamp((12-ticks)/6f,0,1);
    }
    /** Heavy press above the target, using the larger barrage body/head. */
    public static void poseMasterPress(PoseStack poses) {
        poses.translate(0,2.3,0);
        poses.scale(2.3f,2.3f,2.3f);
        poses.translate(-.5,1.4,-.5);
    }
    /** Spout hangs above the target; its parts already point down. */
    public static void poseSpout(PoseStack poses) {
        poses.translate(-.5,0,-.5);
    }
    public static void renderSwordArm(PoseStack poses,MultiBufferSource buffers,int light,float ticks) {
        ArcaneModels.renderArm(poses,buffers,0xf000f0,.72f,armExtension(ticks));
        renderSword(poses,buffers,light,ticks);
    }
    private static void renderSword(PoseStack poses,MultiBufferSource buffers,int light,float ticks) {
        poses.pushPose();
        poses.translate(0,.55,-1.2);
        poses.mulPose(Axis.XP.rotationDegrees(90));
        poses.mulPose(Axis.ZP.rotationDegrees(-35+Math.min(35,ticks*8)));
        poses.scale(.85f,.85f,.85f);
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD),
                net.minecraft.world.item.ItemDisplayContext.GROUND,light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,poses,buffers,
                net.minecraft.client.Minecraft.getInstance().level,0);
        poses.popPose();
    }
    /** Horizontal cutting disc: plane XZ, spin around world Y, never a vertical shield. */
    public static void poseSaw(PoseStack poses,float ticks) {
        poses.translate(0,.15,0);
        poses.mulPose(Axis.YP.rotationDegrees(ticks*25));
        poses.scale(SAW_SIZE,SAW_SIZE,SAW_SIZE);
        poses.translate(-.5,-.5,-.5);
    }
    /** Rolling wheel: plane contains travel and up, axis horizontal and perpendicular. */
    public static void poseWheel(PoseStack poses,Vec3 motion,float ticks) {
        Vec3 horizontal=new Vec3(motion.x,0,motion.z);
        double speed=horizontal.length();
        float heading=speed<1.0E-4?0:(float)Math.toDegrees(Math.atan2(-horizontal.z,horizontal.x));
        float radius=WHEEL_MODEL_RADIUS*WHEEL_SIZE;
        // Rolling without slipping along the axis the heading maps to: 0.22 blocks
        // per tick over the visual radius, negative so the contact point moves backwards.
        float roll=-(float)Math.toDegrees(ticks*speed/radius);
        poses.translate(0,radius,0);
        poses.mulPose(Axis.YP.rotationDegrees(heading));
        poses.mulPose(Axis.XP.rotationDegrees(90));
        poses.mulPose(Axis.YP.rotationDegrees(roll));
        poses.scale(WHEEL_SIZE,WHEEL_SIZE,WHEEL_SIZE);
        poses.translate(-.5,-.5,-.5);
    }
    /** Static press head falling onto the target. */
    public static void posePress(PoseStack poses,float yaw,float ticks) {
        poses.translate(0,.15,0);
        poses.mulPose(Axis.YP.rotationDegrees(yaw));
        poses.translate(0,Math.max(0,.8-ticks*.09),0);
        poses.scale(PRESS_SIZE,PRESS_SIZE,PRESS_SIZE);
        poses.translate(-.5,-.5,-.5);
    }
    /** Drill head points +Z in its model; align +Z with velocity and spin about it. */
    public static void poseDrill(PoseStack poses,Vec3 motion,float ticks) {
        Vec3 direction=motion.lengthSqr()<1.0E-6?new Vec3(0,0,1):motion.normalize();
        float heading=(float)Math.toDegrees(Math.atan2(direction.x,direction.z));
        float pitch=(float)Math.toDegrees(-Math.asin(Math.clamp(direction.y,-1,1)));
        poses.mulPose(Axis.YP.rotationDegrees(heading));
        poses.mulPose(Axis.XP.rotationDegrees(pitch));
        poses.mulPose(Axis.ZP.rotationDegrees(ticks*30));
        poses.scale(DRILL_SIZE,DRILL_SIZE,DRILL_SIZE);
        poses.translate(-.5,-.5,-.5);
        // Centre the 0..20px head on the projectile instead of its back face.
        poses.translate(0,0,-2/16d);
    }
    /** Bottom-to-top stroke for a 20-tick extend / hold / retract animation. */
    public static float pistonExtension(float ticks) {
        if(ticks<=0) return 0;
        if(ticks<6) { float t=ticks/6;return .95f*(1-(1-t)*(1-t)*(1-t)); }
        if(ticks<9) return .95f;
        return .95f*Math.clamp((20-ticks)/11,0,1);
    }
    public static float pressExtension(float ticks) {
        if(ticks<0) return 0;
        if(ticks<8) { float t=ticks/8;return t*t*1.4f; }
        if(ticks<11) return 1.4f;
        return 1.4f*Math.clamp((26-ticks)/15,0,1);
    }
    public static void poseBarrage(PoseStack poses,Vec3 direction,float ticks) {
        float yaw=(float)Math.toDegrees(Math.atan2(direction.x,direction.z));
        poses.mulPose(Axis.YP.rotationDegrees(yaw));
        poses.scale(BARRAGE_SIZE,BARRAGE_SIZE,BARRAGE_SIZE);
        poses.translate(-.5,1.4,-.5);
    }
    public static void renderBarrage(PoseStack poses,MultiBufferSource buffers,float ticks) {
        ArcaneModels.render(ArcaneModels.PRESS_BODY,poses,buffers,0xf000f0,.55f);
        poses.pushPose();
        poses.translate(0,-pressExtension(ticks),0);
        ArcaneModels.render(ArcaneModels.CONSTRUCTS[6],poses,buffers,0xf000f0,.62f);
        poses.popPose();
    }
    /** Align the full moving piston, including straight up/down and diagonal casts. */
    public static void posePiston(PoseStack poses,Vec3 direction) {
        poses.translate(0,.15,0);
        Vec3 forward=direction.lengthSqr()<1.0E-6?new Vec3(0,0,1):direction.normalize();
        // template_piston_head has its pushing face at z=0 and rod toward +Z.
        poses.mulPose(new org.joml.Quaternionf().rotationTo(0,0,-1,(float)forward.x,(float)forward.y,(float)forward.z));
        poses.scale(PISTON_SIZE,PISTON_SIZE,PISTON_SIZE);
        poses.translate(-.5,-.5,-.5);
    }
    /** Gear missile keeps its accepted size and pose. */
    public static void poseGear(PoseStack poses,float yaw,float ticks) {
        poses.translate(0,.15,0);
        poses.mulPose(Axis.YP.rotationDegrees(yaw));
        poses.mulPose(Axis.ZP.rotationDegrees(ticks*25));
        poses.mulPose(Axis.XP.rotationDegrees(90));
        poses.scale(GEAR_SIZE,GEAR_SIZE,GEAR_SIZE);
        poses.translate(-.5,-.5,-.5);
    }
    @Override public ResourceLocation getTextureLocation(MechanicalEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }
}
