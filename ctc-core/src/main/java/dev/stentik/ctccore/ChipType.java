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
    GUARDIAN("guardian", 0xC77DFF, "cataclysm:void_core", true),
    EMBER("ember", 0xFF6A2B, "minecraft:blaze_rod", false),
    TERRA("terra", 0x7FD36B, "minecraft:amethyst_shard", false),
    COPPER("copper", 0xE07B4A, "minecraft:copper_ingot", false),
    IRON("iron", 0xD8DCE4, "minecraft:iron_ingot", false),
    KINETIC("kinetic", 0x6FE3C9, "create:andesite_alloy", false),
    LUMBER("lumber", 0xB88A4E, "minecraft:oak_log", false),
    VEIN("vein", 0x9AA7B8, "create:zinc_ingot", false),
    HARVEST("harvest", 0xE8C84A, "minecraft:wheat", false),
    QUARRY("quarry", 0xC9A35A, "create:precision_mechanism", false),
    // only from the planet minibosses in their outposts
    GRAVITY("gravity", 0xB8C8F0, "ctccore:selenite", false),
    DUST("dust", 0xD2693C, "ctccore:raw_rubrium", false),
    ACID("acid", 0x9BE04A, "ctccore:sulfurite", false),
    FLARE("flare", 0xFFB23A, "ctccore:solarite", false);

    /** Basic chips need no Research Station: a Smithing Table with redstone implants them. */
    public boolean basic() {
        return this == COPPER || this == IRON || this == EMBER || this == TERRA || this == KINETIC
                || this == LUMBER || this == VEIN || this == HARVEST || this == QUARRY;
    }

    /** Gathering chips only go into their own tool: Lumber into an axe, Vein into a pickaxe, Harvest into a hoe. */
    /** Chips that change what a tool gathers; these can be switched off. */
    public boolean gathering() {
        return this == LUMBER || this == VEIN || this == HARVEST || this == QUARRY;
    }

    public boolean fits(ItemStack gear) {
        return switch (this) {
            case LUMBER -> gear.is(net.minecraft.tags.ItemTags.AXES);
            case VEIN -> gear.is(net.minecraft.tags.ItemTags.PICKAXES);
            case HARVEST -> gear.is(net.minecraft.tags.ItemTags.HOES);
            case QUARRY -> gear.is(net.minecraft.tags.ItemTags.PICKAXES) || gear.is(net.minecraft.tags.ItemTags.SHOVELS);
            default -> true;
        };
    }

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

    /** How many of the same chip add up on elite mobs and bosses (players use levels since 3.0). */
    public int cap() {
        return switch (this) {
            case MERCURY, TERRA, IRON -> 4;
            case COPPER, VOID, EMBER, KINETIC, ACID -> 3;
            case MARTIAN, IGNIS, HARBINGER, LEVIATHAN, REMNANT, GUARDIAN, LUNAR, GRAVITY, DUST, FLARE -> 2;
            default -> 1;   // Maledictus and the gathering chips do not stack
        };
    }

    /** How many of this chip the stack holds. */
    public static int count(ItemStack stack, ChipType type) {
        List<String> ids = stack.get(CtcCore.CHIPS.get());
        if (ids == null) return 0;
        int n = 0;
        for (String s : ids) if (s.equals(type.id)) n++;
        return n;
    }

    /** 3.0: a chip goes into a piece only once (it gets stronger with Amplifiers instead). */
    public static boolean canAdd(ItemStack stack, ChipType type) {
        return count(stack, type) == 0;
    }

    /** Chips that take Amplifiers: the plain ones. Boss chips do one strong thing, gathering chips just switch on. */
    public boolean leveled() {
        return !boss && !gathering();
    }

    /** Ember on a tool can be switched off like the gathering chips. */
    public boolean switchable(Gear gear) {
        return gathering() || this == EMBER && gear == Gear.TOOL;
    }

    /** Chip -> count, each capped at its stack limit. */
    public static java.util.EnumMap<ChipType, Integer> counts(Iterable<ChipType> chips) {
        java.util.EnumMap<ChipType, Integer> out = new java.util.EnumMap<>(ChipType.class);
        for (ChipType t : chips) out.merge(t, 1, Integer::sum);
        out.replaceAll((t, n) -> Math.min(n, t.cap()));
        return out;
    }

    public static boolean has(ItemStack stack, ChipType type) {
        List<String> ids = stack.get(CtcCore.CHIPS.get());
        return ids != null && ids.contains(type.id);
    }
}
