package dev.stentik.ctccore;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * Creeper Totem: creepers exploding inside the 16 x 16 chunks around it (8 chunks each way) break no blocks and
 * destroy no dropped items. They still hurt. Right-click shows the edge of the protected area.
 */
public class CreeperTotem extends Block {
    public static final MapCodec<CreeperTotem> CODEC = simpleCodec(CreeperTotem::new);
    static final int RADIUS = 8;   // chunks: [-8, +7] around the totem's chunk = 16 x 16

    public CreeperTotem(Properties p) { super(p); }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (level instanceof ServerLevel sl && !old.is(this)) store(sl).add(pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moving) {
        if (level instanceof ServerLevel sl && !now.is(this)) store(sl).remove(pos);
        super.onRemove(state, level, pos, now, moving);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) {
            ChunkPos c = new ChunkPos(pos);
            int x0 = (c.x - RADIUS) * 16, z0 = (c.z - RADIUS) * 16, x1 = (c.x + RADIUS) * 16, z1 = (c.z + RADIUS) * 16;
            double y = player.getY() + 1.2;
            DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.35f, 0.95f, 0.4f), 2.0f);
            for (int i = x0; i <= x1; i += 2) {
                sl.sendParticles(dust, i, y, z0, 1, 0, 0.4, 0, 0);
                sl.sendParticles(dust, i, y, z1, 1, 0, 0.4, 0, 0);
            }
            for (int k = z0; k <= z1; k += 2) {
                sl.sendParticles(dust, x0, y, k, 1, 0, 0.4, 0, 0);
                sl.sendParticles(dust, x1, y, k, 1, 0, 0.4, 0, 0);
            }
            player.displayClientMessage(Component.translatable("ctccore.creeper_totem.area", x0, x1 - 1, z0, z1 - 1)
                    .withStyle(net.minecraft.ChatFormatting.GREEN), true);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.8f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) == 0)
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 1.05, pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0.02, 0);
    }

    // ---------------------------------------------------------------- where the totems are

    static Store store(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Store::new, Store::load, null), "ctccore_creeper_totems");
    }

    public static final class Store extends SavedData {
        final Set<Long> totems = new HashSet<>();

        void add(BlockPos p) { if (totems.add(p.asLong())) setDirty(); }
        void remove(BlockPos p) { if (totems.remove(p.asLong())) setDirty(); }

        boolean covers(BlockPos at) {
            ChunkPos c = new ChunkPos(at);
            for (long l : totems) {
                ChunkPos t = new ChunkPos(BlockPos.of(l));
                if (c.x >= t.x - RADIUS && c.x < t.x + RADIUS && c.z >= t.z - RADIUS && c.z < t.z + RADIUS) return true;
            }
            return false;
        }

        static Store load(CompoundTag tag, HolderLookup.Provider reg) {
            Store s = new Store();
            for (long l : tag.getLongArray("pos")) s.totems.add(l);
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
            tag.put("pos", new LongArrayTag(totems.stream().mapToLong(Long::longValue).toArray()));
            return tag;
        }
    }

    @EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
    public static final class Events {
        @SubscribeEvent
        public static void explode(ExplosionEvent.Detonate event) {
            if (!(event.getLevel() instanceof ServerLevel level)) return;
            if (!(event.getExplosion().getDirectSourceEntity() instanceof Creeper creeper)) return;
            Store s = store(level);
            if (s.totems.isEmpty() || !s.covers(creeper.blockPosition())) return;
            event.getAffectedBlocks().clear();
            event.getAffectedEntities().removeIf(e -> e instanceof ItemEntity);
        }
    }
}
