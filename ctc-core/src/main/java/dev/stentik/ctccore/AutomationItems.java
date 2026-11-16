package dev.stentik.ctccore;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** What hoppers, funnels and pipes see: they may fill the three input slots and empty the two output slots. */
public class AutomationItems implements IItemHandler {
    private final IItemHandler items;

    public AutomationItems(IItemHandler items) {
        this.items = items;
    }

    private static boolean input(int slot) {
        return slot <= ResearchStationBlockEntity.SLOT_FUEL;
    }

    @Override
    public int getSlots() {
        return items.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return items.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return input(slot) ? items.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return input(slot) ? ItemStack.EMPTY : items.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return items.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return input(slot) && items.isItemValid(slot, stack);
    }
}
