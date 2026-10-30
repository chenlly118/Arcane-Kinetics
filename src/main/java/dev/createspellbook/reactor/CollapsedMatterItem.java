package dev.createspellbook.reactor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;

/** Waste has an ordinary pickup/merge path, but no ordinary destruction path. */
@EventBusSubscriber(modid=dev.createspellbook.CreateSpellbook.ID)
public final class CollapsedMatterItem extends Item {
    public CollapsedMatterItem(){super(new Properties().rarity(Rarity.RARE).fireResistant());}
    @Override public boolean hasCustomEntity(ItemStack stack){return true;}
    @Override public Entity createEntity(Level level,Entity original,ItemStack stack){
        if(original instanceof CollapsedMatterEntity || level.isClientSide)return null;
        return protectedDrop(level,original,stack);
    }
    private static CollapsedMatterEntity protectedDrop(Level level,Entity original,ItemStack stack){
        var replacement=new CollapsedMatterEntity(ReactorContent.MATTER_ENTITY.get(),level);
        // Preserve owner, pickup delay, motion, components and persisted item data.
        replacement.load(original.saveWithoutId(new CompoundTag()));replacement.setItem(stack.copy());replacement.protectLifetime();return replacement;
    }
    public static boolean containsWaste(ItemStack stack,Level level){
        if(stack.is(ReactorContent.MATTER.get()))return true;
        if(!stack.has(DataComponents.BLOCK_ENTITY_DATA) && !stack.has(DataComponents.CONTAINER) && !stack.has(DataComponents.BUNDLE_CONTENTS))return false;
        return containsWaste(stack.save(level.registryAccess()),0,new int[]{2048});
    }
    private static boolean containsWaste(Tag tag,int depth,int[] budget){
        // Fail closed for oversized nested containers. No recursive inventory ticking.
        if(depth>24 || --budget[0]<0)return true;
        if(tag instanceof CompoundTag compound){
            if(compound.getString("id").equals("create_spellbook:collapsed_matter"))return true;
            for(var key:compound.getAllKeys())if(containsWaste(compound.get(key),depth+1,budget))return true;
        }else if(tag instanceof ListTag list)for(var child:list)if(containsWaste(child,depth+1,budget))return true;
        return false;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void protectContainers(EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ItemEntity item) || item instanceof CollapsedMatterEntity || !containsWaste(item.getItem(),level))return;
        var replacement=protectedDrop(level,item,item.getItem());item.discard();event.setCanceled(true);
        // Same deferred replacement lifecycle used by NeoForge's custom item API.
        level.getServer().tell(new TickTask(0,()->level.addFreshEntity(replacement)));
    }
    @Override public boolean onDroppedByPlayer(ItemStack stack,Player player){
        if(!player.level().isClientSide)player.displayClientMessage(Component.translatable("message.create_spellbook.waste_drop").withStyle(net.minecraft.ChatFormatting.GOLD),true);return true;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,lines,flag);lines.add(Component.translatable("tooltip.create_spellbook.collapsed_matter").withStyle(net.minecraft.ChatFormatting.GOLD));}
}
