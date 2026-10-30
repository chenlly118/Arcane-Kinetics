package dev.createspellbook.compat;

import dev.createspellbook.devices.SpellRequirement;
import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Genuine native scrolls at the lowest qualifying level, one example per accepted spell. */
public final class SpellRecipeDisplays {
    public static List<ItemStack> scrolls(SpellRequirement requirement){
        var result=new ArrayList<ItemStack>();
        for(var spell:SpellRegistry.REGISTRY.stream().sorted(Comparator.comparing(s->s.getSpellId())).toList()){
            if(!spell.isEnabled() || spell==SpellRegistry.none())continue;
            int level=Math.max(1,requirement.minLevel());
            if(requirement.requireMaxLevel())level=Math.max(level,spell.getMaxLevel());
            if(requirement.minRarity().isPresent())level=Math.max(level,spell.getMinLevelForRarity(requirement.minRarity().get()));
            if(requirement.accepts(spell.getSpellResource(),spell.getSchoolType().getId(),level)){
                var scroll=new ItemStack(ItemRegistry.SCROLL.get());ISpellContainer.createScrollContainer(spell,level,scroll);result.add(scroll);
            }
        }return result;
    }
    public static Component title(SpellRequirement requirement){
        if(requirement.spell().isPresent())return SpellRegistry.getSpell(requirement.spell().get().toString()).getDisplayName(Minecraft.getInstance().player);
        var school=requirement.school().map(SchoolRegistry.REGISTRY::get).orElse(null);
        return school==null?Component.empty():school.getDisplayName();
    }
    public static Component condition(SpellRequirement r){
        var text=Component.translatable(r.requireMaxLevel()?"jei.create_spellbook.maximum_level":"jei.create_spellbook.required_level",r.minLevel());
        r.minRarity().ifPresent(rarity->text.append(" · ").append(Component.translatable("jei.create_spellbook.minimum_rarity",rarity.getDisplayName())));return text;
    }
    private SpellRecipeDisplays(){}
}
