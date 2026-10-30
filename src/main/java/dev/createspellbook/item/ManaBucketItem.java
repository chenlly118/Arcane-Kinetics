package dev.createspellbook.item;

import dev.createspellbook.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** Normal 1000 mB bucket capability; pouring releases one harmless-to-blocks burst. */
public final class ManaBucketItem extends BucketItem {
    public ManaBucketItem(Properties properties) { super(Content.MANA.get(),properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.NONE);
        if(hit.getType()!=HitResult.Type.MISS) return super.use(level,player,hand);
        var stack=player.getItemInHand(hand);
        Vec3 center=player.getEyePosition().add(player.getLookAngle().scale(2.5));
        if(!player.mayBuild() || !level.mayInteract(player,BlockPos.containing(center))) return InteractionResultHolder.fail(stack);
        if(level instanceof ServerLevel server) {
            burst(server,player,center);
            player.awardStat(Stats.ITEM_USED.get(this));
            player.getCooldowns().addCooldown(this,10);
        }
        return InteractionResultHolder.sidedSuccess(getEmptySuccessItem(stack,player),level.isClientSide);
    }
    @Override public boolean emptyContents(Player player,Level level,BlockPos pos,BlockHitResult hit) {
        if(player!=null && (!player.mayBuild() || !level.mayInteract(player,pos))) return false;
        if(level instanceof ServerLevel server) {
            burst(server,player,Vec3.atCenterOf(pos));
            if(player!=null) player.getCooldowns().addCooldown(this,10);
        }
        return true;
    }
    // NeoForge's player-use path calls the stack-aware overload directly; the
    // four-argument overload alone only covers legacy/dispenser callers.
    @Override public boolean emptyContents(Player player,Level level,BlockPos pos,BlockHitResult hit,ItemStack stack) {
        return emptyContents(player,level,pos,hit);
    }
    public static void burst(ServerLevel level,Player player,Vec3 center) {
        var calculator=new ExplosionDamageCalculator() {
            @Override public float getEntityDamageAmount(Explosion explosion,Entity entity) {
                return Explosion.getSeenPercent(center,entity)>0 ? 5f:0f;
            }
            @Override public float getKnockbackMultiplier(Entity entity) { return .35f; }
            @Override public boolean shouldBlockExplode(Explosion explosion,net.minecraft.world.level.BlockGetter world,BlockPos pos,
                                                        net.minecraft.world.level.block.state.BlockState state,float power) { return false; }
        };
        level.explode(player,null,calculator,center,1f,false,Level.ExplosionInteraction.NONE);
    }
}
