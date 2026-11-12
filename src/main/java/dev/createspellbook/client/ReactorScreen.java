package dev.createspellbook.client;

import dev.createspellbook.Content;
import dev.createspellbook.reactor.ReactorMenu;
import dev.createspellbook.reactor.ReactorOperation;
import dev.createspellbook.reactor.ReactorResources;
import dev.createspellbook.reactor.ReactorStabilizers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Container-style controls for the reactor's existing slots and telemetry. */
public final class ReactorScreen extends AbstractContainerScreen<ReactorMenu> {
    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/slot");
    private static final ResourceLocation DISABLED_SLOT = ResourceLocation.withDefaultNamespace("container/crafter/disabled_slot");
    private static final int INK = 0xff404040;
    private static final int HEALTHY = 0xff25683a;
    private static final int WARNING = 0xff922a25;
    private static final int TELEMETRY_X = 184;
    private static final int TELEMETRY_WIDTH = 184;
    private static final int FIELD_Y = 46, FIELD_HEIGHT = 12;
    private static final int TANK_X = 334, TANK_Y = 102, TANK_WIDTH = 23, TANK_HEIGHT = 62;
    private static final int STAT_WIDTH = TANK_X - TELEMETRY_X - 13;

    private Button start;
    private Button previous;
    private Button next;

    public ReactorScreen(ReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 376;
        imageHeight = 252;
        inventoryLabelX = 8;
        inventoryLabelY = 161;
    }

    @Override
    protected void init() {
        super.init();
        start = addRenderableWidget(Button.builder(Component.translatable("gui.create_spellbook.reactor.start"),
                        button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1))
                .bounds(leftPos + TELEMETRY_X, topPos + 63, 70, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.create_spellbook.reactor.stop"),
                        button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2))
                .bounds(leftPos + 258, topPos + 63, 110, 20).build());
        previous = addRenderableWidget(Button.builder(Component.literal("<"),
                        button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 100 + menu.page() - 1))
                .bounds(leftPos + 106, topPos + 31, 16, 14).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"),
                        button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 100 + menu.page() + 1))
                .bounds(leftPos + 154, topPos + 31, 16, 14).build());
        updateButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        start.active = menu.formed() && menu.pairs() > 0 && menu.accidentPhase() < 2;
        previous.active = menu.page() > 0;
        next.active = menu.page() + 1 < menu.pages();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int right = leftPos + imageWidth;
        int bottom = topPos + imageHeight;
        g.fill(leftPos, topPos, right, bottom, 0xff000000);
        g.fill(leftPos + 1, topPos + 1, right - 1, bottom - 1, 0xffc6c6c6);
        g.fill(leftPos + 1, topPos + 1, right - 3, topPos + 3, 0xffffffff);
        g.fill(leftPos + 1, topPos + 3, leftPos + 3, bottom - 3, 0xffffffff);
        g.fill(leftPos + 3, bottom - 3, right - 1, bottom - 1, 0xff555555);
        g.fill(right - 3, topPos + 3, right - 1, bottom - 3, 0xff555555);
        g.fill(leftPos + 176, topPos + 32, leftPos + 177, bottom - 7, 0xff929292);
        g.fill(leftPos + 177, topPos + 32, leftPos + 178, bottom - 7, 0xffe5e5e5);
        g.fill(leftPos + TELEMETRY_X, topPos + 184, right - 8, topPos + 185, 0xff929292);
        renderField(g, partial);
        renderTank(g);

        for (int i = 0; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            if (!slot.isActive()) continue;
            boolean disabled = i < ReactorStabilizers.MAX_SLOTS && i >= menu.capacity();
            g.blitSprite(disabled ? DISABLED_SLOT : SLOT, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
        }
    }

    private void renderField(GuiGraphics g, float partial) {
        int x = leftPos + TELEMETRY_X;
        int y = topPos + FIELD_Y;
        g.fill(x, y, x + TELEMETRY_WIDTH, y + FIELD_HEIGHT, 0xff777777);
        g.fill(x + 1, y + 1, x + TELEMETRY_WIDTH - 1, y + FIELD_HEIGHT - 1, 0xffa4a4a4);
        g.fill(x + 1, y + FIELD_HEIGHT - 1, x + TELEMETRY_WIDTH, y + FIELD_HEIGHT, 0xffededed);
        int filled = (int) Math.round((TELEMETRY_WIDTH - 2) * Math.clamp(menu.fieldProgress(), 0d, 1d));
        if (filled == 0) return;
        int end = x + 1 + filled;
        int glow = 174 + (int) (35 * Math.sin((minecraft.level.getGameTime() + partial) * .16));
        int purple = glow << 24 | 0xa568eb;
        g.fill(x + 1, y + 1, end, y + FIELD_HEIGHT - 1, 0xff08050e);
        g.fillGradient(x + 1, y + 1, end, y + 5, purple, 0x00160024);
        g.fillGradient(x + 1, y + 7, end, y + FIELD_HEIGHT - 1, 0x00160024, purple);
        g.fill(end - 1, y + 2, end, y + FIELD_HEIGHT - 2, 0xffbd85ff);
    }

    private void renderTank(GuiGraphics g) {
        int x = leftPos + TANK_X;
        int y = topPos + TANK_Y;
        int bottom = y + TANK_HEIGHT;
        g.fill(x - 2, y - 2, x + TANK_WIDTH + 2, bottom + 2, 0xff6b6b6b);
        g.fill(x - 1, bottom + 1, x + TANK_WIDTH + 2, bottom + 2, 0xfff3f3f3);
        g.fill(x + TANK_WIDTH + 1, y - 1, x + TANK_WIDTH + 2, bottom + 1, 0xffeeeeee);
        g.fill(x, y, x + TANK_WIDTH, bottom, 0xff26272e);

        int amount = menu.mana();
        if (amount > 0) {
            int filled = Math.clamp((int) Math.ceil(TANK_HEIGHT * (double) amount / Math.max(1, menu.manaCapacity())), 1, TANK_HEIGHT);
            // The tank only accepts the current canonical mana, which can be Wizardry's fluid.
            var fluid = new FluidStack(Content.MANA.get(), amount);
            var appearance = IClientFluidTypeExtensions.of(fluid.getFluid());
            var texture = appearance.getStillTexture(fluid);
            if (texture != null) {
                var sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
                int color = appearance.getTintColor(fluid);
                float red = ((color >>> 16) & 255) / 255f;
                float green = ((color >>> 8) & 255) / 255f;
                float blue = (color & 255) / 255f;
                float alpha = (color >>> 24) / 255f;
                g.enableScissor(x, bottom - filled, x + TANK_WIDTH, bottom);
                for (int row = bottom - 16; row > bottom - filled - 16; row -= 16)
                    for (int col = x; col < x + TANK_WIDTH; col += 16)
                        g.blit(col, row, 0, 16, 16, sprite, red, green, blue, alpha);
                g.disableScissor();
            }
        }
        for (int tick = 1; tick <= 3; tick++) {
            int mark = bottom - tick * TANK_HEIGHT / 4;
            g.fill(x + TANK_WIDTH - 4, mark, x + TANK_WIDTH, mark + 1, 0xffa8a5b3);
        }
    }

    private void line(GuiGraphics g, Component text, int y, int color) {
        g.drawString(font, font.substrByWidth(text, TELEMETRY_WIDTH).getString(), TELEMETRY_X, y, color, false);
    }
    private void stat(GuiGraphics g, Component text, int y) {
        g.drawString(font, font.substrByWidth(text, STAT_WIDTH).getString(), TELEMETRY_X, y, INK, false);
    }
    private void tankLabel(GuiGraphics g, String text, int y) {
        int width = font.width(text);
        float scale = Math.min(1f, 45f / Math.max(1, width));
        g.pose().pushPose();
        g.pose().translate(TANK_X + TANK_WIDTH / 2f, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, -width / 2, 0, INK, false);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 8, INK, false);
        var status = Component.translatable("gui.create_spellbook.reactor.structure",
                Component.translatable("reactor.create_spellbook.issue." + menu.issue().name().toLowerCase(Locale.ROOT)));
        g.drawString(font, font.substrByWidth(status, imageWidth - 16).getString(), 8, 21, menu.formed() ? HEALTHY : WARNING, false);
        g.drawString(font, Component.translatable("gui.create_spellbook.reactor.slot_capacity", menu.capacity()), 8, 35, INK, false);
        String page = (menu.page() + 1) + " / " + menu.pages();
        g.drawString(font, page, 138 - font.width(page) / 2, 35, INK, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, INK, false);

        var field = menu.accidentPhase() >= 2
                ? Component.translatable("gui.create_spellbook.reactor.physical_radius", String.format(Locale.ROOT, "%.1f", menu.accidentRadius()))
                : Component.translatable("gui.create_spellbook.reactor.field_size", String.format(Locale.ROOT, "%.1f", menu.sizePercent()), menu.side());
        line(g, field, 35, menu.accidentPhase() != 0 ? WARNING : INK);
        stat(g, Component.translatable("reactor.create_spellbook.run." + menu.state().name().toLowerCase(Locale.ROOT)), 91);
        stat(g, Component.translatable("gui.create_spellbook.reactor.core_pairs", menu.reactions(), menu.energies(), menu.pairs()), 102);
        stat(g, Component.translatable("gui.create_spellbook.reactor.capacity", String.format(Locale.ROOT, "%,d", menu.actual())), 113);
        stat(g, Component.translatable("gui.create_spellbook.reactor.rpm", menu.rpm()), 124);
        stat(g, Component.translatable("gui.create_spellbook.reactor.mana_rate", String.format(Locale.ROOT, "%.0f", menu.manaRate())), 135);
        stat(g, Component.translatable("gui.create_spellbook.reactor.active", menu.working(), menu.required()), 146);
        stat(g, Component.translatable("gui.create_spellbook.reactor.waste_stored", menu.matter(), ReactorResources.MATTER_CAPACITY), 157);
        stat(g, Component.translatable("gui.create_spellbook.reactor.matter_rate", String.format(Locale.ROOT, "%.2f", menu.matterRate())), 168);
        tankLabel(g, Component.translatable("gui.create_spellbook.reactor.mana_tank").getString(), 89);
        tankLabel(g, String.format(Locale.ROOT, "%,d", menu.mana()), 167);
        tankLabel(g, "mB", 176);

        boolean missingCores = menu.formed() && menu.pairs() == 0;
        var alarm = Component.translatable("reactor.create_spellbook.alarm." + menu.alarm().name().toLowerCase(Locale.ROOT));
        if (missingCores) alarm = Component.translatable("reactor.create_spellbook.alarm.no_cores");
        if (menu.accidentPhase() == 1) alarm = Component.translatable("gui.create_spellbook.reactor.growing");
        if (menu.accidentPhase() >= 2) {
            var phase = Component.translatable("reactor.create_spellbook.accident.phase." + menu.accidentPhase());
            alarm = Component.translatable("gui.create_spellbook.reactor.accident", phase, menu.accidentSeconds());
        }
        int alarmColor = missingCores || menu.accidentPhase() != 0 || menu.alarm() != ReactorOperation.Alarm.NONE ? WARNING : INK;
        var lines = font.split(alarm, TELEMETRY_WIDTH);
        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            g.drawString(font, lines.get(i), TELEMETRY_X, 193 + i * 11, alarmColor, false);
        }
        if (menu.reserves() > 0) line(g, Component.translatable("gui.create_spellbook.reactor.overflow", menu.reserves()), 239, WARNING);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float partial) {
        super.render(g, x, y, partial);
        renderTooltip(g, x, y);
        if (isHovering(TANK_X - 9, TANK_Y - 13, TANK_WIDTH + 18, TANK_HEIGHT + 34, x, y)) {
            var fluid = new FluidStack(Content.MANA.get(), 1);
            var amount = Component.translatable("gui.create_spellbook.reactor.mana_stored",
                    String.format(Locale.ROOT, "%,d", menu.mana()), String.format(Locale.ROOT, "%,d", menu.manaCapacity()));
            g.renderTooltip(font, List.of(fluid.getHoverName(), amount), Optional.empty(), x, y);
        } else if (menu.accidentPhase() < 2 && isHovering(TELEMETRY_X, FIELD_Y, TELEMETRY_WIDTH, FIELD_HEIGHT, x, y)) {
            g.renderTooltip(font, Component.translatable("gui.create_spellbook.reactor.stable_size", String.format(Locale.ROOT, "%.1f", menu.stableSizePercent())), x, y);
        }
    }
}
