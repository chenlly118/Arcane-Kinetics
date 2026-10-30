package dev.createspellbook.reactor;

import dev.createspellbook.CreateSpellbook;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Per-level chunk index and a coalescing scan queue. No region tickets or force loading. */
@EventBusSubscriber(modid=CreateSpellbook.ID)
public final class ReactorManager {
    private static final Map<ServerLevel,ReactorManager> LEVELS=new ConcurrentHashMap<>();
    private final ServerLevel level;
    private final Map<Long,Set<ReactorControllerBlockEntity>> byChunk=new HashMap<>();
    private final Map<ReactorControllerBlockEntity,ReactorLayout.Box> watched=new IdentityHashMap<>();
    private final Map<ReactorControllerBlockEntity,Long> pending=new LinkedHashMap<>();
    private final Set<Long> unloading=new HashSet<>();
    private ReactorManager(ServerLevel level){this.level=level;}
    public static ReactorManager get(ServerLevel level){return LEVELS.computeIfAbsent(level,ReactorManager::new);}
    public static ReactorManager existing(ServerLevel level){return LEVELS.get(level);}
    public ReactorControllerBlockEntity controllerAt(BlockPos center){for(var c:watched.keySet())if(!c.isRemoved() && loaded(c.getBlockPos()) && c.lastLayout()!=null && c.lastLayout().center().equals(center))return c;return null;}
    /** Native kinetic snapshots contain no unvalidated reactor contribution. */
    public static void saveKinetics(ServerLevel level,Runnable save){
        var manager=LEVELS.get(level);if(manager==null){save.run();return;}
        var sources=manager.watched.keySet().stream().map(c->c.operation.source()).filter(Objects::nonNull).distinct().toList();
        try{for(var source:sources)source.beginSnapshot();save.run();}
        finally{for(var source:sources)source.endSnapshot();}
    }
    public boolean loaded(BlockPos pos){return loaded(ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4));}
    public boolean loaded(long key){return !unloading.contains(key) && level.getChunkSource().getChunkNow(ChunkPos.getX(key),ChunkPos.getZ(key))!=null;}
    public void register(ReactorControllerBlockEntity controller){reindex(controller);enqueue(controller);}
    public void enqueue(ReactorControllerBlockEntity controller){pending.put(controller,level.getGameTime()+2);}
    public void scanned(ReactorControllerBlockEntity controller){pending.remove(controller);reindex(controller);}
    public void reindex(ReactorControllerBlockEntity controller){
        unwatch(controller);var box=controller.structurallyFormed() && controller.lastLayout()!=null?controller.lastLayout().bounds():ReactorLayout.searchBounds(controller.getBlockPos(),controller.facing());
        watched.put(controller,box);for(long key:box.chunks())byChunk.computeIfAbsent(key,k->Collections.newSetFromMap(new IdentityHashMap<>())).add(controller);
    }
    private void unwatch(ReactorControllerBlockEntity controller){var prior=watched.remove(controller);if(prior==null)return;for(long key:prior.chunks()){var set=byChunk.get(key);if(set!=null){set.remove(controller);if(set.isEmpty())byChunk.remove(key);}}}
    public void unregister(ReactorControllerBlockEntity controller){pending.remove(controller);unwatch(controller);controller.suspend();}
    /** Only the controller's derived indicator is irrelevant to geometry. */
    public static boolean relevantChange(BlockState before,BlockState after){return before!=after && !(before.is(ReactorContent.CONTROLLER.get()) && after.is(ReactorContent.CONTROLLER.get()) && before.getValue(ReactorControllerBlock.FACING)==after.getValue(ReactorControllerBlock.FACING));}
    public static void changed(ServerLevel level,BlockPos pos,BlockState before,BlockState after){
        var manager=LEVELS.get(level);if(manager==null || !relevantChange(before,after))return;
        if(!level.getServer().isSameThread()){var copy=pos.immutable();level.getServer().execute(()->changed(level,copy,before,after));return;}
        var set=manager.byChunk.get(ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4));if(set==null)return;
        for(var controller:List.copyOf(set)){var box=manager.watched.get(controller);if(box!=null && box.contains(pos)){controller.operation.structureBroken();controller.requestValidation();}}
    }
    private void tick(){
        int scans=0;long now=level.getGameTime();
        for(var entry:List.copyOf(pending.entrySet())){
            var controller=entry.getKey();if(controller.isRemoved() || controller.isChunkUnloaded()){unregister(controller);continue;}
            if(entry.getValue()>now)continue;pending.remove(controller);controller.validateNow();if(++scans>=2)break;
        }
    }
    public void chunkUnloaded(long key){
        unloading.add(key);var set=byChunk.get(key);if(set==null)return;
        for(var controller:List.copyOf(set)){
            if(ChunkPos.asLong(controller.getBlockPos().getX()>>4,controller.getBlockPos().getZ()>>4)==key)unregister(controller);
            else controller.chunkUnavailable();
        }
    }
    public void chunkLoaded(long key){
        unloading.remove(key);var set=byChunk.get(key);
        // ChunkEvent.Load runs inside the chunk's unfinished FULL future. A
        // synchronous status/neighbor write here can request that same future
        // and deadlock loading. Unload already revoked the old certificate;
        // queue validation for the ordinary level tick after completion.
        if(set!=null)for(var controller:List.copyOf(set))enqueue(controller);
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){if(event.getLevel() instanceof ServerLevel level){var manager=LEVELS.get(level);if(manager!=null)manager.tick();}}
    @SubscribeEvent public static void unload(ChunkEvent.Unload event){if(event.getLevel() instanceof ServerLevel level){var manager=LEVELS.get(level);if(manager!=null)manager.chunkUnloaded(event.getChunk().getPos().toLong());}}
    @SubscribeEvent public static void load(ChunkEvent.Load event){if(event.getLevel() instanceof ServerLevel level){var manager=LEVELS.get(level);if(manager!=null)manager.chunkLoaded(event.getChunk().getPos().toLong());}}
    @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level){var manager=LEVELS.remove(level);if(manager!=null){for(var controller:List.copyOf(manager.watched.keySet()))controller.suspend();manager.watched.clear();manager.byChunk.clear();manager.pending.clear();manager.unloading.clear();}}}
}
