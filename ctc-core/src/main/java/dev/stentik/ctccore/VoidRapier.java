package dev.stentik.ctccore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Voidstep Rapier (ctc:voidsteel_rapier, a KubeJS sword): right-click blinks you where you look.
 * Upgrades through chips: every Kinetic Chip +4 blocks of range, every Void Chip a shorter cooldown.
 */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class VoidRapier {
    private VoidRapier() {}

    public static boolean is(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("ctc:voidsteel_rapier");
    }

    public static int range(ItemStack stack) {
        return 10 + 4 * Math.min(ChipType.count(stack, ChipType.KINETIC), ChipType.KINETIC.cap());
    }

    public static int cooldown(ItemStack stack) {
        return 100 - 25 * Math.min(ChipType.count(stack, ChipType.VOID), ChipType.VOID.cap());
    }

    @SubscribeEvent
    public static void use(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!is(stack)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer p) || p.getCooldowns().isOnCooldown(stack.getItem())) return;
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        for (int d = range(stack); d >= 2; d--) {
            Vec3 to = p.position().add(look.scale(d)).add(0, 0.2, 0);
            BlockPos feet = BlockPos.containing(to);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.3, 0.6, 0.3, 0.05);
                p.teleportTo(to.x, to.y, to.z);
                p.resetFallDistance();
                p.getCooldowns().addCooldown(stack.getItem(), cooldown(stack));
                level.sendParticles(ParticleTypes.END_ROD, to.x, to.y + 1, to.z, 20, 0.3, 0.6, 0.3, 0.03);
                level.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);
                return;
            }
        }
    }

    /** Tooltip line: current range and cooldown. */
    public static net.minecraft.network.chat.MutableComponent info(ItemStack stack) {
        return Component.translatable("ctccore.rapier.info", range(stack), String.format(java.util.Locale.ROOT, "%.2f", cooldown(stack) / 20.0));
    }
}
