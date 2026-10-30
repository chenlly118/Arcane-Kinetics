package dev.createspellbook.client;

import com.simibubi.create.foundation.block.connected.*;
import com.simibubi.create.foundation.model.ModelSwapper;
import dev.createspellbook.reactor.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Shared train-casing shell. Internal cores retain independent steel housings. */
public final class ReactorConnectedTextures {
    public static final CTSpriteShiftEntry CASING=shift("railway_casing","spellbook_reactor");
    public static final CTSpriteShiftEntry CASING_SIDE=shift("railway_casing_side","spellbook_reactor");
    public static final CTSpriteShiftEntry GLASS=shift("palettes/framed_glass","spellbook_glass");
    public static final CTSpriteShiftEntry GLASS_SIDE=GLASS;
    private static CTSpriteShiftEntry shift(String name,String suffix){return CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
        texture(name+"_"+suffix),texture(name+"_connected_"+suffix));}
    private static ResourceLocation texture(String name){return ResourceLocation.fromNamespaceAndPath("create","block/"+name);}
    public static boolean shell(BlockState state){var role=ReactorContent.role(state.getBlock());return role!=null && (role.shell() || role==ReactorRole.SHAFT);}
    public static final class ShellBehaviour extends ConnectedTextureBehaviour.Base {
        @Override public CTSpriteShiftEntry getShift(BlockState state,Direction face,TextureAtlasSprite sprite){
            // Actual sprite selection also handles rotated controller/shaft models.
            // Fixtures and display panels must never receive casing UVs.
            if(sprite!=null){
                if(sprite==CASING.getOriginal())return CASING;
                if(sprite==CASING_SIDE.getOriginal())return CASING_SIDE;
                if(sprite==GLASS.getOriginal())return GLASS;
                if(sprite==GLASS_SIDE.getOriginal())return GLASS_SIDE;
                return null;
            }
            boolean glass=state.is(ReactorContent.GLASS.get());
            return face.getAxis().isVertical()?(glass?GLASS:CASING):(glass?GLASS_SIDE:CASING_SIDE);
        }
        @Override public boolean connectsTo(BlockState state,BlockState other,BlockAndTintGetter level,BlockPos pos,BlockPos otherPos,Direction face){
            if(state.is(ReactorContent.GLASS.get()) || other.is(ReactorContent.GLASS.get()))
                return state.is(ReactorContent.GLASS.get()) && other.is(ReactorContent.GLASS.get()) && !isBeingBlocked(state,level,pos,otherPos,face);
            return shell(state) && shell(other) && !isBeingBlocked(state,level,pos,otherPos,face);
        }
    }
    public static void init(){} // Initialize all four shifts before the first atlas load.
    public static void bake(ModelEvent.ModifyBakingResult event){
        for(var role:ReactorRole.values()){
            if(!role.shell() && role!=ReactorRole.SHAFT)continue;
            ModelSwapper.swapModels(event.getModels(),ModelSwapper.getAllBlockStateModelLocations(ReactorContent.block(role)),model->new CTModel(model,new ShellBehaviour()));
        }
    }
    private ReactorConnectedTextures(){}
}
