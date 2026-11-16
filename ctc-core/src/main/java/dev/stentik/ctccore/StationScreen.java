package dev.stentik.ctccore;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class StationScreen extends AbstractContainerScreen<StationMenu> {
    private static final ResourceLocation TEXTURE = CtcCore.id("textures/gui/research_station.png");
    private Button button;

    public StationScreen(StationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        button = addRenderableWidget(Button.builder(Component.translatable("ctccore.station.implant"), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 102, topPos + 58, 62, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // energy bar (left), filled from the bottom
        int h = 52 * Math.min(menu.energy(), ResearchStationBlockEntity.CAPACITY) / ResearchStationBlockEntity.CAPACITY;
        g.fill(leftPos + 10, topPos + 68 - h, leftPos + 20, topPos + 68, 0xFFE8483C);
        g.fill(leftPos + 10, topPos + 68 - h, leftPos + 13, topPos + 68, 0xFFFF8A70);
        // progress arrow between the chip column and the output
        int w = 24 * menu.progress() / ResearchStationBlockEntity.DURATION;
        g.fill(leftPos + 104, topPos + 40, leftPos + 104 + w, topPos + 46, 0xFF9A5CFF);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int status = menu.status();
        button.active = status == ResearchStationBlockEntity.OK;
        super.render(g, mouseX, mouseY, partialTick);
        Component line = Component.translatable("ctccore.station.status." + status);
        g.drawString(font, line, leftPos + 28, topPos + 72, status == ResearchStationBlockEntity.OK || status == ResearchStationBlockEntity.WORKING ? 0x3A8F4A : 0x8A2F2F, false);
        renderTooltip(g, mouseX, mouseY);
        if (mouseX >= leftPos + 9 && mouseX < leftPos + 21 && mouseY >= topPos + 15 && mouseY < topPos + 69) {
            g.renderTooltip(font, Component.literal(String.format("%,d / %,d FE", menu.energy(), ResearchStationBlockEntity.CAPACITY)), mouseX, mouseY);
        }
    }
}
