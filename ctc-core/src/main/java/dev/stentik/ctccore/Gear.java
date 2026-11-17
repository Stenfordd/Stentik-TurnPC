package dev.stentik.ctccore;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;

/** What kind of gear a stack is, which decides the effect a chip has in it. */
public enum Gear {
    WEAPON, ARMOR, TOOL, NONE;

    public static final TagKey<Item> ONE_SLOT = TagKey.create(Registries.ITEM, CtcCore.id("one_chip_slot"));
    public static final TagKey<Item> TWO_SLOTS = TagKey.create(Registries.ITEM, CtcCore.id("two_chip_slots"));

    /** Slots by quality: leather, copper, iron, gold and steel gear holds 1 chip, diamond and better holds 2. */
    public static int base(ItemStack stack) {
        if (stack.is(TWO_SLOTS)) return 2;
        if (stack.is(ONE_SLOT)) return 1;
        var item = stack.getItem();
        if (item instanceof ArmorItem armor) {
            int diamond = switch (armor.getType()) {
                case HELMET -> 3; case CHESTPLATE -> 8; case LEGGINGS -> 6; case BOOTS -> 3; default -> 99;
            };
            return armor.getDefense() >= diamond ? 2 : 1;
        }
        if (item instanceof MaceItem || item instanceof TridentItem) return 2;
        if (item instanceof TieredItem tiered) return tiered.getTier().getAttackDamageBonus() >= 3.0f ? 2 : 1;
        return 1;
    }

    /** Whether a Chip Socket was already implanted. */
    public static boolean socketed(ItemStack stack) {
        Integer extra = stack.get(CtcCore.SOCKETS.get());
        return extra != null && extra > 0;
    }

    /** Chip slots of this piece: 1 or 2 by quality, one more after a Chip Socket. */
    public static int slots(ItemStack stack) {
        Integer extra = stack.get(CtcCore.SOCKETS.get());
        return base(stack) + (extra == null ? 0 : extra);
    }

    public static Gear of(ItemStack stack) {
        if (stack.isEmpty() || stack.getMaxStackSize() != 1) return NONE;
        var item = stack.getItem();
        if (item instanceof ArmorItem) return ARMOR;
        if (item instanceof SwordItem || item instanceof TridentItem || item instanceof MaceItem || item instanceof AxeItem
                || stack.is(ItemTags.SWORDS)) return WEAPON;
        if (item instanceof DiggerItem) return TOOL;
        return NONE;
    }
}
