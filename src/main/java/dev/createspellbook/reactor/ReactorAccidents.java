package dev.createspellbook.reactor;

import dev.createspellbook.*;
import dev.createspellbook.network.ReactorAccidentPayload;
import dev.createspellbook.network.ReactorDebrisPayload;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
import java.lang.ref.WeakReference;

/** Per-dimension authority, durable timers and bounded native loading windows.
 * No combat entity or controller-owned countdown. */
@EventBusSubscriber(modid=CreateSpellbook.ID)
public final class ReactorAccidents extends SavedData {
    public static final String DATA_ID="create_spellbook_reactor_accidents";
    public static final int VISIBILITY_RANGE=2048;
    public static final TagKey<Block> UNBREAKABLE=TagKey.create(Registries.BLOCK,CreateSpellbook.id("reactor_unbreakable"));
    // Cleanup listeners may request telemetry after the unload callback. Neither
    // key nor value may retain an abandoned world if that recreates an entry.
    private static final Map<ServerLevel,WeakReference<ReactorAccidents>> CACHE=new WeakHashMap<>();
    private static final ThreadLocal<Boolean> ERODING=ThreadLocal.withInitial(()->false);
    private static final Map<ServerLevel,List<ReactorAccident>> SPAWN_ZONES=Collections.synchronizedMap(new WeakHashMap<>());
    private final ServerLevel level;
    private final Map<UUID,ReactorAccident> active=new LinkedHashMap<>();
    private final Map<BlockPos,UUID> centers=new HashMap<>();
    private final Map<UUID,Long> discharged=new HashMap<>();
    private record ProbeWindow(long refreshed,int minX,int maxX,int minZ,int maxZ,List<BlockPos> sections){}
    private final Map<UUID,ProbeWindow> probeWindows=new HashMap<>();
    private final ReactorAccidentChunks chunks;
    private final ReactorAttraction attraction;
    private final Map<UUID,List<ReactorDebrisPayload.Sample>> debris=new LinkedHashMap<>();
    private final Deque<Long> debrisWindow=new ArrayDeque<>();
    private int lastDebrisSamples;
    private long debrisSamplesSent;
    public long dischargedEpoch(UUID reactor){return discharged.getOrDefault(reactor,-1L);}
    private long lastTick=Long.MIN_VALUE,finished,repaired;
    private long lastWorkNanos,deadlineNanos;
    private int turn,lastProbes,lastBlocks,lastEntities;
    public ReactorAccidents(ServerLevel level){this.level=level;this.chunks=new ReactorAccidentChunks(level);this.attraction=new ReactorAttraction(level);}
    public long debrisSamplesSent(){return debrisSamplesSent;}
    public int lastDebrisSamples(){return lastDebrisSamples;}
    public int forcedTickets(){return chunks.tickets();}
    public boolean loading(UUID id){return chunks.holds(id);}
    public static ReactorAccidents get(ServerLevel level){var reference=CACHE.get(level);var data=reference==null?null:reference.get();if(data==null){data=level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(()->new ReactorAccidents(level),(tag,lookup)->load(level,tag)),DATA_ID);CACHE.put(level,new WeakReference<>(data));}return data;}
    public Collection<ReactorAccident> active(){return Collections.unmodifiableCollection(active.values());}
    public int activeCount(){return active.size();}
    public long finishedCount(){return finished;}
    public long repairedCount(){return repaired;}
    public int lastProbes(){return lastProbes;}
    public int lastBlocks(){return lastBlocks;}
    public int lastEntities(){return lastEntities;}
    public long lastWorkNanos(){return lastWorkNanos;}
    public ReactorAccident at(BlockPos center){var id=centers.get(center);return id==null?null:active.get(id);}
    public static ReactorAccident forController(ReactorControllerBlockEntity controller){
        return controller.getLevel() instanceof ServerLevel server && controller.lastLayout()!=null?get(server).at(controller.lastLayout().center()):null;
    }
    public static ReactorAccident trigger(ReactorControllerBlockEntity controller,ReactorAccident.Cause cause){return trigger(controller,cause,ReactorAccident.Settings.forSize(controller.lastLayout()==null?9:controller.lastLayout().size()));}
    public static ReactorAccident trigger(ReactorControllerBlockEntity controller,ReactorAccident.Cause cause,ReactorAccident.Settings settings){
        if(!(controller.getLevel() instanceof ServerLevel server) || controller.lastLayout()==null)return null;
        var data=get(server);var center=controller.lastLayout().center();var prior=data.at(center);
        if(prior!=null){prior.causes|=1<<cause.ordinal();data.setDirty();return prior;}
        // Preflight failures and idle dismantling are safe. Only live power can
        // create a new accident; later faults merge without extending deadlines.
        if(controller.operation.sizeUnits()<=0)return null;
        var a=new ReactorAccident(controller.identity(),controller.getBlockPos(),controller.lastLayout(),controller.operation.pairs(),cause,server.getGameTime(),settings,controller.operation.sizeUnits(),controller.operation.epoch());
        data.active.put(a.id,a);data.centers.put(center,a.id);data.setDirty();data.broadcast(a,false);return a;
    }
    private ReactorControllerBlockEntity controller(ReactorAccident a){
        if(loaded(a.controller) && level.getBlockEntity(a.controller) instanceof ReactorControllerBlockEntity c)return c;
        var manager=ReactorManager.existing(level);return manager==null?null:manager.controllerAt(a.center);
    }
    private boolean repairable(ReactorAccident a){
        var c=controller(a);return !a.escaped && c!=null && c.isFormed() && c.lastLayout().bounds().equals(a.layout().bounds()) && c.operation.repairContainment(a.required);
    }
    public boolean repairNow(ReactorControllerBlockEntity controller){
        var a=forController(controller);if(a==null)return true;
        if(a.phase(level.getGameTime())!=ReactorAccident.Phase.WARNING || !repairable(a))return false;
        controller.operation.recoverSize(a.containedSize(level.getGameTime()));remove(a,true);return true;
    }
    private void remove(ReactorAccident a,boolean repair){
        active.remove(a.id);centers.remove(a.center,a.id);probeWindows.remove(a.id);chunks.forget(a.id);attraction.remove(a.id);if(repair)repaired++;else finished++;broadcast(a,true);setDirty();
    }
    private void broadcast(ReactorAccident a,boolean remove){
        var message=ReactorAccidentPayload.of(a,level.dimension().location(),level.getGameTime(),remove);
        if(remove)PacketDistributor.sendToPlayersInDimension(level,message);
        else PacketDistributor.sendToPlayersNear(level,null,a.center.getX(),a.center.getY(),a.center.getZ(),a.radius+VISIBILITY_RANGE,message);
    }
    public boolean loaded(BlockPos pos){return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    /** All block changes use the ordinary native removal lifecycle, with drops
     * suppressed in a narrow synchronous scope (including container onRemove). */
    private boolean swallow(ReactorAccident a,BlockPos pos){
        if(!loaded(pos) || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos))return false;
        var state=level.getBlockState(pos);if(state.isAir() || state.is(Blocks.BEDROCK) || state.is(UNBREAKABLE) || state.getDestroySpeed(level,pos)<0)return false;
        // Native neighbor callbacks must not cause a chunk fetch at a seam.
        for(var direction:Direction.values())if(!loaded(pos.relative(direction)))return false;
        boolean before=ERODING.get();ERODING.set(true);
        try{boolean removed=level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS);if(removed)sampleDebris(a,pos,state);return removed;}
        finally{ERODING.set(before);}
    }
    private void sampleDebris(ReactorAccident a,BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
        long now=level.getGameTime();
        // Sample removals on every tick, sharing a rolling five-tick allowance.
        // A thin wall swallowed between cadence ticks must still show debris.
        if(!debrisWindow.isEmpty() && debrisWindow.getFirst()>now)debrisWindow.clear();
        while(!debrisWindow.isEmpty() && debrisWindow.getFirst()<=now-5)debrisWindow.removeFirst();
        if(debrisWindow.size()>=ReactorDebrisPayload.MAX_SAMPLES || state.hasBlockEntity() || state.getRenderShape()!=RenderShape.MODEL)return;
        if(pos.getCenter().distanceToSqr(a.position())<=Math.pow(a.visualRadius(now)+1.5,2))return;
        boolean visible=false;for(var p:level.players())if(p.position().distanceToSqr(pos.getCenter())<=160*160){visible=true;break;}if(!visible)return;
        debris.computeIfAbsent(a.id,id->new ArrayList<>()).add(new ReactorDebrisPayload.Sample(pos.immutable(),Block.getId(state)));debrisWindow.addLast(now);lastDebrisSamples++;
    }
    private void sendDebris(long now){
        for(var entry:debris.entrySet()){
            var a=active.get(entry.getKey());if(a==null)continue;
            for(var player:level.players()){
                var samples=entry.getValue().stream().filter(s->player.position().distanceToSqr(s.pos().getCenter())<=160*160).toList();
                if(!samples.isEmpty()){PacketDistributor.sendToPlayer(player,new ReactorDebrisPayload(a.id,level.dimension().location(),a.position(),now,samples));debrisSamplesSent+=samples.size();}
            }
        }debris.clear();
    }
    /** Cache only currently loaded, nonempty 16-cube sections. The bounded scan
     * visits chunk metadata, never every block in the sphere or an unloaded chunk.
     * A durable cursor then sweeps these sections rather than spending almost all
     * of a large field's budget on random air and unavailable terrain. */
    private List<BlockPos> probeSections(ReactorAccident a,long now,double radius){
        var center=a.position();int minX=net.minecraft.util.Mth.floor(center.x-radius)>>4,maxX=net.minecraft.util.Mth.floor(center.x+radius)>>4;
        int minZ=net.minecraft.util.Mth.floor(center.z-radius)>>4,maxZ=net.minecraft.util.Mth.floor(center.z+radius)>>4;
        var previous=probeWindows.get(a.id);
        if(previous!=null && now>=previous.refreshed && now-previous.refreshed<20 && previous.minX==minX && previous.maxX==maxX && previous.minZ==minZ && previous.maxZ==maxZ)return previous.sections;
        var sections=new ArrayList<BlockPos>();double radiusSqr=radius*radius;
        for(int cx=minX;cx<=maxX;cx++)for(int cz=minZ;cz<=maxZ;cz++){
            double dx=Math.max(0,Math.max((cx<<4)-center.x,center.x-((cx<<4)+16d))),dz=Math.max(0,Math.max((cz<<4)-center.z,center.z-((cz<<4)+16d)));
            if(dx*dx+dz*dz>radiusSqr)continue;
            var chunk=level.getChunkSource().getChunkNow(cx,cz);if(chunk==null)continue;
            var values=chunk.getSections();
            for(int i=0;i<values.length;i++){
                if(values[i].hasOnlyAir())continue;int y=chunk.getSectionYFromSectionIndex(i)<<4;
                double dy=Math.max(0,Math.max(y-center.y,center.y-(y+16d)));
                if(dx*dx+dy*dy+dz*dz<=radiusSqr)sections.add(new BlockPos(cx<<4,y,cz<<4));
            }
        }
        sections.sort(Comparator.comparingDouble(p->p.getCenter().add(7.5,7.5,7.5).distanceToSqr(center)));
        probeWindows.put(a.id,new ProbeWindow(now,minX,maxX,minZ,maxZ,List.copyOf(sections)));return sections;
    }
    private void erode(ReactorAccident a,long now,int probes,int blocks){
        double radius=a.destructionRadius(now),radiusSqr=radius*radius;var sections=probeSections(a,now,radius);if(sections.isEmpty())return;
        for(int i=0;i<probes && blocks>0;i++){
            if((i&127)==0 && System.nanoTime()>deadlineNanos)break;
            long index=a.sweepCursor++;int cell=(int)(index&4095);var base=sections.get((int)((index>>>12)%sections.size()));
            var pos=base.offset(cell&15,cell>>>8,(cell>>>4)&15);a.probes++;lastProbes++;
            if(pos.getCenter().distanceToSqr(a.position())>radiusSqr)continue;
            if(swallow(a,pos)){a.swallowed++;lastBlocks++;blocks--;}
        }
    }
    /** Prioritize the moving forced window so terrain outside player view gets
     * a complete, saved sweep instead of losing all work to nearby loaded air.
     * Empty sections skip in O(1); normal block and CPU budgets still apply. */
    private void erodeForced(ReactorAccident a,long now,int probes,int blocks){
        var target=chunks.work(a.id);if(target==null || probes<=0 || blocks<=0)return;
        if(!chunks.ready(a.id))return;
        var chunk=level.getChunkSource().getChunkNow(target.x,target.z);int height=level.getHeight(),limit=height*256;
        double radius=a.destructionRadius(now),squared=radius*radius;int spent=0;
        while(a.workCellCursor<limit && spent<probes && blocks>0){
            if((spent&127)==0 && System.nanoTime()>deadlineNanos)break;
            int cell=a.workCellCursor,y=level.getMinBuildHeight()+(cell>>>8);
            if(chunk.getSection(chunk.getSectionIndex(y)).hasOnlyAir()){a.workCellCursor=(cell+4096)&~4095;continue;}
            a.workCellCursor++;spent++;a.probes++;lastProbes++;
            var pos=new BlockPos(target.getMinBlockX()+(cell&15),y,target.getMinBlockZ()+((cell>>>4)&15));
            if(pos.getCenter().distanceToSqr(a.position())<=squared && swallow(a,pos)){a.swallowed++;lastBlocks++;blocks--;}
        }
        if(a.workCellCursor>=limit){a.workCellCursor=0;a.workChunkCursor++;a.loadedChunkSweeps++;}
    }
    private void affect(ReactorAccident a,long now,int limit){lastEntities+=attraction.apply(a,now,limit);}
    private void tick(){
        long now=level.getGameTime();if(now==lastTick)return;long workStarted=System.nanoTime();lastTick=now;deadlineNanos=workStarted+Balance.REACTOR_WORK_MILLIS.get()*1000000L;lastProbes=lastBlocks=lastEntities=lastDebrisSamples=0;debris.clear();
        var hazards=new ArrayList<ReactorAccident>();int sounds=0;
        for(var a:List.copyOf(active.values())){
            var phase=a.phase(now);
            if(phase==ReactorAccident.Phase.FINISHED){discharged.merge(a.reactorId,a.fieldEpoch,Math::max);var c=controller(a);if(c!=null)c.operation.accidentEscaped();remove(a,false);continue;}
            if(phase==ReactorAccident.Phase.WARNING && repairable(a)){var repairedController=controller(a);if(repairedController!=null)repairedController.operation.recoverSize(a.containedSize(now));remove(a,true);continue;}
            if(phase!=ReactorAccident.Phase.WARNING){if(!a.escaped){a.escaped=true;discharged.merge(a.reactorId,a.fieldEpoch,Math::max);var c=controller(a);if(c!=null)c.operation.accidentEscaped();setDirty();}hazards.add(a);}
            if(a.announced!=phase.ordinal()){a.announced=phase.ordinal();broadcast(a,false);setDirty();}
            if(now%40==0 && sounds++<4)level.playSound(null,a.center,phase==ReactorAccident.Phase.WARNING?SoundEvents.WARDEN_HEARTBEAT:SoundEvents.BEACON_DEACTIVATE,SoundSource.BLOCKS,4,phase==ReactorAccident.Phase.WARNING?.7f:.5f);
        }
        attraction.begin(hazards,now);chunks.update(hazards,now);
        int size=hazards.size(),probeBudget=Balance.REACTOR_PROBE_BUDGET.get(),blockBudget=Balance.REACTOR_BLOCK_BUDGET.get(),entityBudget=Balance.REACTOR_ENTITY_BUDGET.get();
        if(size>0){
            turn=Math.floorMod(turn+1,size);
            for(int n=0;n<size;n++){
                var a=hazards.get((turn+n)%size);int left=size-n;
                int probes=Math.max(0,probeBudget-lastProbes),blocks=Math.max(0,blockBudget-lastBlocks),entities=Math.max(0,entityBudget-lastEntities);
                if(entities>0)affect(a,now,Math.max(1,entities/left));
                if(System.nanoTime()<deadlineNanos && probes>0 && blocks>0){
                    int beforeProbes=lastProbes,beforeBlocks=lastBlocks;
                    erodeForced(a,now,Math.max(1,probes/left/2),Math.max(1,blocks/left/2));
                    int remainingProbes=probes/left-(lastProbes-beforeProbes),remainingBlocks=blocks/left-(lastBlocks-beforeBlocks);
                    if(remainingProbes>0 && remainingBlocks>0)erode(a,now,remainingProbes,remainingBlocks);
                }
            }setDirty();
        }
        sendDebris(now);
        // Periodic snapshots also cover players joining/rejoining or approaching
        // an already-running accident. At most 16 nearby visuals per player.
        if(now%10==0)for(var player:level.players()){
            int sent=0;for(var a:active.values().stream().sorted(Comparator.comparingDouble(a->player.position().distanceToSqr(a.position()))).toList())if(player.position().distanceToSqr(a.position())<(a.radius+(double)VISIBILITY_RANGE)*(a.radius+(double)VISIBILITY_RANGE)){
                PacketDistributor.sendToPlayer(player,ReactorAccidentPayload.of(a,level.dimension().location(),now,false));if(++sent>=16)break;
            }
        }
        SPAWN_ZONES.put(level,List.copyOf(active.values()));lastWorkNanos=System.nanoTime()-workStarted;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){var list=new ListTag();for(var a:active.values())list.add(a.save());tag.put("Active",list);tag.putLong("Finished",finished);tag.putLong("Repaired",repaired);tag.putInt("Turn",turn);var epochs=new ListTag();for(var e:discharged.entrySet()){var t=new CompoundTag();t.putUUID("Reactor",e.getKey());t.putLong("Epoch",e.getValue());epochs.add(t);}tag.put("Discharged",epochs);return tag;}
    public static ReactorAccidents load(ServerLevel level,CompoundTag tag){
        var data=new ReactorAccidents(level);data.finished=Math.max(0,tag.getLong("Finished"));data.repaired=Math.max(0,tag.getLong("Repaired"));data.turn=tag.getInt("Turn");for(var e:tag.getList("Discharged",10)){var t=(CompoundTag)e;if(t.hasUUID("Reactor"))data.discharged.put(t.getUUID("Reactor"),Math.max(0,t.getLong("Epoch")));}
        for(var entry:tag.getList("Active",10)){var raw=(CompoundTag)entry;if(!raw.contains("Schema") && !raw.getBoolean("Escaped"))continue;var a=ReactorAccident.load(raw);if(a!=null && !data.active.containsKey(a.id) && !data.centers.containsKey(a.center)){if(a.phase(level.getGameTime())==ReactorAccident.Phase.FINISHED){data.discharged.merge(a.reactorId,a.fieldEpoch,Math::max);data.finished++;data.setDirty();continue;}data.active.put(a.id,a);data.centers.put(a.center,a.id);if(a.escaped)data.discharged.merge(a.reactorId,a.fieldEpoch,Math::max);}}return data;
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){if(event.getLevel() instanceof ServerLevel level)get(level).tick();}
    @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event){
        // LevelEvent.Unload is too late: vanilla first drains chunk work and
        // waits for forced tickets during stopServer. Release before that drain,
        // while preserving the durable accident for the next server session.
        for(var level:event.getServer().getAllLevels()){var reference=CACHE.get(level);var data=reference==null?null:reference.get();if(data!=null)data.chunks.finishAndClose();}
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level){var reference=CACHE.remove(level);var data=reference==null?null:reference.get();if(data!=null)data.chunks.close();SPAWN_ZONES.remove(level);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void suppressSwallowedDrops(EntityJoinLevelEvent event){if(ERODING.get() && event.getEntity() instanceof ItemEntity item){item.discard();event.setCanceled(true);}}
    public boolean forbidsNaturalSpawn(net.minecraft.world.phys.Vec3 pos){long now=level.getGameTime();for(var a:active.values()){double r=a.destructionRadius(now);if(r>0 && pos.distanceToSqr(a.position())<=r*r)return true;}return false;}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void suppressNaturalSpawn(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck event){
        var kind=event.getSpawnType();if(kind!=net.minecraft.world.entity.MobSpawnType.NATURAL && kind!=net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION)return;
        var server=event.getLevel().getLevel();long now=server.getGameTime();
        for(var a:SPAWN_ZONES.getOrDefault(server,List.of())){double radius=a.destructionRadius(now);if(radius>0 && event.getEntity().position().distanceToSqr(a.position())<=radius*radius){event.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.FAIL);return;}}
    }
}
