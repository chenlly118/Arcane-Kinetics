package dev.createspellbook.devices;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.event.BlockEntityBehaviourEvent;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.*;
import dev.createspellbook.CreateSpellbook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import java.util.*;

/** One implementation for native Depots, the reskinned Depot and moving belts. */
@EventBusSubscriber(modid=CreateSpellbook.ID)
public final class SpellProcessingBehaviour extends BlockEntityBehaviour implements SpellTarget {
    public static final BehaviourType<SpellProcessingBehaviour> TYPE=new BehaviourType<>();
    private UUID probeId;
    private final LinkedHashMap<UUID,Long> contacts=new LinkedHashMap<>(),projectiles=new LinkedHashMap<>(),casts=new LinkedHashMap<>();
    public SpellProcessingBehaviour(SmartBlockEntity be){super(be);}
    @SubscribeEvent public static void attach(BlockEntityBehaviourEvent event){
        event.forType(AllBlockEntityTypes.DEPOT.get(),be->event.attach(new SpellProcessingBehaviour(be)));
        event.forType(DeviceContent.DEPOT_BE.get(),be->event.attach(new SpellProcessingBehaviour(be)));
        event.forType(AllBlockEntityTypes.BELT.get(),be->event.attach(new SpellProcessingBehaviour(be)));
    }
    @Override public BehaviourType<?> getType(){return TYPE;}
    @Override public SmartBlockEntity blockEntity(){return blockEntity;}
    @Override public void initialize(){super.initialize();ensureProbe();}
    private TransportedItemStackHandlerBehaviour transport(){return blockEntity.getBehaviour(TransportedItemStackHandlerBehaviour.TYPE);}
    private boolean occupied(){
        if(blockEntity instanceof DepotBlockEntity)return true;
        var handler=transport();if(handler==null)return false;
        boolean[] occupied={false};handler.handleCenteredProcessingOnAllItems(.5f,item->{occupied[0]=true;return TransportedResult.doNothing();});return occupied[0];
    }
    @Override public void tick(){
        if(getWorld()==null || getWorld().isClientSide)return;
        if(getWorld().getGameTime()%4==0){if(occupied())ensureProbe();else release();}
    }
    private void ensureProbe(){
        if(!(getWorld() instanceof ServerLevel server) || blockEntity.isRemoved() || blockEntity.isChunkUnloaded() || probe()!=null || !occupied())return;
        var probe=DeviceContent.SPELL_PROBE.get().create(server);
        if(probe!=null){probe.bind(getPos());probeId=probe.getUUID();if(!server.addFreshEntity(probe))probeId=null;}
    }
    @Override public SpellDeviceProbe probe(){return getWorld() instanceof ServerLevel server && probeId!=null && server.getEntity(probeId) instanceof SpellDeviceProbe probe && !probe.isRemoved()?probe:null;}
    @Override public boolean ownsProbe(UUID id){return id.equals(probeId);}
    @Override public boolean hasSeenProjectile(UUID id){var tick=projectiles.get(id);return tick!=null && tick+1200>=getWorld().getGameTime();}
    private void release(){var probe=probe();if(probe!=null)probe.discard();probeId=null;}
    @Override public void unload(){release();super.unload();}
    @Override public void destroy(){release();super.destroy();}
    private boolean repeated(LinkedHashMap<UUID,Long> cache,UUID id){
        long now=getWorld().getGameTime();cache.entrySet().removeIf(e->e.getValue()+1200<now);
        if(cache.containsKey(id))return true;cache.put(id,now);
        while(cache.size()>256)cache.remove(cache.keySet().iterator().next());return false;
    }
    public Optional<dev.createspellbook.processing.SpellProcessing.Plan> matchingRecipe(net.minecraft.resources.ResourceLocation spell,net.minecraft.resources.ResourceLocation school,int level){
        var handler=transport();if(handler==null)return Optional.empty();
        var found=new ArrayList<dev.createspellbook.processing.SpellProcessing.Plan>(1);
        handler.handleCenteredProcessingOnAllItems(.5f,item->{
            if(found.isEmpty())matching(item.stack,spell,school,level).ifPresent(found::add);
            return TransportedResult.doNothing();
        });return found.stream().findFirst();
    }
    private Optional<dev.createspellbook.processing.SpellProcessing.Plan> matching(ItemStack input,net.minecraft.resources.ResourceLocation spell,net.minecraft.resources.ResourceLocation school,int level){
        return dev.createspellbook.processing.SpellProcessing.find(getWorld(),input,spell,school,level)
                .filter(plan->outputFitsPlan(input,plan));
    }
    private boolean outputFitsPlan(ItemStack input,dev.createspellbook.processing.SpellProcessing.Plan plan){
        if(plan.assembly()!=null && input.getCount()>1 && blockEntity instanceof DepotBlockEntity){
            var slots=getWorld().getCapability(Capabilities.ItemHandler.BLOCK,getPos(),net.minecraft.core.Direction.UP);
            if(slots==null)return false;
            // Progress components differ per step. Reserve an empty native output slot,
            // never assume that differently progressed intermediate items can merge.
            for(int i=1;i<slots.getSlots();i++)if(slots.getStackInSlot(i).isEmpty())return true;
            return false;
        }
        return outputFits(input,plan.inputCount(),plan.preview());
    }
    public boolean outputFits(ItemStack input,int consumed,ItemStack output){
        if(output.isEmpty() || output.getCount()>output.getMaxStackSize())return false;
        if(input.getCount()==consumed || !(blockEntity instanceof DepotBlockEntity))return true;
        var handler=getWorld().getCapability(Capabilities.ItemHandler.BLOCK,getPos(),net.minecraft.core.Direction.UP);
        if(handler==null)return false;
        int remaining=output.getCount();
        for(int i=1;i<handler.getSlots();i++){
            var stack=handler.getStackInSlot(i);
            if(stack.isEmpty() || ItemStack.isSameItemSameComponents(stack,output))remaining-=Math.max(0,Math.min(handler.getSlotLimit(i),output.getMaxStackSize())-stack.getCount());
        }return remaining<=0;
    }
    @Override public void acceptSpell(SpellInteraction hit){
        if(getWorld()==null || getWorld().isClientSide || blockEntity.isRemoved())return;
        if(hit.caster() instanceof dev.createspellbook.constructs.ConstructCaster caster && dev.createspellbook.constructs.ConstructSafety.sealedCaster(caster))return;
        if(repeated(contacts,hit.hitId()) || hit.projectileId()!=null && repeated(projectiles,hit.projectileId()))return;
        if(hit.castId()!=null && casts.containsKey(hit.castId()) && casts.get(hit.castId())+1200>=getWorld().getGameTime())return;
        var handler=transport();if(handler==null)return;
        boolean[] processed={false};
        handler.handleCenteredProcessingOnAllItems(.5f,item->{
            if(processed[0] || ((dev.createspellbook.processing.SpellTransportHistory)item).createSpellbook$seen(hit.castId()))return TransportedResult.doNothing();
            var match=matching(item.stack,hit.spellId(),hit.schoolId(),hit.level());if(match.isEmpty())return TransportedResult.doNothing();
            var recipe=match.get();var result=recipe.craft(getWorld(),item.stack);if(!outputFits(item.stack,recipe.inputCount(),result))return TransportedResult.doNothing();
            ((dev.createspellbook.processing.SpellTransportHistory)item).createSpellbook$mark(hit.castId());
            if(hit.castId()!=null)repeated(casts,hit.castId());
            var output=item.copy();output.stack=result;output.clearFanProcessingData();processed[0]=true;
            if(item.stack.getCount()==recipe.inputCount())return TransportedResult.convertTo(output);
            var remainder=item.copy();remainder.stack.shrink(recipe.inputCount());
            return TransportedResult.convertToAndLeaveHeld(List.of(output),remainder);
        });
    }
}
