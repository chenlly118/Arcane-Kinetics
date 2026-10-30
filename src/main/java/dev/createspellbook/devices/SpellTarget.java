package dev.createspellbook.devices;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.UUID;

/** Shared receiving surface for devices and native Create transport behaviours. */
public interface SpellTarget extends SpellReceiver {
    SmartBlockEntity blockEntity();
    SpellDeviceProbe probe();
    boolean ownsProbe(UUID id);
    boolean hasSeenProjectile(UUID id);
    default BlockPos getBlockPos(){return blockEntity().getBlockPos();}
    default Level getLevel(){return blockEntity().getLevel();}
    default boolean isRemoved(){return blockEntity().isRemoved();}
    default InteractionResult playerUse(Player player,InteractionHand hand){return InteractionResult.PASS;}
    static SpellTarget at(Level level,BlockPos pos){
        if(level==null || !level.hasChunkAt(pos))return null;
        var be=level.getBlockEntity(pos);
        if(be instanceof SpellTarget target)return target;
        return com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(be,SpellProcessingBehaviour.TYPE);
    }
}
