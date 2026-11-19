package dev.stentik.ctccore.mixin.creative;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.stentik.ctccore.CreativeWorld;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** FTB Quests tasks don't count in the creative world (items, XP, stats, checkmarks...). */
@Pseudo
@Mixin(targets = {
        "dev.ftb.mods.ftbquests.quest.task.Task",
        "dev.ftb.mods.ftbquests.quest.task.ItemTask",
        "dev.ftb.mods.ftbquests.quest.task.AbstractBooleanTask",
        "dev.ftb.mods.ftbquests.quest.task.CustomTask",
        "dev.ftb.mods.ftbquests.quest.task.StatTask",
        "dev.ftb.mods.ftbquests.quest.task.XPTask"
})
public abstract class QuestTaskMixin {
    @Inject(method = "submitTask(Ldev/ftb/mods/ftbquests/quest/TeamData;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void ctccore$noCreative(@Coerce Object team, ServerPlayer player, ItemStack stack, CallbackInfo ci) {
        if (CreativeWorld.isIn(player)) ci.cancel();
    }
}
