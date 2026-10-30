package dev.createspellbook.client;

import dev.createspellbook.CreateSpellbook;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/** Lightweight animated parts from the supplied casting-block meshes. */
public final class ConstructModels {
    public static final ResourceLocation[] MODELS={CastingArmRenderer.NOZZLE,CastingArmRenderer.SHAFT,AutoCasterRenderer.CRYSTAL};
    public static float recoil(long now,long last,float partial) {
        if(last==Long.MIN_VALUE)return 0;float age=now-last+partial;return age>=0 && age<10?(float)Math.sin(age/10*Math.PI)*.10f:0;
    }
    private ConstructModels() {}
}
