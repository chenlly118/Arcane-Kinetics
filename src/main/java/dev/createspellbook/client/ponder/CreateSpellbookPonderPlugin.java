package dev.createspellbook.client.ponder;

import dev.createspellbook.CreateSpellbook;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Registers the arcane engineering chapter with Ponder. */
public final class CreateSpellbookPonderPlugin implements PonderPlugin {
    public static final String ARCANE_TAG = "arcane_engineering";

    @Override
    public String getModId() {
        return CreateSpellbook.ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // Every reactor block entry opens the same four-scene chapter.
        for (String component : new String[]{
                "arcane_casing", "arcane_reinforced_glass", "reactor_controller", "reactor_output",
                "reactor_mana_interface", "reactor_port", "black_hole_reaction_core",
                "black_hole_energy_core", "black_hole_reaction_shaft"
        }) {
            helper.forComponents(id(component))
                    .addStoryBoard("black_hole_reactor", PonderScenes::reactorAssembly)
                    .addStoryBoard("black_hole_reactor", PonderScenes::reactorOperation)
                    .addStoryBoard("black_hole_reactor", PonderScenes::reactorSafety)
                    .addStoryBoard("black_hole_reactor", PonderScenes::reactorAccident);
        }

        helper.forComponents(id("arcane_casting_arm"))
                .addStoryBoard("arcane_casting_arm", PonderScenes::castingArmSetup)
                .addStoryBoard("arcane_casting_arm", PonderScenes::castingArmModes);

        helper.forComponents(id("focusing_basin"))
                .addStoryBoard("focusing_basin", PonderScenes::focusingBasin);

        helper.forComponents(id("arcane_depot"))
                .addStoryBoard("arcane_depot", PonderScenes::arcaneDepot);

        helper.forComponents(id("scroll_printer"))
                .addStoryBoard("scroll_printer", PonderScenes::scrollPrinter);

        helper.forComponents(id("arcane_reactor"))
                .addStoryBoard("arcane_reactor", PonderScenes::arcaneAutoCaster);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation tag = id(ARCANE_TAG);
        helper.registerTag(tag)
                .title("奥术动力学")
                .description("把法术接入 Create 的动力、物流与流体网络。")
                .addToIndex()
                .item(dev.createspellbook.reactor.ReactorContent.CASING.get().asItem())
                .register();

        for (String component : new String[]{
                "focusing_basin", "arcane_depot", "arcane_casting_arm", "scroll_printer", "arcane_reactor",
                "reactor_controller", "arcane_casing", "arcane_reinforced_glass", "reactor_mana_interface", "reactor_port",
                "reactor_output", "black_hole_reaction_core", "black_hole_energy_core", "black_hole_reaction_shaft",
                "black_hole_stabilizer", "collapsed_matter"
        }) {
            helper.addToComponent(id(component)).add(tag);
        }
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        PonderTexts.register(helper);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateSpellbook.ID, path);
    }
}
