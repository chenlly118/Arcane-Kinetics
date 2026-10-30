package dev.createspellbook.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.createspellbook.CreateSpellbook;
import dev.createspellbook.constructs.ScrollTemplate;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.*;
import net.neoforged.neoforge.registries.*;
import java.util.stream.Stream;

/** Native NeoForge ingredient; tests the scroll's actual spell data, including overlevels. */
public record SpellScrollIngredient(ResourceLocation spell,int minimumLevel) implements ICustomIngredient {
    public static final MapCodec<SpellScrollIngredient> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
            ResourceLocation.CODEC.fieldOf("spell").forGetter(SpellScrollIngredient::spell),
            Codec.intRange(1,Integer.MAX_VALUE).fieldOf("minimum_level").forGetter(SpellScrollIngredient::minimumLevel)
    ).apply(instance,SpellScrollIngredient::new));
    private static final DeferredRegister<IngredientType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES,CreateSpellbook.ID);
    public static final DeferredHolder<IngredientType<?>,IngredientType<SpellScrollIngredient>> TYPE=TYPES.register("spell_scroll",()->new IngredientType<>(CODEC));
    public static void register(IEventBus bus){TYPES.register(bus);}
    @Override public boolean test(ItemStack stack){return ScrollTemplate.read(stack).filter(data->data.getSpell().getSpellId().equals(spell.toString()) && data.getLevel()>=minimumLevel).isPresent();}
    @Override public Stream<ItemStack> getItems(){
        var resolved=SpellRegistry.getSpell(spell.toString());if(resolved==SpellRegistry.none())return Stream.empty();
        var stack=new ItemStack(ItemRegistry.SCROLL.get());ISpellContainer.createScrollContainer(resolved,minimumLevel,stack);return Stream.of(stack);
    }
    @Override public boolean isSimple(){return false;}
    @Override public IngredientType<?> getType(){return TYPE.get();}
}
