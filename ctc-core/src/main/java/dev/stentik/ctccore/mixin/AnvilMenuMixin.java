package dev.stentik.ctccore.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;

/** No "Too Expensive!": the anvil never refuses, and a job never costs more than 39 levels. */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Shadow @Final private DataSlot cost;

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40), require = 0)
    private int ctccore$noLimit(int limit) {
        return Integer.MAX_VALUE;
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void ctccore$cap(CallbackInfo ci) {
        if (cost.get() > 39) cost.set(39);
    }
}
