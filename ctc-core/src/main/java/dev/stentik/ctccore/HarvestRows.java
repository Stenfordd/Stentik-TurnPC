package dev.stentik.ctccore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Harvest ability (Harvest Chip or the VoidSteel hoe): breaking or right-clicking a ripe crop with the hoe harvests
 * every ripe crop of the same kind touching it (up to MAX), with the bonus yield, and plants each one again from its
 * own seeds. Sneaking turns it off, like the other gathering chips.
 */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class HarvestRows {
    public static final int MAX = 64;

    private HarvestRows() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND) return;
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof HoeItem) || player.isShiftKeyDown() || GatherEvents.off(tool)) return;
        if (!VoidsteelGear.active(tool, ChipType.HARVEST)) return;
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) return;
        harvest(player, event.getPos(), tool);
        event.setCanceled(true);
    }

    static void harvest(ServerPlayer player, BlockPos origin, ItemStack tool) {
        ServerLevel level = player.serverLevel();
        BlockState first = level.getBlockState(origin);
        Block kind = first.getBlock();
        if (!(kind instanceof CropBlock)) return;
        for (BlockPos pos : field(level, origin, kind)) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) continue;
            List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, tool);
            ItemStack seed = crop.getCloneItemStack(level, pos, state);
            boolean replant = false;
            for (ItemStack d : drops) {
                if (!seed.isEmpty() && d.is(seed.getItem()) && d.getCount() > 0) {
                    d.shrink(1);
                    replant = true;
                    break;
                }
            }
            var random = level.getRandom();
            for (ItemStack d : drops) {
                if (d.isEmpty()) continue;
                if (!d.is(Tags.Items.SEEDS) && !d.is(seed.getItem())) {
                    if (random.nextFloat() < 0.6f) d.grow(1);
                    if (random.nextFloat() < 0.2f) d.grow(1);
                }
                Block.popResource(level, pos, d);
            }
            level.setBlock(pos, replant ? crop.getStateForAge(0) : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        level.playSound(null, origin, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
        tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.causeFoodExhaustion(0.1f);
    }

    /** Ripe crops of the same kind touching the first one, on the same level, up to MAX. */
    private static List<BlockPos> field(ServerLevel level, BlockPos origin, Block kind) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(origin);
        seen.add(origin);
        while (!todo.isEmpty() && found.size() < MAX) {
            BlockPos p = todo.poll();
            BlockState s = level.getBlockState(p);
            if (s.getBlock() != kind || !((CropBlock) kind).isMaxAge(s)) continue;
            found.add(p);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) todo.add(n);
            }
        }
        return found;
    }
}
