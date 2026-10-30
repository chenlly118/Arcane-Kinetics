package dev.createspellbook;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

@EventBusSubscriber(modid=CreateSpellbook.ID)
public final class EquipmentBonuses {
    private record Bonus(EquipmentSlotGroup slot,double power) {}
    private static Bonus bonus(ResourceLocation item) {
        // Native client search trees can finish tooltips after a world's SERVER
        // config has unloaded. There is no authoritative equipment bonus then.
        if(!Balance.SPEC.isLoaded())return null;
        for(String entry:Balance.EQUIPMENT.get()) {
            String[] fields=entry.split("\\|");
            if(fields.length!=3 || !fields[0].equals(item.toString())) continue;
            try {
                double value=Double.parseDouble(fields[2]);
                if(!Double.isFinite(value) || value<0 || value>5) continue;
                EquipmentSlot slot=EquipmentSlot.byName(fields[1]);
                return new Bonus(EquipmentSlotGroup.bySlot(slot),value);
            } catch(IllegalArgumentException ignored) { /* Invalid optional configuration cannot corrupt item attributes. */ }
        }
        return null;
    }
    private static ResourceLocation modifier(ResourceLocation item) { return CreateSpellbook.id("equipment/"+item.getNamespace()+"/"+item.getPath()); }
    @SubscribeEvent public static void item(ItemAttributeModifierEvent event) {
        var item=BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if(event.getItemStack().is(Content.ENCHANTED_WRENCH.get())) {
            if(Balance.SPEC.isLoaded())event.addModifier(Content.MECHANICAL_POWER,new AttributeModifier(CreateSpellbook.id("wrench_power"),Balance.WRENCH_POWER.get(),AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
            // Player base is 1 damage / 4 speed: +5 and -2.4 give the requested final 6 / 1.6.
            event.addModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(CreateSpellbook.id("wrench_attack_damage"),5,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
            event.addModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new AttributeModifier(CreateSpellbook.id("wrench_attack_speed"),-2.4,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
            return;
        }
        var bonus=bonus(item);
        if(bonus!=null) event.addModifier(Content.MECHANICAL_POWER,new AttributeModifier(modifier(item),bonus.power,AttributeModifier.Operation.ADD_VALUE),bonus.slot);
    }
    @SubscribeEvent public static void curio(CurioAttributeModifierEvent event) {
        var item=BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if(!item.equals(ResourceLocation.fromNamespaceAndPath("create","goggles"))) return;
        var bonus=bonus(item);
        if(bonus!=null) event.addModifier(Content.MECHANICAL_POWER,new AttributeModifier(modifier(item),bonus.power,AttributeModifier.Operation.ADD_VALUE));
    }
}
