package dev.stentik.ctccore;

import java.util.Iterator;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 3.0 tool chips: they change how a tool mines and what the blocks drop, they give the player no effects.
 * Copper faster, Iron saves durability, Ember smelts (can be switched off), Terra more crops and wood, Martian more ore,
 * Kinetic speeds up while you keep mining, Void sends drops into the inventory, Lunar more XP, Mercury/Gravity no
 * slowdown under water / in the air, Dust soft blocks instantly, Acid hard stone like stone, Flare shows nearby ores.
 */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ToolChips {
    private ToolChips() {
    }

    private static Map<ChipType, Integer> chips(ItemStack tool) {
        return Gear.of(tool) == Gear.TOOL ? Chips.levels(tool) : Map.of();
    }

    @SubscribeEvent
    public static void speed(PlayerEvent.BreakSpeed event) {
        Player p = event.getEntity();
        Map<ChipType, Integer> c = chips(p.getMainHandItem());
        if (c.isEmpty()) return;
        BlockState state = event.getState();
        float speed = event.getNewSpeed();
        int n;
        if ((n = c.getOrDefault(ChipType.COPPER, 0)) > 0) speed *= 1 + 0.15f * n;
        if (c.containsKey(ChipType.KINETIC) && p.level().getGameTime() - p.getPersistentData().getLong("ctcMined") < 30)
            speed *= 1 + 0.1f * p.getPersistentData().getInt("ctcMomentum");
        if (c.containsKey(ChipType.MERCURY) && p.isEyeInFluid(FluidTags.WATER)) {
            double sub = p.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
            if (sub > 0 && sub < 1) speed /= (float) sub;
        }
        if (c.containsKey(ChipType.GRAVITY) && !p.onGround()) speed *= 5;
        if (c.containsKey(ChipType.DUST) && (state.is(BlockTags.SAND) || state.is(Tags.Blocks.GRAVELS) || state.is(BlockTags.DIRT) || soft(state)))
            speed = Math.max(speed, 1000f);
        if (c.containsKey(ChipType.ACID) && state.is(BlockTags.MINEABLE_WITH_PICKAXE) && event.getPosition().isPresent()) {
            float h = state.getDestroySpeed(p.level(), event.getPosition().get());
            if (h > 1.5f && h <= 5f) speed *= h / 1.5f;
        }
        event.setNewSpeed(speed);
    }

    private static boolean soft(BlockState s) {
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return path.contains("regolith") || path.endsWith("_sand") || path.contains("dust");
    }

    @SubscribeEvent
    public static void broken(BlockEvent.BreakEvent event) {
        Player p = event.getPlayer();
        ItemStack tool = p.getMainHandItem();
        Map<ChipType, Integer> c = chips(tool);
        if (c.isEmpty() || !(p.level() instanceof ServerLevel level)) return;
        int n;
        var data = p.getPersistentData();
        if ((n = c.getOrDefault(ChipType.KINETIC, 0)) > 0) {
            long now = level.getGameTime();
            int m = now - data.getLong("ctcMined") < 30 ? Math.min(3 * n, data.getInt("ctcMomentum") + 1) : 1;
            data.putInt("ctcMomentum", m);
            data.putLong("ctcMined", now);
        }
        if ((n = c.getOrDefault(ChipType.IRON, 0)) > 0 && tool.isDamaged() && p.getRandom().nextFloat() < 0.2f * n)
            tool.setDamageValue(tool.getDamageValue() - 1);   // the break costs one again right after: no wear
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        if (c.containsKey(ChipType.GRAVITY)) {
            BlockPos up = pos.above();
            for (int i = 0; i < 32 && level.getBlockState(up).getBlock() instanceof FallingBlock; i++, up = up.above())
                level.destroyBlock(up, true, p);
        }
        if ((n = c.getOrDefault(ChipType.FLARE, 0)) > 0 && state.is(Tags.Blocks.ORES) && p instanceof ServerPlayer sp) {
            int r = 4 + 2 * n, shown = 0;
            for (BlockPos q : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r))) {
                if (q.equals(pos) || !level.getBlockState(q).is(Tags.Blocks.ORES)) continue;
                level.sendParticles(sp, ParticleTypes.WAX_ON, true, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 6, 0.35, 0.35, 0.35, 0.0);
                if (++shown >= 48) break;
            }
        }
    }

    /** Runs after the gathering chips' own drop changes (Harvest). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void drops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        Map<ChipType, Integer> c = chips(tool);
        if (c.isEmpty()) return;
        ServerLevel level = event.getLevel();
        BlockState state = event.getState();
        var random = level.getRandom();
        int n;
        // more drops first, so the smelting below counts them too
        if ((n = c.getOrDefault(ChipType.TERRA, 0)) > 0 && (state.getBlock() instanceof CropBlock || state.is(BlockTags.CROPS) || state.is(BlockTags.LOGS)))
            for (ItemEntity d : event.getDrops())
                if (!d.getItem().is(Tags.Items.SEEDS) && random.nextFloat() < 0.15f * n) d.getItem().grow(1);
        if ((n = c.getOrDefault(ChipType.MARTIAN, 0)) > 0 && state.is(Tags.Blocks.ORES))
            for (ItemEntity d : event.getDrops())
                if ((d.getItem().is(Tags.Items.RAW_MATERIALS) || d.getItem().is(Tags.Items.GEMS)) && random.nextFloat() < 0.15f * n) d.getItem().grow(1);
        if ((n = c.getOrDefault(ChipType.LUNAR, 0)) > 0 && event.getDroppedExperience() > 0)
            event.setDroppedExperience(Math.round(event.getDroppedExperience() * (1 + 0.5f * n)));
        if (c.containsKey(ChipType.EMBER) && !GatherEvents.off(tool)) smelt(event, level, state);
        if (c.containsKey(ChipType.VOID) && event.getBreaker() instanceof Player p) {
            for (Iterator<ItemEntity> it = event.getDrops().iterator(); it.hasNext();) {
                ItemStack st = it.next().getItem();
                p.getInventory().add(st);
                if (st.isEmpty()) it.remove();
            }
        }
    }

    /** Ember: ores (with their Fortune drops), raw metal and sand come out smelted; logs come out stripped. */
    private static void smelt(BlockDropsEvent event, ServerLevel level, BlockState state) {
        float xp = 0;
        BlockState stripped = null;
        if (state.is(BlockTags.LOGS) && event.getBreaker() instanceof Player p) {
            try {
                var hit = new BlockHitResult(Vec3.atCenterOf(event.getPos()), Direction.UP, event.getPos(), false);
                stripped = state.getToolModifiedState(new UseOnContext(level, p, InteractionHand.MAIN_HAND, event.getTool(), hit), ItemAbilities.AXE_STRIP, true);
            } catch (RuntimeException ignored) {
                stripped = null;
            }
        }
        for (ItemEntity d : event.getDrops()) {
            ItemStack st = d.getItem();
            if (stripped != null && st.is(state.getBlock().asItem()) && stripped.getBlock().asItem() != net.minecraft.world.item.Items.AIR) {
                d.setItem(new ItemStack(stripped.getBlock().asItem(), st.getCount()));
                continue;
            }
            if (!(st.is(Tags.Items.RAW_MATERIALS) || st.is(Tags.Items.ORES) || st.is(ItemTags.SAND) || st.is(Tags.Items.SANDS))) continue;
            var input = new SingleRecipeInput(st);
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
            if (recipe.isEmpty()) continue;
            ItemStack out = recipe.get().value().assemble(input, level.registryAccess());
            if (out.isEmpty()) continue;
            xp += recipe.get().value().getExperience() * st.getCount();
            d.setItem(out.copyWithCount(Math.min(out.getMaxStackSize(), out.getCount() * st.getCount())));
        }
        int whole = (int) xp;
        if (level.getRandom().nextFloat() < xp - whole) whole++;
        if (whole > 0) event.setDroppedExperience(event.getDroppedExperience() + whole);
        if (xp > 0) level.sendParticles(ParticleTypes.FLAME, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.01);
    }
}
