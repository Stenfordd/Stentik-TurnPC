package dev.stentik.ctccore;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Every chip does something different depending on what it is implanted into: a weapon, armor or a tool. */
public enum ChipType {
    LUNAR("lunar", 0x9FC4FF, "northstar:polished_lunar_sapphire", false),
    MARTIAN("martian", 0xE8683C, "northstar:martian_steel_ingot", false),
    MERCURY("mercury", 0x8FA3C0, "northstar:tungsten_ingot", false),
    VOID("void", 0x9A5CFF, "ctc:voidsteel_ingot", false),
    HARBINGER("harbinger", 0xC0303A, "cataclysm:witherite_ingot", true),
    IGNIS("ignis", 0xFF9A2E, "cataclysm:ignitium_ingot", true),
    LEVIATHAN("leviathan", 0x3CC8D8, "minecraft:prismarine_crystals", true),
    REMNANT("remnant", 0xD8C27A, "minecraft:bone_block", true),
    MALEDICTUS("maledictus", 0x9EE6FF, "minecraft:blue_ice", true),
    GUARDIAN("guardian", 0xC77DFF, "cataclysm:void_core", true);

    public final String id;
    public final int color;
    public final String refundId;
    public final boolean boss;

    ChipType(String id, int color, String refundId, boolean boss) {
        this.id = id;
        this.color = color;
        this.refundId = refundId;
        this.boss = boss;
    }

    public static ChipType byId(String id) {
        for (ChipType t : values()) if (t.id.equals(id)) return t;
        return null;
    }

    /** What comes back when this chip is replaced: a piece of what it was made of, or chip scrap when that item is missing. */
    public ItemStack refund() {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(refundId));
        if (item == Items.AIR) return new ItemStack(CtcCore.CHIP_SCRAP.get());
        return new ItemStack(item);
    }

    public static List<ChipType> of(ItemStack stack) {
        List<ChipType> out = new ArrayList<>();
        List<String> ids = stack.get(CtcCore.CHIPS.get());
        if (ids != null) {
            for (String s : ids) {
                ChipType t = byId(s);
                if (t != null) out.add(t);
            }
        }
        return out;
    }

    public static boolean has(ItemStack stack, ChipType type) {
        List<String> ids = stack.get(CtcCore.CHIPS.get());
        return ids != null && ids.contains(type.id);
    }
}
