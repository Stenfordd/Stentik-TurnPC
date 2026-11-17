package dev.stentik.ctccore;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/** What kind of gear a stack is, which decides the effect a chip has in it. */
public enum Gear {
    WEAPON, ARMOR, TOOL, NONE;

    public static final int SLOTS = 2;

    /** Chip slots of this piece: 2, or 3 after a Chip Socket. */
    public static int slots(net.minecraft.world.item.ItemStack stack) {
        Integer extra = stack.get(CtcCore.SOCKETS.get());
        return SLOTS + (extra == null ? 0 : extra);
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
