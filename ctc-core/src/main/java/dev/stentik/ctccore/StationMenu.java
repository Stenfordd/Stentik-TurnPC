package dev.stentik.ctccore;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class StationMenu extends AbstractContainerMenu {
    public static final int GEAR_X = 26, GEAR_Y = 20, CHIP_X = 26, CHIP_Y = 44, FUEL_X = 26, FUEL_Y = 68;
    public static final int OUT_X = 150, OUT_Y = 20, REFUND_X = 150, REFUND_Y = 68, INV_Y = 124;
    private static final int STATION_SLOTS = 5;

    public final ResearchStationBlockEntity station;
    public final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client side. */
    public StationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (ResearchStationBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(4));
    }

    /** Server side. */
    public StationMenu(int id, Inventory inventory, ResearchStationBlockEntity station) {
        this(id, inventory, station, station.data);
    }

    private StationMenu(int id, Inventory inventory, ResearchStationBlockEntity station, ContainerData data) {
        super(CtcCore.STATION_MENU.get(), id);
        this.station = station;
        this.data = data;
        this.access = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());
        addSlot(new SlotItemHandler(station.items, ResearchStationBlockEntity.SLOT_GEAR, GEAR_X, GEAR_Y));
        addSlot(new SlotItemHandler(station.items, ResearchStationBlockEntity.SLOT_CHIP, CHIP_X, CHIP_Y));
        addSlot(new SlotItemHandler(station.items, ResearchStationBlockEntity.SLOT_FUEL, FUEL_X, FUEL_Y));
        addSlot(new SlotItemHandler(station.items, ResearchStationBlockEntity.SLOT_OUT, OUT_X, OUT_Y));
        addSlot(new SlotItemHandler(station.items, ResearchStationBlockEntity.SLOT_REFUND, REFUND_X, REFUND_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, INV_Y + 58));
        addDataSlots(data);
    }

    public int progress() {
        return data.get(0);
    }

    public int energy() {
        return data.get(1) * 100;
    }

    public int status() {
        return data.get(2);
    }

    public int missing() {
        return data.get(3);
    }

    public ItemStack gear() {
        return slots.get(0).getItem();
    }

    public ItemStack chip() {
        return slots.get(1).getItem();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            station.start();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < STATION_SLOTS) {
            if (!moveItemStackTo(stack, STATION_SLOTS, STATION_SLOTS + 36, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 3, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, CtcCore.STATION.get());
    }
}
