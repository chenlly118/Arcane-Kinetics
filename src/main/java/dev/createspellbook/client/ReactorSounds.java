package dev.createspellbook.client;

import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.reactor.ReactorControllerBlockEntity;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.lang.ref.WeakReference;
import java.util.*;

/** At most one positional loop per field, including the contained/escaped
 * handoff. Native OpenAL looping avoids playing a new sample every tick. */
@EventBusSubscriber(modid=CreateSpellbook.ID,value=Dist.CLIENT)
public final class ReactorSounds {
    private static final Map<BlockPos,WeakReference<ReactorControllerBlockEntity>> CONTROLLERS=new HashMap<>();
    private static final Map<BlockPos,Loop> LOOPS=new HashMap<>();
    private static Object lastLevel;
    private static long starts;
    private static void scope(){
        var level=Minecraft.getInstance().level;
        if(lastLevel!=level){clear();lastLevel=level;}
    }
    public static void watch(ReactorControllerBlockEntity controller){scope();CONTROLLERS.put(controller.getBlockPos().immutable(),new WeakReference<>(controller));}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){clear();lastLevel=null;NativeBlackHoleAura.clear();}
    private static void clear(){
        var manager=Minecraft.getInstance().getSoundManager();for(var loop:LOOPS.values())manager.stop(loop);
        LOOPS.clear();CONTROLLERS.clear();
        NativeBlackHoleAura.clear();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        scope();var mc=Minecraft.getInstance();if(mc.level==null || mc.player==null)return;
        Map<BlockPos,Vec3> candidates=new HashMap<>();
        CONTROLLERS.values().removeIf(ref->{var c=ref.get();return c==null || c.isRemoved() || c.getLevel()!=mc.level || !mc.level.hasChunkAt(c.getBlockPos());});
        for(var ref:CONTROLLERS.values()){
            var c=ref.get();if(c==null || !c.isFormed() || c.lastLayout()==null || c.operation.sizeUnits()<=0 || c.operation.accidentPhase()>=2)continue;
            candidates.put(c.lastLayout().center(),c.lastLayout().position());
        }
        for(var state:ReactorAccidentVisual.snapshots())candidates.put(state.center(),state.position());
        Set<BlockPos> wanted=new HashSet<>();
        candidates.entrySet().stream().filter(e->e.getValue().distanceToSqr(mc.player.position())<96*96)
                .sorted(Comparator.comparingDouble(e->e.getValue().distanceToSqr(mc.player.position()))).limit(8).forEach(e->{
                    wanted.add(e.getKey());
                    var loop=LOOPS.computeIfAbsent(e.getKey(),key->{var value=new Loop(e.getValue());mc.getSoundManager().play(value);starts++;return value;});
                    // Resource-pack reload recreates OpenAL channels without
                    // unloading block entities. Retry a stopped channel at a
                    // bounded rate, never stack an already playing instance.
                    if(mc.level.getGameTime()>=loop.retryAt && !mc.getSoundManager().isActive(loop)){
                        loop.retryAt=mc.level.getGameTime()+100;mc.getSoundManager().play(loop);starts++;
                    }
                });
        LOOPS.entrySet().removeIf(e->{if(wanted.contains(e.getKey()))return false;mc.getSoundManager().stop(e.getValue());return true;});
    }
    public static int activeLoops(){return LOOPS.size();}
    public static long playingLoops(){return LOOPS.values().stream().filter(Minecraft.getInstance().getSoundManager()::isActive).count();}
    public static long startedLoops(){return starts;}
    private static final class Loop extends AbstractTickableSoundInstance {
        long retryAt;
        Loop(Vec3 position){
            super(SoundRegistry.BLACK_HOLE_LOOP.get(),SoundSource.BLOCKS,RandomSource.create());
            x=position.x;y=position.y;z=position.z;volume=.85f;pitch=1;
            looping=true;delay=0;attenuation=SoundInstance.Attenuation.LINEAR;
            retryAt=Minecraft.getInstance().level.getGameTime()+40;
        }
        @Override public void tick(){}
    }
    private ReactorSounds(){}
}
