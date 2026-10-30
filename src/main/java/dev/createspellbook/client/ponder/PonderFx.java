package dev.createspellbook.client.ponder;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import dev.createspellbook.Content;
import dev.createspellbook.constructs.ArcaneCastingArmBlockEntity;
import dev.createspellbook.constructs.EmitterMode;
import dev.createspellbook.devices.ArcaneDepotBlockEntity;
import dev.createspellbook.devices.FocusingBasinBlockEntity;
import dev.createspellbook.printing.ScrollPrinterBlockEntity;
import dev.createspellbook.reactor.ReactorControllerBlock;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.FluidRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Client-side helpers shared by the Ponder storyboards. */
public final class PonderFx {
    private PonderFx() {}

    public static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }

    /** Builds a real Iron's Spells scroll, so the arm renders the same item players use. */
    public static ItemStack scroll(String spellPath, int level) {
        AbstractSpell spell = SpellRegistry.getSpell(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", spellPath));
        ItemStack stack = new ItemStack(ItemRegistry.SCROLL.get());
        if (spell != null) {
            ISpellContainer.createScrollContainer(spell, level, stack);
        }
        return stack;
    }

    public static void text(SceneBuilder scene, String key, Vec3 point, int ticks) {
        int duration = Math.min(ticks, 50);
        TextElementBuilder text = scene.overlay().showText(duration).sharedText(key).pointAt(point).placeNearTarget();
        text.attachKeyFrame();
        // Ponder has no "replace current caption" call. Waiting for the element
        // to expire keeps long Chinese lines from being drawn on top of each other.
        scene.idle(duration + 5);
    }

    public static void outlineText(SceneBuilder scene, Selection selection, PonderPalette color, String key, Vec3 point, int ticks) {
        int duration = Math.min(ticks, 50);
        TextElementBuilder text = scene.overlay().showOutlineWithText(selection, duration)
                .colored(color).sharedText(key).pointAt(point).placeNearTarget();
        text.attachKeyFrame();
        scene.idle(duration + 5);
    }

    public static void outline(SceneBuilder scene, Selection selection, PonderPalette color, Object key, int ticks) {
        scene.overlay().showOutline(color, key, selection, ticks);
    }

    public static void rightClick(SceneBuilder scene, Vec3 point, ItemStack item, int ticks) {
        var input = scene.overlay().showControls(point, Pointing.RIGHT, ticks);
        if (!item.isEmpty()) {
            input.withItem(item);
        }
        input.rightClick();
    }

    public static void fillCreateTank(SceneBuilder scene, BlockPos pos, int amount) {
        scene.world().modifyBlockEntity(pos, FluidTankBlockEntity.class, tank -> {
            // A Ponder schematic is never connected/ticked, so a one-block tank
            // must be sized explicitly before its inventory accepts fluid.
            if (tank.getTotalTankSize() <= 0) {
                tank.applyFluidTankSize(1);
            }
            var inventory = tank.getTankInventory();
            int fill = Math.min(amount, inventory.getCapacity());
            if (fill > 0) {
                inventory.fill(new FluidStack(Content.MANA.get(), fill), IFluidHandler.FluidAction.EXECUTE);
                // PonderLevel does not tick block entities, so the tank's lerped
                // render level would otherwise stay at zero forever.
                if (tank.getFluidLevel() == null) {
                    tank.setFluidLevel(LerpedFloat.linear());
                }
                tank.getFluidLevel().startWithValue((double) inventory.getFluidAmount() / Math.max(1, inventory.getCapacity()));
            }
        });
    }

    public static void fillBasin(SceneBuilder scene, BlockPos pos, int amount) {
        scene.world().modifyBlockEntity(pos, FocusingBasinBlockEntity.class, basin ->
                basin.tank.fill(new FluidStack(Content.MANA.get(), amount), IFluidHandler.FluidAction.EXECUTE));
    }

    public static void fillArm(SceneBuilder scene, BlockPos pos, int amount) {
        scene.world().modifyBlockEntity(pos, ArcaneCastingArmBlockEntity.class, arm ->
                arm.tank.fill(new FluidStack(Content.MANA.get(), amount), IFluidHandler.FluidAction.EXECUTE));
    }

    public static void drainArm(SceneBuilder scene, BlockPos pos, int amount) {
        scene.world().modifyBlockEntity(pos, ArcaneCastingArmBlockEntity.class, arm ->
                arm.tank.drain(amount, IFluidHandler.FluidAction.EXECUTE));
    }

    public static void setArmScroll(SceneBuilder scene, BlockPos pos, ItemStack scroll) {
        ItemStack copy = scroll.copy();
        scene.world().modifyBlockEntity(pos, ArcaneCastingArmBlockEntity.class, arm -> arm.scroll.setStackInSlot(0, copy));
    }

    public static void setArmMode(SceneBuilder scene, BlockPos pos, EmitterMode mode) {
        scene.world().modifyBlockEntity(pos, ArcaneCastingArmBlockEntity.class, arm -> arm.setMode(mode));
    }

    public static void setDepotItem(SceneBuilder scene, BlockPos pos, ItemStack item) {
        ItemStack copy = item.copy();
        scene.world().modifyBlockEntity(pos, ArcaneDepotBlockEntity.class, depot -> depot.setHeldItem(copy));
    }

    public static void fillPrinterInk(SceneBuilder scene, BlockPos pos, int amount) {
        scene.world().modifyBlockEntity(pos, ScrollPrinterBlockEntity.class, printer ->
                printer.tank.fill(new FluidStack(FluidRegistry.COMMON_INK.get(), amount), IFluidHandler.FluidAction.EXECUTE));
    }

    public static void setPrinterTemplate(SceneBuilder scene, BlockPos pos, ItemStack template, ItemStack focus) {
        ItemStack templateCopy = template.copy();
        ItemStack focusCopy = focus.copy();
        scene.world().modifyBlockEntity(pos, ScrollPrinterBlockEntity.class, printer -> {
            printer.inventory.setStackInSlot(0, templateCopy);
            printer.inventory.setStackInSlot(1, focusCopy);
        });
    }

    public static void fillAutoCaster(SceneBuilder scene, BlockPos pos, int amount) {
        scene.world().modifyBlockEntity(pos, dev.createspellbook.constructs.ArcaneAutoCasterBlockEntity.class, caster -> {
            var inlet = caster.inputPort(caster.facing().getOpposite());
            if (inlet != null) {
                inlet.fill(new FluidStack(Content.MANA.get(), amount), IFluidHandler.FluidAction.EXECUTE);
            }
        });
    }

    public static void setAutoCasterScroll(SceneBuilder scene, BlockPos pos, ItemStack scroll) {
        ItemStack copy = scroll.copy();
        scene.world().modifyBlockEntity(pos, dev.createspellbook.constructs.ArcaneAutoCasterBlockEntity.class,
                caster -> caster.scroll.setStackInSlot(0, copy));
    }

    public static void setControllerFormed(SceneBuilder scene, BlockPos pos) {
        scene.world().modifyBlock(pos, state -> state.setValue(ReactorControllerBlock.FORMED, true), false);
    }

    /** A spherical emitter; particles drift inwards for a contained black hole, outwards for a burst. */
    public static ParticleEmitter orb(Vec3 center, double radius, ParticleOptions type, boolean inward) {
        return (level, x, y, z) -> {
            var random = ThreadLocalRandom.current();
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * random.nextDouble() - 1);
            double dx = Math.sin(phi) * Math.cos(theta) * radius;
            double dy = Math.cos(phi) * radius;
            double dz = Math.sin(phi) * Math.sin(theta) * radius;
            double speed = inward ? -.055 : .04;
            level.addParticle(type, center.x + dx, center.y + dy, center.z + dz, dx * speed, dy * speed, dz * speed);
        };
    }

    /** Particles travelling along one line; used for cast beams. */
    public static ParticleEmitter beam(Vec3 from, Vec3 to, ParticleOptions type) {
        Vec3 delta = to.subtract(from);
        return (level, x, y, z) -> {
            double t = ThreadLocalRandom.current().nextDouble();
            level.addParticle(type, from.x + delta.x * t, from.y + delta.y * t, from.z + delta.z * t, 0, 0, 0);
        };
    }

    /** Particles following a multi-segment pipe/route. */
    public static ParticleEmitter flow(List<Vec3> points, ParticleOptions type) {
        return (level, x, y, z) -> {
            if (points.size() < 2) return;
            var random = ThreadLocalRandom.current();
            int index = random.nextInt(points.size() - 1);
            Vec3 start = points.get(index), end = points.get(index + 1);
            double t = random.nextDouble();
            Vec3 p = start.add(end.subtract(start).scale(t));
            Vec3 dir = end.subtract(start).normalize().scale(.12);
            level.addParticle(type, p.x, p.y, p.z, dir.x, dir.y, dir.z);
        };
    }

    /** A ring of warning sparks at the reactor core height. */
    public static ParticleEmitter ring(Vec3 center, double radius, ParticleOptions type) {
        return (level, x, y, z) -> {
            var random = ThreadLocalRandom.current();
            double angle = random.nextDouble() * Math.PI * 2;
            level.addParticle(type, center.x + Math.cos(angle) * radius, center.y + (random.nextDouble() - .5) * .4,
                    center.z + Math.sin(angle) * radius, -Math.cos(angle) * .04, .02, -Math.sin(angle) * .04);
        };
    }

    public static ParticleOptions mana() {
        return ParticleTypes.WITCH;
    }

    public static ParticleOptions arcane() {
        return ParticleTypes.END_ROD;
    }

    public static ParticleOptions danger() {
        return ParticleTypes.SOUL_FIRE_FLAME;
    }

    public static ParticleOptions accident() {
        return ParticleTypes.FLAME;
    }
}
