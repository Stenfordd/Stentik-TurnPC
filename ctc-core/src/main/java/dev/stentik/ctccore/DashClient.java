package dev.stentik.ctccore;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Watches the jump key: pressed again while in the air (and released since leaving the ground) = dash. */
@EventBusSubscriber(modid = CtcCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class DashClient {
    private static boolean released, used;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.screen != null) return;
        boolean down = mc.options.keyJump.isDown();
        if (player.onGround() || player.isInWater() || player.getAbilities().flying || player.isPassenger() || player.isFallFlying()) {
            released = false;
            used = false;
            return;
        }
        if (!down) {
            released = true;
            return;
        }
        if (released && !used && ChipEvents.armorChips(player).contains(ChipType.KINETIC)) {
            used = true;
            PacketDistributor.sendToServer(DashPayload.INSTANCE);
        }
    }
}
