package dev.stentik.ctccore.mixin.creative;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.stentik.ctccore.CreativeWorld;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Quest rewards can't be claimed in the creative world (they would land in the creative inventory and be lost). */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbquests.quest.TeamData")
public abstract class QuestRewardMixin {
    @Inject(method = "claimReward(Lnet/minecraft/server/level/ServerPlayer;Ldev/ftb/mods/ftbquests/quest/reward/Reward;Z)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void ctccore$noCreative(ServerPlayer player, @Coerce Object reward, boolean notify, CallbackInfo ci) {
        if (CreativeWorld.isIn(player)) {
            player.sendSystemMessage(Component.literal("§cOdměny si vyzvedni až v survivalu (/ctc survival)."));
            ci.cancel();
        }
    }

    @Inject(method = "claimReward(Lnet/minecraft/server/level/ServerPlayer;Ldev/ftb/mods/ftbquests/quest/reward/Reward;ZJ)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void ctccore$noCreativeAt(ServerPlayer player, @Coerce Object reward, boolean notify, long at, CallbackInfo ci) {
        if (CreativeWorld.isIn(player)) ci.cancel();
    }
}
