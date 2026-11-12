package dev.createspellbook.client.ponder;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import dev.createspellbook.utility.*;
import net.createmod.ponder.api.scene.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Three short stages per machine, with moving items and the normal machine renderers. */
public final class UtilityPonderScenes {
    private UtilityPonderScenes(){}
    private static void start(SceneBuilder scene,String id,String title,int size){scene.title(id,title);scene.configureBasePlate(0,0,size);scene.scaleSceneView(size==9?1:1.2f);scene.showBasePlate();scene.idle(22);}
    private static void text(SceneBuilder scene,String key,BlockPos pos){PonderFx.text(scene,"brief_"+key,Vec3.atCenterOf(pos),80);}
    public static void belts(SceneBuilder scene,SceneBuildingUtil util){
        start(scene,"utility_portal_belts","传送门漏斗：配对、输送与过滤",9);var create=new CreateSceneBuilder(scene);var a=new BlockPos(2,2,3);var b=new BlockPos(6,2,5);
        scene.world().showSection(util.select().fromTo(0,1,3,8,2,5),Direction.UP);create.world().setKineticSpeed(util.select().fromTo(0,1,3,8,1,5),32);scene.idle(25);
        PonderFx.rightClick(scene,Vec3.atCenterOf(a),new ItemStack(Items.ENDER_PEARL),24);PonderFx.rightClick(scene,Vec3.atCenterOf(b),new ItemStack(Items.ENDER_PEARL),24);text(scene,"portal_pair",a);
        var iron = new ItemStack(Items.IRON_INGOT, 8);
        create.world().createItemOnBelt(new BlockPos(5, 1, 3), Direction.UP, iron.copy());
        scene.idle(45);
        create.world().removeItemsFromBelt(a.below());
        create.world().flapFunnel(a, true);
        scene.idle(8);

        var delivered = create.world().createItemOnBelt(b.below(), Direction.UP, iron.copy());
        create.world().flapFunnel(b, false);
        scene.idle(48);
        create.world().stallBeltItem(delivered, true);
        text(scene,"portal_flow",b);scene.idle(20);
        PonderFx.rightClick(scene,Vec3.atCenterOf(a),AllItems.WRENCH.asStack(),24);scene.world().modifyBlock(a,state->state.setValue(BeltFunnelBlock.SHAPE,BeltFunnelBlock.Shape.PUSHING),false);create.world().setFilterData(util.select().position(a),PortalFunnelBlockEntity.class,new ItemStack(Items.IRON_INGOT));text(scene,"portal_filter",a);scene.idle(100);scene.markAsFinished();
    }

    public static void fireworks(SceneBuilder scene,SceneBuildingUtil util){
        start(scene,"utility_firework","烟花反应装置：配置与发射",7);var p=new BlockPos(3,1,3);scene.world().showSection(util.select().fromTo(1,1,3,3,1,3),Direction.UP);scene.world().showSection(util.select().position(3,1,1),Direction.UP);scene.idle(25);
        var rocket=new ItemStack(Items.FIREWORK_ROCKET);PonderFx.rightClick(scene,Vec3.atCenterOf(p),rocket,24);scene.world().modifyBlockEntity(p,FireworkReactorBlockEntity.class,be->{be.configure(rocket);be.mana.fill(new net.neoforged.neoforge.fluids.FluidStack(dev.createspellbook.Content.MANA.get(),500),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);});text(scene,"firework_config",p);
        for(int i=0;i<3;i++){
            scene.world().createEntity(level->{var entity=new FireworkRocketEntity(level,3.5,2.5,3.5,rocket);entity.noPhysics=true;return entity;});
            // Rockets expire on the server in a real world. A PonderLevel is
            // client-only, so explicitly bound their flight and lifetime here.
            PonderFx.animate(scene,20,(current,frame)->current.forEachWorldEntity(FireworkRocketEntity.class,entity->{
                entity.setPos(3.5,2.5+frame*.15,3.5);entity.setDeltaMovement(Vec3.ZERO);if(frame==19)entity.discard();
            }));
            scene.idle(20);
        }text(scene,"firework_work",p);
        scene.world().modifyBlock(new BlockPos(3,1,1),state->state.setValue(BlockStateProperties.POWERED,true),false);text(scene,"firework_stop",p);scene.idle(75);scene.markAsFinished();
    }
}
