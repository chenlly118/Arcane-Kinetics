package dev.createspellbook.reactor;

import dev.createspellbook.Balance;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import java.util.*;

/** Transient native tickets, reconstructed from accident SavedData after reload.
 * A center and a moving work window load at most 50 full chunks per worker;
 * the engine also needs surrounding generation/status dependency chunks.
 * Tickets expire after 40 ticks if the owning level stops refreshing them. */
public final class ReactorAccidentChunks {
    private static final TicketType<UUID> TICKET=TicketType.create("create_spellbook_black_hole",UUID::compareTo,40);
    private final ServerLevel level;
    private final Map<UUID,Set<ChunkPos>> held=new HashMap<>();
    private final Map<UUID,List<ChunkPos>> footprints=new HashMap<>();
    private final Map<UUID,ChunkPos> work=new HashMap<>();
    private final Set<UUID> retiring=new HashSet<>();
    private boolean closed;
    public ReactorAccidentChunks(ServerLevel level){this.level=level;}
    public int tickets(){return held.values().stream().mapToInt(Set::size).sum();}
    public boolean holds(UUID id){return held.containsKey(id) && !retiring.contains(id);}
    public ChunkPos work(UUID id){return work.get(id);}
    public void update(List<ReactorAccident> active,long now){
        if(closed)return;
        // Do not cancel unfinished generation. Vanilla 1.21's unload queue can
        // repeatedly reschedule a completed save future while generation still
        // owns it, starving the main-thread completion that would release it.
        for(var id:List.copyOf(retiring))if(ready(id))release(id);else refresh(id,held.get(id));
        int workers=Math.min(active.size(),Math.max(0,Balance.REACTOR_LOAD_WORKERS.get()-retiring.size()));
        var chosen=new LinkedHashMap<UUID,ReactorAccident>();
        for(var a:active)if(holds(a.id) && !ready(a.id))chosen.put(a.id,a);
        // Ten-second leases give asynchronous generation time to finish. More
        // simultaneous accidents share the bounded workers in rotating order.
        for(int i=0;i<active.size() && chosen.size()<workers;i++){var a=active.get((int)Math.floorMod(now/200+i,active.size()));chosen.put(a.id,a);}
        for(var id:List.copyOf(held.keySet()))if(!chosen.containsKey(id) && !retiring.contains(id))forget(id);
        var live=active.stream().map(a->a.id).collect(java.util.stream.Collectors.toSet());
        footprints.keySet().removeIf(id->!live.contains(id));
        for(var a:chosen.values()){
            var target=work.get(a.id);if(target==null || ready(a.id))target=next(a,now);
            var desired=new HashSet<ChunkPos>();desired.add(new ChunkPos(a.center));desired.add(target);
            var prior=held.getOrDefault(a.id,Set.of());
            for(var pos:prior)if(!desired.contains(pos))level.getChunkSource().removeRegionTicket(TICKET,pos,2,a.id);
            // Refreshing an existing ticket also renews its native timeout.
            refresh(a.id,desired);
            held.put(a.id,Set.copyOf(desired));work.put(a.id,target);
        }
    }
    private void refresh(UUID id,Set<ChunkPos> positions){for(var pos:positions)level.getChunkSource().addRegionTicket(TICKET,pos,2,id);}
    public boolean ready(UUID id){
        for(var pos:held.getOrDefault(id,Set.of()))for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(level.getChunkSource().getChunkNow(pos.x+x,pos.z+z)==null)return false;
        return true;
    }
    private ChunkPos next(ReactorAccident a,long now){
        var positions=footprints.computeIfAbsent(a.id,id->footprint(a));double radius=a.destructionRadius(now),squared=radius*radius;
        for(int i=0;i<positions.size();i++){
            var pos=positions.get((int)Math.floorMod(a.workChunkCursor,positions.size()));
            if(distanceSquared(pos,a)<=squared)return pos;
            a.workChunkCursor++;a.workCellCursor=0;
        }return new ChunkPos(a.center);
    }
    private List<ChunkPos> footprint(ReactorAccident a){
        var c=a.position();var positions=new ArrayList<ChunkPos>();
        for(int x=net.minecraft.util.Mth.floor(c.x-a.radius)>>4;x<=net.minecraft.util.Mth.floor(c.x+a.radius)>>4;x++)
            for(int z=net.minecraft.util.Mth.floor(c.z-a.radius)>>4;z<=net.minecraft.util.Mth.floor(c.z+a.radius)>>4;z++){
                var pos=new ChunkPos(x,z);
                if(distanceSquared(pos,a)<=a.radius*(double)a.radius && level.getWorldBorder().isWithinBounds(pos.getMiddleBlockPosition(a.center.getY())))positions.add(pos);
            }
        positions.sort(Comparator.comparingDouble((ChunkPos p)->distanceSquared(p,a)).thenComparingInt(p->p.x).thenComparingInt(p->p.z));
        return positions.isEmpty()?List.of(new ChunkPos(a.center)):List.copyOf(positions);
    }
    private static double distanceSquared(ChunkPos p,ReactorAccident a){
        var c=a.position();double x=Math.max(0,Math.max(p.getMinBlockX()-c.x,c.x-(p.getMaxBlockX()+1d))),z=Math.max(0,Math.max(p.getMinBlockZ()-c.z,c.z-(p.getMaxBlockZ()+1d)));
        return x*x+z*z;
    }
    public void release(UUID id){
        for(var pos:held.getOrDefault(id,Set.of()))level.getChunkSource().removeRegionTicket(TICKET,pos,2,id);
        held.remove(id);work.remove(id);
        retiring.remove(id);
    }
    public void forget(UUID id){if(ready(id))release(id);else{retiring.add(id);work.remove(id);}footprints.remove(id);}
    /** Stop-time drain only, outside chunk load/unload callbacks. The main
     * executor pumps generation completions before any owned ticket is removed.
     * Ordinary game ticks never synchronously request an unavailable chunk. */
    public void finishAndClose(){
        closed=true;
        try{for(var positions:List.copyOf(held.values()))for(var pos:positions)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.getChunkSource().getChunk(pos.x+x,pos.z+z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);}
        finally{close();}
    }
    public void close(){closed=true;for(var id:List.copyOf(held.keySet()))release(id);footprints.clear();}
}
