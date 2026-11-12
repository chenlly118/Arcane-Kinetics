package dev.createspellbook.client.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.createspellbook.constructs.EmitterMode;
import dev.createspellbook.reactor.ReactorContent;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Concise storyboards: one clear idea per caption, with the real machine renderers. */
public final class PonderScenes {
    private PonderScenes() {}

    private static final BlockPos CONTROL = new BlockPos(6, 5, 8);
    private static final BlockPos MANA = new BlockPos(10, 5, 4);
    private static final BlockPos STABILIZER = new BlockPos(4, 5, 8);
    private static final BlockPos OUTPUT = new BlockPos(6, 3, 8);
    private static final BlockPos REACTION = new BlockPos(6, 9, 4);
    private static final BlockPos ENERGY = new BlockPos(6, 1, 4);
    private static final BlockPos SHAFT = new BlockPos(2, 5, 4);
    private static final BlockPos REACTOR_TANK = new BlockPos(12, 1, 4);

    private static final BlockPos ARM = new BlockPos(4, 3, 5);
    private static final BlockPos ARM_SHAFT = new BlockPos(4, 2, 5);
    private static final BlockPos ARM_TANK = new BlockPos(0, 1, 5);
    private static final BlockPos ARM_DEPOT = new BlockPos(4, 2, 8);

    private static final BlockPos BASIN = new BlockPos(4, 1, 4);
    private static final BlockPos BASIN_PUMP = new BlockPos(6, 1, 4);
    private static final BlockPos BASIN_TANK = new BlockPos(8, 1, 4);

    private static final BlockPos DEPOT = new BlockPos(5, 2, 5);
    private static final BlockPos DEPOT_ARM = new BlockPos(5, 3, 8);
    private static final BlockPos DEPOT_TANK = new BlockPos(9, 1, 8);

    private static final BlockPos PRINTER = new BlockPos(4, 3, 4);
    private static final BlockPos PRINTER_DEPOT = new BlockPos(4, 1, 4);
    private static final BlockPos PRINTER_TANK = new BlockPos(6, 1, 4);

    private static final BlockPos CASTER = new BlockPos(4, 2, 4);
    private static final BlockPos CASTER_TANK = new BlockPos(3, 1, 4);

    private static void start(SceneBuilder scene, String id, String title, int offset, int size, float scale) {
        scene.title(id, title);
        scene.configureBasePlate(offset, offset, size);
        scene.scaleSceneView(scale);
        scene.showBasePlate();
        scene.idle(18);
    }

    private static void text(SceneBuilder scene, String key, BlockPos pos) {
        PonderFx.text(scene, key, Vec3.atCenterOf(pos), 80);
    }

    private static void outlineText(SceneBuilder scene, Selection selection, String key, BlockPos pos, PonderPalette color) {
        PonderFx.outlineText(scene, selection, color, key, Vec3.atCenterOf(pos), 80);
    }

    private static void mark(SceneBuilder scene, String key, Selection selection, PonderPalette color) {
        scene.overlay().showOutline(color, key, selection, 70);
    }

    private static Selection reactorLayer(SceneBuildingUtil util, int y) {
        return util.select().fromTo(2, y, 0, 10, y, 8);
    }

    private static Selection reactorShell(SceneBuildingUtil util) {
        return util.select().fromTo(2, 1, 0, 10, 9, 8);
    }

    private static Selection reactorPower(SceneBuildingUtil util) {
        return util.select().fromTo(0, 5, 4, 2, 5, 4);
    }

    private static Selection reactorSupply(SceneBuildingUtil util) {
        return util.select().fromTo(11, 1, 4, 12, 5, 4);
    }

    public static void reactorAssembly(SceneBuilder scene, SceneBuildingUtil util) {
        start(scene, "black_hole_reactor_assembly", "黑洞反应器：搭建", 0, 13, .50f);
        scene.rotateCameraY(180f);
        scene.special().movePointOfInterest(new Vec3(6.5, 5.0, 4.5));

        scene.world().showSection(reactorLayer(util, 1), Direction.UP);
        scene.idle(16);
        text(scene, PonderTexts.REACTOR_ASSEMBLY_INTRO, new BlockPos(6, 1, 4));
        text(scene, PonderTexts.REACTOR_ASSEMBLY_FLOOR, new BlockPos(6, 1, 4));
        mark(scene, "energy_core", util.select().position(ENERGY), PonderPalette.GREEN);

        scene.world().showSection(reactorLayer(util, 2), Direction.UP);
        scene.idle(14);
        scene.world().showSection(reactorLayer(util, 3), Direction.UP);
        scene.idle(14);
        text(scene, PonderTexts.REACTOR_ASSEMBLY_WALLS, new BlockPos(6, 3, 8));

        scene.world().showSection(reactorLayer(util, 4), Direction.UP);
        scene.idle(12);
        scene.world().showSection(reactorLayer(util, 5), Direction.UP);
        scene.idle(18);
        text(scene, PonderTexts.REACTOR_ASSEMBLY_CONTROLLER, CONTROL);
        mark(scene, "reactor_controller", util.select().position(CONTROL), PonderPalette.WHITE);
        mark(scene, "reactor_mana", util.select().position(MANA), PonderPalette.INPUT);
        mark(scene, "reactor_stabilizer", util.select().position(STABILIZER), PonderPalette.GREEN);
        mark(scene, "reactor_output", util.select().position(OUTPUT), PonderPalette.OUTPUT);

        scene.world().showSection(reactorLayer(util, 6), Direction.UP);
        scene.idle(12);
        scene.world().showSection(reactorLayer(util, 7), Direction.UP);
        scene.idle(12);
        scene.world().showSection(reactorLayer(util, 8), Direction.UP);
        scene.idle(12);
        scene.world().showSection(reactorLayer(util, 9), Direction.UP);
        scene.idle(18);
        text(scene, PonderTexts.REACTOR_ASSEMBLY_REACTION_CORE, REACTION);
        mark(scene, "reaction_core", util.select().position(REACTION), PonderPalette.RED);

        scene.world().showSection(reactorPower(util), Direction.WEST);
        scene.idle(14);
        scene.rotateCameraY(180f);
        scene.idle(18);
        outlineText(scene, util.select().position(SHAFT), PonderTexts.REACTOR_ASSEMBLY_SHAFT,
                SHAFT, PonderPalette.OUTPUT);
        scene.rotateCameraY(180f);
        scene.idle(18);

        scene.world().showSection(reactorSupply(util), Direction.EAST);
        PonderFx.fillCreateTank(scene, REACTOR_TANK, 8000);
        scene.effects().emitParticles(new Vec3(12.5, 2.5, 4.5), PonderFx.flow(List.of(
                new Vec3(12.5, 2.5, 4.5), new Vec3(12.5, 5.5, 4.5),
                new Vec3(11.5, 5.5, 4.5), new Vec3(10.2, 5.5, 4.5)), PonderFx.mana()), 4f, 80);
        scene.idle(16);
        text(scene, PonderTexts.REACTOR_ASSEMBLY_CHECK, CONTROL);

        PonderFx.setControllerFormed(scene, CONTROL);
        mark(scene, "reactor_complete", reactorShell(util), PonderPalette.GREEN);
        scene.idle(50);
        scene.markAsFinished();
    }

    public static void reactorOperation(SceneBuilder scene, SceneBuildingUtil util) {
        start(scene, "black_hole_reactor_operation", "黑洞反应器：运行", 0, 13, .50f);
        scene.rotateCameraY(180f);
        scene.special().movePointOfInterest(new Vec3(6.5, 5.0, 4.5));
        var create = new CreateSceneBuilder(scene);

        scene.world().showSection(reactorShell(util), Direction.UP);
        scene.world().showSection(reactorPower(util), Direction.WEST);
        scene.world().showSection(reactorSupply(util), Direction.EAST);
        PonderFx.setControllerFormed(scene, CONTROL);
        PonderFx.fillCreateTank(scene, REACTOR_TANK, 8000);
        scene.idle(22);
        text(scene, PonderTexts.REACTOR_OPERATION_PANEL, CONTROL);
        PonderFx.rightClick(scene, Vec3.atCenterOf(CONTROL), ItemStack.EMPTY, 36);

        scene.effects().emitParticles(new Vec3(12.5, 2.5, 4.5), PonderFx.flow(List.of(
                new Vec3(12.5, 2.5, 4.5), new Vec3(12.5, 5.5, 4.5),
                new Vec3(11.5, 5.5, 4.5), new Vec3(10.2, 5.5, 4.5)), PonderFx.mana()), 4f, 80);
        outlineText(scene, util.select().position(MANA), PonderTexts.REACTOR_OPERATION_MANA,
                MANA, PonderPalette.INPUT);
        outlineText(scene, util.select().fromTo(6, 1, 4, 6, 9, 4), PonderTexts.REACTOR_OPERATION_PAIRS,
                new BlockPos(6, 5, 4), PonderPalette.RED);
        mark(scene, "reactor_stabilizer", util.select().position(STABILIZER), PonderPalette.GREEN);

        PonderFx.rightClick(scene, Vec3.atCenterOf(STABILIZER), new ItemStack(ReactorContent.STABILIZER.get()), 36);
        PonderFx.reactorField(scene, CONTROL, 800);
        create.world().setKineticSpeed(util.select().fromTo(0, 5, 4, 2, 5, 4), 256);
        scene.idle(18);
        text(scene, PonderTexts.REACTOR_OPERATION_START, CONTROL);
        scene.rotateCameraY(180f);
        scene.idle(30);
        scene.rotateCameraY(180f);
        scene.idle(18);

        scene.world().createItemEntity(new Vec3(6.5, 3.4, 8.8), Vec3.ZERO, new ItemStack(ReactorContent.MATTER.get(), 4));
        outlineText(scene, util.select().position(OUTPUT), PonderTexts.REACTOR_OPERATION_OUTPUT,
                OUTPUT, PonderPalette.OUTPUT);
        text(scene, PonderTexts.REACTOR_OPERATION_STOP, CONTROL);
        scene.markAsFinished();
    }

    public static void reactorSafety(SceneBuilder scene, SceneBuildingUtil util) {
        start(scene, "black_hole_reactor_safety", "黑洞反应器：失稳与事故", 0, 13, .50f);
        scene.rotateCameraY(180f);
        scene.special().movePointOfInterest(new Vec3(6.5, 5.0, 4.5));

        scene.world().showSection(reactorShell(util), Direction.UP);
        scene.world().showSection(reactorPower(util), Direction.WEST);
        scene.world().showSection(reactorSupply(util), Direction.EAST);
        PonderFx.setControllerFormed(scene, CONTROL);
        PonderFx.fillCreateTank(scene, REACTOR_TANK, 8000);
        PonderFx.reactorField(scene, CONTROL, 800);
        scene.idle(20);
        mark(scene, "reactor_danger", reactorShell(util), PonderPalette.RED);
        text(scene, PonderTexts.REACTOR_SAFETY_TRIGGER, CONTROL);
        PonderFx.reactorField(scene, CONTROL, 950);
        scene.effects().emitParticles(new Vec3(6.5, 5.5, 4.5),
                PonderFx.orb(new Vec3(6.5, 5.5, 4.5), 3.2, PonderFx.accident(), false), 7f, 80);
        text(scene, PonderTexts.REACTOR_SAFETY_GROWTH, REACTION);
        text(scene, PonderTexts.REACTOR_SAFETY_REPAIR, CONTROL);
        text(scene, PonderTexts.REACTOR_SAFETY_ADVICE, STABILIZER);
        scene.markAsFinished();
    }

    public static void castingArmSetup(SceneBuilder scene, SceneBuildingUtil util) {
        start(scene, "arcane_casting_arm_setup", "奥术施法装置：配置与加工", 0, 9, .82f);
        scene.rotateCameraY(180f);
        var create = new CreateSceneBuilder(scene);

        scene.world().showSection(util.select().fromTo(4, 1, 5, 4, 2, 5), Direction.DOWN);
        scene.world().showSection(util.select().position(ARM), Direction.DOWN);
        scene.idle(16);
        text(scene, PonderTexts.CASTING_SETUP_POWER, ARM_SHAFT);

        scene.world().showSection(util.select().fromTo(0, 1, 5, 3, 3, 5), Direction.WEST);
        PonderFx.fillCreateTank(scene, ARM_TANK, 8000);
        PonderFx.fillArm(scene, ARM, 1000);
        create.world().setKineticSpeed(util.select().fromTo(2, 1, 5, 4, 2, 5), 64);
        scene.effects().emitParticles(new Vec3(0.5, 2.5, 5.5), PonderFx.flow(List.of(
                new Vec3(0.5, 2.5, 5.5), new Vec3(0.5, 3.5, 5.5),
                new Vec3(3.5, 3.5, 5.5), new Vec3(4.2, 3.5, 5.5)), PonderFx.mana()), 4f, 80);
        scene.idle(16);
        scene.rotateCameraY(180f);
        scene.idle(18);
        outlineText(scene, util.select().position(ARM_TANK), PonderTexts.CASTING_SETUP_MANA,
                ARM_TANK, PonderPalette.INPUT);
        scene.rotateCameraY(180f);
        scene.idle(18);

        ItemStack scroll = PonderFx.scroll("cone_of_cold", 1);
        PonderFx.setArmScroll(scene, ARM, scroll);
        PonderFx.rightClick(scene, Vec3.atCenterOf(ARM), scroll, 36);
        text(scene, PonderTexts.CASTING_SETUP_SCROLL, ARM);

        PonderFx.setArmMode(scene, ARM, EmitterMode.PROCESSING);
        scene.world().showSection(util.select().position(new BlockPos(4, 4, 5)), Direction.UP);
        scene.world().showSection(util.select().fromTo(4, 1, 8, 4, 2, 8), Direction.SOUTH);
        PonderFx.setDepotItem(scene, ARM_DEPOT, new ItemStack(Items.BONE));
        scene.idle(16);
        text(scene, PonderTexts.CASTING_MODES_PROCESSING, ARM_DEPOT);

        PonderFx.casts(scene, ARM, 110);
        scene.effects().emitParticles(new Vec3(4.5, 4, 6.6),
                PonderFx.beam(new Vec3(4.5, 4, 6.6), new Vec3(4.5, 3, 8.5), PonderFx.arcane()), 3, 90);
        scene.idle(45);
        PonderFx.setDepotItem(scene, ARM_DEPOT, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "frozen_bone"))));
        text(scene, PonderTexts.CASTING_MODES_RESULT, ARM_DEPOT);
        scene.markAsFinished();
    }

    public static void focusingBasin(SceneBuilder scene, SceneBuildingUtil util) {
        start(scene, "focusing_basin", "聚魔盆：产液与输送", 1, 7, 1.1f);
        scene.world().showSection(util.select().fromTo(4, 1, 4, 8, 3, 4), Direction.UP);
        scene.idle(20);

        text(scene, PonderTexts.BASIN_INTRO, BASIN);
        scene.effects().emitParticles(new Vec3(4.5, 4, 4.5),
                PonderFx.beam(new Vec3(4.5, 4, 4.5), new Vec3(4.5, 1.5, 4.5), PonderFx.arcane()), 3, 70);
        scene.idle(25);
        PonderFx.fillBasin(scene, BASIN, 2000);
        PonderFx.fillCreateTank(scene, BASIN_TANK, 1200);
        scene.effects().emitParticles(new Vec3(4.5, 2, 4.5),
                PonderFx.ring(new Vec3(4.5, 2, 4.5), .45, PonderFx.mana()), 3f, 90);
        text(scene, PonderTexts.BASIN_RATE, BASIN);
        text(scene, PonderTexts.BASIN_CAPACITY, BASIN);
        text(scene, PonderTexts.BASIN_PIPE, BASIN_PUMP);
        PonderFx.rightClick(scene, Vec3.atCenterOf(BASIN), ItemStack.EMPTY, 36);
        text(scene, PonderTexts.BASIN_READ, BASIN);
        scene.markAsFinished();
    }

    public static void arcaneDepot(SceneBuilder scene, SceneBuildingUtil util) {
        start(scene, "arcane_depot", "魔化置物台：法术加工", 3, 7, 1.1f);
        scene.rotateCameraY(180f);
        var create = new CreateSceneBuilder(scene);
        scene.world().showSection(util.select().fromTo(5, 1, 5, 9, 4, 8), Direction.UP);
        create.world().setKineticSpeed(util.select().fromTo(5, 1, 8, 7, 3, 8), 64);
        PonderFx.fillCreateTank(scene, DEPOT_TANK, 8000);
        PonderFx.fillArm(scene, DEPOT_ARM, 1000);
        PonderFx.setArmScroll(scene, DEPOT_ARM, PonderFx.scroll("cone_of_cold", 1));
        PonderFx.setArmMode(scene, DEPOT_ARM, EmitterMode.PROCESSING);
        scene.idle(20);

        PonderFx.setDepotItem(scene, DEPOT, new ItemStack(Items.BONE));
        PonderFx.rightClick(scene, Vec3.atCenterOf(DEPOT), new ItemStack(Items.BONE), 36);
        text(scene, PonderTexts.DEPOT_INTRO, DEPOT);
        text(scene, PonderTexts.DEPOT_RECIPE, DEPOT);

        PonderFx.casts(scene, DEPOT_ARM, 100);
        scene.effects().emitParticles(new Vec3(5.5, 4, 7),
                PonderFx.beam(new Vec3(5.5, 4, 7), new Vec3(5.5, 2.9, 5.5), PonderFx.arcane()), 3, 90);
        scene.idle(40);
        PonderFx.setDepotItem(scene, DEPOT, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "frozen_bone"))));
        text(scene, PonderTexts.DEPOT_EXAMPLE, DEPOT);
        text(scene, PonderTexts.DEPOT_RESULT, DEPOT);
        text(scene, PonderTexts.DEPOT_AUTOMATION, DEPOT);
        scene.markAsFinished();
    }

    public static void scrollPrinter(SceneBuilder scene, SceneBuildingUtil util) {
        start(scene, "scroll_printer", "卷轴印刷机：配置与印刷", 1, 7, 1.1f);
        scene.rotateCameraY(180f);
        scene.world().showSection(util.select().fromTo(3, 1, 3, 6, 4, 5), Direction.UP);
        scene.idle(20);

        text(scene, PonderTexts.PRINTER_SETUP, PRINTER);
        ItemStack template = PonderFx.scroll("firebolt", 1);
        PonderFx.setPrinterTemplate(scene, PRINTER, template, new ItemStack(Items.BLAZE_ROD));
        PonderFx.rightClick(scene, Vec3.atCenterOf(PRINTER), template, 36);
        text(scene, PonderTexts.PRINTER_TEMPLATE, PRINTER);

        PonderFx.fillPrinterInk(scene, PRINTER, 4000);
        PonderFx.fillCreateTank(scene, PRINTER_TANK, 4000);
        scene.effects().emitParticles(new Vec3(6.5, 2.5, 4.5), PonderFx.flow(List.of(
                new Vec3(6.5, 1.5, 4.5), new Vec3(6.5, 4.5, 4.5),
                new Vec3(5.5, 4.5, 4.5), new Vec3(4.5, 4.5, 4.5)), PonderFx.mana()), 4f, 80);
        text(scene, PonderTexts.PRINTER_INK, PRINTER);

        PonderFx.setDepotItem(scene, PRINTER_DEPOT, new ItemStack(Items.PAPER));
        PonderFx.rightClick(scene, Vec3.atCenterOf(PRINTER_DEPOT), new ItemStack(Items.PAPER), 36);
        text(scene, PonderTexts.PRINTER_PAPER, PRINTER_DEPOT);

        PonderFx.printCycles(scene, PRINTER, PRINTER_DEPOT, 140);
        scene.idle(70);
        text(scene, PonderTexts.PRINTER_CYCLE, PRINTER_DEPOT);
        scene.markAsFinished();
    }

    public static void arcaneAutoCaster(SceneBuilder scene, SceneBuildingUtil util) {
        start(scene, "arcane_reactor", "奥术反应装置：自动施法", 1, 7, 1.1f);
        scene.rotateCameraY(180f);
        scene.world().showSection(util.select().fromTo(3, 1, 3, 4, 2, 5), Direction.UP);
        scene.idle(20);

        text(scene, PonderTexts.CASTER_NO_POWER, CASTER);
        ItemStack scroll = PonderFx.scroll("firebolt", 1);
        PonderFx.setAutoCasterScroll(scene, CASTER, scroll);
        PonderFx.rightClick(scene, Vec3.atCenterOf(CASTER), scroll, 36);
        text(scene, PonderTexts.CASTER_SCROLL, CASTER);

        PonderFx.fillAutoCaster(scene, CASTER, 1000);
        PonderFx.fillCreateTank(scene, CASTER_TANK, 8000);
        scene.effects().emitParticles(new Vec3(3.5, 2.5, 4.5), PonderFx.flow(List.of(
                new Vec3(3.5, 1.5, 4.5), new Vec3(3.5, 2.5, 4.5), new Vec3(4.1, 2.5, 4.5)), PonderFx.mana()), 4f, 80);
        text(scene, PonderTexts.CASTER_MANA, CASTER);

        var target = scene.world().createEntity(level -> {
            Zombie entity = new Zombie(level);
            entity.setPos(7, 2, 4.5);
            entity.setNoAi(true);
            entity.setNoGravity(true);
            entity.setInvulnerable(true);
            return entity;
        });
        scene.idle(16);
        text(scene, PonderTexts.CASTER_AUTO, CASTER);
        PonderFx.casts(scene, CASTER, 100);
        scene.effects().emitParticles(new Vec3(4.5, 3.4, 4.5),
                PonderFx.beam(new Vec3(4.5, 3.4, 4.5), new Vec3(7, 2.5, 4.5), PonderFx.arcane()), 3, 80);
        scene.idle(30);
        text(scene, PonderTexts.CASTER_LIMITS, CASTER);
        scene.world().modifyEntity(target, entity -> entity.discard());
        scene.markAsFinished();
    }
}
