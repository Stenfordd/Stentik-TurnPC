package dev.stentik.ctccore.mixin.creative;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.stentik.ctccore.CreativeWorld;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

/** No advancements in the creative world (the hidden quest advancements included). */
@Mixin(PlayerAdvancements.class)
public abstract class AdvancementsMixin {
    @Shadow private ServerPlayer player;

    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private void ctccore$noCreative(AdvancementHolder adv, String criterion, CallbackInfoReturnable<Boolean> cir) {
        if (CreativeWorld.isIn(player)) cir.setReturnValue(false);
    }
}
