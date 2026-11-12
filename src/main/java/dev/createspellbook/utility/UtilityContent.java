package dev.createspellbook.utility;

import dev.createspellbook.Content;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Additions use the existing block/item/entity deferred registers. */
public final class UtilityContent {
    public static final DeferredHolder<Block,FireworkReactorBlock> FIREWORK=Content.BLOCKS.register("firework_reactor",()->new FireworkReactorBlock(properties()));
    public static final DeferredHolder<Block,PortalFunnelBlock> PORTAL=Content.BLOCKS.register("portal_funnel",()->new PortalFunnelBlock(properties()));
    public static final DeferredHolder<Block,PortalBeltFunnelBlock> PORTAL_BELT=Content.BLOCKS.register("portal_belt_funnel",()->new PortalBeltFunnelBlock(properties()));
    public static final DeferredHolder<Item,BlockItem> FIREWORK_ITEM=item("firework_reactor",FIREWORK);
    public static final DeferredHolder<Item,BlockItem> PORTAL_ITEM=Content.ITEMS.register("portal_funnel",()->new PortalFunnelItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<FireworkReactorBlockEntity>> FIREWORK_BE=Content.BLOCK_ENTITIES.register("firework_reactor",()->BlockEntityType.Builder.of(FireworkReactorBlockEntity::new,FIREWORK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<PortalFunnelBlockEntity>> PORTAL_BE=Content.BLOCK_ENTITIES.register("portal_funnel",()->BlockEntityType.Builder.of(PortalFunnelBlockEntity::new,PORTAL.get(),PORTAL_BELT.get()).build(null));
    public static final DeferredHolder<com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType,PortalFunnelArmType> PORTAL_ARM=dev.createspellbook.devices.DeviceContent.ARM_TYPES.register("portal_funnel",PortalFunnelArmType::new);
    private static BlockBehaviour.Properties properties(){return BlockBehaviour.Properties.of().strength(3,12).requiresCorrectToolForDrops().noOcclusion();}
    private static DeferredHolder<Item,BlockItem> item(String id,DeferredHolder<Block,? extends Block> block){
        return Content.ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties().stacksTo(1)));
    }
    public static void init(IEventBus bus){bus.addListener(PortalChunkTickets::register);}
    private UtilityContent(){}
}
