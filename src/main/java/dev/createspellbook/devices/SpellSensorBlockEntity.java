package dev.createspellbook.devices;

import dev.createspellbook.Balance;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class SpellSensorBlockEntity extends SpellDeviceBlockEntity {
    private ResourceLocation school;
    private long expiresAt,lastHitTick=Long.MIN_VALUE;
    public SpellSensorBlockEntity(BlockPos pos,BlockState state) {super(DeviceContent.SENSOR_BE.get(),pos,state);}
    public ResourceLocation selectedSchool() {return school;}
    public void selectSchool(ResourceLocation id) {school=id;changed();}
    public int signal() {return level!=null && level.getGameTime()<expiresAt?Balance.SENSOR_STRENGTH.get():0;}
    public long expiry() {return expiresAt;}
    @Override protected void handleSpell(SpellInteraction hit) {
        if(school!=null && !school.equals(hit.schoolId()))return;
        long now=level.getGameTime();if(lastHitTick==now)return;lastHitTick=now;
        expiresAt=now+Balance.SENSOR_TICKS.get();updatePowered(true);changed();
    }
    @Override protected void serverDeviceTick() {if(expiresAt!=0 && level.getGameTime()>=expiresAt){expiresAt=0;updatePowered(false);changed();}}
    @Override public void initialize() {super.initialize();if(level!=null && !level.isClientSide)updatePowered(signal()>0);}
    private void updatePowered(boolean powered) {
        var state=getBlockState();if(state.getValue(SpellDeviceBlock.POWERED)==powered)return;
        level.setBlock(worldPosition,state.setValue(SpellDeviceBlock.POWERED,powered),Block.UPDATE_ALL);
    }
    @Override public InteractionResult playerUse(Player player,InteractionHand hand) {
        if(!player.getItemInHand(hand).isEmpty())return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(player.isShiftKeyDown()) {
            var schools=new ArrayList<>(SchoolRegistry.REGISTRY.keySet());schools.sort(Comparator.comparing(ResourceLocation::toString));
            int next=school==null?0:schools.indexOf(school)+1;school=next>=schools.size()?null:schools.get(next);changed();
        }
        var selected=school==null?null:SchoolRegistry.REGISTRY.get(school);
        var text=school==null?Component.translatable("message.create_spellbook.all_schools"):selected==null?Component.literal(school.toString()):selected.getDisplayName();
        player.displayClientMessage(Component.translatable("message.create_spellbook.sensor_filter",text),true);return InteractionResult.SUCCESS;
    }
    @Override protected void write(CompoundTag tag,HolderLookup.Provider lookup,boolean packet) {
        super.write(tag,lookup,packet);tag.putLong("ExpiresAt",expiresAt);if(school!=null)tag.putString("School",school.toString());
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet) {
        super.read(tag,lookup,packet);expiresAt=tag.getLong("ExpiresAt");school=tag.contains("School")?ResourceLocation.tryParse(tag.getString("School")):null;
    }
}
