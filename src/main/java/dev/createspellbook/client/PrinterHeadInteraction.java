package dev.createspellbook.client;

import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.devices.DeviceContent;
import dev.createspellbook.printing.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import java.util.*;

/** The idle stamp extends into air below its block, which vanilla voxel picking
 * cannot reach from the side. Route that small visible surface through the
 * normal use-item-on packet, retaining server reach and protection checks. */
@EventBusSubscriber(modid=CreateSpellbook.ID,value=Dist.CLIENT)
public final class PrinterHeadInteraction {
    public static BlockHitResult pick(Vec3 eye,Vec3 end,HitResult existing){
        var mc=Minecraft.getInstance();if(mc.level==null)return null;
        double closest=existing!=null && existing.getType()!=HitResult.Type.MISS?existing.getLocation().distanceToSqr(eye):eye.distanceToSqr(end);
        BlockHitResult result=null;Set<BlockPos> visited=new HashSet<>();
        int steps=(int)Math.ceil(eye.distanceTo(end)*4);
        for(int i=0;i<=steps;i++){
            var pos=BlockPos.containing(eye.lerp(end,i/(double)Math.max(1,steps))).above();if(!visited.add(pos))continue;
            if(!mc.level.getBlockState(pos).is(DeviceContent.SCROLL_PRINTER.get()) || !(mc.level.getBlockEntity(pos) instanceof ScrollPrinterBlockEntity p) || p.remainingTicks()>=0)continue;
            var box=new AABB(pos.getX()+3d/16,pos.getY()-7d/16,pos.getZ()+3d/16,pos.getX()+13d/16,pos.getY(),pos.getZ()+13d/16);
            var intersection=box.clip(eye,end);if(intersection.isEmpty())continue;var hit=intersection.get();double distance=hit.distanceToSqr(eye);if(distance>=closest)continue;
            Direction side=Math.abs(hit.x-box.minX)<1e-5?Direction.WEST:Math.abs(hit.x-box.maxX)<1e-5?Direction.EAST:Math.abs(hit.z-box.minZ)<1e-5?Direction.NORTH:Math.abs(hit.z-box.maxZ)<1e-5?Direction.SOUTH:Math.abs(hit.y-box.minY)<1e-5?Direction.DOWN:Direction.UP;
            result=new BlockHitResult(hit,side,pos,false);closest=distance;
        }
        return result;
    }
    @SubscribeEvent public static void use(InputEvent.InteractionKeyMappingTriggered event){
        var mc=Minecraft.getInstance();if(!event.isUseItem() || mc.screen!=null || mc.player==null || mc.gameMode==null || mc.player.isSpectator())return;
        var held=mc.player.getItemInHand(event.getHand());if(!held.isEmpty() && !ScrollPrinting.validFocus(held))return;
        var eye=mc.player.getEyePosition();var end=eye.add(mc.player.getLookAngle().scale(Math.min(8,mc.player.blockInteractionRange())));
        var hit=pick(eye,end,mc.hitResult);if(hit==null)return;
        var result=mc.gameMode.useItemOn(mc.player,event.getHand(),hit);
        if(result.consumesAction()){event.setCanceled(true);event.setSwingHand(false);mc.player.swing(event.getHand());}
    }
    private PrinterHeadInteraction(){}
}
