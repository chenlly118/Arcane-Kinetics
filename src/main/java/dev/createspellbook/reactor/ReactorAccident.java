package dev.createspellbook.reactor;

import dev.createspellbook.Balance;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** One durable accident, independent of block/entity lifetimes. Deadlines use
 * world game time: unloaded chunks do not pause them, an offline server does. */
public final class ReactorAccident {
    public enum Phase {WARNING,RUNAWAY,TERMINAL,FINISHED}
    public enum Cause {STABILIZERS,WASTE,STRUCTURE}
    public record Settings(int warningTicks,int runawayTicks,int terminalTicks,int radius){
        public Settings{warningTicks=Math.clamp(warningTicks,0,72000);runawayTicks=Math.clamp(runawayTicks,1,72000);terminalTicks=Math.clamp(terminalTicks,1,72000);radius=Math.clamp(radius,1,475);}
        public static Settings configured(){return forSize(9);}
        public static Settings forSize(int side){int total=12000+(int)Math.round((Math.clamp(side,5,19)-5)*60000d/14);return new Settings(0,total/5,total-total/5,Math.clamp(side,5,19)*25);}
    }
    public final UUID id,reactorId;
    public final BlockPos controller,origin,center;
    public final Direction facing;
    public final long started,warningEnd,runawayEnd,end,fieldEpoch;
    public final int required,radius,side,startUnits;
    public int causes;
    public long probes,swallowed,interactions,sweepCursor;
    public long workChunkCursor,loadedChunkSweeps;
    public int workCellCursor;
    public boolean escaped;
    int announced=-1;
    public ReactorAccident(UUID reactorId,BlockPos controller,ReactorLayout layout,int required,Cause cause,long now,Settings settings){this(reactorId,controller,layout,required,cause,now,settings,500);}
    public ReactorAccident(UUID reactorId,BlockPos controller,ReactorLayout layout,int required,Cause cause,long now,Settings settings,int units){this(reactorId,controller,layout,required,cause,now,settings,units,0);}
    public ReactorAccident(UUID reactorId,BlockPos controller,ReactorLayout layout,int required,Cause cause,long now,Settings settings,int units,long epoch){
        this(UUID.randomUUID(),reactorId,controller,layout.origin(),layout.facing(),layout.size(),required,1<<cause.ordinal(),now,
             now+(1001-Math.clamp(units,0,1000))*5L,
             now+(1001-Math.clamp(units,0,1000))*5L+settings.runawayTicks(),
             now+(1001-Math.clamp(units,0,1000))*5L+settings.runawayTicks()+settings.terminalTicks(),settings.radius(),units,epoch);
    }
    private ReactorAccident(UUID id,UUID reactorId,BlockPos controller,BlockPos origin,Direction facing,int side,int required,int causes,long started,long warningEnd,long runawayEnd,long end,int radius,int units,long epoch){
        this.id=id;this.reactorId=reactorId;this.controller=controller.immutable();this.origin=origin.immutable();this.facing=facing;this.side=Math.clamp(side,5,19);this.center=new ReactorLayout(origin,facing,this.side).center();this.required=Math.clamp(required,1,289);this.causes=causes;
        this.fieldEpoch=Math.max(0,epoch);this.started=started;this.warningEnd=warningEnd;this.runawayEnd=runawayEnd;this.end=end;this.radius=Math.clamp(radius,1,475);this.startUnits=Math.clamp(units,0,1000);
    }
    public ReactorLayout layout(){return new ReactorLayout(origin,facing,side);}
    public int containedSize(long now){return Math.clamp(startUnits+(int)Math.max(0,(now-started)/5),0,1001);}
    public Vec3 position(){return layout().position();}
    public Phase phase(long now){return now>=end?Phase.FINISHED:now>=runawayEnd?Phase.TERMINAL:now>=warningEnd || escaped?Phase.RUNAWAY:Phase.WARNING;}
    public int remaining(long now){long deadline=switch(phase(now)){case WARNING->warningEnd;case RUNAWAY->runawayEnd;default->end;};return (int)Math.max(0,deadline-now);}
    public static float projectionRadius(int phase,long now,long started,long warningEnd,long runawayEnd,long end){return projectionRadius(phase,now,started,warningEnd,runawayEnd,end,225);}
    public static float projectionRadius(int phase,long now,long started,long warningEnd,long runawayEnd,long end,int radius){
        double seed=Math.max(1,(radius/25d-2)/2);
        if(phase==1){double units=1001-Math.max(0,warningEnd-now)/5d;return (float)(seed*Math.clamp(units/1000d,0,1));}
        if(phase==2)return (float)(seed+(radius-seed)*Math.clamp((now-warningEnd)/(double)Math.max(1,runawayEnd-warningEnd),0,1));
        if(phase==3)return (float)(radius*(1-Math.clamp((now-runawayEnd)/(double)Math.max(1,end-runawayEnd),0,1)));
        return 0;
    }
    public float coreRadius(long now){return projectionRadius(phase(now).ordinal()+1,now,started,warningEnd,runawayEnd,end,radius);}
    public double destructionRadius(long now){return phase(now)==Phase.WARNING || phase(now)==Phase.FINISHED?0:coreRadius(now);}
    /** The old envelope and saved maximum remain authoritative for terrain.
     * Only the visible peak is smaller; no jump at the containment boundary. */
    public static float visibleRadius(int phase,long now,long started,long warningEnd,long runawayEnd,long end,int radius,double fraction){
        if(phase<2)return projectionRadius(phase,now,started,warningEnd,runawayEnd,end,radius);
        double seed=Math.max(1,(radius/25d-2)/2),peak=Math.max(seed,radius*Math.clamp(fraction,.5,.95));
        if(phase==2)return (float)(seed+(peak-seed)*Math.clamp((now-warningEnd)/(double)Math.max(1,runawayEnd-warningEnd),0,1));
        if(phase==3)return (float)(peak*(1-Math.clamp((now-runawayEnd)/(double)Math.max(1,end-runawayEnd),0,1)));
        return 0;
    }
    public float visualRadius(long now){return visibleRadius(phase(now).ordinal()+1,now,started,warningEnd,runawayEnd,end,radius,Balance.REACTOR_VISUAL_FRACTION.get());}
    public double gravityRadius(long now){return destructionRadius(now)*Balance.REACTOR_GRAVITY_FACTOR.get();}
    /** Fixed core sweep then deterministic, shell-biased erosion. O(1) memory and
     * O(probe budget), never a full sphere scan or a growing visited-position set. */
    public BlockPos nextProbe(long now){
        long index=probes++;
        if(index<343){int i=(int)index;return center.offset(i%7-3,i/49-3,i/7%7-3);}
        long seed=id.getMostSignificantBits()^id.getLeastSignificantBits()^(index*0x9e3779b97f4a7c15L);
        double x=unit(seed)*2-1,y=unit(seed+1)*2-1,z=unit(seed+2)*2-1;double length=Math.sqrt(x*x+y*y+z*z);
        if(length<1e-8){x=1;length=1;}
        double u=unit(seed+3),fraction=index%3==0?Math.cbrt(u):.65+.35*u;
        double distance=destructionRadius(now)*fraction/length;
        return BlockPos.containing(position().add(x*distance,y*distance,z*distance));
    }
    private static double unit(long value){value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;value=(value^(value>>>27))*0x94d049bb133111ebL;return ((value^(value>>>31))>>>11)*0x1.0p-53;}
    public CompoundTag save(){
        var tag=new CompoundTag();tag.putUUID("Id",id);tag.putUUID("Reactor",reactorId);tag.putLong("Controller",controller.asLong());tag.putLong("Origin",origin.asLong());tag.putString("Facing",facing.getName());tag.putLong("FieldEpoch",fieldEpoch);tag.putInt("Schema",3);tag.putInt("Side",side);tag.putInt("StartUnits",startUnits);tag.putInt("Required",required);tag.putInt("Causes",causes);
        tag.putLong("WorkChunkCursor",workChunkCursor);tag.putInt("WorkCellCursor",workCellCursor);tag.putLong("LoadedChunkSweeps",loadedChunkSweeps);
        tag.putLong("Started",started);tag.putLong("WarningEnd",warningEnd);tag.putLong("RunawayEnd",runawayEnd);tag.putLong("End",end);tag.putInt("Radius",radius);tag.putLong("Probes",probes);tag.putLong("SweepCursor",sweepCursor);tag.putLong("Swallowed",swallowed);tag.putLong("Interactions",interactions);tag.putBoolean("Escaped",escaped);return tag;
    }
    public static ReactorAccident load(CompoundTag tag){
        var facing=Direction.byName(tag.getString("Facing"));
        if(!tag.hasUUID("Id") || !tag.hasUUID("Reactor") || facing==null || !facing.getAxis().isHorizontal() || tag.getLong("WarningEnd")<=tag.getLong("Started") || tag.getLong("RunawayEnd")<=tag.getLong("WarningEnd") || tag.getLong("End")<=tag.getLong("RunawayEnd"))return null;
        long peak=tag.getLong("RunawayEnd");
        // Upgrade the old half/half production curve without resetting either
        // the escape deadline or final expiration of an already saved field.
        int side=tag.contains("Side")?tag.getInt("Side"):9;
        if(tag.getInt("Schema")==2 && tag.getLong("End")-tag.getLong("WarningEnd")==Settings.forSize(side).runawayTicks()+Settings.forSize(side).terminalTicks())peak=tag.getLong("WarningEnd")+(tag.getLong("End")-tag.getLong("WarningEnd"))/5;
        var a=new ReactorAccident(tag.getUUID("Id"),tag.getUUID("Reactor"),BlockPos.of(tag.getLong("Controller")),BlockPos.of(tag.getLong("Origin")),facing,side,tag.getInt("Required"),tag.getInt("Causes"),tag.getLong("Started"),tag.getLong("WarningEnd"),peak,tag.getLong("End"),tag.getInt("Radius"),tag.contains("StartUnits")?tag.getInt("StartUnits"):500,tag.getLong("FieldEpoch"));
        a.workChunkCursor=Math.max(0,tag.getLong("WorkChunkCursor"));a.workCellCursor=Math.clamp(tag.getInt("WorkCellCursor"),0,1048576);a.loadedChunkSweeps=Math.max(0,tag.getLong("LoadedChunkSweeps"));
        a.probes=Math.max(0,tag.getLong("Probes"));a.sweepCursor=Math.max(0,tag.getLong("SweepCursor"));a.swallowed=Math.max(0,tag.getLong("Swallowed"));a.interactions=Math.max(0,tag.getLong("Interactions"));a.escaped=tag.getBoolean("Escaped");return a;
    }
}
