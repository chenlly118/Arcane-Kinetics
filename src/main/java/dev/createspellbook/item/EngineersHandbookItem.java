package dev.createspellbook.item;

import dev.createspellbook.Content;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** Native inscription, spell selection and Curios handling belong to SpellBook. */
public final class EngineersHandbookItem extends SpellBook {
    public EngineersHandbookItem() {
        super(10, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
        withSpellbookAttributes(
                new AttributeContainer(AttributeRegistry.MAX_MANA, 200, Operation.ADD_VALUE),
                new AttributeContainer(Content.MECHANICAL_POWER, .10, Operation.ADD_VALUE),
                new AttributeContainer(AttributeRegistry.COOLDOWN_REDUCTION, .05, Operation.ADD_VALUE));
    }
}
