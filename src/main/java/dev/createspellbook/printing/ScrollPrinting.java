package dev.createspellbook.printing;

import com.simibubi.create.content.logistics.filter.*;
import dev.createspellbook.constructs.ScrollTemplate;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.registries.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

/** Native scribing rules, shared by the machine and its read-only preview. */
public final class ScrollPrinting {
    public record Selection(ItemStack output,int nextIndex) {}
    public static Optional<SpellRarity> inkRarity(FluidStack stack) {
        if(stack.isEmpty())return Optional.empty();
        if(stack.is(FluidRegistry.COMMON_INK.get()))return Optional.of(SpellRarity.COMMON);
        if(stack.is(FluidRegistry.UNCOMMON_INK.get()))return Optional.of(SpellRarity.UNCOMMON);
        if(stack.is(FluidRegistry.RARE_INK.get()))return Optional.of(SpellRarity.RARE);
        if(stack.is(FluidRegistry.EPIC_INK.get()))return Optional.of(SpellRarity.EPIC);
        if(stack.is(FluidRegistry.LEGENDARY_INK.get()))return Optional.of(SpellRarity.LEGENDARY);
        return Optional.empty();
    }
    /** Only an explicit allow-list is a manufacturing template. Never invent blacklist outputs. */
    public static List<SpellData> templates(ItemStack stack) {
        var scroll=ScrollTemplate.read(stack);
        if(scroll.isPresent())return List.of(scroll.get());
        if(!(stack.getItem() instanceof ListFilterItem))return List.of();
        // Create normalizes filters by mutating their component patch. Inspect a
        // copy so the real reusable template (including cosmetic data) stays exact.
        var wrapper=FilterItemStack.of(stack.copy());
        if(!(wrapper instanceof FilterItemStack.ListFilterItemStack list) || list.isBlacklist)return List.of();
        var result=new ArrayList<SpellData>();var seen=new HashSet<String>();
        for(var entry:list.containedItems) {
            var data=ScrollTemplate.read(entry.item());
            if(data.isPresent() && seen.add(data.get().getSpell().getSpellId()))result.add(data.get());
            if(result.size()>=18)break;
        }
        return List.copyOf(result);
    }
    public static boolean validTemplate(ItemStack stack){return !templates(stack).isEmpty();}
    public static boolean validFocus(ItemStack stack){return !stack.isEmpty() && !SchoolRegistry.getSchoolsFromFocus(stack).isEmpty();}
    public static Optional<Selection> select(ItemStack template,ItemStack focus,FluidStack ink,int cursor) {
        var rarity=inkRarity(ink);if(rarity.isEmpty() || !validFocus(focus))return Optional.empty();
        var choices=templates(template);if(choices.isEmpty())return Optional.empty();
        var schools=SchoolRegistry.getSchoolsFromFocus(focus);
        for(int offset=0;offset<choices.size();offset++) {
            int index=Math.floorMod(cursor+offset,choices.size());var spell=choices.get(index).getSpell();
            if(!spell.isEnabled() || !spell.allowCrafting() || !schools.contains(spell.getSchoolType()))continue;
            if(rarity.get().getValue()>spell.getMaxRarity())continue;
            int level=spell.getMinLevelForRarity(rarity.get());
            // getMinLevelForRarity also returns numbers for unsupported ink ranges.
            if(level<1 || level>spell.getMaxLevel() || spell.getRarity(level)!=rarity.get())continue;
            var output=new ItemStack(ItemRegistry.SCROLL.get());ISpellContainer.createScrollContainer(spell,level,output);
            return Optional.of(new Selection(output,(index+1)%choices.size()));
        }
        return Optional.empty();
    }
    private ScrollPrinting(){}
}
