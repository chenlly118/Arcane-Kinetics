package dev.createspellbook.devices;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public abstract class SpellDeviceBlockEntity extends SmartBlockEntity implements SpellTarget {
    @Override public SmartBlockEntity blockEntity(){return this;}
    private UUID probeId;
    private boolean syncQueued;
    private final LinkedHashMap<UUID,Long> contacts=new LinkedHashMap<>();
    private final LinkedHashMap<UUID,Long> projectiles=new LinkedHashMap<>();
    protected SpellDeviceBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state) {super(type,pos,state);}
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
    @Override public void initialize() {super.initialize();ensureProbe();}
    @Override public void tick() {
        super.tick();
        if(level==null || level.isClientSide)return;
        ensureProbe();serverDeviceTick();
        if(syncQueued) {syncQueued=false;sendData();}
    }
    protected void serverDeviceTick() {}
    public void changed() {setChanged();if(level!=null && !level.isClientSide)syncQueued=true;}
    public SpellDeviceProbe probe() {
        if(level instanceof ServerLevel server && probeId!=null && server.getEntity(probeId) instanceof SpellDeviceProbe probe && !probe.isRemoved())return probe;
        return null;
    }
    private void ensureProbe() {
        if(!(level instanceof ServerLevel server) || isRemoved() || isChunkUnloaded() || probe()!=null)return;
        var probe=DeviceContent.SPELL_PROBE.get().create(server);
        if(probe!=null) {probe.bind(worldPosition);probeId=probe.getUUID();server.addFreshEntity(probe);}
    }
    public boolean ownsProbe(UUID id) {return id.equals(probeId);}
    public boolean hasSeenProjectile(UUID id) {var tick=projectiles.get(id);return tick!=null && level!=null && tick+1200>=level.getGameTime();}
    private void releaseProbe() {var probe=probe();if(probe!=null)probe.discard();probeId=null;}
    @Override public void onChunkUnloaded() {releaseProbe();super.onChunkUnloaded();}
    @Override public void invalidate() {releaseProbe();super.invalidate();}
    @Override public final void acceptSpell(SpellInteraction hit) {
        if(level==null || level.isClientSide || isRemoved())return;
        // Self-charged attacks cannot export the device's sealed mana through processing.
        if(hit.caster() instanceof dev.createspellbook.constructs.ConstructCaster caster && dev.createspellbook.constructs.ConstructSafety.sealedCaster(caster))return;
        long now=level.getGameTime();
        if(repeated(contacts,hit.hitId(),now))return;
        if(hit.projectileId()!=null && repeated(projectiles,hit.projectileId(),now))return;
        handleSpell(hit);
    }
    private static boolean repeated(LinkedHashMap<UUID,Long> cache,UUID id,long now) {
        cache.entrySet().removeIf(e->e.getValue()+1200<now);
        if(cache.containsKey(id))return true;
        cache.put(id,now);
        while(cache.size()>256)cache.remove(cache.keySet().iterator().next());
        return false;
    }
    protected abstract void handleSpell(SpellInteraction hit);
    public abstract InteractionResult playerUse(Player player,InteractionHand hand);
}
