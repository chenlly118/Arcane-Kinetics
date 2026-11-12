package dev.createspellbook.client;

import com.mojang.serialization.MapCodec;
import dev.createspellbook.CreateSpellbook;
import net.minecraft.client.renderer.texture.atlas.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.Map;

/** Derive sprites from the separately installed Create pack on each atlas reload. */
public final class PortalFunnelSprites implements SpriteSource {
    public static final SpriteSourceType TYPE=new SpriteSourceType(MapCodec.unit(new PortalFunnelSprites()));
    public static final ResourceLocation ID=CreateSpellbook.id("arcane_funnel");
    private static final Map<String,String> SOURCES=Map.of(
            "base","funnel/brass_funnel","block","brass_block","direction","funnel/brass_funnel_push",
            "neutral","funnel/brass_funnel_neutral","frame","funnel/brass_funnel_frame","redstone","funnel/brass_funnel_unpowered",
            "pull","funnel/brass_funnel_pull","powered","funnel/brass_funnel_powered");
    // Samples from the installed arcane ingot: blue metal with violet trim and pale highlights.
    private static final int[] BLUE={0x1e2c46,0x29406b,0x39599c,0x496fc2,0x6398de,0x96d1f2,0xb8e5f3};
    private static final int[] VIOLET={0x241b3a,0x46306b,0x7a56c7,0x8270c4,0xb08be8,0xd191ff,0xb8e5f3};
    @Override public void run(ResourceManager manager,Output output){
        SOURCES.forEach((key,path)->{
            var source=ResourceLocation.fromNamespaceAndPath("create","textures/block/"+path+".png");
            manager.getResource(source).ifPresentOrElse(resource->output.add(texture(key),loader->{
                var contents=loader.loadSprite(texture(key),resource);if(contents==null)return null;
                var image=contents.getOriginalImage();boolean trim=key.equals("block") || key.equals("frame");
                for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)image.setPixelRGBA(x,y,recolor(image.getPixelRGBA(x,y),trim));
                return contents;
            }),()->org.apache.logging.log4j.LogManager.getLogger().warn("Missing Create funnel source texture {}",source));
        });
    }
    public static ResourceLocation texture(String key){return CreateSpellbook.id("block/portal_funnel/"+key);}
    public static int recolor(int abgr,boolean trim){
        if((abgr>>>24)==0)return abgr;
        int red=abgr&255,green=(abgr>>>8)&255,blue=(abgr>>>16)&255;
        // Restrict the operation to brass hues; rubber, steel, redstone and transparency stay native.
        if(red<=blue+10 || green<=blue+5 || red<green*.90 || red>green*2.35)return abgr;
        double luminance=(red*.2126+green*.7152+blue*.0722)/255;
        var palette=trim?VIOLET:BLUE;double position=Math.clamp(luminance,0d,1d)*(palette.length-1);
        int low=(int)position,high=Math.min(palette.length-1,low+1);double fraction=position-low;
        int a=palette[low],b=palette[high];int rgb=0;
        for(int shift:new int[]{0,8,16})rgb|=((int)Math.round(((a>>>shift)&255)*(1-fraction)+((b>>>shift)&255)*fraction))<<shift;
        return (abgr&0xff000000)|((rgb&255)<<16)|(rgb&0xff00)|((rgb>>>16)&255);
    }
    @Override public SpriteSourceType type(){return TYPE;}
}
