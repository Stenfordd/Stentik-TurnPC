package dev.stentik.ctccore.client;

import dev.stentik.ctccore.ChipType;
import dev.stentik.ctccore.CtcCore;
import dev.stentik.ctccore.GatherEvents;
import dev.stentik.ctccore.VoidRapier;
import dev.stentik.ctccore.VoidsteelGear;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 2.0 HUD:
 * - above the hotbar: the gathering mode of the held tool (Quarry / Vein / Lumber / Harvest chips, VoidSteel modes), green on, red off
 * - under the crosshair: the Voidstep Rapier blink recharging (violet bar), "ready" flash when it is back
 */
@EventBusSubscriber(modid = CtcCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class Hud {
    private static long readyFlash;
    private static boolean wasCooling;

    @SubscribeEvent
    public static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(CtcCore.MODID, "gather_mode"), Hud::gatherMode);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ResourceLocation.fromNamespaceAndPath(CtcCore.MODID, "rapier"), Hud::rapier);
    }

    private static void gatherMode(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        ItemStack tool = mc.player.getMainHandItem();
        if (tool.isEmpty()) return;
        MutableComponent line;
        if (VoidsteelGear.hasBuiltins(tool)) {
            line = VoidsteelGear.modeLine(tool).copy();
        } else {
            MutableComponent names = null;
            java.util.Set<ChipType> seen = java.util.EnumSet.noneOf(ChipType.class);
            for (ChipType t : ChipType.of(tool)) {
                if (!t.gathering() || !t.fits(tool) || !seen.add(t)) continue;
                Component n = Component.translatable("item.ctccore." + t.id + "_chip");
                names = names == null ? n.copy() : names.append(", ").append(n);
            }
            if (names == null) return;
            boolean off = GatherEvents.off(tool);
            line = names.append(": ").append(Component.translatable(off ? "ctccore.hud.off" : "ctccore.hud.on"))
                    .withStyle(off ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.GREEN);
        }
        int w = mc.font.width(line);
        int x = (g.guiWidth() - w) / 2;
        boolean survival = mc.gameMode != null && mc.gameMode.canHurtPlayer();
        int y = g.guiHeight() - (survival ? 72 : 46);   // above health/armor rows and the held-item name
        if (survival && mc.player.getArmorValue() > 0) y -= 10;
        g.fill(x - 3, y - 2, x + w + 3, y + 10, 0x80000000);
        g.drawString(mc.font, line, x, y, 0xFFFFFF, true);
        if (!VoidsteelGear.hasBuiltins(tool))
            g.drawString(mc.font, Component.translatable("ctccore.hud.toggle"), x + w + 6, y, 0x808080, true);
    }

    private static void rapier(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson()) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!VoidRapier.is(stack)) { wasCooling = false; return; }
        float left = mc.player.getCooldowns().getCooldownPercent(stack.getItem(), delta.getGameTimeDeltaPartialTick(true));
        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2 + 9;
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        if (left > 0) {
            wasCooling = true;
            int w = 24, filled = Math.round(w * (1 - left));
            g.fill(cx - w / 2 - 1, cy - 1, cx + w / 2 + 1, cy + 3, 0xA0000000);
            g.fill(cx - w / 2, cy, cx - w / 2 + filled, cy + 2, 0xFFB278F6);
        } else if (wasCooling) {
            wasCooling = false;
            readyFlash = now + 12;
        }
        if (now < readyFlash) {
            Component t = Component.translatable("ctccore.hud.blink_ready");
            g.drawString(mc.font, t, cx - mc.font.width(t) / 2, cy, 0xDCBCFF, true);
        }
    }
}
