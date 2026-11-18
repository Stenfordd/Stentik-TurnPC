package dev.stentik.ctccore;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Planet outposts (structures of this mod) keep one miniboss each, tagged ctc_boss_<planet>.
 * Out there nothing but a player may hurt it (no air, heat, acid...), and it always drops its planet chip.
 */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public class Outposts {
    public static ChipType chipOf(Entity entity) {
        var tags = entity.getTags();
        if (tags.contains("ctc_boss_moon")) return ChipType.GRAVITY;
        if (tags.contains("ctc_boss_mars")) return ChipType.DUST;
        if (tags.contains("ctc_boss_venus")) return ChipType.ACID;
        if (tags.contains("ctc_boss_mercury")) return ChipType.FLARE;
        return null;
    }

    @SubscribeEvent(receiveCanceled = false)
    public static void environment(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (chipOf(target) == null) return;
        if (event.getSource().getEntity() == null) event.setCanceled(true);
    }
}
