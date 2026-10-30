package dev.createspellbook.reactor;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Owns the geometry certificate. Neither saved formed flags nor saved port links authorize operation. */
public final class ReactorControllerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public final ReactorOperation operation=new ReactorOperation(this);
    private UUID identity=UUID.randomUUID();
    private ReactorLayout lastLayout;
    private ReactorScanner.Issue issue=ReactorScanner.Issue.CHECKING;
    private BlockPos fault;
    private List<ReactorLinked> members=List.of();
    private List<Map.Entry<BlockPos,BlockState>> snapshot=List.of();
    private int auditCursor;
    private long lastManual=Long.MIN_VALUE;
    private long scans,audited;
    private boolean pendingOperationSync;
    private long nextOperationSync;
    public ReactorControllerBlockEntity(BlockPos pos,BlockState state){super(ReactorContent.CONTROLLER_BE.get(),pos,state);}
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours){}
    public Direction facing(){return getBlockState().getValue(ReactorControllerBlock.FACING);}
    public UUID identity(){return identity;}
    public ReactorLayout lastLayout(){return lastLayout;}
    public ReactorScanner.Issue issue(){return issue;}
    public BlockPos fault(){return fault;}
    public long scanCount(){return scans;}
    public long auditedCells(){return audited;}
    public int cachedCellCount(){return snapshot.size();}
    boolean structurallyFormed(){return issue==ReactorScanner.Issue.NONE;}
    public boolean isFormed(){
        if(!structurallyFormed() || lastLayout==null || isRemoved() || isChunkUnloaded())return false;
        if(level instanceof ServerLevel server){var manager=ReactorManager.existing(server);if(manager==null)return false;for(long key:lastLayout.bounds().chunks())if(!manager.loaded(key))return false;}
        return true;
    }
    @Override public void initialize(){super.initialize();if(level instanceof ServerLevel server){var manager=ReactorManager.get(server);suspend();manager.register(this);}}
    public void requestValidation(){
        if(!(level instanceof ServerLevel server) || isRemoved() || isChunkUnloaded())return;
        if(issue!=ReactorScanner.Issue.CHECKING){detach();issue=ReactorScanner.Issue.CHECKING;fault=null;publish();ReactorManager.get(server).reindex(this);}
        ReactorManager.get(server).enqueue(this);
    }
    public void chunkUnavailable(){detach();issue=ReactorScanner.Issue.UNLOADED;fault=null;publish();if(level instanceof ServerLevel server)ReactorManager.get(server).reindex(this);}
    public void suspend(){detach();issue=ReactorScanner.Issue.CHECKING;fault=null;publish();}
    private void detach(){operation.suspend();var previous=members;members=List.of();snapshot=List.of();for(var member:previous)if(member.reactorBinding().ownedBy(this))member.reactorBinding().clear();}
    public void checkManually(){if(level!=null && (lastManual==Long.MIN_VALUE || level.getGameTime()-lastManual>=10)){lastManual=level.getGameTime();validateNow();}}
    public void validateNow(){
        if(!(level instanceof ServerLevel server) || isRemoved() || isChunkUnloaded() || server.getBlockEntity(worldPosition)!=this)return;
        var manager=ReactorManager.get(server);detach();scans++;
        var result=ReactorScanner.find(server,this,manager);issue=result.issue();fault=result.fault();
        if(result.valid()){
            var selected=new ArrayList<ReactorLinked>();
            for(var pos:result.components()){
                if(!(server.getBlockEntity(pos) instanceof ReactorLinked member)){issue=ReactorScanner.Issue.MISSING_BLOCK_ENTITY;fault=pos;break;}
                var other=member.reactorBinding().owner();if(other!=null && other!=this && other.isFormed()){issue=ReactorScanner.Issue.SHARED_COMPONENT;fault=pos;break;}
                selected.add(member);
            }
            if(issue==ReactorScanner.Issue.NONE){lastLayout=result.layout();snapshot=List.copyOf(result.snapshot().entrySet());members=List.copyOf(selected);auditCursor=0;for(var member:members)member.reactorBinding().bind(this);operation.bind(members);}
        }
        publish();manager.scanned(this);
    }
    private void publish(){
        if(!(level instanceof ServerLevel server) || isRemoved() || isChunkUnloaded())return;
        var manager=ReactorManager.existing(server);if(manager==null || !manager.loaded(worldPosition))return;
        var state=getBlockState();if(state.is(ReactorContent.CONTROLLER.get()) && state.getValue(ReactorControllerBlock.FORMED)!=structurallyFormed())server.setBlock(worldPosition,state.setValue(ReactorControllerBlock.FORMED,structurallyFormed()),net.minecraft.world.level.block.Block.UPDATE_CLIENTS|net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
        operationChanged(true);
    }
    @Override public void onLoad(){
        super.onLoad();
        if(level!=null && level.isClientSide)dev.createspellbook.client.ReactorSounds.watch(this);
    }
    @Override public void tick(){
        super.tick();if(!(level instanceof ServerLevel server))return;operation.tick();
        // Storage can change while stopped, unstable, or with no menu open.
        // Coalesce pump updates into at most one telemetry packet per 10 ticks.
        if(pendingOperationSync && server.getGameTime()>=nextOperationSync)operationChanged(true);
        if(!isFormed() || snapshot.isEmpty() || server.getGameTime()%20!=0)return;
        // A small recovery audit covers code that bypasses normal Level state
        // notifications. Normal changes invalidate immediately through the hook.
        var manager=ReactorManager.get(server);
        for(int i=0;i<32;i++){var entry=snapshot.get(auditCursor);auditCursor=(auditCursor+1)%snapshot.size();audited++;
            if(!manager.loaded(entry.getKey())){chunkUnavailable();return;}
            if(ReactorManager.relevantChange(entry.getValue(),server.getBlockState(entry.getKey()))){requestValidation();return;}
            var role=ReactorContent.role(entry.getValue().getBlock());
            if(role!=null && role.linked() && (!(server.getBlockEntity(entry.getKey()) instanceof ReactorLinked member) || !member.reactorBinding().ownedBy(this))){requestValidation();return;}
        }
    }
    @Override public void onChunkUnloaded(){if(level instanceof ServerLevel server){var manager=ReactorManager.existing(server);if(manager!=null)manager.unregister(this);else suspend();}super.onChunkUnloaded();}
    @Override public void remove(){if(level instanceof ServerLevel server){var manager=ReactorManager.existing(server);if(manager!=null)manager.unregister(this);else suspend();}super.remove();}
    public Component statusText(){return issue==ReactorScanner.Issue.NONE?Component.translatable("message.create_spellbook.reactor.formed"):Component.translatable("message.create_spellbook.reactor.invalid",Component.translatable("reactor.create_spellbook.issue."+issue.name().toLowerCase(java.util.Locale.ROOT)),fault==null?"—":fault.toShortString());}
    public void operationChanged(boolean sync){
        if(level==null || level.isClientSide || isRemoved() || isChunkUnloaded())return;
        setChanged();pendingOperationSync=true;
        if(sync && level.hasChunkAt(worldPosition)){
            pendingOperationSync=false;nextOperationSync=level.getGameTime()+10;sendData();
        }
    }
    @Override public boolean addToGoggleTooltip(List<Component> lines,boolean sneaking){lines.add(statusText());lines.add(Component.translatable("reactor.create_spellbook.run."+operation.state().name().toLowerCase(java.util.Locale.ROOT)));lines.add(Component.translatable("message.create_spellbook.reactor.power",operation.actual(),operation.target()));lines.add(Component.translatable("message.create_spellbook.reactor.stabilizers",operation.working(),operation.required(),operation.reserves()));return true;}
    @Override protected void write(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.write(tag,lookup,packet);tag.putUUID("ReactorIdentity",identity);
        if(lastLayout!=null){tag.putLong("ReactorOrigin",lastLayout.origin().asLong());tag.putString("ReactorFacing",lastLayout.facing().getName());tag.putInt("ReactorSize",lastLayout.size());}
        if(packet){tag.putString("ReactorIssue",issue.name());if(fault!=null)tag.putLong("ReactorFault",fault.asLong());}
        var run=new CompoundTag();operation.write(run,lookup,packet);tag.put("Operation",run);
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.read(tag,lookup,packet);detach();if(tag.hasUUID("ReactorIdentity"))identity=tag.getUUID("ReactorIdentity");
        var facing=Direction.byName(tag.getString("ReactorFacing"));lastLayout=tag.contains("ReactorOrigin") && facing!=null && facing.getAxis().isHorizontal()?new ReactorLayout(BlockPos.of(tag.getLong("ReactorOrigin")),facing,tag.contains("ReactorSize")?Math.clamp(tag.getInt("ReactorSize"),5,19):9):null;
        issue=ReactorScanner.Issue.CHECKING;fault=null;
        if(packet){try{issue=ReactorScanner.Issue.valueOf(tag.getString("ReactorIssue"));}catch(IllegalArgumentException ignored){}if(tag.contains("ReactorFault"))fault=BlockPos.of(tag.getLong("ReactorFault"));}
        operation.read(tag.getCompound("Operation"),lookup,packet);
    }
}
