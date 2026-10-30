package dev.createspellbook.reactor;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.createspellbook.Balance;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** Native Create source. The energy core decides capacity; the controller supplies only paid leases. */
public final class ReactorShaftBlockEntity extends GeneratingKineticBlockEntity implements ReactorLinked {
    private final ReactorBinding binding=new ReactorBinding(this::changed);
    private int leasedCapacity;
    private long validUntil=Long.MIN_VALUE;
    private float savedNativeRPM;
    private boolean restoringNativeSource;
    private int snapshotDepth;
    private int displayedCapacity;
    private float displayedRPM;
    public ReactorShaftBlockEntity(BlockPos pos,BlockState state){super(ReactorContent.SHAFT_BE.get(),pos,state);}
    @Override public ReactorBinding reactorBinding(){return binding;}
    @Override public float calculateStressApplied(){lastStressApplied=0;return 0;}
    private float rpm(){return Math.min(Balance.REACTOR_RPM.get(),AllConfigs.server().kinetics.maxRotationSpeed.get());}
    public int totalCapacity(){return level!=null && level.isClientSide?displayedCapacity:live()?leasedCapacity:0;}
    private boolean live(){return snapshotDepth==0 && leasedCapacity>0 && level!=null && level.getGameTime()<=validUntil && binding.valid() && binding.owner().operation.actual()==leasedCapacity && binding.owner().operation.energyCapacity()==leasedCapacity;}
    @Override public float getGeneratedSpeed(){return level!=null && level.isClientSide?displayedCapacity>0?displayedRPM:0:restoringNativeSource?savedNativeRPM:live()?rpm():0;}
    @Override public float calculateAddedStressCapacity(){lastCapacityProvided=level!=null && level.isClientSide?displayedCapacity/Math.max(1,displayedRPM):live()?leasedCapacity/rpm():0;return lastCapacityProvided;}
    public void supply(int capacity){
        if(level==null || level.isClientSide || !binding.valid())return;
        boolean change=leasedCapacity!=capacity;leasedCapacity=capacity;validUntil=level.getGameTime()+1;
        if(change || getTheoreticalSpeed()==0)updateGeneratedRotation();
    }
    public void withdraw(){
        boolean powered=leasedCapacity!=0;leasedCapacity=0;validUntil=Long.MIN_VALUE;
        if(level!=null && !level.isClientSide && (powered || isSource()))updateGeneratedRotation();
    }
    /** Disk snapshots must not authorize a source whose reactor chunks might never load.
     * Save is synchronous on the server thread: withdraw and restore through Create's
     * public generator lifecycle without advancing time or spending another cycle. */
    void beginSnapshot(){if(++snapshotDepth==1 && leasedCapacity>0)updateGeneratedRotation();}
    void endSnapshot(){if(snapshotDepth>0 && --snapshotDepth==0 && live())updateGeneratedRotation();}
    private void changed(){if(!binding.valid())withdraw();if(level!=null && !isRemoved() && !isChunkUnloaded() && level.hasChunkAt(worldPosition)){setChanged();if(!level.isClientSide)sendData();}}
    @Override public void initialize(){
        // Native addSilently must subtract a saved source's historical contribution
        // from Create's unloaded cache. It adds ZERO new capacity during this join.
        // The saved RPM is never a lease and is withdrawn before initialization returns.
        restoringNativeSource=level!=null && !level.isClientSide && savedNativeRPM>0;
        try{super.initialize();}finally{restoringNativeSource=false;savedNativeRPM=0;}
        if(level!=null && !level.isClientSide)updateGeneratedRotation();
    }
    @Override public void tick(){super.tick();if(level!=null && !level.isClientSide && leasedCapacity>0 && !live())withdraw();}
    @Override public void onChunkUnloaded(){withdraw();binding.suspended();super.onChunkUnloaded();}
    @Override public void remove(){withdraw();binding.suspended();super.remove();}
    @Override protected void write(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){super.write(tag,lookup,packet);binding.write(tag);if(!packet)tag.putFloat("ReactorSavedNativeRPM",getGeneratedSpeed());else{tag.putInt("ReactorCapacity",totalCapacity());tag.putFloat("ReactorRPM",rpm());}}
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){leasedCapacity=0;validUntil=Long.MIN_VALUE;savedNativeRPM=packet?0:tag.getFloat("ReactorSavedNativeRPM");super.read(tag,lookup,packet);binding.read(tag);if(packet){displayedCapacity=Math.clamp(tag.getInt("ReactorCapacity"),0,ReactorOperation.MAX_POWER);displayedRPM=Math.clamp(tag.getFloat("ReactorRPM"),1,256);}}
}
