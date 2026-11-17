package dev.stentik.ctccore;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Kinetic Chip in armor: jumping again in mid-air dashes forward (4 s cooldown). The client asks, the server checks and moves. */
public record DashPayload() implements CustomPacketPayload {
    public static final Type<DashPayload> TYPE = new Type<>(CtcCore.id("dash"));
    public static final DashPayload INSTANCE = new DashPayload();
    public static final StreamCodec<ByteBuf, DashPayload> CODEC = StreamCodec.unit(INSTANCE);
    public static final int COOLDOWN = 80;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!ChipEvents.armorChips(player).contains(ChipType.KINETIC) || player.onGround() || player.isPassenger()) return;
            long now = player.level().getGameTime();
            long last = player.getPersistentData().getLong("ctcDash");
            if (now - last < COOLDOWN) return;
            player.getPersistentData().putLong("ctcDash", now);
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            if (flat.lengthSqr() < 1.0e-4) flat = new Vec3(0, 0, 1);
            flat = flat.normalize().scale(1.25);
            player.setDeltaMovement(flat.x, 0.42, flat.z);
            player.hurtMarked = true;
            player.fallDistance = 0;
            ServerLevel level = player.serverLevel();
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 12, 0.3, 0.1, 0.3, 0.02);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.8, player.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.6f, 1.7f);
        });
    }
}
