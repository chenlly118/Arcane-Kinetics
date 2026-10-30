package dev.createspellbook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.createspellbook.Balance;
import dev.createspellbook.Content;
import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.network.MechanicSwingPayload;
import dev.createspellbook.spell.MechanicHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Buff-synchronized, purely client-side projection. No entity, tick task or AI. */
@EventBusSubscriber(modid=CreateSpellbook.ID,value=Dist.CLIENT)
public final class MechanicProjection {
    private record Swing(int targetId,long started) {}
    private static final Map<UUID,Swing> SWINGS=new HashMap<>();
    private static Object lastLevel;
    public static void swing(MechanicSwingPayload message) {
        var level=Minecraft.getInstance().level;
        if(level==null || !(level.getEntity(message.playerId()) instanceof Player player)) return;
        if(lastLevel!=level) { SWINGS.clear(); lastLevel=level; }
        SWINGS.put(player.getUUID(),new Swing(message.targetId(),message.started()));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        SWINGS.clear();lastLevel=null;
    }
    public static float extension(float ticks) {
        if(ticks<0 || ticks>=MechanicHandler.SWING_TICKS) return 0;
        if(ticks<=MechanicHandler.HIT_TICKS) return ticks/MechanicHandler.HIT_TICKS;
        return 1-(ticks-MechanicHandler.HIT_TICKS)/(MechanicHandler.SWING_TICKS-MechanicHandler.HIT_TICKS);
    }
    public static Vec3 origin(Player player,float partial) {
        return MechanicHandler.armOrigin(player.getPosition(partial),Mth.rotLerp(partial,player.yBodyRotO,player.yBodyRot));
    }
    public static float swingAge(Player player) {
        var swing=SWINGS.get(player.getUUID());
        return swing==null?-1:player.level().getGameTime()-swing.started();
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var mc=Minecraft.getInstance();var level=mc.level;
        if(level==null) return;
        if(lastLevel!=level) { SWINGS.clear();lastLevel=level; }
        SWINGS.entrySet().removeIf(entry->level.getGameTime()-entry.getValue().started()>MechanicHandler.SWING_TICKS
                || level.getPlayerByUUID(entry.getKey())==null);
        var camera=event.getCamera().getPosition();
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers=mc.renderBuffers().bufferSource();
        int count=0;
        for(var player:level.players()) {
            if(!player.isAlive() || player.isSpectator() || player.isInvisible()
                    || !player.hasEffect(Content.MECHANIC_EFFECT) || player.position().distanceToSqr(camera)>4096) continue;
            if(count++>=Balance.MASTER_VISUAL_CAP.get()) break;
            Vec3 base=origin(player,partial);
            var swing=SWINGS.get(player.getUUID());
            float amount=swing==null?0:extension(level.getGameTime()-swing.started()+partial);
            float yaw=Mth.rotLerp(partial,player.yBodyRotO,player.yBodyRot);
            Vec3 forward=new Vec3(-Math.sin(Math.toRadians(yaw)),0,Math.cos(Math.toRadians(yaw)));
            Vec3 idle=base.add(forward.scale(1.5)).add(0,.4,0);
            Vec3 aim=idle;
            if(swing!=null && level.getEntity(swing.targetId()) instanceof net.minecraft.world.entity.LivingEntity target)
                aim=idle.lerp(target.getPosition(partial).add(0,target.getBbHeight()*.5,0),amount);
            renderArm(event.getPoseStack(),buffers,base.subtract(camera),aim.subtract(base));
        }
        buffers.endBatch(ArcaneRenderTypes.BLUE);
    }
    /** Public pose entry used by the live-client regression preview. */
    public static void renderArm(PoseStack poses,net.minecraft.client.renderer.MultiBufferSource buffers,
                                 Vec3 relativeBase,Vec3 direction) {
        poses.pushPose();
        poses.translate(relativeBase.x,relativeBase.y,relativeBase.z);
        Vec3 forward=direction.lengthSqr()<1.0E-5?new Vec3(0,0,1):direction.normalize();
        poses.mulPose(new org.joml.Quaternionf().rotationTo(0,0,-1,(float)forward.x,(float)forward.y,(float)forward.z));
        // Stow compactly behind the shoulder; extend the claw to the chosen victim on impact.
        float size=.7f;
        poses.scale(size,size,size);
        ArcaneModels.renderReachingArm(poses,buffers,0xf000f0,.58f,(float)direction.length()/size);
        poses.popPose();
    }
    private MechanicProjection() {}
}
