package dev.stentik.ctccore.client;

import java.util.Map;

import dev.stentik.ctccore.CtcCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/** Own boss bars for the Ender Dragon, the Wither and the Void Herald (Cataclysm bosses already bring theirs). */
@EventBusSubscriber(modid = CtcCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class BossBars {
    private BossBars() {}

    private static final Map<String, String> BARS = Map.of(
            "entity.minecraft.ender_dragon", "ender_dragon",
            "entity.minecraft.wither", "wither",
            "entity.ctccore.void_herald", "void_herald");

    private static String key(Component name) {
        if (name.getContents() instanceof TranslatableContents t) return t.getKey();
        for (Component c : name.getSiblings()) if (c.getContents() instanceof TranslatableContents t) return t.getKey();
        return "";
    }

    @SubscribeEvent
    public static void bar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        var boss = event.getBossEvent();
        String bar = BARS.get(key(boss.getName()));
        if (bar == null) return;
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        ResourceLocation base = CtcCore.id("textures/gui/boss_bar/" + bar + "_base.png");
        ResourceLocation over = CtcCore.id("textures/gui/boss_bar/" + bar + "_overlay.png");
        int x = event.getX(), y = event.getY() + 12;
        int fill = (int) (boss.getProgress() * 182.0f);
        g.blit(base, x, y, 0, 0, 182, 5, 256, 16);
        if (fill > 0) g.blit(base, x, y, 0, 5, fill, 5, 256, 16);
        g.blit(over, x - 37, y - 11, 0, 0, 256, 32, 256, 32);
        var font = Minecraft.getInstance().font;
        Component name = boss.getName();
        int w = font.width(name);
        g.drawString(font, name, x + 91 - w / 2, event.getY() - 9, 0xFFFFFF);
        event.setIncrement(event.getIncrement() + 14);
    }
}
