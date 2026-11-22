package dev.stentik.ctccore;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Implants a chip into a piece of gear. Needs FE and one reactor fuel rod per implant, and only works inside its frame:
 * a 3x3 floor of Research Casing, four casing pillars at the corners, a 3x3 casing roof with a glass block in the middle.
 */
public class ResearchStationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_GEAR = 0, SLOT_CHIP = 1, SLOT_FUEL = 2, SLOT_OUT = 3, SLOT_REFUND = 4;
    public static final int CAPACITY = 200_000, ENERGY_PER_IMPLANT = 60_000, DURATION = 200;
    public static final int FRAME_BLOCKS = 22;
    public static final ResourceLocation FUEL = ResourceLocation.fromNamespaceAndPath("createnuclear", "uranium_rod");

    /** Status shown in the screen. */
    public static final int OK = 0, NO_GEAR = 1, NO_CHIP = 2, HAS_CHIP = 3, NO_FUEL = 4, NO_ENERGY = 5, OUT_FULL = 6, WORKING = 7, NOT_FORMED = 8;

    public final ItemStackHandler items = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == SLOT_GEAR || slot == SLOT_CHIP) running = false;   // taking the gear out cancels the job
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_GEAR -> Gear.of(stack) != Gear.NONE;
                case SLOT_CHIP -> stack.getItem() instanceof ChipItem || stack.is(Materials.CHIP_SOCKET.get());
                case SLOT_FUEL -> isFuel(stack);
                default -> false;
            };
        }
    };

    public final AutomationItems automation = new AutomationItems(items);
    public final Battery energy = new Battery();
    private int progress;
    private boolean running;
    private int missing = FRAME_BLOCKS;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> energy.getEnergyStored() / 100;      // synced as a short: hundreds of FE
                case 2 -> status();
                case 3 -> missing;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 4;
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

    /** Counts the frame blocks that are missing and switches the core on or off. */
    public void checkStructure() {
        if (level == null) return;
        int miss = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.getBlockState(p.setWithOffset(worldPosition, dx, -1, dz)).is(CtcCore.CASING.get())) miss++;          // floor
                boolean centre = dx == 0 && dz == 0;
                BlockState roof = level.getBlockState(p.setWithOffset(worldPosition, dx, 1, dz));
                if (centre ? !roof.is(Tags.Blocks.GLASS_BLOCKS) : !roof.is(CtcCore.CASING.get())) miss++;                       // roof
                if (dx != 0 && dz != 0 && !level.getBlockState(p.setWithOffset(worldPosition, dx, 0, dz)).is(CtcCore.CASING.get())) miss++;  // pillars
            }
        }
        missing = miss;
        BlockState state = getBlockState();
        boolean formed = miss == 0;
        if (state.hasProperty(ResearchStationBlock.FORMED) && state.getValue(ResearchStationBlock.FORMED) != formed) {
            level.setBlock(worldPosition, state.setValue(ResearchStationBlock.FORMED, formed), Block.UPDATE_ALL);
            if (formed) level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.4f);
        }
        setFrame(formed);
    }

    /** Lights the frame up (or switches it off) and tells cables and pipes on it to look again. */
    public void setFrame(boolean formed) {
        if (level == null) return;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockState s = level.getBlockState(p.setWithOffset(worldPosition, dx, dy, dz));
                    if (s.is(CtcCore.CASING.get()) && s.getValue(ResearchCasingBlock.FORMED) != formed) {
                        level.setBlock(p, s.setValue(ResearchCasingBlock.FORMED, formed), Block.UPDATE_CLIENTS);
                        level.invalidateCapabilities(p.immutable());
                    }
                }
            }
        }
    }

    /** The core a frame block belongs to, when its multiblock is complete. */
    public static ResearchStationBlockEntity findCore(Level level, BlockPos casing, BlockState state) {
        if (!state.hasProperty(ResearchCasingBlock.FORMED) || !state.getValue(ResearchCasingBlock.FORMED)) return null;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (level.getBlockEntity(p.setWithOffset(casing, dx, dy, dz)) instanceof ResearchStationBlockEntity core && core.missing == 0) return core;
                }
            }
        }
        return null;
    }

    public int status() {
        if (missing > 0) return NOT_FORMED;
        ItemStack gear = items.getStackInSlot(SLOT_GEAR), chip = items.getStackInSlot(SLOT_CHIP);
        if (Gear.of(gear) == Gear.NONE) return NO_GEAR;
        boolean socket = chip.is(Materials.CHIP_SOCKET.get());
        if (!socket && !(chip.getItem() instanceof ChipItem)) return NO_CHIP;
        if (!socket && !((ChipItem) chip.getItem()).type.fits(gear)) return NO_GEAR;
        if (socket ? Gear.socketed(gear) : !ChipType.canAdd(gear, ((ChipItem) chip.getItem()).type)) return HAS_CHIP;
        if (fuelRequired() && items.getStackInSlot(SLOT_FUEL).isEmpty()) return NO_FUEL;
        if (energy.getEnergyStored() < ENERGY_PER_IMPLANT && !running) return NO_ENERGY;
        if (!items.getStackInSlot(SLOT_OUT).isEmpty()) return OUT_FULL;
        List<ChipType> current = ChipType.of(gear);
        if (!socket && current.size() >= Gear.slots(gear)) {
            ItemStack out = items.getStackInSlot(SLOT_REFUND), refund = current.get(0).refund();
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
        if (level.getGameTime() % 20 == 0) {
            be.checkStructure();
            // a redstone signal on the core means automatic: it starts as soon as everything is in place
            if (!be.running && level.hasNeighborSignal(pos)) be.start();
        }
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
        } else {
            if (be.progress % 20 == 0) level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.6f, 1.6f);
            if (level instanceof ServerLevel server && be.progress % 4 == 0) {
                // a beam of sparks into the floating chip, spinning faster as it gets closer to done
                double a = be.progress * 0.35, r = 0.45 - 0.3 * be.progress / DURATION;
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5 + Math.cos(a) * r, pos.getY() + 0.85,
                        pos.getZ() + 0.5 + Math.sin(a) * r, 2, 0.02, 0.05, 0.02, 0.01);
                server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, 1, 0.05, 0.12, 0.05, 0.005);
            }
        }
        be.setChanged();
    }

    private void finish(Level level, BlockPos pos) {
        ItemStack gear = items.getStackInSlot(SLOT_GEAR).copy();
        if (items.getStackInSlot(SLOT_CHIP).is(Materials.CHIP_SOCKET.get())) {
            gear.set(CtcCore.SOCKETS.get(), 1);
            done(level, pos, gear);
            return;
        }
        ChipItem chip = (ChipItem) items.getStackInSlot(SLOT_CHIP).getItem();
        List<String> ids = new ArrayList<>();
        List<String> old = gear.get(CtcCore.CHIPS.get());
        if (old != null) ids.addAll(old);
        if (ids.size() >= Gear.slots(gear)) {          // full: the oldest chip is replaced and part of its material comes back
            ChipType removed = ChipType.byId(ids.remove(0));
            if (removed != null) {
                ItemStack out = items.getStackInSlot(SLOT_REFUND), refund = removed.refund();
                if (out.isEmpty()) items.setStackInSlot(SLOT_REFUND, refund);
                else out.grow(1);
            }
        }
        ids.add(chip.type.id);
        gear.set(CtcCore.CHIPS.get(), List.copyOf(ids));
        done(level, pos, gear);
    }

    private void done(Level level, BlockPos pos, ItemStack gear) {
        running = false;
        progress = 0;
        items.getStackInSlot(SLOT_CHIP).shrink(1);
        if (fuelRequired()) items.getStackInSlot(SLOT_FUEL).shrink(1);
        items.setStackInSlot(SLOT_OUT, gear);                 // the finished piece moves to the output
        items.setStackInSlot(SLOT_GEAR, ItemStack.EMPTY);
        running = false;
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.2f);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8f, 1.8f);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.05);
        }
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
        if (tag.contains("items")) {
            CompoundTag saved = tag.getCompound("items");
            saved.putInt("Size", 5);              // stations saved by version 0.1 had four slots
            items.deserializeNBT(registries, saved);
        }
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
