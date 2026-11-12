package dev.createspellbook.utility;

import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.logistics.funnel.*;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.*;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;
import dev.createspellbook.mixin.FunnelBehaviourAccess;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import dev.createspellbook.Balance;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import java.util.*;

/** Separate send/receive queues prevent bounce-back; demand leases never bypass peer validation. */
public final class PortalFunnelBlockEntity extends FunnelBlockEntity {
    private static final int QUEUE_SLOTS = 8;
    private static final int BATCH_STACKS = 4;
    private UUID identity=UUID.randomUUID(),owner,peerIdentity,pairIdentity;
    private GlobalPos partner;
    private int cooldown, cursor, outputCursor;
    private boolean transacting;
    private long transferred;
    private long lastActionTick=Long.MIN_VALUE;
    private long retryAfter;
    private FilteringBehaviour filtering;
    private InvManipulationBehaviour inventoryBehaviour;
    private VersionedInventoryTrackerBehaviour tracker;
    private final ItemStackHandler outgoing=inventory(),received=inventory();
    private ItemStackHandler inventory(){return new ItemStackHandler(QUEUE_SLOTS){@Override protected void onContentsChanged(int slot){setChanged();if(tracker!=null)tracker.reset();}};}
    private final IItemHandler port=new IItemHandler(){
        private void check(int slot){if(slot<0 || slot>=16)throw new IndexOutOfBoundsException(slot);}
        @Override public int getSlots(){return 16;}
        @Override public ItemStack getStackInSlot(int slot){check(slot);return (slot<8?outgoing.getStackInSlot(slot):received.getStackInSlot(slot-8)).copy();}
        @Override public int getSlotLimit(int slot){check(slot);return 64;}
        @Override public boolean isItemValid(int slot,ItemStack stack){check(slot);return slot<8 && accepts(stack);}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){check(slot);return slot<8 && available() && !paused() && collecting() && !transacting && accepts(stack)?outgoing.insertItem(slot,stack.copy(),simulate):stack.copy();}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){
            check(slot);if(slot<8 || !available() || transacting || amount<=0)return ItemStack.EMPTY;
            return received.extractItem(slot-8,batchSize(received.getStackInSlot(slot-8),amount),simulate);
        }
    };
    private final IItemHandler nativePort=new IItemHandler(){
        @Override public int getSlots(){return port.getSlots();}
        @Override public ItemStack getStackInSlot(int slot){return port.getStackInSlot(slot);}
        @Override public int getSlotLimit(int slot){return port.getSlotLimit(slot);}
        @Override public boolean isItemValid(int slot,ItemStack stack){return port.isItemValid(slot,stack);}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return port.insertItem(slot,stack,simulate);}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){
            // Preserve the portal's existing inventory-to-inventory output and its backpressure.
            // Otherwise Create itself performs the actual belt/depot/world extraction.
            return getBlockState().getBlock() instanceof BeltFunnelBlock || hasFrontDestination()?ItemStack.EMPTY:port.extractItem(slot,amount,simulate);
        }
    };
    public PortalFunnelBlockEntity(BlockPos pos,BlockState state){super(UtilityContent.PORTAL_BE.get(),pos,state);}
    private boolean available(){return level instanceof ServerLevel && !isRemoved() && !isChunkUnloaded();}
    public UUID identity(){return identity;}
    public GlobalPos partner(){return partner;}
    public long transferred(){return transferred;}
    public IItemHandler itemPort(){return port;}
    IItemHandler nativeInventory(){return nativePort;}
    public boolean paused(){return getBlockState().getOptionalValue(AbstractFunnelBlock.POWERED).orElse(false);}
    public boolean collecting(){
        var state=getBlockState();
        if(state.getBlock() instanceof FunnelBlock)return !state.getValue(FunnelBlock.EXTRACTING);
        var shape=state.getValue(BeltFunnelBlock.SHAPE);
        if(shape==BeltFunnelBlock.Shape.PULLING)return true;
        if(shape==BeltFunnelBlock.Shape.PUSHING)return false;
        return level!=null && level.getBlockEntity(worldPosition.below()) instanceof BeltBlockEntity belt && belt.getMovementFacing()!=PortalFunnelBlock.facing(state);
    }
    @Override public int getAmountToExtract(){return inventoryBehaviour.getAmountFromFilter();}
    @Override public ExtractionCountMode getModeToExtract(){return inventoryBehaviour.getModeFromFilter();}
    public FilteringBehaviour filtering(){return filtering;}
    private boolean accepts(ItemStack stack){return !stack.isEmpty() && (filtering==null || filtering.test(stack));}
    private int batchSize(ItemStack stack,int capacity){
        if(!accepts(stack))return 0;
        int available=Math.min(stack.getCount(),capacity);
        if(filtering==null || filtering.anyAmount())return available;
        int requested=Math.clamp(filtering.getAmount(),1,stack.getMaxStackSize());
        return !filtering.upTo && available<requested?0:Math.min(available,requested);
    }
    public int pendingCount(){return count(outgoing);}
    public int receivedCount(){return count(received);}
    private int count(ItemStackHandler handler){int total=0;for(int i=0;i<8;i++)total+=handler.getStackInSlot(i).getCount();return total;}
    public void setOwner(Player player){owner=player==null?null:player.getUUID();changed();}
    public boolean canConfigure(Player player){return owner==null || owner.equals(player.getUUID()) || player.isCreative();}
    public GlobalPos globalPos(){return GlobalPos.of(level.dimension(),worldPosition);}
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours){
        inventoryBehaviour=new PortalFunnelInventoryBehaviour(this);tracker=new VersionedInventoryTrackerBehaviour(this);
        filtering=new FilteringBehaviour(this,new PortalFunnelFilterSlot()){
            @Override public boolean mayInteract(Player player){return canConfigure(player) && super.mayInteract(player);}
            @Override public void onShortInteract(Player player,InteractionHand hand,Direction side,net.minecraft.world.phys.BlockHitResult hit){if(mayInteract(player))super.onShortInteract(player,hand,side,hit);}
            @Override public void setValueSettings(Player player,ValueSettings settings,boolean ctrlDown){if(mayInteract(player)){super.setValueSettings(player,settings,ctrlDown);retryAfter=0;}}
            @Override public boolean readFromClipboard(HolderLookup.Provider lookup,CompoundTag tag,Player player,Direction side,boolean simulate){return mayInteract(player) && super.readFromClipboard(lookup,tag,player,side,simulate);}
        }.showCount().withCallback(stack->{retryAfter=0;tracker.reset();});
        behaviours.add(inventoryBehaviour);behaviours.add(tracker);
        behaviours.add(filtering);
        behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen(side->collecting() && !paused()).setInsertionHandler((stack,side,simulate)->insertInput(stack.stack,simulate)));
        // Create hard-checks FunnelBlockEntity and keeps these three behaviours private.
        // Bind only this subclass to a virtual inventory; native funnels are untouched.
        var access=(FunnelBehaviourAccess)(Object)this;access.spellbook$inventory(inventoryBehaviour);access.spellbook$tracker(tracker);access.spellbook$filter(filtering);
        registerAwardables(behaviours,com.simibubi.create.foundation.advancement.AllAdvancements.FUNNEL);
    }
    public ItemStack insertInput(ItemStack stack,boolean simulate){return available() && collecting() && !paused() && !transacting && accepts(stack)?ItemHandlerHelper.insertItemStacked(outgoing,stack.copy(),simulate):stack.copy();}
    public boolean pair(PortalFunnelBlockEntity other){
        if(other==this || !available() || !other.available() || level.getServer()!=other.level.getServer())return false;
        unlink();other.unlink();UUID key=UUID.randomUUID();
        partner=other.globalPos();peerIdentity=other.identity;pairIdentity=key;
        other.partner=globalPos();other.peerIdentity=identity;other.pairIdentity=key;changed();other.changed();return true;
    }
    public void unlink(){
        PortalChunkTickets.release(this);retryAfter=0;
        var peer=findPeer(false);UUID old=pairIdentity;
        partner=null;peerIdentity=null;pairIdentity=null;changed();
        if(peer!=null && old!=null && old.equals(peer.pairIdentity) && identity.equals(peer.peerIdentity)){
            PortalChunkTickets.release(peer);peer.retryAfter=0;
            peer.partner=null;peer.peerIdentity=null;peer.pairIdentity=null;peer.changed();
        }
    }
    private PortalFunnelBlockEntity findPeer(boolean reciprocal){
        if(!(level instanceof ServerLevel server) || partner==null || peerIdentity==null || pairIdentity==null)return null;
        var destination=server.getServer().getLevel(partner.dimension());
        if(destination==null || !destination.hasChunkAt(partner.pos()))return null;
        if(!(destination.getBlockEntity(partner.pos()) instanceof PortalFunnelBlockEntity peer) || !peer.available() || !peer.identity.equals(peerIdentity))return null;
        if(reciprocal && (!pairIdentity.equals(peer.pairIdentity) || !identity.equals(peer.peerIdentity) || !globalPos().equals(peer.partner)))return null;
        return peer;
    }
    @Override public void tick(){
        if(level==null)return;
        if(!level.isClientSide){if(!available() || transacting || lastActionTick==level.getGameTime())return;lastActionTick=level.getGameTime();}
        if (available() && !paused() && !collecting() && receivedCount() > 0) exportReceived();
        super.tick();if(!available() || transacting)return;
        if(paused()){PortalChunkTickets.release(this);return;}
        if(cooldown>0){cooldown--;setChanged();return;}
        cooldown=Math.max(0,Balance.PORTAL_INTERVAL.get()-1);setChanged();
        if(pendingCount()==0 || partner==null || level.getGameTime()<retryAfter)return;
        var readiness=PortalChunkTickets.prepare(this);
        if(readiness==PortalChunkTickets.Result.BUSY)return;
        if(readiness!=PortalChunkTickets.Result.READY){transferFailed();return;}
        // The ticket only makes the destination accessible. Revalidate identity, reciprocal link,
        // pair token and capacity on this server thread before removing any source item.
        var peer=findPeer(true);if(peer==null || peer.transacting || peer.paused()){transferFailed();return;}
        int firstSlot = cursor;
        int movedStacks = 0;
        boolean compatibleStack = false;
        transacting = true;
        peer.transacting = true;
        try {
            for (int offset = 0; offset < QUEUE_SLOTS && movedStacks < BATCH_STACKS; offset++) {
                int slot = (firstSlot + offset) % QUEUE_SLOTS;
                var original = outgoing.getStackInSlot(slot);
                int requested = batchSize(original, original.getCount());
                if (requested == 0 || !peer.accepts(original)) continue;
                compatibleStack = true;

                var offered = original.copyWithCount(requested);
                var remainder = ItemHandlerHelper.insertItemStacked(peer.received, offered, true);
                int amount = batchSize(original, requested - remainder.getCount());
                if (amount == 0) continue;

                var rejected = ItemHandlerHelper.insertItemStacked(peer.received, offered.copyWithCount(amount), false);
                int accepted = amount - rejected.getCount();
                if (accepted == 0) continue;
                outgoing.extractItem(slot, accepted, false);
                transferred += accepted;
                cursor = (slot + 1) % QUEUE_SLOTS;
                movedStacks++;
            }
        } finally {
            transacting = false;
            peer.transacting = false;
        }
        if (movedStacks > 0) {
            PortalChunkTickets.transferred(this);
            particles((ServerLevel) level, worldPosition);
            particles((ServerLevel) peer.level, peer.worldPosition);
        } else {
            boolean releasedTicket = PortalChunkTickets.release(this);
            // A loaded receiver can retry on the next cycle as its belt clears.
            if (!compatibleStack || releasedTicket) deferTransfer();
        }
    }
    private void transferFailed(){PortalChunkTickets.release(this);deferTransfer();}
    private void deferTransfer(){retryAfter=level.getGameTime()+Balance.PORTAL_RETRY.get();}
    @Override public void onChunkUnloaded(){PortalChunkTickets.release(this);super.onChunkUnloaded();}
    @Override public void invalidate(){PortalChunkTickets.release(this);super.invalidate();}
    private boolean hasFrontDestination(){
        if(level==null || getBlockState().getBlock() instanceof BeltFunnelBlock)return false;
        Direction facing=PortalFunnelBlock.facing(getBlockState());var target=worldPosition.relative(facing);
        return level.hasChunkAt(target) && !(level.getBlockEntity(target) instanceof PortalFunnelBlockEntity)
                && (BlockEntityBehaviour.get(level,target,DirectBeltInputBehaviour.TYPE)!=null || level.getCapability(Capabilities.ItemHandler.BLOCK,target,facing.getOpposite())!=null);
    }
    private void exportReceived(){
        Direction facing=PortalFunnelBlock.facing(getBlockState());BlockPos target=getBlockState().getBlock() instanceof BeltFunnelBlock?worldPosition.below():worldPosition.relative(facing);
        if(!level.hasChunkAt(target) || level.getBlockEntity(target) instanceof PortalFunnelBlockEntity)return;
        var belt=BlockEntityBehaviour.get(level,target,DirectBeltInputBehaviour.TYPE);
        var inventory = belt == null ? level.getCapability(Capabilities.ItemHandler.BLOCK, target, facing.getOpposite()) : null;
        if(belt==null && inventory==null)return;
        if(belt!=null && (!belt.canInsertFromSide(facing) || belt.isOccupied(facing)))return;
        int firstSlot = outputCursor;
        int maxStacks = belt == null ? BATCH_STACKS : 1;
        int movedStacks = 0;
        transacting = true;
        try {
            for (int offset = 0; offset < QUEUE_SLOTS && movedStacks < maxStacks; offset++) {
                int slot = (firstSlot + offset) % QUEUE_SLOTS;
                var stack = received.getStackInSlot(slot);
                int requested = batchSize(stack, stack.getCount());
                if (requested == 0) continue;
                var sample=stack.copyWithCount(requested);
                ItemStack simulated=belt!=null?belt.handleInsertion(sample.copy(),facing,true):ItemHandlerHelper.insertItemStacked(inventory,sample.copy(),true);
                int amount=batchSize(stack,requested-simulated.getCount());if(amount<=0)continue;
                // Keep the reserved stack until the target acknowledges the real insertion.
                ItemStack rejected=belt!=null?belt.handleInsertion(sample.copyWithCount(amount),facing,false):ItemHandlerHelper.insertItemStacked(inventory,sample.copyWithCount(amount),false);
                int accepted=Math.clamp(amount-rejected.getCount(),0,amount);
                if (accepted == 0) continue;
                received.extractItem(slot, accepted, false);
                outputCursor = (slot + 1) % QUEUE_SLOTS;
                movedStacks++;
            }
        } finally {
            transacting = false;
        }
        if (movedStacks > 0) flap(false);
    }
    private static void particles(ServerLevel level,BlockPos pos){level.sendParticles(ParticleTypes.PORTAL,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,2,.18,.18,.18,.01);}
    private void changed(){setChanged();if(level!=null && !level.isClientSide)sendData();}
    public void dropContentsAndUnlink(){
        if(!available())return;unlink();transacting=true;
        try{for(var handler:List.of(outgoing,received))for(int i=0;i<8;i++){
            var stack=handler.extractItem(i,Integer.MAX_VALUE,false);
            if(!stack.isEmpty())net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,stack);
        }}finally{transacting=false;}
    }
    @Override protected void write(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        super.write(tag,lookup,packet);tag.putUUID("Endpoint",identity);if(owner!=null)tag.putUUID("Owner",owner);
        if(partner!=null && peerIdentity!=null && pairIdentity!=null){
            tag.putString("PeerDimension",partner.dimension().location().toString());tag.putLong("PeerPosition",partner.pos().asLong());tag.putUUID("PeerEndpoint",peerIdentity);tag.putUUID("Pair",pairIdentity);
        }
        if(!packet){tag.put("SendQueue",outgoing.serializeNBT(lookup));tag.put("ReceiveQueue",received.serializeNBT(lookup));tag.putInt("PortalTransferCooldown",cooldown);tag.putInt("PortalOutputCursor",outputCursor);tag.putInt("TransferCursor",cursor);tag.putLong("Transferred",transferred);}
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet){
        if(!packet){PortalChunkTickets.release(this);retryAfter=0;}
        super.read(tag,lookup,packet);if(tag.hasUUID("Endpoint"))identity=tag.getUUID("Endpoint");owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;
        partner=null;peerIdentity=null;pairIdentity=null;
        var dimension=ResourceLocation.tryParse(tag.getString("PeerDimension"));
        if(dimension!=null && tag.hasUUID("PeerEndpoint") && tag.hasUUID("Pair")){
            partner=GlobalPos.of(ResourceKey.create(Registries.DIMENSION,dimension),BlockPos.of(tag.getLong("PeerPosition")));peerIdentity=tag.getUUID("PeerEndpoint");pairIdentity=tag.getUUID("Pair");
        }
        if(!packet){outgoing.deserializeNBT(lookup,tag.getCompound("SendQueue"));received.deserializeNBT(lookup,tag.getCompound("ReceiveQueue"));cooldown=Math.clamp(tag.getInt(tag.contains("PortalTransferCooldown")?"PortalTransferCooldown":"TransferCooldown"),0,Balance.PORTAL_INTERVAL.get());outputCursor=Math.floorMod(tag.getInt("PortalOutputCursor"),QUEUE_SLOTS);cursor=Math.floorMod(tag.getInt("TransferCursor"),QUEUE_SLOTS);transferred=Math.max(0,tag.getLong("Transferred"));}
    }
}
