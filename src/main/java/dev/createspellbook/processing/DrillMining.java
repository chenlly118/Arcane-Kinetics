package dev.createspellbook.processing;

import dev.createspellbook.CreateSpellbook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class DrillMining {
    public static final TagKey<Block> MINEABLE = TagKey.create(Registries.BLOCK,CreateSpellbook.id("drill_mineable"));
    public static boolean mine(ServerPlayer player,BlockPos pos,int spellLevel) {
        var level=player.serverLevel();
        if(!level.hasChunkAt(pos) || !player.mayBuild() || !level.mayInteract(player,pos) || player.isSpectator()) return false;
        var state=level.getBlockState(pos);
        if(!state.is(MINEABLE) || state.hasBlockEntity() || state.getDestroySpeed(level,pos)<0) return false;
        var tool=new ItemStack(spellLevel>=7 ? Items.DIAMOND_PICKAXE : spellLevel>=4 ? Items.IRON_PICKAXE : Items.STONE_PICKAXE);
        if(state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) return false;
        // The normal real-player break path provides permissions, NeoForge events, tier-aware loot and XP.
        // This synchronous server-thread swap is always restored, including on cancellation or exceptions.
        var original=player.getMainHandItem();
        try { player.setItemInHand(InteractionHand.MAIN_HAND,tool); return player.gameMode.destroyBlock(pos); }
        finally { player.setItemInHand(InteractionHand.MAIN_HAND,original); }
    }
    private DrillMining() {}
}
