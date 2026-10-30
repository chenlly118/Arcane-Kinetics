package dev.createspellbook.constructs;

import dev.createspellbook.Balance;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.*;

/** The two constructs share one real scroll slot and a deliberately audited adapter list. */
public final class ScrollTemplate extends ItemStackHandler {
    private final Runnable changed;
    public ScrollTemplate(Runnable changed) {super(1);this.changed=changed;}
    @Override public int getSlotLimit(int slot) {return 1;}
    @Override public boolean isItemValid(int slot,ItemStack stack) {return slot==0 && read(stack).isPresent();}
    @Override protected void onContentsChanged(int slot) {changed.run();}
    public static Optional<SpellData> read(ItemStack stack) {
        if(!stack.is(ItemRegistry.SCROLL.get()) || !ISpellContainer.isSpellContainer(stack))return Optional.empty();
        var container=ISpellContainer.get(stack);
        if(container.getActiveSpellCount()!=1)return Optional.empty();
        var data=container.getActiveSpells().getFirst();
        return data.getSpell()==SpellRegistry.none() || data.getLevel()<1?Optional.empty():Optional.of(new SpellData(data.getSpell(),data.getLevel()));
    }
    public static boolean supported(AbstractSpell spell,int level) {
        return MachineSpellCompatibility.supports(spell,level);
    }
    public static Set<String> supportedIds() {
        var result=new TreeSet<String>();for(var spell:SpellRegistry.REGISTRY)if(supported(spell,1))result.add(spell.getSpellId());return result;
    }
}
