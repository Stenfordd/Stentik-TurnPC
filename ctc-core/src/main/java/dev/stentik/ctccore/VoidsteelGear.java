package dev.stentik.ctccore;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * VoidSteel tools carry their gathering chips built in (they do not take a chip slot):
 * axe = Lumber, pickaxe = Vein + Quarry, shovel = Quarry, hoe = Harvest. Right-click the tool in the inventory to
 * switch: the pickaxe cycles Vein + Quarry -> Vein -> Quarry -> off, the others on -> off.
 */
public final class VoidsteelGear {
    private VoidsteelGear() {}

    /** Pickaxe mode: 0 = Vein + Quarry, 1 = Vein only, 2 = Quarry only (off is CHIPS_OFF). */
    public static int mode(ItemStack tool) {
        Integer m = tool.get(CtcCore.TOOL_MODE.get());
        return m == null ? 0 : m;
    }

    public static String path(ItemStack tool) {
        var id = BuiltInRegistries.ITEM.getKey(tool.getItem());
        return id.getNamespace().equals(CtcCore.MODID) ? id.getPath() : "";
    }

    public static boolean hasBuiltins(ItemStack tool) {
        return switch (path(tool)) {
            case "voidsteel_axe", "voidsteel_pickaxe", "voidsteel_shovel", "voidsteel_hoe" -> true;
            default -> false;
        };
    }

    public static boolean builtin(ItemStack tool, ChipType t) {
        return switch (path(tool)) {
            case "voidsteel_axe" -> t == ChipType.LUMBER;
            case "voidsteel_pickaxe" -> (t == ChipType.VEIN && mode(tool) != 2) || (t == ChipType.QUARRY && mode(tool) != 1);
            case "voidsteel_shovel" -> t == ChipType.QUARRY;
            case "voidsteel_hoe" -> t == ChipType.HARVEST;
            default -> false;
        };
    }

    /** A gathering ability is on when an implanted chip or the built-in one provides it. */
    public static boolean active(ItemStack tool, ChipType t) {
        return (ChipType.has(tool, t) && t.fits(tool)) || builtin(tool, t);
    }

    /** Inventory right-click on a VoidSteel tool: next mode. Returns the message to show. */
    public static Component cycle(ItemStack tool) {
        boolean off = GatherEvents.off(tool);
        if (path(tool).equals("voidsteel_pickaxe")) {
            if (off) {
                tool.remove(CtcCore.CHIPS_OFF.get());
                tool.set(CtcCore.TOOL_MODE.get(), 0);
            } else if (mode(tool) < 2) {
                tool.set(CtcCore.TOOL_MODE.get(), mode(tool) + 1);
            } else {
                tool.remove(CtcCore.TOOL_MODE.get());
                tool.set(CtcCore.CHIPS_OFF.get(), true);
            }
        } else if (off) {
            tool.remove(CtcCore.CHIPS_OFF.get());
        } else {
            tool.set(CtcCore.CHIPS_OFF.get(), true);
        }
        return Component.translatable("ctccore.voidsteel.mode." + modeKey(tool));
    }

    public static String modeKey(ItemStack tool) {
        if (GatherEvents.off(tool)) return "off";
        if (path(tool).equals("voidsteel_pickaxe")) return switch (mode(tool)) { case 1 -> "vein"; case 2 -> "quarry"; default -> "both"; };
        return "on";
    }

    static void notify(Player player, Component msg) {
        player.displayClientMessage(msg, true);
    }
}
