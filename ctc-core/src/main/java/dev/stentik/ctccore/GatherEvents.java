package dev.stentik.ctccore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Lumber Chip fells the whole tree, Vein Chip mines the whole ore vein. Sneaking turns both off. Harvest Chip = farming fortune. */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public class GatherEvents {
    public static final int MAX_LOGS = 128, MAX_ORES = 64;
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
    private static boolean busy;

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (busy || !(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown() || player.isCreative()) return;
        ItemStack tool = player.getMainHandItem();
        BlockState state = event.getState();
        BlockPos origin = event.getPos();
        List<BlockPos> extra;
        if (state.is(BlockTags.LOGS) && ChipType.has(tool, ChipType.LUMBER) && ChipType.LUMBER.fits(tool)) {
            extra = tree(player, origin);
        } else if (state.is(ORES) && ChipType.has(tool, ChipType.VEIN) && ChipType.VEIN.fits(tool)) {
            extra = vein(player, origin, state.getBlock());
        } else {
            return;
        }
        if (extra.isEmpty()) return;
        busy = true;
        try {
            for (BlockPos pos : extra) {
                if (tool.isEmpty() || tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage() - 1) break;   // never break the tool
                if (player.getFoodData().getFoodLevel() <= 0) break;
                if (player.gameMode.destroyBlock(pos)) player.causeFoodExhaustion(0.025f);
            }
        } finally {
            busy = false;
        }
    }

    /** Connected logs, only when there are natural leaves on them (so log cabins are safe). */
    private static List<BlockPos> tree(ServerPlayer player, BlockPos origin) {
        var level = player.level();
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(origin);
        seen.add(origin);
        boolean leaves = false;
        while (!todo.isEmpty() && found.size() < MAX_LOGS) {
            BlockPos p = todo.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos n = p.offset(dx, dy, dz);
                if (!seen.add(n)) continue;
                BlockState s = level.getBlockState(n);
                if (s.is(BlockTags.LOGS)) {
                    found.add(n);
                    todo.add(n);
                } else if (s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT)) {
                    leaves = true;
                }
            }
        }
        return leaves ? found : List.of();
    }

    /** The same ore block, touching, up to MAX_ORES. */
    private static List<BlockPos> vein(ServerPlayer player, BlockPos origin, Block ore) {
        var level = player.level();
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(origin);
        seen.add(origin);
        while (!todo.isEmpty() && found.size() < MAX_ORES) {
            BlockPos p = todo.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos n = p.offset(dx, dy, dz);
                if (!seen.add(n)) continue;
                if (level.getBlockState(n).is(ore)) {
                    found.add(n);
                    todo.add(n);
                }
            }
        }
        return found;
    }

    /** Harvest Chip: ripe crops cut with the hoe drop more (each stack +1 with 60% chance, seeds excluded). */
    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (!ChipType.has(tool, ChipType.HARVEST) || !ChipType.HARVEST.fits(tool)) return;
        if (!(event.getState().getBlock() instanceof CropBlock crop) || !crop.isMaxAge(event.getState())) return;
        var random = event.getLevel().getRandom();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(net.neoforged.neoforge.common.Tags.Items.SEEDS)) continue;
            if (random.nextFloat() < 0.6f) stack.grow(1);
            if (random.nextFloat() < 0.2f) stack.grow(1);
        }
    }
}
