package dev.createspellbook.utility;

import dev.createspellbook.Balance;
import dev.createspellbook.CreateSpellbook;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.*;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Server-thread-only, bounded, transient leases. A ticket never authorizes an item debit. */
@EventBusSubscriber(modid=CreateSpellbook.ID)
public final class PortalChunkTickets {
    public static final int IDLE_TICKS=40;
    private static final TicketController CONTROLLER=new TicketController(
            ResourceLocation.fromNamespaceAndPath(CreateSpellbook.ID,"portal_funnel"),PortalChunkTickets::validateSavedTickets);
    private static final Map<MinecraftServer,State> SERVERS=new IdentityHashMap<>();
    public enum Result { READY, BUSY, INVALID }
    private static final class State {
        final Map<UUID,Lease> leases=new LinkedHashMap<>();
        int budgetTick=Integer.MIN_VALUE,requests;
    }
    private static final class Lease {
        final UUID owner;
        final GlobalPos source,target;
        final ServerLevel destination;
        final ChunkPos chunk;
        int lastTransferTick;
        boolean loading=true;
        Lease(PortalFunnelBlockEntity source,ServerLevel destination,int now){
            this.owner=source.identity();this.source=source.globalPos();this.target=source.partner();
            this.destination=destination;this.chunk=new ChunkPos(target.pos());this.lastTransferTick=now;
        }
    }
    public static void register(RegisterTicketControllersEvent event){event.register(CONTROLLER);}

    /** Ephemeral tickets from an interrupted process must not be restored as permanent force loads. */
    private static void validateSavedTickets(ServerLevel level,TicketHelper helper){
        for(var owner:List.copyOf(helper.getBlockTickets().keySet()))helper.removeAllTickets(owner);
        for(var owner:List.copyOf(helper.getEntityTickets().keySet()))helper.removeAllTickets(owner);
    }

    public static Result prepare(PortalFunnelBlockEntity source){
        if(!(source.getLevel() instanceof ServerLevel level) || source.partner()==null)return Result.INVALID;
        var server=level.getServer();var target=source.partner();var destination=server.getLevel(target.dimension());
        if(destination==null || destination.isOutsideBuildHeight(target.pos()) || !destination.getWorldBorder().isWithinBounds(target.pos())){
            release(source);return Result.INVALID;
        }
        var state=SERVERS.computeIfAbsent(server,key->new State());
        var previous=state.leases.get(source.identity());
        if(previous!=null && (!previous.source.equals(source.globalPos()) || !previous.target.equals(target)))close(state,previous);
        // Already loaded endpoints need no new ticket, including chunks held by another sender.
        if(destination.hasChunkAt(target.pos()))return Result.READY;
        if(state.leases.containsKey(source.identity()))return Result.BUSY;
        int now=server.getTickCount();
        if(state.budgetTick!=now){state.budgetTick=now;state.requests=0;}
        if(state.leases.size()>=Balance.PORTAL_LOAD_LIMIT.get() || state.requests>=Balance.PORTAL_LOAD_REQUESTS.get())return Result.BUSY;
        state.requests++;
        var lease=new Lease(source,destination,now);
        // Reserve the global slot before the standard API synchronously brings the chunk to FULL.
        state.leases.put(lease.owner,lease);
        try{
            boolean added=CONTROLLER.forceChunk(destination,lease.owner,lease.chunk.x,lease.chunk.z,true,true);
            lease.loading=false;
            if(added && state.leases.get(lease.owner)==lease && validSource(server,lease) && destination.hasChunkAt(target.pos()))return Result.READY;
        }catch(RuntimeException exception){
            org.apache.logging.log4j.LogManager.getLogger().warn("Portal funnel could not load destination {}",target,exception);
        }finally{lease.loading=false;}
        close(state,lease);return Result.INVALID;
    }
    public static void transferred(PortalFunnelBlockEntity source){
        if(!(source.getLevel() instanceof ServerLevel level))return;
        var state=SERVERS.get(level.getServer());if(state==null)return;
        var lease=state.leases.get(source.identity());if(lease!=null)lease.lastTransferTick=level.getServer().getTickCount();
    }
    public static boolean release(PortalFunnelBlockEntity source){
        if (!(source.getLevel() instanceof ServerLevel level)) return false;
        var state = SERVERS.get(level.getServer());
        if (state == null) return false;
        var lease = state.leases.get(source.identity());
        if (lease == null) return false;
        close(state, lease);
        return true;
    }
    private static void close(State state,Lease lease){
        state.leases.remove(lease.owner,lease);
        // forceChunk obtains FULL before it installs the region ticket. An unload callback during
        // that synchronous wait must defer removal until prepare() returns from the native call.
        if(lease.loading)return;
        CONTROLLER.forceChunk(lease.destination,lease.owner,lease.chunk.x,lease.chunk.z,false,true);
    }
    private static boolean validSource(MinecraftServer server,Lease lease){
        var level=server.getLevel(lease.source.dimension());
        return level!=null && level.hasChunkAt(lease.source.pos())
                && level.getBlockEntity(lease.source.pos()) instanceof PortalFunnelBlockEntity source
                && !source.isRemoved() && !source.isChunkUnloaded() && lease.owner.equals(source.identity())
                && lease.target.equals(source.partner());
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var server=event.getServer();var state=SERVERS.get(server);if(state==null)return;
        for(var lease:List.copyOf(state.leases.values()))
            if(server.getTickCount()-lease.lastTransferTick>=IDLE_TICKS || !validSource(server,lease))close(state,lease);
    }
    @SubscribeEvent public static void stopping(ServerStoppingEvent event){
        var state=SERVERS.remove(event.getServer());if(state!=null)for(var lease:List.copyOf(state.leases.values()))close(state,lease);
    }
    @SubscribeEvent public static void unloading(LevelEvent.Unload event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var state=SERVERS.get(level.getServer());if(state==null)return;
        for(var lease:List.copyOf(state.leases.values()))
            if(lease.destination==level || lease.source.dimension().equals(level.dimension()))close(state,lease);
    }
    private PortalChunkTickets(){}
}
