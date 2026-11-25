package dev.stentik.ctccore;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * 3.0 chip levels. A piece of gear holds each chip once; Amplifiers (installed in the Research Station) raise one chip's
 * level. Brass takes a chip to level 2, Titanium to 3, Voidsteel to 4. Gear has upgrade slots for them by quality.
 * Stored as the AMPS component: one "chip:tier" entry per Amplifier.
 */
public final class Chips {
    public static final int MAX_LEVEL = 4;
    public static final String[] ROMAN = { "", "I", "II", "III", "IV" };
    public static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    private Chips() {
    }

    public static List<String> amps(ItemStack stack) {
        List<String> a = stack.get(CtcCore.AMPS.get());
        return a == null ? List.of() : a;
    }

    /** Amplifiers assigned to this chip. */
    public static int ampCount(ItemStack stack, ChipType type) {
        int n = 0;
        for (String a : amps(stack)) if (a.startsWith(type.id + ":")) n++;
        return n;
    }

    /** 0 = not in this piece, else 1 + its Amplifiers. */
    public static int level(ItemStack stack, ChipType type) {
        if (!ChipType.has(stack, type)) return 0;
        return Math.min(MAX_LEVEL, 1 + ampCount(stack, type));
    }

    /** Every chip of the piece with its level, in implant order. */
    public static Map<ChipType, Integer> levels(ItemStack stack) {
        Map<ChipType, Integer> out = new LinkedHashMap<>();
        for (ChipType t : ChipType.of(stack)) out.putIfAbsent(t, level(stack, t));
        return out;
    }

    /** Whether an Amplifier of this tier can go onto that chip: a Brass one lifts level 1 to 2, Titanium up to 3, Voidsteel up to 4. */
    public static boolean canAmplify(ItemStack gear, ChipType type, int tier) {
        if (type == null || !type.leveled() || !ChipType.has(gear, type)) return false;
        if (amps(gear).size() >= Gear.upgradeSlots(gear)) return false;
        int lvl = level(gear, type);
        return lvl < MAX_LEVEL && lvl <= tier;
    }

    public static void addAmp(ItemStack gear, ChipType type, int tier) {
        List<String> a = new ArrayList<>(amps(gear));
        a.add(type.id + ":" + tier);
        gear.set(CtcCore.AMPS.get(), List.copyOf(a));
    }

    /** Old (2.x) gear had the same chip several times: those copies become levels (Amplifiers of rising tier). */
    public static boolean migrate(ItemStack stack) {
        List<String> ids = stack.get(CtcCore.CHIPS.get());
        if (ids == null || ids.size() < 2) return false;
        List<String> kept = new ArrayList<>();
        List<String> a = new ArrayList<>(amps(stack));
        boolean changed = false;
        for (String id : ids) {
            if (!kept.contains(id)) {
                kept.add(id);
                continue;
            }
            changed = true;
            ChipType t = ChipType.byId(id);
            if (t == null || !t.leveled()) continue;
            int have = 0;
            for (String x : a) if (x.startsWith(id + ":")) have++;
            if (have + 1 < MAX_LEVEL) a.add(id + ":" + Math.min(3, have + 1));
        }
        if (!changed) return false;
        stack.set(CtcCore.CHIPS.get(), List.copyOf(kept));
        if (!a.isEmpty()) stack.set(CtcCore.AMPS.get(), List.copyOf(a));
        return true;
    }

    /** The armor piece in that slot, when it is armor. */
    public static ItemStack piece(LivingEntity e, EquipmentSlot slot) {
        ItemStack s = e.getItemBySlot(slot);
        return Gear.of(s) == Gear.ARMOR ? s : ItemStack.EMPTY;
    }

    /** Level of a chip in the armor piece worn in that slot (0 = none). */
    public static int worn(LivingEntity e, EquipmentSlot slot, ChipType type) {
        ItemStack s = piece(e, slot);
        return s.isEmpty() ? 0 : level(s, type);
    }

    /** Boss chips work from any armor piece. */
    public static boolean anyArmor(LivingEntity e, ChipType type) {
        for (EquipmentSlot slot : ARMOR) if (ChipType.has(piece(e, slot), type)) return true;
        return false;
    }

    /** Which of the four armor pieces a stack is ("helmet", "chest", "legs", "boots"), or null. */
    public static String pieceName(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armor)) return null;
        return switch (armor.getType()) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chest";
            case LEGGINGS -> "legs";
            case BOOTS -> "boots";
            default -> null;
        };
    }

    /** Lang key of what a chip does in this piece of gear (armor: per piece for the plain chips). */
    public static String effectKey(ChipType type, ItemStack gear) {
        Gear g = Gear.of(gear);
        if (g == Gear.ARMOR && type.leveled()) {
            String p = pieceName(gear);
            if (p != null) return "ctccore.chip." + type.id + "." + p;
        }
        return "ctccore.chip." + type.id + "." + g.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Levels of the plain chips in one piece. */
    public static EnumMap<ChipType, Integer> leveled(ItemStack stack) {
        EnumMap<ChipType, Integer> out = new EnumMap<>(ChipType.class);
        for (var e : levels(stack).entrySet()) if (e.getKey().leveled()) out.put(e.getKey(), e.getValue());
        return out;
    }
}
