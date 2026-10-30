package dev.createspellbook.reactor;

import dev.createspellbook.Content;
import dev.createspellbook.CreateSpellbook;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.*;

/** Reuse the mod's deferred registers. No pack-local materials or recipes. */
public final class ReactorContent {
    public static final DeferredHolder<Item,BlackHoleStabilizerItem> STABILIZER=Content.ITEMS.register("black_hole_stabilizer",BlackHoleStabilizerItem::new);
    public static final DeferredHolder<Item,CollapsedMatterItem> MATTER=Content.ITEMS.register("collapsed_matter",CollapsedMatterItem::new);
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>,net.minecraft.world.entity.EntityType<net.minecraft.world.entity.item.ItemEntity>> MATTER_ENTITY=Content.ENTITIES.register("collapsed_matter_item",()->net.minecraft.world.entity.EntityType.Builder.<net.minecraft.world.entity.item.ItemEntity>of(CollapsedMatterEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(6).updateInterval(20).fireImmune().build("collapsed_matter_item"));
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,CreateSpellbook.ID);
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<ReactorMenu>> CONTROL_MENU=MENUS.register("reactor_control",()->new net.minecraft.world.inventory.MenuType<>(ReactorMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    private static BlockBehaviour.Properties properties(){return BlockBehaviour.Properties.of().strength(4.5f,30).requiresCorrectToolForDrops();}
    public static final DeferredHolder<Block,Block> CASING=Content.BLOCKS.register(ReactorRole.CASING.id,()->new Block(properties()));
    public static final DeferredHolder<Block,ReactorGlassBlock> GLASS=Content.BLOCKS.register(ReactorRole.GLASS.id,()->new ReactorGlassBlock(properties().noOcclusion().isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false)));
    public static final DeferredHolder<Block,ReactorPartBlock> OUTPUT=part(ReactorRole.OUTPUT),MANA=part(ReactorRole.MANA),PORT=part(ReactorRole.STABILIZER),
            REACTION_CORE=part(ReactorRole.REACTION_CORE),ENERGY_CORE=part(ReactorRole.ENERGY_CORE);
    public static final DeferredHolder<Block,ReactorControllerBlock> CONTROLLER=Content.BLOCKS.register(ReactorRole.CONTROLLER.id,()->new ReactorControllerBlock(properties()));
    public static final DeferredHolder<Block,ReactorShaftBlock> SHAFT=Content.BLOCKS.register(ReactorRole.SHAFT.id,()->new ReactorShaftBlock(properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ReactorPartBlockEntity>> OUTPUT_BE=partType(ReactorRole.OUTPUT,OUTPUT),
            MANA_BE=partType(ReactorRole.MANA,MANA),PORT_BE=partType(ReactorRole.STABILIZER,PORT),
            REACTION_BE=partType(ReactorRole.REACTION_CORE,REACTION_CORE),ENERGY_BE=partType(ReactorRole.ENERGY_CORE,ENERGY_CORE);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ReactorControllerBlockEntity>> CONTROLLER_BE=Content.BLOCK_ENTITIES.register(ReactorRole.CONTROLLER.id,()->BlockEntityType.Builder.of(ReactorControllerBlockEntity::new,CONTROLLER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ReactorShaftBlockEntity>> SHAFT_BE=Content.BLOCK_ENTITIES.register(ReactorRole.SHAFT.id,()->BlockEntityType.Builder.of(ReactorShaftBlockEntity::new,SHAFT.get()).build(null));
    public static final List<DeferredHolder<Item,BlockItem>> ITEMS=Arrays.stream(ReactorRole.values()).map(role->Content.ITEMS.<BlockItem>register(role.id,()->new BlockItem(block(role),new Item.Properties()){
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,lines,flag);lines.add(Component.translatable("tooltip.create_spellbook."+role.id).withStyle(net.minecraft.ChatFormatting.GRAY));}
    })).toList();
    private static DeferredHolder<Block,ReactorPartBlock> part(ReactorRole role){return Content.BLOCKS.register(role.id,()->new ReactorPartBlock(role,properties()));}
    private static DeferredHolder<BlockEntityType<?>,BlockEntityType<ReactorPartBlockEntity>> partType(ReactorRole role,DeferredHolder<Block,ReactorPartBlock> block){return Content.BLOCK_ENTITIES.register(role.id,()->BlockEntityType.Builder.of((pos,state)->new ReactorPartBlockEntity(role,pos,state),block.get()).build(null));}
    public static BlockEntityType<ReactorPartBlockEntity> partType(ReactorRole role){return switch(role){case OUTPUT->OUTPUT_BE.get();case MANA->MANA_BE.get();case STABILIZER->PORT_BE.get();case REACTION_CORE->REACTION_BE.get();case ENERGY_CORE->ENERGY_BE.get();default->throw new IllegalArgumentException(role.name());};}
    public static Block block(ReactorRole role){return switch(role){case CASING->CASING.get();case GLASS->GLASS.get();case OUTPUT->OUTPUT.get();case MANA->MANA.get();case STABILIZER->PORT.get();case CONTROLLER->CONTROLLER.get();case REACTION_CORE->REACTION_CORE.get();case ENERGY_CORE->ENERGY_CORE.get();case SHAFT->SHAFT.get();};}
    public static ReactorRole role(Block block){for(var role:ReactorRole.values())if(block(role)==block)return role;return null;}
    public static void init(net.neoforged.bus.api.IEventBus bus){MENUS.register(bus);}
    private ReactorContent(){}
}
