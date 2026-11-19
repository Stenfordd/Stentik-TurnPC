package dev.stentik.ctccore.mixin.creative;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.stentik.ctccore.CreativeWorld;
import net.minecraft.world.entity.LivingEntity;

/** Kills (bosses from spawn eggs) in the creative world don't count for quests. */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbquests.quest.task.KillTask")
public abstract class QuestKillMixin {
    @Inject(method = "kill(Ldev/ftb/mods/ftbquests/quest/TeamData;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void ctccore$noCreative(@Coerce Object team, LivingEntity entity, CallbackInfo ci) {
        if (CreativeWorld.isIn(entity)) ci.cancel();
    }
}
