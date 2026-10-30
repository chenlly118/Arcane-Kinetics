package dev.createspellbook.constructs;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/** Direct, server-authoritative template exchange. No menu or settings packets. */
public final class ConstructInteraction {
    private static final TagKey<Item> WRENCH=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("c","tools/wrench"));
    public static boolean isWrench(ItemStack stack){return stack.is(WRENCH);}
    public static InteractionResult template(ConstructHost host,Player player,InteractionHand hand){
        if(!host.canConfigure(player)){
            if(!player.level().isClientSide)player.displayClientMessage(Component.translatable("message.create_spellbook.construct_owner"),true);
            return InteractionResult.SUCCESS;
        }
        var held=player.getItemInHand(hand);var selected=ScrollTemplate.read(held);
        if(!held.isEmpty() && selected.isEmpty())return InteractionResult.PASS;
        if(player.level().isClientSide)return InteractionResult.SUCCESS;
        if(selected.isPresent() && !ScrollTemplate.supported(selected.get().getSpell(),selected.get().getLevel())){
            player.displayClientMessage(Component.translatable("message.create_spellbook.construct_unsupported",selected.get().getSpell().getDisplayName(player)),true);
            return InteractionResult.SUCCESS;
        }
        var previous=host.scroll().getStackInSlot(0).copy();
        if(held.isEmpty() && previous.isEmpty()){
            player.displayClientMessage(Component.translatable("message.create_spellbook.construct_mana",host.storedMana(),host.manaCapacity()),true);
            return InteractionResult.SUCCESS;
        }
        if(host.caster()!=null)host.caster().cancelCast();
        host.scroll().setStackInSlot(0,held.isEmpty()?ItemStack.EMPTY:held.copyWithCount(1));
        if(!held.isEmpty() && !player.isCreative())held.shrink(1);
        if(!previous.isEmpty()){
            if(player.getItemInHand(hand).isEmpty())player.setItemInHand(hand,previous);
            else if(!player.getInventory().add(previous))player.drop(previous,false);
        }
        host.constructChanged();
        return InteractionResult.SUCCESS;
    }
    public static InteractionResult cycle(ArcaneCastingArmBlockEntity arm,Player player){
        if(!arm.canConfigure(player))return InteractionResult.SUCCESS;
        if(!player.level().isClientSide){
            arm.setMode(EmitterMode.fromId((arm.mode().ordinal()+1)%EmitterMode.values().length));
            player.displayClientMessage(Component.translatable("gui.create_spellbook.construct.mode."+arm.mode().name().toLowerCase(java.util.Locale.ROOT)),true);
        }
        return InteractionResult.SUCCESS;
    }
    private ConstructInteraction(){}
}
