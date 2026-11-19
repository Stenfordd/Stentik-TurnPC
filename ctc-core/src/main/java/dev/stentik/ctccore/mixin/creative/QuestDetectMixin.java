package dev.stentik.ctccore.mixin.creative;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.stentik.ctccore.CreativeWorld;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** FTB Quests' inventory scan is skipped in the creative world. */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbquests.util.FTBQuestsInventoryListener")
public abstract class QuestDetectMixin {
    @Inject(method = "detect(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;J)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void ctccore$noCreative(ServerPlayer player, ItemStack stack, long id, CallbackInfo ci) {
        if (CreativeWorld.isIn(player)) ci.cancel();
    }
}
