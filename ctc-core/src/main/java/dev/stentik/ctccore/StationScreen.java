package dev.stentik.ctccore;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class StationScreen extends AbstractContainerScreen<StationMenu> {
    private static final ResourceLocation TEXTURE = CtcCore.id("textures/gui/research_station.png");
    private static final int INFO_X = 48, INFO_Y = 18, INFO_W = 96, INFO_H = 60;
    private static final int TEXT = 0xD9DCF2, DIM = 0x8C92B8, GOOD = 0x6FE39B, BAD = 0xFF8A80, GOLD = 0xF0C060;
    private Button button;

    public StationScreen(StationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 206;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        button = addRenderableWidget(Button.builder(Component.translatable("ctccore.station.implant"), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + INFO_X, topPos + 88, INFO_W, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // energy bar (left), filled from the bottom
        int h = 72 * Math.min(menu.energy(), ResearchStationBlockEntity.CAPACITY) / ResearchStationBlockEntity.CAPACITY;
        g.fill(leftPos + 9, topPos + 92 - h, leftPos + 17, topPos + 92, 0xFFE8483C);
        g.fill(leftPos + 9, topPos + 92 - h, leftPos + 11, topPos + 92, 0xFFFF9A84);
        // progress bar under the info panel
        int w = INFO_W * menu.progress() / ResearchStationBlockEntity.DURATION;
        g.fill(leftPos + INFO_X, topPos + 80, leftPos + INFO_X + w, topPos + 84, 0xFFB07CFF);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, DIM, false);

        // info panel: what is in the gear now and what pressing the button will change
        List<Component> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int status = menu.status();
        ItemStack gearStack = menu.gear();
        Gear gear = Gear.of(gearStack);
        if (status == ResearchStationBlockEntity.NOT_FORMED) {
            lines.add(Component.translatable("ctccore.station.status.8", menu.missing())); colors.add(BAD);
            lines.add(Component.translatable("ctccore.station.build.short")); colors.add(DIM);
        } else {
            if (gear != Gear.NONE) {
                List<ChipType> chips = ChipType.of(gearStack);
                lines.add(Component.translatable("ctccore.chips", chips.size(), Gear.SLOTS)); colors.add(TEXT);
                for (ChipType c : chips) {
                    lines.add(Component.literal("◆ ").append(Component.translatable("item.ctccore." + c.id + "_chip"))); colors.add(c.color);
                }
                if (menu.chip().getItem() instanceof ChipItem chipItem && !chips.contains(chipItem.type)) {
                    ChipType t = chipItem.type;
                    lines.add(Component.literal("+ ").append(Component.translatable("ctccore.chip." + t.id + "." + gear.name().toLowerCase()))); colors.add(GOOD);
                    if (chips.size() >= Gear.SLOTS) {
                        lines.add(Component.translatable("ctccore.station.replaces", Component.translatable("item.ctccore." + chips.get(0).id + "_chip"),
                                chips.get(0).refund().getHoverName())); colors.add(GOLD);
                    }
                }
            }
            lines.add(Component.translatable("ctccore.station.status." + status));
            colors.add(status == ResearchStationBlockEntity.OK || status == ResearchStationBlockEntity.WORKING ? GOOD : BAD);
        }
        g.pose().pushPose();
        g.pose().translate(INFO_X + 3, INFO_Y + 3, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        int y = 0, width = (int) ((INFO_W - 6) / 0.75f);
        for (int i = 0; i < lines.size() && y < 70; i++) {
            g.drawWordWrap(font, lines.get(i), 0, y, width, colors.get(i));
            y += font.wordWrapHeight(lines.get(i), width) + 1;
        }
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        button.active = menu.status() == ResearchStationBlockEntity.OK;
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 18 && mouseY >= topPos + 19 && mouseY < topPos + 93) {
            g.renderTooltip(font, Component.literal(String.format("%,d / %,d FE", menu.energy(), ResearchStationBlockEntity.CAPACITY)), mouseX, mouseY);
        }
    }
}
