package dev.createspellbook.reactor;

import net.minecraft.core.*;
import java.util.*;

/** Local +X runs along the front face; local +Z points inward, opposite the controller. */
public record ReactorLayout(BlockPos origin,Direction facing,int size) {
    public static final int SIZE=9,MIN_SIZE=5,MAX_SIZE=19;
    public ReactorLayout(BlockPos origin,Direction facing){this(origin,facing,SIZE);}
    public int inner(){return size-2;}
    public int middle(){return (size-1)/2;}
    public net.minecraft.world.phys.Vec3 position(){var b=bounds();return new net.minecraft.world.phys.Vec3((b.minX()+b.maxX()+1)/2d,(b.minY()+b.maxY()+1)/2d,(b.minZ()+b.maxZ()+1)/2d);}
    public BlockPos center(){return BlockPos.containing(position());}
    public ReactorLayout{if(size<MIN_SIZE || size>MAX_SIZE)throw new IllegalArgumentException("Reactor side must be 5..19");origin=origin.immutable();if(facing.getAxis().isVertical())throw new IllegalArgumentException("Horizontal reactor facing required");}
    public BlockPos at(int x,int y,int z){return origin.relative(facing.getClockWise(),x).above(y).relative(facing.getOpposite(),z);}
    public BlockPos reactionCore(){return at(middle(),size-1,middle());}
    public BlockPos energyCore(){return at(middle(),0,middle());}
    public BlockPos shaft(){return at(size-1,middle(),middle());}
    public Box bounds(){return Box.between(at(0,0,0),at(size-1,size-1,size-1));}
    public static List<ReactorLayout> candidates(BlockPos controller,Direction facing){var layouts=new ArrayList<ReactorLayout>();for(int n=MIN_SIZE;n<=MAX_SIZE;n++)for(int y=1;y<n-1;y++)for(int x=1;x<n-1;x++)layouts.add(new ReactorLayout(controller.relative(facing.getClockWise(),-x).below(y),facing,n));return layouts;}
    public static Box searchBounds(BlockPos controller,Direction facing){return Box.between(controller.relative(facing.getClockWise(),-17).below(17),controller.relative(facing.getClockWise(),17).above(17).relative(facing.getOpposite(),18));}
    public static int boundaryCount(int x,int y,int z){return boundaryCount(x,y,z,SIZE);}
    public static int boundaryCount(int x,int y,int z,int n){return (x==0 || x==n-1?1:0)+(y==0 || y==n-1?1:0)+(z==0 || z==n-1?1:0);}
    public record Box(int minX,int minY,int minZ,int maxX,int maxY,int maxZ){
        public static Box between(BlockPos a,BlockPos b){return new Box(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()),Math.max(a.getX(),b.getX()),Math.max(a.getY(),b.getY()),Math.max(a.getZ(),b.getZ()));}
        public boolean contains(BlockPos p){return p.getX()>=minX && p.getX()<=maxX && p.getY()>=minY && p.getY()<=maxY && p.getZ()>=minZ && p.getZ()<=maxZ;}
        public Set<Long> chunks(){var result=new HashSet<Long>();for(int x=minX>>4;x<=maxX>>4;x++)for(int z=minZ>>4;z<=maxZ>>4;z++)result.add(net.minecraft.world.level.ChunkPos.asLong(x,z));return result;}
    }
}
