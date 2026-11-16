package dev.stentik.ctccore;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class ClientSetup {
    @EventBusSubscriber(modid = CtcCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void screens(RegisterMenuScreensEvent event) {
            event.register(CtcCore.STATION_MENU.get(), StationScreen::new);
        }
    }

    @EventBusSubscriber(modid = CtcCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static class GameBus {
        /** Gear shows its chips and what they do in it. */
        @SubscribeEvent
        public static void tooltip(ItemTooltipEvent event) {
            List<ChipType> chips = ChipType.of(event.getItemStack());
            if (chips.isEmpty()) return;
            Gear gear = Gear.of(event.getItemStack());
            if (gear == Gear.NONE) return;
            List<Component> lines = event.getToolTip();
            lines.add(Component.translatable("ctccore.chips", chips.size(), Gear.SLOTS).withStyle(ChatFormatting.GRAY));
            for (ChipType chip : chips) {
                lines.add(Component.literal(" ◆ ").withStyle(s -> s.withColor(chip.color))
                        .append(Component.translatable("item.ctccore." + chip.id + "_chip").withStyle(s -> s.withColor(chip.color)))
                        .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.translatable("ctccore.chip." + chip.id + "." + gear.name().toLowerCase()).withStyle(ChatFormatting.GRAY)));
            }
        }
    }
}
