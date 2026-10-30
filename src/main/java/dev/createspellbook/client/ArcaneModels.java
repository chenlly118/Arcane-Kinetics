package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.createspellbook.CreateSpellbook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Render installed Create resources directly; no dependency assets are copied. */
public final class ArcaneModels {
    public static final int ARCANE_COLOR=0xff6398de;
    public static final ResourceLocation[] CONSTRUCTS = {
            create("block/cogwheel_shaftless"),create("block/mechanical_saw/blade_horizontal_active"),
            create("block/crushing_wheel/block"),create("block/mechanical_press/head"),
            create("block/mechanical_drill/head"),ResourceLocation.withDefaultNamespace("block/piston_head"),create("block/mechanical_press/head")};
    public static final ResourceLocation PISTON_BODY=CreateSpellbook.id("block/piston_body");
    public static final ResourceLocation PRESS_BODY=create("block/mechanical_press/block");
    public static final ResourceLocation CRANK_HANDLE=CreateSpellbook.id("block/arcane_crank_handle");
    public static final ResourceLocation EJECTOR_BLOCK=create("block/weighted_ejector/block");
    public static final ResourceLocation EJECTOR_TOP=create("block/weighted_ejector/top");
    public static final ResourceLocation ARM_BASE=create("block/mechanical_arm/base");
    public static final ResourceLocation ARM_LOWER=create("block/mechanical_arm/lower_body");
    public static final ResourceLocation ARM_UPPER=create("block/mechanical_arm/upper_body");
    public static final ResourceLocation ARM_CLAW=create("block/mechanical_arm/claw_base");
    public static final ResourceLocation ARM_UPPER_GRIP=create("block/mechanical_arm/upper_claw_grip");
    public static final ResourceLocation ARM_LOWER_GRIP=create("block/mechanical_arm/lower_claw_grip");
    public static final ResourceLocation SPOUT_BLOCK=create("block/spout/block");
    public static final ResourceLocation SPOUT_TOP=create("block/spout/top");
    public static final ResourceLocation SPOUT_MIDDLE=create("block/spout/middle");
    public static final ResourceLocation SPOUT_BOTTOM=create("block/spout/bottom");
    public static final ResourceLocation[] BATCH2_MODELS={EJECTOR_BLOCK,EJECTOR_TOP,ARM_BASE,ARM_LOWER,ARM_UPPER,ARM_CLAW,ARM_UPPER_GRIP,ARM_LOWER_GRIP,
            SPOUT_BLOCK,SPOUT_TOP,SPOUT_MIDDLE,SPOUT_BOTTOM};
    private static ResourceLocation create(String path) { return ResourceLocation.fromNamespaceAndPath("create",path); }
    public static void render(ResourceLocation model,PoseStack poses,MultiBufferSource buffers,int light,float alpha) {
        render(model,poses,buffers,light,alpha,true);
    }
    public static void render(ResourceLocation model,PoseStack poses,MultiBufferSource buffers,int light,float alpha,boolean arcane) {
        BakedModel baked=Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(model));
        var consumer=buffers.getBuffer(arcane ? ArcaneRenderTypes.BLUE : RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        var random=RandomSource.create(42);
        for(int i=0;i<7;i++) {
            Direction direction=i==6 ? null:Direction.values()[i]; random.setSeed(42);
            for(var quad:baked.getQuads(null,direction,random,ModelData.EMPTY,null))
                consumer.putBulkData(poses.last(),quad,1f,1f,1f,alpha,light,OverlayTexture.NO_OVERLAY);
        }
    }
    /** Two installed half blades, joined at their actual diameter (x=8,z=11). */
    public static void renderConstruct(int kind,PoseStack poses,MultiBufferSource buffers,int light,float alpha) {
        if(kind!=1) { render(CONSTRUCTS[kind],poses,buffers,light,alpha); return; }
        poses.pushPose();
        poses.translate(0,0,-3/16d);
        render(CONSTRUCTS[1],poses,buffers,light,alpha);
        poses.translate(.5,0,11/16d);
        poses.mulPose(Axis.YP.rotationDegrees(180));
        poses.translate(-.5,0,-11/16d);
        render(CONSTRUCTS[1],poses,buffers,light,alpha);
        poses.popPose();
    }
    /** Model-local forward is -Z. The head extends out of the hollow vanilla body. */
    public static void renderPiston(PoseStack poses,MultiBufferSource buffers,int light,float alpha,float ticks) {
        render(PISTON_BODY,poses,buffers,light,alpha);
        poses.pushPose();
        poses.translate(0,0,-MechanicalRenderer.pistonExtension(ticks));
        render(CONSTRUCTS[5],poses,buffers,light,alpha);
        poses.popPose();
    }
    /** Weighted ejector block with its top plate lifting during the launch. */
    public static void renderEjector(PoseStack poses,MultiBufferSource buffers,int light,float alpha,float lift) {
        render(EJECTOR_BLOCK,poses,buffers,light,alpha);
        poses.pushPose();
        poses.translate(0,lift,0);
        render(EJECTOR_TOP,poses,buffers,light,alpha);
        poses.popPose();
    }
    /**
     * Create mechanical-arm parts.  The native parts already extend along local -Z;
     * the extension parameter slides the upper limb and claw toward the target.
     */
    public static void renderArm(PoseStack poses,MultiBufferSource buffers,int light,float alpha,float extension) {
        render(ARM_BASE,poses,buffers,light,alpha);
        poses.pushPose();
        poses.translate(0,0,0);
        render(ARM_LOWER,poses,buffers,light,alpha);
        poses.translate(0,0,-.55f*extension);
        render(ARM_UPPER,poses,buffers,light,alpha);
        poses.translate(0,0,-.55f*extension);
        render(ARM_CLAW,poses,buffers,light,alpha);
        render(ARM_UPPER_GRIP,poses,buffers,light,alpha);
        render(ARM_LOWER_GRIP,poses,buffers,light,alpha);
        poses.popPose();
    }
    /** Two contiguous arm segments ending at the melee victim; only length is stretched. */
    public static void renderReachingArm(PoseStack poses,MultiBufferSource buffers,int light,float alpha,float reach) {
        render(ARM_BASE,poses,buffers,light,alpha);
        float half=Math.max(.2f,reach*.5f);
        poses.pushPose();
        poses.scale(1,1,half/(15f/16));
        render(ARM_LOWER,poses,buffers,light,alpha);
        poses.popPose();
        poses.pushPose();
        poses.translate(0,0,-half);
        poses.scale(1,1,half/(15f/16));
        render(ARM_UPPER,poses,buffers,light,alpha);
        poses.popPose();
        poses.pushPose();
        poses.translate(0,0,-reach);
        render(ARM_CLAW,poses,buffers,light,alpha);
        render(ARM_UPPER_GRIP,poses,buffers,light,alpha);
        render(ARM_LOWER_GRIP,poses,buffers,light,alpha);
        poses.popPose();
    }
    /** Create spout stack, nozzle pointing down; parts already carry their own -Y offsets. */
    public static void renderSpout(PoseStack poses,MultiBufferSource buffers,int light,float alpha) {
        render(SPOUT_BLOCK,poses,buffers,light,alpha);
        render(SPOUT_TOP,poses,buffers,light,alpha);
        render(SPOUT_MIDDLE,poses,buffers,light,alpha);
        render(SPOUT_BOTTOM,poses,buffers,light,alpha);
    }
    private ArcaneModels() {}
}
