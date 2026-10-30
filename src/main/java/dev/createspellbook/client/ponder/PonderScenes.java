package dev.createspellbook.client.ponder;

import dev.createspellbook.constructs.EmitterMode;
import dev.createspellbook.reactor.ReactorContent;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * All Create: Spellbook Ponder scenes. Positions match the generated schematics
 * in {@code assets/create_spellbook/ponder}; see scripts/generate_ponder_schematics.py.
 */
public final class PonderScenes {
    private PonderScenes() {}

    // Black hole reactor (9^3 schematic, controller on the south face).
    private static final BlockPos CONTROLLER = new BlockPos(6, 5, 8);
    private static final BlockPos MANA_PORT = new BlockPos(10, 5, 4);
    private static final BlockPos STABILIZER_PORT = new BlockPos(4, 5, 8);
    private static final BlockPos OUTPUT_PORT = new BlockPos(6, 3, 8);
    private static final BlockPos REACTION_CORE = new BlockPos(6, 9, 4);
    private static final BlockPos ENERGY_CORE = new BlockPos(6, 1, 4);
    private static final BlockPos REACTOR_SHAFT = new BlockPos(2, 5, 4);
    private static final BlockPos REACTOR_TANK = new BlockPos(12, 1, 4);
    private static final Vec3 REACTOR_CENTER = new Vec3(6.5, 5.0, 4.5);
    private static final Vec3 CHAMBER = new Vec3(6.5, 5.5, 4.5);

    // Arcane casting arm.
    private static final BlockPos ARM = new BlockPos(4, 3, 5);
    private static final BlockPos ARM_MOTOR = new BlockPos(4, 1, 5);
    private static final BlockPos ARM_SHAFT = new BlockPos(4, 2, 5);
    private static final BlockPos ARM_LEVER = new BlockPos(4, 4, 5);
    private static final BlockPos ARM_TANK = new BlockPos(0, 1, 5);
    private static final BlockPos ARM_DEPOT = new BlockPos(4, 2, 8);
    private static final Vec3 ARM_MUZZLE = new Vec3(4.5, 4.0, 6.6);

    // Focusing basin.
    private static final BlockPos BASIN = new BlockPos(4, 1, 4);
    private static final BlockPos BASIN_PUMP = new BlockPos(6, 1, 4);
    private static final BlockPos BASIN_TANK = new BlockPos(8, 1, 4);
    private static final Vec3 BASIN_IMPACT = new Vec3(4.5, 3.0, 4.5);

    // Arcane depot.
    private static final BlockPos DEPOT = new BlockPos(5, 2, 5);
    private static final BlockPos DEPOT_ARM = new BlockPos(5, 3, 8);
    private static final BlockPos DEPOT_MOTOR = new BlockPos(5, 1, 8);
    private static final BlockPos DEPOT_LEVER = new BlockPos(5, 4, 8);
    private static final BlockPos DEPOT_TANK = new BlockPos(9, 1, 8);
    private static final Vec3 DEPOT_MUZZLE = new Vec3(5.5, 4.0, 7.0);

    // Scroll printer.
    private static final BlockPos PRINTER = new BlockPos(4, 3, 4);
    private static final BlockPos PRINTER_DEPOT = new BlockPos(4, 1, 4);
    private static final BlockPos PRINTER_TANK = new BlockPos(6, 1, 4);
    private static final Vec3 PRINTER_HEAD = new Vec3(4.5, 2.7, 4.5);

    // Arcane auto caster.
    private static final BlockPos AUTO_CASTER = new BlockPos(4, 2, 4);
    private static final BlockPos AUTO_TANK = new BlockPos(3, 1, 4);
    private static final Vec3 AUTO_MUZZLE = new Vec3(4.5, 3.37, 4.5);
    private static final Vec3 AUTO_TARGET = new Vec3(7.0, 2.5, 4.5);

    // ---------------------------------------------------------------------
    // Black Hole Reactor
    // ---------------------------------------------------------------------

    public static void reactorAssembly(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("black_hole_reactor_assembly", "黑洞反应器：搭建");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(.56f);
        scene.showBasePlate();
        scene.special().movePointOfInterest(new Vec3(6.5, 5.0, 4.5));
        scene.rotateCameraY(180f);
        scene.idle(10);

        PonderFx.text(scene, PonderTexts.REACTOR_ASSEMBLY_INTRO, util.vector().centerOf(2, 1, 4), 70);
        scene.idle(20);

        scene.world().showSection(reactorLayer(util, 1), Direction.UP);
        scene.idle(15);
        PonderFx.text(scene, PonderTexts.REACTOR_ASSEMBLY_FLOOR, util.vector().blockSurface(new BlockPos(2, 1, 4), Direction.WEST), 70);
        scene.idle(25);

        PonderFx.outlineText(scene, util.select().position(ENERGY_CORE), PonderPalette.GREEN,
                PonderTexts.REACTOR_ASSEMBLY_ENERGY_CORE, util.vector().centerOf(ENERGY_CORE), 75);
        scene.idle(30);

        scene.world().showSection(reactorLayer(util, 2), Direction.UP);
        scene.idle(12);
        PonderFx.text(scene, PonderTexts.REACTOR_ASSEMBLY_WALLS, util.vector().centerOf(6, 3, 8), 75);
        scene.idle(25);
        scene.world().showSection(reactorLayer(util, 3), Direction.UP);
        scene.idle(10);
        scene.world().showSection(reactorLayer(util, 4), Direction.UP);
        scene.idle(10);

        scene.world().showSection(reactorLayer(util, 5), Direction.UP);
        scene.idle(15);
        PonderFx.outlineText(scene, util.select().position(CONTROLLER), PonderPalette.WHITE,
                PonderTexts.REACTOR_ASSEMBLY_CONTROLLER, util.vector().blockSurface(CONTROLLER, Direction.SOUTH), 75);
        scene.idle(30);
        PonderFx.outlineText(scene, util.select().position(MANA_PORT), PonderPalette.INPUT,
                PonderTexts.REACTOR_ASSEMBLY_MANA, util.vector().blockSurface(MANA_PORT, Direction.EAST), 75);
        scene.idle(30);
        PonderFx.outlineText(scene, util.select().position(STABILIZER_PORT), PonderPalette.GREEN,
                PonderTexts.REACTOR_ASSEMBLY_STABILIZER, util.vector().blockSurface(STABILIZER_PORT, Direction.SOUTH), 75);
        scene.idle(30);
        PonderFx.outlineText(scene, util.select().position(OUTPUT_PORT), PonderPalette.OUTPUT,
                PonderTexts.REACTOR_ASSEMBLY_OUTPUT, util.vector().blockSurface(OUTPUT_PORT, Direction.SOUTH), 75);
        scene.idle(30);

        scene.world().showSection(reactorLayer(util, 6), Direction.UP);
        scene.idle(10);
        scene.world().showSection(reactorLayer(util, 7), Direction.UP);
        scene.idle(10);
        scene.world().showSection(reactorLayer(util, 8), Direction.UP);
        scene.idle(10);
        PonderFx.text(scene, PonderTexts.REACTOR_ASSEMBLY_ROOF, util.vector().centerOf(6, 8, 4), 60);
        scene.idle(20);
        scene.world().showSection(reactorLayer(util, 9), Direction.UP);
        scene.idle(15);
        PonderFx.outlineText(scene, util.select().position(REACTION_CORE), PonderPalette.RED,
                PonderTexts.REACTOR_ASSEMBLY_REACTION_CORE, util.vector().centerOf(REACTION_CORE), 80);
        scene.idle(40);

        scene.rotateCameraY(-35f);
        scene.world().showSection(util.select().fromTo(0, 5, 4, 1, 5, 4), Direction.WEST);
        scene.idle(15);
        PonderFx.outlineText(scene, util.select().position(REACTOR_SHAFT), PonderPalette.OUTPUT,
                PonderTexts.REACTOR_ASSEMBLY_SHAFT, util.vector().blockSurface(REACTOR_SHAFT, Direction.WEST), 80);
        scene.idle(35);
        scene.rotateCameraY(35f);

        scene.world().showSection(util.select().fromTo(11, 1, 4, 12, 5, 4), Direction.EAST);
        PonderFx.fillCreateTank(scene, REACTOR_TANK, 8000);
        scene.effects().emitParticles(new Vec3(12.5, 2.5, 4.5), PonderFx.flow(List.of(
                new Vec3(12.5, 2.5, 4.5), new Vec3(12.5, 5.5, 4.5),
                new Vec3(11.5, 5.5, 4.5), new Vec3(10.2, 5.5, 4.5)), PonderFx.mana()), 4f, 50);
        scene.effects().emitParticles(new Vec3(12.5, 2.5, 4.5), PonderFx.ring(new Vec3(12.5, 2.4, 4.5), .4, PonderFx.mana()), 2f, 160);
        scene.idle(20);
        PonderFx.setControllerFormed(scene, CONTROLLER);
        PonderFx.rightClick(scene, util.vector().blockSurface(CONTROLLER, Direction.SOUTH), ItemStack.EMPTY, 40);
        PonderFx.text(scene, PonderTexts.REACTOR_ASSEMBLY_CHECK, util.vector().blockSurface(CONTROLLER, Direction.SOUTH), 90);
        scene.idle(50);

        scene.markAsFinished();
    }

    public static void reactorOperation(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("black_hole_reactor_operation", "黑洞反应器：运行");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(.56f);
        scene.showBasePlate();
        scene.special().movePointOfInterest(REACTOR_CENTER);
        scene.rotateCameraY(180f);
        scene.idle(8);

        scene.world().showSection(reactorShell(util), Direction.UP);
        scene.world().showSection(util.select().fromTo(0, 5, 4, 1, 5, 4), Direction.WEST);
        scene.world().showSection(util.select().fromTo(11, 1, 4, 12, 5, 4), Direction.EAST);
        PonderFx.setControllerFormed(scene, CONTROLLER);
        PonderFx.fillCreateTank(scene, REACTOR_TANK, 8000);
        scene.idle(35);

        PonderFx.rightClick(scene, util.vector().blockSurface(CONTROLLER, Direction.SOUTH), ItemStack.EMPTY, 40);
        PonderFx.text(scene, PonderTexts.REACTOR_OPERATION_PANEL, util.vector().blockSurface(CONTROLLER, Direction.SOUTH), 75);
        scene.idle(35);

        scene.effects().emitParticles(new Vec3(12.5, 2.5, 4.5), PonderFx.flow(List.of(
                new Vec3(12.5, 2.5, 4.5), new Vec3(12.5, 5.5, 4.5),
                new Vec3(11.5, 5.5, 4.5), new Vec3(10.2, 5.5, 4.5)), PonderFx.mana()), 4f, 60);
        PonderFx.outlineText(scene, util.select().position(MANA_PORT), PonderPalette.INPUT,
                PonderTexts.REACTOR_OPERATION_MANA, util.vector().blockSurface(MANA_PORT, Direction.EAST), 80);
        scene.idle(35);

        PonderFx.outlineText(scene, util.select().fromTo(6, 1, 4, 6, 9, 4), PonderPalette.RED,
                PonderTexts.REACTOR_OPERATION_PAIRS, util.vector().centerOf(6, 5, 4), 80);
        scene.idle(35);

        PonderFx.rightClick(scene, util.vector().blockSurface(STABILIZER_PORT, Direction.SOUTH),
                new ItemStack(ReactorContent.STABILIZER.get()), 40);
        PonderFx.outlineText(scene, util.select().position(STABILIZER_PORT), PonderPalette.GREEN,
                PonderTexts.REACTOR_OPERATION_STABILIZERS, util.vector().blockSurface(STABILIZER_PORT, Direction.SOUTH), 80);
        scene.idle(40);

        PonderFx.rightClick(scene, util.vector().blockSurface(CONTROLLER, Direction.SOUTH), ItemStack.EMPTY, 40);
        scene.effects().emitParticles(CHAMBER, PonderFx.orb(CHAMBER, 2.4, PonderFx.danger(), true), 6f, 100);
        PonderFx.text(scene, PonderTexts.REACTOR_OPERATION_START, util.vector().blockSurface(CONTROLLER, Direction.SOUTH), 75);
        scene.idle(30);

        scene.rotateCameraY(-35f);
        PonderFx.outlineText(scene, util.select().fromTo(0, 5, 4, 1, 5, 4), PonderPalette.OUTPUT,
                PonderTexts.REACTOR_OPERATION_STRESS, util.vector().blockSurface(REACTOR_SHAFT, Direction.WEST), 80);
        scene.idle(40);
        scene.rotateCameraY(35f);

        PonderFx.rightClick(scene, util.vector().blockSurface(OUTPUT_PORT, Direction.SOUTH),
                new ItemStack(ReactorContent.MATTER.get()), 40);
        PonderFx.outlineText(scene, util.select().position(OUTPUT_PORT), PonderPalette.OUTPUT,
                PonderTexts.REACTOR_OPERATION_OUTPUT, util.vector().blockSurface(OUTPUT_PORT, Direction.SOUTH), 80);
        scene.idle(40);

        PonderFx.text(scene, PonderTexts.REACTOR_OPERATION_NO_MANA, util.vector().blockSurface(MANA_PORT, Direction.EAST), 75);
        scene.idle(35);
        PonderFx.text(scene, PonderTexts.REACTOR_OPERATION_STOP, util.vector().blockSurface(CONTROLLER, Direction.SOUTH), 75);
        scene.idle(40);

        scene.markAsFinished();
    }

    public static void reactorSafety(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("black_hole_reactor_safety", "黑洞反应器：失稳与失控");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(.56f);
        scene.showBasePlate();
        scene.special().movePointOfInterest(REACTOR_CENTER);
        scene.rotateCameraY(180f);
        scene.idle(8);

        scene.world().showSection(reactorShell(util), Direction.UP);
        scene.world().showSection(util.select().fromTo(0, 5, 4, 1, 5, 4), Direction.WEST);
        scene.world().showSection(util.select().fromTo(11, 1, 4, 12, 5, 4), Direction.EAST);
        PonderFx.setControllerFormed(scene, CONTROLLER);
        PonderFx.fillCreateTank(scene, REACTOR_TANK, 8000);
        PonderFx.outline(scene, reactorShell(util), PonderPalette.RED, 1, 160);
        scene.idle(20);

        PonderFx.text(scene, PonderTexts.REACTOR_SAFETY_TRIGGER, util.vector().blockSurface(CONTROLLER, Direction.SOUTH), 80);
        scene.idle(30);
        PonderFx.text(scene, PonderTexts.REACTOR_SAFETY_GROWTH, util.vector().centerOf(REACTION_CORE), 80);
        scene.effects().emitParticles(CHAMBER, PonderFx.ring(CHAMBER, 2.6, PonderFx.danger()), 6f, 90);
        scene.idle(35);
        PonderFx.text(scene, PonderTexts.REACTOR_SAFETY_THRESHOLD, util.vector().centerOf(6, 7, 4), 80);
        scene.idle(30);
        PonderFx.text(scene, PonderTexts.REACTOR_SAFETY_TIME, util.vector().centerOf(6, 7, 4), 80);
        scene.idle(30);

        PonderFx.outlineText(scene, util.select().fromTo(4, 5, 8, 8, 5, 8), PonderPalette.GREEN,
                PonderTexts.REACTOR_SAFETY_REPAIR, util.vector().centerOf(6, 5, 8), 90);
        scene.idle(40);
        PonderFx.outlineText(scene, util.select().position(REACTION_CORE), PonderPalette.RED,
                PonderTexts.REACTOR_SAFETY_RUNAWAY, util.vector().centerOf(REACTION_CORE), 90);
        scene.effects().emitParticles(CHAMBER, PonderFx.orb(CHAMBER, 3.0, PonderFx.danger(), false), 8f, 60);
        scene.idle(45);
        PonderFx.text(scene, PonderTexts.REACTOR_SAFETY_ADVICE, util.vector().blockSurface(STABILIZER_PORT, Direction.SOUTH), 90);
        scene.idle(45);

        scene.markAsFinished();
    }

    // ---------------------------------------------------------------------
    // Arcane Casting Arm
    // ---------------------------------------------------------------------

    public static void castingArmSetup(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("arcane_casting_arm_setup", "奥术施法装置：配置");
        scene.configureBasePlate(0, 2, 7);
        scene.scaleSceneView(1.2f);
        scene.showBasePlate();
        scene.special().movePointOfInterest(new Vec3(4.5, 3.2, 5.5));
        scene.rotateCameraY(180f);
        scene.idle(8);

        scene.world().showSection(util.select().fromTo(4, 1, 5, 4, 2, 5), Direction.DOWN);
        scene.world().showSection(util.select().position(ARM), Direction.DOWN);
        scene.idle(15);
        PonderFx.outlineText(scene, util.select().fromTo(4, 1, 5, 4, 2, 5), PonderPalette.OUTPUT,
                PonderTexts.CASTING_SETUP_POWER, util.vector().blockSurface(ARM_SHAFT, Direction.WEST), 80);
        scene.idle(30);
        PonderFx.text(scene, PonderTexts.CASTING_SETUP_STRESS, util.vector().blockSurface(ARM, Direction.SOUTH), 75);
        scene.idle(30);

        scene.world().showSection(util.select().fromTo(0, 1, 5, 3, 3, 5), Direction.WEST);
        PonderFx.fillCreateTank(scene, ARM_TANK, 8000);
        PonderFx.fillArm(scene, ARM, 1000);
        scene.effects().emitParticles(new Vec3(0.5, 2.5, 5.5), PonderFx.flow(List.of(
                new Vec3(0.5, 2.5, 5.5), new Vec3(0.5, 3.5, 5.5),
                new Vec3(3.5, 3.5, 5.5), new Vec3(4.2, 3.5, 5.5)), PonderFx.mana()), 4f, 50);
        scene.idle(15);
        PonderFx.outlineText(scene, util.select().fromTo(0, 1, 5, 3, 3, 5), PonderPalette.INPUT,
                PonderTexts.CASTING_SETUP_MANA, util.vector().blockSurface(ARM, Direction.WEST), 80);
        scene.idle(35);

        ItemStack firebolt = PonderFx.scroll("firebolt", 1);
        PonderFx.setArmScroll(scene, ARM, firebolt);
        PonderFx.rightClick(scene, util.vector().blockSurface(ARM, Direction.SOUTH), firebolt, 40);
        PonderFx.text(scene, PonderTexts.CASTING_SETUP_SCROLL, util.vector().blockSurface(ARM, Direction.SOUTH), 75);
        scene.idle(30);
        PonderFx.text(scene, PonderTexts.CASTING_SETUP_TEMPLATE, util.vector().blockSurface(ARM, Direction.SOUTH), 75);
        scene.idle(30);

        scene.world().showSection(util.select().position(ARM_LEVER), Direction.UP);
        scene.idle(10);
        PonderFx.text(scene, PonderTexts.CASTING_SETUP_MODE, util.vector().blockSurface(ARM, Direction.SOUTH), 75);
        scene.idle(30);

        scene.world().toggleRedstonePower(util.select().position(ARM_LEVER));
        scene.effects().emitParticles(ARM_MUZZLE, PonderFx.beam(ARM_MUZZLE, new Vec3(4.5, 4.0, 9.5), PonderFx.arcane()), 8f, 35);
        PonderFx.drainArm(scene, ARM, 40);
        PonderFx.text(scene, PonderTexts.CASTING_SETUP_PULSE, util.vector().blockSurface(ARM_LEVER, Direction.UP), 75);
        scene.idle(35);
        scene.world().toggleRedstonePower(util.select().position(ARM_LEVER));
        scene.idle(10);

        PonderFx.text(scene, PonderTexts.CASTING_SETUP_COST, util.vector().blockSurface(ARM, Direction.WEST), 75);
        scene.idle(30);
        PonderFx.text(scene, PonderTexts.CASTING_SETUP_COOLDOWN, util.vector().blockSurface(ARM, Direction.SOUTH), 80);
        scene.idle(40);

        scene.markAsFinished();
    }

    public static void castingArmModes(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("arcane_casting_arm_modes", "奥术施法装置：三种模式");
        scene.configureBasePlate(0, 2, 7);
        scene.scaleSceneView(1.2f);
        scene.showBasePlate();
        scene.special().movePointOfInterest(new Vec3(4.5, 3.2, 6.0));
        scene.rotateCameraY(180f);
        scene.idle(8);

        scene.world().showSection(util.select().fromTo(4, 1, 5, 4, 2, 5), Direction.DOWN);
        scene.world().showSection(util.select().position(ARM), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(0, 1, 5, 3, 3, 5), Direction.WEST);
        scene.world().showSection(util.select().position(ARM_LEVER), Direction.UP);
        scene.world().showSection(util.select().fromTo(4, 1, 8, 4, 2, 8), Direction.SOUTH);
        PonderFx.fillCreateTank(scene, ARM_TANK, 8000);
        PonderFx.fillArm(scene, ARM, 1000);
        PonderFx.setArmScroll(scene, ARM, PonderFx.scroll("eldritch_blast", 1));
        scene.idle(25);

        PonderFx.text(scene, PonderTexts.CASTING_MODES_CYCLE, util.vector().blockSurface(ARM, Direction.SOUTH), 80);
        scene.idle(30);

        PonderFx.setArmMode(scene, ARM, EmitterMode.PULSE);
        PonderFx.text(scene, PonderTexts.CASTING_MODES_PULSE, util.vector().blockSurface(ARM, Direction.SOUTH), 80);
        scene.effects().emitParticles(ARM_MUZZLE, PonderFx.beam(ARM_MUZZLE, new Vec3(4.5, 4.0, 9.5), PonderFx.arcane()), 8f, 30);
        scene.idle(35);

        PonderFx.setArmMode(scene, ARM, EmitterMode.TURRET);
        PonderFx.text(scene, PonderTexts.CASTING_MODES_TURRET, util.vector().blockSurface(ARM, Direction.SOUTH), 80);
        var zombie = scene.world().createEntity(level -> {
            Zombie entity = new Zombie(level);
            entity.setPos(6.5, 3.0, 8.5);
            entity.setNoAi(true);
            entity.setNoGravity(true);
            entity.setInvulnerable(true);
            entity.setPersistenceRequired();
            return entity;
        });
        scene.world().modifyEntity(zombie, entity -> {
            entity.setPos(6.5, 3.0, 8.5);
            entity.setDeltaMovement(Vec3.ZERO);
        });
        scene.idle(10);
        scene.effects().emitParticles(ARM_MUZZLE, PonderFx.beam(ARM_MUZZLE, new Vec3(6.5, 3.6, 8.5), PonderFx.arcane()), 8f, 30);
        scene.idle(35);
        scene.world().modifyEntity(zombie, entity -> entity.discard());

        PonderFx.setArmMode(scene, ARM, EmitterMode.PROCESSING);
        PonderFx.outlineText(scene, util.select().position(ARM_DEPOT), PonderPalette.OUTPUT,
                PonderTexts.CASTING_MODES_PROCESSING, util.vector().centerOf(ARM_DEPOT), 85);
        PonderFx.setDepotItem(scene, ARM_DEPOT, new ItemStack(Items.AMETHYST_SHARD));
        scene.idle(35);
        PonderFx.text(scene, PonderTexts.CASTING_MODES_RECIPE, util.vector().centerOf(ARM_DEPOT), 75);
        scene.idle(30);
        scene.effects().emitParticles(ARM_MUZZLE, PonderFx.beam(ARM_MUZZLE, new Vec3(4.5, 2.6, 8.0), PonderFx.arcane()), 8f, 30);
        PonderFx.setDepotItem(scene, ARM_DEPOT, new ItemStack(Items.ECHO_SHARD));
        PonderFx.rightClick(scene, util.vector().centerOf(ARM_DEPOT), new ItemStack(Items.ECHO_SHARD), 30);
        PonderFx.text(scene, PonderTexts.CASTING_MODES_RESULT, util.vector().centerOf(ARM_DEPOT), 80);
        scene.idle(35);

        PonderFx.text(scene, PonderTexts.CASTING_MODES_BELT, util.vector().centerOf(ARM_DEPOT), 75);
        scene.idle(30);
        PonderFx.text(scene, PonderTexts.CASTING_MODES_LIMITS, util.vector().blockSurface(ARM, Direction.SOUTH), 80);
        scene.idle(40);

        scene.markAsFinished();
    }

    // ---------------------------------------------------------------------
    // Focusing Basin
    // ---------------------------------------------------------------------

    public static void focusingBasin(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("focusing_basin", "聚魔盆：收集液态法力");
        scene.configureBasePlate(2, 2, 7);
        scene.scaleSceneView(1.4f);
        scene.showBasePlate();
        scene.special().movePointOfInterest(new Vec3(4.5, 1.9, 4.5));
        scene.idle(8);

        scene.world().showSection(util.select().position(BASIN), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(5, 1, 4, 7, 1, 4), Direction.EAST);
        scene.world().showSection(util.select().position(BASIN_TANK), Direction.EAST);
        scene.idle(15);
        PonderFx.outlineText(scene, util.select().position(BASIN), PonderPalette.INPUT,
                PonderTexts.BASIN_INTRO, util.vector().centerOf(BASIN), 80);
        scene.idle(30);

        scene.effects().emitParticles(BASIN_IMPACT, PonderFx.beam(new Vec3(4.5, 4.8, 4.5), new Vec3(4.5, 1.4, 4.5), PonderFx.arcane()), 8f, 30);
        scene.idle(12);
        PonderFx.fillBasin(scene, BASIN, 2000);
        PonderFx.fillCreateTank(scene, BASIN_TANK, 1200);
        scene.effects().emitParticles(new Vec3(4.5, 2.0, 4.5), PonderFx.ring(new Vec3(4.5, 2.0, 4.5), .45, PonderFx.mana()), 3f, 200);
        PonderFx.text(scene, PonderTexts.BASIN_DAMAGE, util.vector().centerOf(BASIN), 75);
        scene.idle(30);
        PonderFx.text(scene, PonderTexts.BASIN_RATE, util.vector().centerOf(BASIN), 75);
        scene.idle(30);
        PonderFx.text(scene, PonderTexts.BASIN_CAPACITY, util.vector().centerOf(BASIN), 75);
        scene.idle(30);

        PonderFx.outlineText(scene, util.select().fromTo(5, 1, 4, 7, 1, 4), PonderPalette.OUTPUT,
                PonderTexts.BASIN_PIPE, util.vector().centerOf(BASIN_PUMP), 75);
        scene.idle(30);
        PonderFx.fillCreateTank(scene, BASIN_TANK, 1200);
        scene.effects().emitParticles(new Vec3(5.5, 1.5, 4.5), PonderFx.flow(List.of(
                new Vec3(5.5, 1.5, 4.5), new Vec3(8.5, 1.5, 4.5)), PonderFx.mana()), 4f, 45);
        PonderFx.text(scene, PonderTexts.BASIN_STORAGE, util.vector().centerOf(BASIN_TANK), 75);
        scene.idle(30);

        PonderFx.rightClick(scene, util.vector().centerOf(BASIN), ItemStack.EMPTY, 40);
        PonderFx.text(scene, PonderTexts.BASIN_READ, util.vector().centerOf(BASIN), 75);
        scene.idle(30);
        PonderFx.text(scene, PonderTexts.BASIN_NOTE, util.vector().centerOf(BASIN), 80);
        scene.idle(40);

        scene.markAsFinished();
    }

    // ---------------------------------------------------------------------
    // Arcane Depot
    // ---------------------------------------------------------------------

    public static void arcaneDepot(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("arcane_depot", "魔化置物台：法术加工");
        scene.configureBasePlate(3, 2, 7);
        scene.scaleSceneView(1.1f);
        scene.showBasePlate();
        scene.special().movePointOfInterest(new Vec3(5.5, 3.2, 6.5));
        scene.rotateCameraY(180f);
        scene.idle(8);

        scene.world().showSection(util.select().fromTo(5, 1, 5, 5, 2, 5), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(5, 1, 8, 5, 2, 8), Direction.DOWN);
        scene.world().showSection(util.select().position(DEPOT_ARM), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(6, 1, 8, 9, 3, 8), Direction.EAST);
        PonderFx.fillCreateTank(scene, DEPOT_TANK, 8000);
        PonderFx.fillArm(scene, DEPOT_ARM, 1000);
        PonderFx.setArmScroll(scene, DEPOT_ARM, PonderFx.scroll("eldritch_blast", 1));
        scene.idle(20);

        PonderFx.outlineText(scene, util.select().position(DEPOT), PonderPalette.WHITE,
                PonderTexts.DEPOT_INTRO, util.vector().centerOf(DEPOT), 80);
        scene.idle(30);
        PonderFx.setDepotItem(scene, DEPOT, new ItemStack(Items.AMETHYST_SHARD));
        PonderFx.rightClick(scene, util.vector().centerOf(DEPOT), new ItemStack(Items.AMETHYST_SHARD), 35);
        PonderFx.text(scene, PonderTexts.DEPOT_PLACE, util.vector().centerOf(DEPOT), 75);
        scene.idle(30);

        PonderFx.outlineText(scene, util.select().position(DEPOT_ARM), PonderPalette.OUTPUT,
                PonderTexts.DEPOT_ARM, util.vector().centerOf(DEPOT_ARM), 80);
        scene.idle(35);
        PonderFx.text(scene, PonderTexts.DEPOT_RECIPE, util.vector().centerOf(DEPOT), 75);
        scene.idle(30);
        scene.effects().emitParticles(DEPOT_MUZZLE, PonderFx.beam(DEPOT_MUZZLE, new Vec3(5.5, 2.6, 5.2), PonderFx.arcane()), 8f, 35);
        PonderFx.text(scene, PonderTexts.DEPOT_EXAMPLE, util.vector().centerOf(DEPOT), 80);
        scene.idle(30);

        PonderFx.setDepotItem(scene, DEPOT, new ItemStack(Items.ECHO_SHARD));
        PonderFx.text(scene, PonderTexts.DEPOT_RESULT, util.vector().centerOf(DEPOT), 80);
        PonderFx.rightClick(scene, util.vector().centerOf(DEPOT), new ItemStack(Items.ECHO_SHARD), 35);
        scene.idle(35);

        PonderFx.text(scene, PonderTexts.DEPOT_AUTOMATION, util.vector().centerOf(DEPOT), 75);
        scene.idle(30);
        PonderFx.text(scene, PonderTexts.DEPOT_VANILLA, util.vector().centerOf(DEPOT), 80);
        scene.idle(40);

        scene.markAsFinished();
    }

    // ---------------------------------------------------------------------
    // Scroll Printer
    // ---------------------------------------------------------------------

    public static void scrollPrinter(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("scroll_printer", "卷轴印刷机：纸张变卷轴");
        scene.configureBasePlate(1, 1, 7);
        scene.scaleSceneView(1.1f);
        scene.showBasePlate();
        scene.special().movePointOfInterest(new Vec3(4.5, 2.8, 4.5));
        scene.rotateCameraY(180f);
        scene.idle(8);

        scene.world().showSection(util.select().position(PRINTER), Direction.UP);
        scene.world().showSection(util.select().position(PRINTER_DEPOT), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(6, 1, 4, 6, 4, 4), Direction.EAST);
        scene.world().showSection(util.select().fromTo(4, 4, 4, 5, 4, 4), Direction.EAST);
        PonderFx.fillCreateTank(scene, PRINTER_TANK, 4000);
        PonderFx.fillPrinterInk(scene, PRINTER, 4000);
        scene.idle(20);

        PonderFx.outlineText(scene, util.select().fromTo(3, 1, 3, 6, 4, 5), PonderPalette.WHITE,
                PonderTexts.PRINTER_SETUP, util.vector().blockSurface(PRINTER, Direction.SOUTH), 80);
        scene.idle(30);
        PonderFx.text(scene, PonderTexts.PRINTER_NO_POWER, util.vector().blockSurface(PRINTER, Direction.SOUTH), 75);
        scene.idle(25);

        ItemStack template = PonderFx.scroll("firebolt", 1);
        ItemStack focus = new ItemStack(Items.BLAZE_ROD);
        PonderFx.setPrinterTemplate(scene, PRINTER, template, focus);
        PonderFx.rightClick(scene, util.vector().blockSurface(PRINTER, Direction.SOUTH), template, 35);
        PonderFx.text(scene, PonderTexts.PRINTER_TEMPLATE, util.vector().blockSurface(PRINTER, Direction.SOUTH), 75);
        scene.idle(25);
        PonderFx.rightClick(scene, util.vector().blockSurface(PRINTER, Direction.SOUTH), focus, 35);
        PonderFx.text(scene, PonderTexts.PRINTER_FOCUS, util.vector().blockSurface(PRINTER, Direction.SOUTH), 75);
        scene.idle(25);

        scene.effects().emitParticles(new Vec3(6.5, 2.5, 4.5), PonderFx.flow(List.of(
                new Vec3(6.5, 1.5, 4.5), new Vec3(6.5, 4.5, 4.5),
                new Vec3(5.5, 4.5, 4.5), new Vec3(4.5, 4.5, 4.5)), PonderFx.mana()), 4f, 50);
        PonderFx.text(scene, PonderTexts.PRINTER_INK, util.vector().topOf(PRINTER), 75);
        scene.idle(25);

        PonderFx.setDepotItem(scene, PRINTER_DEPOT, new ItemStack(Items.PAPER));
        PonderFx.rightClick(scene, util.vector().centerOf(PRINTER_DEPOT), new ItemStack(Items.PAPER), 35);
        PonderFx.text(scene, PonderTexts.PRINTER_PAPER, util.vector().centerOf(PRINTER_DEPOT), 75);
        scene.idle(30);

        scene.effects().emitParticles(PRINTER_HEAD, PonderFx.beam(PRINTER_HEAD, new Vec3(4.5, 1.6, 4.5), PonderFx.arcane()), 8f, 30);
        PonderFx.setDepotItem(scene, PRINTER_DEPOT, PonderFx.scroll("firebolt", 1));
        PonderFx.rightClick(scene, util.vector().centerOf(PRINTER_DEPOT), PonderFx.scroll("firebolt", 1), 35);
        PonderFx.text(scene, PonderTexts.PRINTER_CYCLE, util.vector().centerOf(PRINTER_DEPOT), 80);
        scene.idle(35);

        PonderFx.text(scene, PonderTexts.PRINTER_FILTER, util.vector().blockSurface(PRINTER, Direction.SOUTH), 75);
        scene.idle(25);
        PonderFx.text(scene, PonderTexts.PRINTER_NOTE, util.vector().centerOf(PRINTER_DEPOT), 80);
        scene.idle(40);

        scene.markAsFinished();
    }

    // ---------------------------------------------------------------------
    // Arcane Auto Caster
    // ---------------------------------------------------------------------

    public static void arcaneAutoCaster(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("arcane_reactor", "奥术反应装置：自动施法");
        scene.configureBasePlate(1, 1, 7);
        scene.scaleSceneView(1.1f);
        scene.showBasePlate();
        scene.special().movePointOfInterest(new Vec3(4.5, 2.5, 4.5));
        scene.rotateCameraY(180f);
        scene.idle(8);

        scene.world().showSection(util.select().fromTo(4, 1, 4, 4, 2, 4), Direction.DOWN);
        scene.world().showSection(util.select().position(AUTO_TANK), Direction.DOWN);
        scene.world().showSection(util.select().position(new BlockPos(3, 2, 4)), Direction.EAST);
        PonderFx.fillCreateTank(scene, AUTO_TANK, 8000);
        PonderFx.fillAutoCaster(scene, AUTO_CASTER, 1000);
        scene.idle(20);

        PonderFx.outlineText(scene, util.select().fromTo(3, 1, 3, 4, 2, 5), PonderPalette.WHITE,
                PonderTexts.CASTER_NO_POWER, util.vector().blockSurface(AUTO_CASTER, Direction.SOUTH), 80);
        scene.idle(30);

        ItemStack scroll = PonderFx.scroll("firebolt", 1);
        PonderFx.setAutoCasterScroll(scene, AUTO_CASTER, scroll);
        PonderFx.rightClick(scene, util.vector().blockSurface(AUTO_CASTER, Direction.SOUTH), scroll, 35);
        PonderFx.text(scene, PonderTexts.CASTER_SCROLL, util.vector().blockSurface(AUTO_CASTER, Direction.SOUTH), 75);
        scene.idle(25);

        scene.effects().emitParticles(new Vec3(3.5, 2.5, 4.5), PonderFx.flow(List.of(
                new Vec3(3.5, 1.5, 4.5), new Vec3(3.5, 2.5, 4.5), new Vec3(4.1, 2.5, 4.5)), PonderFx.mana()), 4f, 60);
        PonderFx.outlineText(scene, util.select().position(new BlockPos(3, 2, 4)), PonderPalette.INPUT,
                PonderTexts.CASTER_MANA, util.vector().blockSurface(new BlockPos(3, 2, 4), Direction.WEST), 80);
        scene.idle(30);

        scene.effects().emitParticles(new Vec3(4.5, 3.4, 4.5), PonderFx.ring(new Vec3(4.5, 3.4, 4.5), .35, PonderFx.mana()), 3f, 80);
        PonderFx.text(scene, PonderTexts.CASTER_REGEN, util.vector().topOf(AUTO_CASTER), 75);
        scene.idle(30);

        var zombie = scene.world().createEntity(level -> {
            Zombie entity = new Zombie(level);
            entity.setPos(AUTO_TARGET.x, AUTO_TARGET.y - .5, AUTO_TARGET.z);
            entity.setNoAi(true);
            entity.setNoGravity(true);
            entity.setInvulnerable(true);
            entity.setPersistenceRequired();
            return entity;
        });
        scene.world().modifyEntity(zombie, entity -> {
            entity.setPos(AUTO_TARGET.x, AUTO_TARGET.y - .5, AUTO_TARGET.z);
            entity.setDeltaMovement(Vec3.ZERO);
        });
        scene.idle(10);
        PonderFx.text(scene, PonderTexts.CASTER_AUTO, util.vector().topOf(AUTO_CASTER), 75);
        scene.effects().emitParticles(AUTO_MUZZLE, PonderFx.beam(AUTO_MUZZLE, AUTO_TARGET, PonderFx.arcane()), 8f, 35);
        scene.idle(30);
        PonderFx.text(scene, PonderTexts.CASTER_COMBAT, util.vector().centerOf(new BlockPos(7, 2, 4)), 75);
        scene.idle(25);
        scene.world().modifyEntity(zombie, entity -> entity.discard());

        PonderFx.text(scene, PonderTexts.CASTER_LIMITS, util.vector().blockSurface(AUTO_CASTER, Direction.SOUTH), 75);
        scene.idle(25);
        PonderFx.text(scene, PonderTexts.CASTER_NOTE, util.vector().blockSurface(AUTO_CASTER, Direction.SOUTH), 80);
        scene.idle(40);

        scene.markAsFinished();
    }

    // ---------------------------------------------------------------------
    // Reactor accident demonstration
    // ---------------------------------------------------------------------

    public static void reactorAccident(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("black_hole_reactor_accident", "黑洞反应器：事故演示");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(.56f);
        scene.showBasePlate();
        scene.special().movePointOfInterest(REACTOR_CENTER);
        scene.rotateCameraY(180f);
        scene.idle(8);

        scene.world().showSection(reactorShell(util), Direction.UP);
        scene.world().showSection(util.select().fromTo(0, 5, 4, 1, 5, 4), Direction.WEST);
        scene.world().showSection(util.select().fromTo(11, 1, 4, 12, 5, 4), Direction.EAST);
        PonderFx.setControllerFormed(scene, CONTROLLER);
        PonderFx.fillCreateTank(scene, REACTOR_TANK, 8000);
        PonderFx.outline(scene, reactorShell(util), PonderPalette.RED, 20, 260);
        scene.idle(20);

        PonderFx.text(scene, PonderTexts.REACTOR_ACCIDENT_SETUP, util.vector().blockSurface(CONTROLLER, Direction.SOUTH), 80);
        scene.effects().emitParticles(CHAMBER, PonderFx.orb(CHAMBER, 2.8, PonderFx.accident(), false), 8f, 80);
        scene.idle(30);

        PonderFx.text(scene, PonderTexts.REACTOR_ACCIDENT_GROWTH, util.vector().centerOf(REACTION_CORE), 80);
        scene.effects().emitParticles(CHAMBER, PonderFx.orb(CHAMBER, 3.8, PonderFx.accident(), false), 10f, 80);
        scene.idle(30);

        PonderFx.text(scene, PonderTexts.REACTOR_ACCIDENT_ESCAPE, util.vector().centerOf(6, 7, 4), 85);
        scene.effects().emitParticles(CHAMBER, PonderFx.orb(CHAMBER, 5.0, PonderFx.accident(), false), 12f, 80);
        scene.idle(30);

        PonderFx.text(scene, PonderTexts.REACTOR_ACCIDENT_EAT, util.vector().centerOf(6, 5, 4), 85);
        scene.effects().emitParticles(CHAMBER, PonderFx.ring(CHAMBER, 4.6, PonderFx.accident()), 8f, 80);
        scene.idle(30);

        PonderFx.outlineText(scene, util.select().fromTo(4, 5, 8, 10, 5, 8), PonderPalette.GREEN,
                PonderTexts.REACTOR_ACCIDENT_REPAIR, util.vector().centerOf(6, 5, 8), 90);
        scene.idle(35);

        PonderFx.text(scene, PonderTexts.REACTOR_ACCIDENT_DEMO, util.vector().blockSurface(CONTROLLER, Direction.SOUTH), 85);
        scene.idle(30);
        PonderFx.text(scene, PonderTexts.REACTOR_ACCIDENT_ADVICE, util.vector().blockSurface(STABILIZER_PORT, Direction.SOUTH), 90);
        scene.idle(40);

        scene.markAsFinished();
    }

    // ---------------------------------------------------------------------
    // Selection helpers
    // ---------------------------------------------------------------------

    private static Selection reactorLayer(SceneBuildingUtil util, int y) {
        return util.select().fromTo(2, y, 0, 10, y, 8);
    }

    private static Selection reactorShell(SceneBuildingUtil util) {
        return util.select().fromTo(2, 1, 0, 10, 9, 8);
    }
}
