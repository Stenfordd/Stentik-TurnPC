package dev.stentik.ctccore;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Implants a chip into a piece of gear. Needs FE and one reactor fuel rod per implant. */
public class ResearchStationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_GEAR = 0, SLOT_CHIP = 1, SLOT_FUEL = 2, SLOT_OUT = 3;
    public static final int CAPACITY = 200_000, ENERGY_PER_IMPLANT = 60_000, DURATION = 200;
    public static final ResourceLocation FUEL = ResourceLocation.fromNamespaceAndPath("createnuclear", "uranium_rod");

    /** Status shown in the screen. */
    public static final int OK = 0, NO_GEAR = 1, NO_CHIP = 2, HAS_CHIP = 3, NO_FUEL = 4, NO_ENERGY = 5, OUT_FULL = 6, WORKING = 7;

    public final ItemStackHandler items = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == SLOT_GEAR || slot == SLOT_CHIP) running = false;   // taking the gear out cancels the job
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_GEAR -> Gear.of(stack) != Gear.NONE;
                case SLOT_CHIP -> stack.getItem() instanceof ChipItem;
                case SLOT_FUEL -> isFuel(stack);
                default -> false;
            };
        }
    };

    public final Battery energy = new Battery();
    private int progress;
    private boolean running;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> energy.getEnergyStored() / 100;      // synced as a short: hundreds of FE
                case 2 -> status();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ResearchStationBlockEntity(BlockPos pos, BlockState state) {
        super(CtcCore.STATION_BE.get(), pos, state);
    }

    /** Without Create Nuclear in the pack the station works without fuel. */
    public static boolean fuelRequired() {
        return BuiltInRegistries.ITEM.get(FUEL) != Items.AIR;
    }

    public static boolean isFuel(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(FUEL);
    }

    public int status() {
        ItemStack gear = items.getStackInSlot(SLOT_GEAR), chip = items.getStackInSlot(SLOT_CHIP);
        if (Gear.of(gear) == Gear.NONE) return NO_GEAR;
        if (!(chip.getItem() instanceof ChipItem chipItem)) return NO_CHIP;
        if (ChipType.has(gear, chipItem.type)) return HAS_CHIP;
        if (fuelRequired() && items.getStackInSlot(SLOT_FUEL).isEmpty()) return NO_FUEL;
        if (energy.getEnergyStored() < ENERGY_PER_IMPLANT && !running) return NO_ENERGY;
        List<ChipType> current = ChipType.of(gear);
        if (current.size() >= Gear.SLOTS) {
            ItemStack out = items.getStackInSlot(SLOT_OUT), refund = current.get(0).refund();
            if (!out.isEmpty() && (!ItemStack.isSameItemSameComponents(out, refund) || out.getCount() >= out.getMaxStackSize())) return OUT_FULL;
        }
        return running ? WORKING : OK;
    }

    /** The button in the screen. */
    public void start() {
        if (!running && status() == OK) {
            running = true;
            progress = 0;
            energy.take(ENERGY_PER_IMPLANT);
            setChanged();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResearchStationBlockEntity be) {
        if (!be.running) {
            if (be.progress != 0) be.progress = 0;
            return;
        }
        int st = be.status();
        if (st != WORKING) {             // something was taken out in the middle: the energy is lost, nothing else
            be.running = false;
            be.progress = 0;
            return;
        }
        if (++be.progress >= DURATION) {
            be.finish(level, pos);
        }
        if (be.progress % 20 == 0) {
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.6f, 1.6f);
        }
        be.setChanged();
    }

    private void finish(Level level, BlockPos pos) {
        ItemStack gear = items.getStackInSlot(SLOT_GEAR);
        ChipItem chip = (ChipItem) items.getStackInSlot(SLOT_CHIP).getItem();
        List<String> ids = new ArrayList<>();
        List<String> old = gear.get(CtcCore.CHIPS.get());
        if (old != null) ids.addAll(old);
        if (ids.size() >= Gear.SLOTS) {          // full: the oldest chip is replaced and part of its material comes back
            ChipType removed = ChipType.byId(ids.remove(0));
            if (removed != null) {
                ItemStack out = items.getStackInSlot(SLOT_OUT), refund = removed.refund();
                if (out.isEmpty()) items.setStackInSlot(SLOT_OUT, refund);
                else out.grow(1);
            }
        }
        ids.add(chip.type.id);
        gear.set(CtcCore.CHIPS.get(), List.copyOf(ids));
        items.getStackInSlot(SLOT_CHIP).shrink(1);
        if (fuelRequired()) items.getStackInSlot(SLOT_FUEL).shrink(1);
        running = false;
        progress = 0;
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.2f);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.8f);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("progress", progress);
        tag.putBoolean("running", running);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) items.deserializeNBT(registries, tag.getCompound("items"));
        energy.set(tag.getInt("energy"));
        progress = tag.getInt("progress");
        running = tag.getBoolean("running");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ctccore.research_station");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new StationMenu(id, inventory, this);
    }

    /** Accepts FE from any side, gives none back. */
    public class Battery extends EnergyStorage {
        Battery() {
            super(CAPACITY, 4000, 0);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int got = super.receiveEnergy(toReceive, simulate);
            if (got > 0 && !simulate) setChanged();
            return got;
        }

        void take(int amount) {
            energy = Math.max(0, energy - amount);
        }

        void set(int amount) {
            energy = Math.max(0, Math.min(capacity, amount));
        }
    }
}
