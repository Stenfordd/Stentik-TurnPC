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
    public static final int GEAR_X = 44, GEAR_Y = 35, CHIP_X = 80, CHIP_Y = 21, FUEL_X = 80, FUEL_Y = 51, OUT_X = 134, OUT_Y = 35;

    public final ResearchStationBlockEntity station;
    public final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client side. */
    public StationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (ResearchStationBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(3));
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
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
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
        if (index < 4) {
            if (!moveItemStackTo(stack, 4, 40, true)) return ItemStack.EMPTY;
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
