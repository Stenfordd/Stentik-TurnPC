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
    private static boolean released;
    private static int used;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.screen != null) return;
        boolean down = mc.options.keyJump.isDown();
        if (player.onGround() || player.isInWater() || player.getAbilities().flying || player.isPassenger() || player.isFallFlying()) {
            released = false;
            used = 0;
            return;
        }
        if (!down) {
            released = true;
            return;
        }
        int lvl = Chips.worn(player, net.minecraft.world.entity.EquipmentSlot.FEET, ChipType.KINETIC);
        if (released && used < (lvl >= 3 ? 2 : 1) && lvl > 0) {
            used++;
            released = false;   // the second dash needs another press
            PacketDistributor.sendToServer(DashPayload.INSTANCE);
        }
    }
}
