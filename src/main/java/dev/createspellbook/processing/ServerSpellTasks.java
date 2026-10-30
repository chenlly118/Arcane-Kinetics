package dev.createspellbook.processing;

import dev.createspellbook.*;
import dev.createspellbook.spell.IndustrialSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;

/** Finite server-only work. Unloading a dimension discards its pending work. */
@EventBusSubscriber(modid=CreateSpellbook.ID)
public final class ServerSpellTasks {
    private interface Work { boolean tick(ServerLevel level); }
    private static final Map<ServerLevel,List<Work>> TASKS=new IdentityHashMap<>();
    private static final Map<ServerLevel,List<Work>> PENDING=new IdentityHashMap<>();
    private static void add(ServerLevel level,Work work) { PENDING.computeIfAbsent(level,k->new ArrayList<>()).add(work); }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level)) return;
        var pending=PENDING.remove(level);
        if(pending!=null) TASKS.computeIfAbsent(level,k->new ArrayList<>()).addAll(pending);
        var work=TASKS.get(level);
        if(work!=null) { work.removeIf(task->task.tick(level)); if(work.isEmpty()) TASKS.remove(level); }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { TASKS.remove(event.getLevel()); PENDING.remove(event.getLevel()); }

    /** Runs once after the given number of level ticks; discarded when the level unloads. */
    public static void runLater(ServerLevel level,int ticks,Runnable action) {
        if(ticks<=0) { action.run(); return; }
        add(level,new Countdown(ticks,action));
    }
    /** Repeats every interval while the predicate stays true; used by the master buff. */
    public static void every(ServerLevel level,int intervalTicks,java.util.function.BooleanSupplier keep,Runnable action) {
        add(level,new Repeated(Math.max(1,intervalTicks),keep,action));
    }
    private static final class Repeated implements Work {
        final int interval; int countdown; final java.util.function.BooleanSupplier keep; final Runnable action;
        Repeated(int interval,java.util.function.BooleanSupplier keep,Runnable action) { this.interval=interval;this.countdown=interval;this.keep=keep;this.action=action; }
        @Override public boolean tick(ServerLevel level) {
            if(!keep.getAsBoolean()) return true;
            if(--countdown<=0) { countdown=interval; action.run(); }
            return false;
        }
    }
    private static final class Countdown implements Work {
        int remaining; final Runnable action;
        Countdown(int ticks,Runnable action) { remaining=ticks;this.action=action; }
        @Override public boolean tick(ServerLevel level) {
            if(--remaining>0) return false;
            action.run(); return true;
        }
    }

    /** Delivers one authorized follow-up hit without globally clearing hurt frames. */
    public static boolean authorizedHit(LivingEntity target,float amount,net.minecraft.world.damagesource.DamageSource source) {
        int saved=target.invulnerableTime;
        target.invulnerableTime=0;
        try { return DamageSources.applyDamage(target,amount,source); }
        finally { target.invulnerableTime=Math.max(saved,target.invulnerableTime); }
    }

    public static void burn(LivingEntity caster,LivingEntity target,IndustrialSpell spell,float damage) {
        if(!(caster.level() instanceof ServerLevel level)) return;
        // Repeated hits refresh this caster/target pair rather than accumulating unbounded timers.
        for(var map:List.of(TASKS,PENDING)) {
            var list=map.get(level);
            if(list!=null) list.removeIf(w->w instanceof Burning b && b.caster.equals(caster.getUUID()) && b.target.equals(target.getUUID()));
        }
        add(level,new Burning(caster,target,spell,damage,level.getGameTime()+Balance.BLAZE_DOT_INTERVAL.get()));
    }
    private static final class Burning implements Work {
        final UUID caster,target; final IndustrialSpell spell; final float damage;
        final LivingEntity source,victim;
        long next; int pulses;
        Burning(LivingEntity source,LivingEntity victim,IndustrialSpell spell,float damage,long next) { this.source=source;this.victim=victim;this.caster=source.getUUID();this.target=victim.getUUID();this.spell=spell;this.damage=damage;this.next=next; }
        public boolean tick(ServerLevel level) {
            if(!source.isAlive() || source.isRemoved() || source.level()!=level || !victim.isAlive() || victim.isRemoved()
                    || victim.level()!=level || !level.hasChunkAt(victim.blockPosition())) return true;
            if(level.getGameTime()<next) return false;
            if(!DamageSources.isFriendlyFireBetween(source,victim)) {
                DamageSources.applyDamage(victim,damage,spell.getDamageSource(source));
                level.sendParticles(ParticleTypes.FLAME,victim.getX(),victim.getY()+victim.getBbHeight()*.5,victim.getZ(),6,.2,.3,.2,.01);
            }
            next+=Balance.BLAZE_DOT_INTERVAL.get(); return ++pulses>=3;
        }
    }

    public static void fell(ServerPlayer player,BlockPos origin) {
        var level=player.serverLevel();
        if(!canBreak(player,origin) || !level.getBlockState(origin).is(BlockTags.LOGS)) return;
        for(var map:List.of(TASKS,PENDING)) {
            var list=map.get(level);
            if(list!=null && list.stream().anyMatch(w->w instanceof Tree t && t.logs.contains(origin))) return;
        }
        var logs=new LinkedHashSet<BlockPos>(); var queue=new ArrayDeque<BlockPos>();
        logs.add(origin.immutable()); queue.add(origin.immutable()); boolean canopy=false;
        while(!queue.isEmpty()) {
            var pos=queue.removeFirst();
            for(var mutable:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1))) {
                if(!bounded(origin,mutable) || !level.hasChunkAt(mutable)) continue;
                var state=level.getBlockState(mutable);
                if(naturalLeaf(state)) canopy=true;
                if(logs.size()<Balance.TREE_MAX_LOGS.get() && state.is(BlockTags.LOGS) && logs.add(mutable.immutable())) queue.add(mutable.immutable());
            }
        }
        // A lone construction log can be cut, but a leafless timber building is not a tree.
        if(!canopy) { logs.clear(); logs.add(origin.immutable()); }
        var leaves=new LinkedHashSet<BlockPos>(); queue.addAll(logs);
        int maxLeaves=Math.min(8192,Math.max(64,logs.size()*64));
        while(!queue.isEmpty() && leaves.size()<maxLeaves) {
            var pos=queue.removeFirst();
            for(var direction:net.minecraft.core.Direction.values()) {
                var next=pos.relative(direction);
                if(!bounded(origin,next) || !level.hasChunkAt(next) || !naturalLeaf(level.getBlockState(next))) continue;
                if(leaves.add(next)) queue.add(next);
                if(leaves.size()>=maxLeaves) break;
            }
        }
        add(level,new Tree(player,logs,leaves));
    }
    private static boolean bounded(BlockPos a,BlockPos b) {
        return Math.abs(a.getX()-b.getX())<=Balance.TREE_RADIUS.get() && Math.abs(a.getZ()-b.getZ())<=Balance.TREE_RADIUS.get()
                && Math.abs(a.getY()-b.getY())<=Balance.TREE_HEIGHT.get();
    }
    private static boolean naturalLeaf(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(BlockTags.LEAVES) && state.hasProperty(BlockStateProperties.PERSISTENT) && !state.getValue(BlockStateProperties.PERSISTENT);
    }
    private static boolean canBreak(ServerPlayer player,BlockPos pos) {
        var level=player.serverLevel();
        return player.isAlive() && player.mayBuild() && !player.isSpectator() && level.hasChunkAt(pos) && level.mayInteract(player,pos)
                && !level.getBlockState(pos).hasBlockEntity() && level.getBlockState(pos).getDestroySpeed(level,pos)>=0;
    }
    private static boolean chop(ServerPlayer player,BlockPos pos) {
        if(!canBreak(player,pos)) return false;
        var held=player.getMainHandItem();
        try { player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE)); return player.gameMode.destroyBlock(pos); }
        finally { player.setItemInHand(InteractionHand.MAIN_HAND,held); }
    }
    private static final class Tree implements Work {
        final ServerPlayer player; final Set<BlockPos> logs; final ArrayDeque<BlockPos> remaining,leaves;
        long cleanAfter=-1;
        Tree(ServerPlayer player,Set<BlockPos> logs,Set<BlockPos> leaves) { this.player=player;this.logs=logs;remaining=new ArrayDeque<>(logs);this.leaves=new ArrayDeque<>(leaves); }
        public boolean tick(ServerLevel level) {
            if(!player.isAlive() || player.isRemoved() || player.level()!=level || player.isSpectator()) return true;
            int quota=Balance.TREE_LOGS_PER_TICK.get();
            while(quota-->0 && !remaining.isEmpty()) {
                var pos=remaining.removeFirst();
                if(level.hasChunkAt(pos) && level.getBlockState(pos).is(BlockTags.LOGS)) chop(player,pos);
            }
            if(!remaining.isEmpty()) return false;
            if(cleanAfter<0) cleanAfter=level.getGameTime()+10;
            if(level.getGameTime()<cleanAfter) return false;
            quota=Balance.TREE_LOGS_PER_TICK.get()*2;
            while(quota-->0 && !leaves.isEmpty()) {
                var pos=leaves.removeFirst();
                if(!level.hasChunkAt(pos)) continue;
                var state=level.getBlockState(pos);
                // Distance is maintained by vanilla; foliage supported by another tree stays intact.
                if(naturalLeaf(state) && state.hasProperty(BlockStateProperties.DISTANCE) && state.getValue(BlockStateProperties.DISTANCE)==7) chop(player,pos);
            }
            return leaves.isEmpty();
        }
    }
    private ServerSpellTasks() {}
}
