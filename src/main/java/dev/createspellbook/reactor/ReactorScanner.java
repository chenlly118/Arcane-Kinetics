package dev.createspellbook.reactor;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** A read-only scan. Candidate probes bound discovery; a successful scan snapshots exactly side cubed cells. */
public final class ReactorScanner {
    public enum Issue {NONE,CHECKING,UNLOADED,OUTSIDE_WORLD,EDGE,FACE,INTERIOR,REACTION_CORE,ENERGY_CORE,SHAFT,CONTROLLER_COUNT,CONTROLLER_FACING,MISSING_BLOCK_ENTITY,SHARED_COMPONENT,MISSING_MANA_INTERFACE,MISSING_STABILIZER_PORT,MISSING_OUTPUT_PORT}
    public record Result(ReactorLayout layout,Issue issue,BlockPos fault,Map<BlockPos,BlockState> snapshot,List<BlockPos> components){
        public boolean valid(){return issue==Issue.NONE;}
        static Result failure(ReactorLayout layout,Issue issue,BlockPos p){return new Result(layout,issue,p==null?null:p.immutable(),Map.of(),List.of());}
    }
    private record Candidate(ReactorLayout layout,int score,boolean preferred){}
    public static Result find(ServerLevel level,ReactorControllerBlockEntity controller,ReactorManager manager){
        Result preferred=null;if(controller.lastLayout()!=null){preferred=check(level,controller.getBlockPos(),controller.facing(),controller.lastLayout(),manager);if(preferred.valid() || preferred.issue()==Issue.UNLOADED)return preferred;}
        var ranked=new ArrayList<Candidate>();
        for(var layout:ReactorLayout.candidates(controller.getBlockPos(),controller.facing())){
            int score=0,n=layout.size()-1;for(int x:new int[]{0,n})for(int y:new int[]{0,n})for(int z:new int[]{0,n})if(is(level,layout.at(x,y,z),ReactorRole.CASING,manager))score++;
            if(score>=4)ranked.add(new Candidate(layout,score,false));
        }
        ranked.sort(Comparator.comparingInt(Candidate::score).reversed());Result first=preferred;
        // At most 16 deep scans; never scan thousands of possible full cubes.
        for(int i=0;i<Math.min(16,ranked.size());i++){var result=check(level,controller.getBlockPos(),controller.facing(),ranked.get(i).layout(),manager);if(result.valid())return result;if(first==null)first=result;}
        return first!=null?first:Result.failure(null,Issue.EDGE,controller.getBlockPos());
    }
    private static boolean is(ServerLevel level,BlockPos pos,ReactorRole role,ReactorManager manager){return pos.getY()>=level.getMinBuildHeight() && pos.getY()<level.getMaxBuildHeight() && manager.loaded(pos) && level.getBlockState(pos).is(ReactorContent.block(role));}
    public static Result check(ServerLevel level,BlockPos controller,Direction facing,ReactorLayout layout,ReactorManager manager){
        if(layout.facing()!=facing)return Result.failure(layout,Issue.CONTROLLER_FACING,controller);
        var bounds=layout.bounds();
        if(bounds.minY()<level.getMinBuildHeight() || bounds.maxY()>=level.getMaxBuildHeight())return Result.failure(layout,Issue.OUTSIDE_WORLD,layout.origin());
        for(long chunk:bounds.chunks())if(!manager.loaded(chunk))return Result.failure(layout,Issue.UNLOADED,new BlockPos(net.minecraft.world.level.ChunkPos.getX(chunk)<<4,layout.origin().getY(),net.minecraft.world.level.ChunkPos.getZ(chunk)<<4));
        var snapshot=new LinkedHashMap<BlockPos,BlockState>();var components=new ArrayList<BlockPos>();var faceRoles=EnumSet.noneOf(ReactorRole.class);int controllers=0,shafts=0;int n=layout.size();
        for(int y=0;y<n;y++)for(int z=0;z<n;z++)for(int x=0;x<n;x++){
            var pos=layout.at(x,y,z);var state=level.getBlockState(pos);var role=ReactorContent.role(state.getBlock());
            snapshot.put(pos,state);int boundary=ReactorLayout.boundaryCount(x,y,z,n);
            if(role==ReactorRole.CONTROLLER){
                if(++controllers>1)return Result.failure(layout,Issue.CONTROLLER_COUNT,pos);
                if(!pos.equals(controller) || boundary!=1 || x==0 || x==n-1 || y==0 || y==n-1 || z!=0 || state.getValue(ReactorControllerBlock.FACING)!=facing)return Result.failure(layout,Issue.CONTROLLER_FACING,pos);
            }
            if(boundary>=2){if(role!=ReactorRole.CASING)return Result.failure(layout,Issue.EDGE,pos);}
            else if(boundary==1){
                if(role==ReactorRole.SHAFT){
                    if(y==0 || y==n-1 || ++shafts>1)return Result.failure(layout,Issue.SHAFT,pos);
                    Direction outward=x==0?layout.facing().getCounterClockWise():x==n-1?layout.facing().getClockWise():z==0?layout.facing():layout.facing().getOpposite();
                    if(state.getValue(ReactorShaftBlock.FACING)!=outward)return Result.failure(layout,Issue.SHAFT,pos);
                }else if(role==ReactorRole.REACTION_CORE){if(y!=n-1)return Result.failure(layout,Issue.REACTION_CORE,pos);}
                else if(role==ReactorRole.ENERGY_CORE){if(y!=0)return Result.failure(layout,Issue.ENERGY_CORE,pos);}
                else if(role==null || !role.shell())return Result.failure(layout,Issue.FACE,pos);
            }else if(role==ReactorRole.REACTION_CORE){
                // Accept a core mounted directly below the roof as well as one
                // replacing the roof. The backing cell must remain shell, so a
                // column cannot contain both mounting variants and count twice.
                if(y!=n-2 || !shell(level,layout.at(x,n-1,z)))return Result.failure(layout,Issue.REACTION_CORE,pos);
            }else if(role==ReactorRole.ENERGY_CORE){
                if(y!=1 || !shell(level,layout.at(x,0,z)))return Result.failure(layout,Issue.ENERGY_CORE,pos);
            }else if(!state.isAir())return Result.failure(layout,Issue.INTERIOR,pos);
            if(boundary==1 && role!=null)faceRoles.add(role);
            if(role!=null && role.linked())components.add(pos);
        }
        if(shafts!=1)return Result.failure(layout,Issue.SHAFT,controller);
        if(controllers!=1)return Result.failure(layout,Issue.CONTROLLER_COUNT,controller);
        if(!faceRoles.contains(ReactorRole.MANA))return Result.failure(layout,Issue.MISSING_MANA_INTERFACE,controller);
        if(!faceRoles.contains(ReactorRole.STABILIZER))return Result.failure(layout,Issue.MISSING_STABILIZER_PORT,controller);
        if(!faceRoles.contains(ReactorRole.OUTPUT))return Result.failure(layout,Issue.MISSING_OUTPUT_PORT,controller);
        return new Result(layout,Issue.NONE,null,Collections.unmodifiableMap(snapshot),List.copyOf(components));
    }
    private static boolean shell(ServerLevel level,BlockPos pos){var role=ReactorContent.role(level.getBlockState(pos).getBlock());return role!=null && role.shell();}
    private ReactorScanner(){}
}
