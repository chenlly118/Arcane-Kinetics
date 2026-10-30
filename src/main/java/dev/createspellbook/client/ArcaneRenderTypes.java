package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;

/** Desaturate the sampled dependency texture in the shader, then color it arcane blue. */
public final class ArcaneRenderTypes extends RenderType {
    public static ShaderInstance shader;
    public static final RenderType BLUE=create("create_spellbook_arcane_blue",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,true,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->shader))
                    .setTextureState(new TextureStateShard(TextureAtlas.LOCATION_BLOCKS,false,false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).createCompositeState(true));
    /** A real two-sided sphere, including the view from inside the horizon. */
    public static final RenderType REACTOR_CORE=create("create_spellbook_reactor_core",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,65536,false,false,
            CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
                    .setTextureState(new TextureStateShard(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("irons_spellbooks","textures/entity/black_hole/black_hole.png"),false,false))
                    .setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
    /** Native energy-swirl shader/blending, with depth writes disabled for the
     * large reactor projection. Transparent beams must not hide absorbed block
     * models or other translucent world effects behind their empty corners. */
    public static final RenderType REACTOR_GLOW=create("create_spellbook_native_black_hole_glow",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,16384,false,true,
            CompositeState.builder().setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                    .setTextureState(new TextureStateShard(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("irons_spellbooks","textures/entity/black_hole/beam.png"),false,false))
                    .setTexturingState(new OffsetTexturingStateShard(0,0)).setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    public static final RenderType REACTOR_INTERIOR=create("create_spellbook_reactor_interior",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,16384,false,false,
            CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL).setDepthTestState(NO_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    /** The tank surface blends with the chamber without hiding the black hole.
     * Opaque world geometry still depth-tests. */
    public static final RenderType REACTOR_FLUID=create("create_spellbook_reactor_fluid",DefaultVertexFormat.BLOCK,VertexFormat.Mode.QUADS,256,false,true,
            CompositeState.builder().setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                    .setTextureState(BLOCK_SHEET_MIPPED).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(true));
    private ArcaneRenderTypes(String name,VertexFormat format,VertexFormat.Mode mode,int size,boolean crumbling,boolean sorted,Runnable setup,Runnable clear) {super(name,format,mode,size,crumbling,sorted,setup,clear);}
}
