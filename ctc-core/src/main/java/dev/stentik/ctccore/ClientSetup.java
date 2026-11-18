package dev.stentik.ctccore;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class ClientSetup {
    @EventBusSubscriber(modid = CtcCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void screens(RegisterMenuScreensEvent event) {
            event.register(CtcCore.STATION_MENU.get(), StationScreen::new);
        }

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            // Ponder comes with Create; without it the pack still works, just without the scenes
            if (ModList.get().isLoaded("ponder")) event.enqueueWork(dev.stentik.ctccore.ponder.CtcPonderPlugin::register);
        }
    }

    @EventBusSubscriber(modid = CtcCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static class GameBus {
        /** Gear shows its chips and what they do in it. */
        @SubscribeEvent
        public static void tooltip(ItemTooltipEvent event) {
            List<ChipType> chips = ChipType.of(event.getItemStack());
            Gear gear = Gear.of(event.getItemStack());
            if (gear == Gear.NONE) return;
            List<Component> lines = event.getToolTip();
            Voidforge.tooltip(event.getItemStack(), lines);
            boolean gathering = false;
            for (ChipType t : ChipType.of(event.getItemStack())) if (t.gathering()) gathering = true;
            if (gathering) lines.add(GatherEvents.off(event.getItemStack())
                    ? Component.translatable("ctccore.chips.off").withStyle(ChatFormatting.RED)
                    : Component.translatable("ctccore.chips.on").withStyle(ChatFormatting.DARK_GRAY));
            String bonus = Armors.bonusKey(event.getItemStack());
            if (bonus != null) lines.add(Component.translatable("ctccore.armor.set", Component.translatable(bonus)).withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("ctccore.chips", chips.size(), Gear.slots(event.getItemStack())).withStyle(ChatFormatting.GRAY));
            for (ChipType chip : chips) {
                lines.add(Component.literal(" ◆ ").withStyle(s -> s.withColor(chip.color))
                        .append(Component.translatable("item.ctccore." + chip.id + "_chip").withStyle(s -> s.withColor(chip.color)))
                        .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.translatable("ctccore.chip." + chip.id + "." + gear.name().toLowerCase()).withStyle(ChatFormatting.GRAY)));
            }
        }
    }
}
