package dev.stentik.ctccore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.stentik.ctccore.ChipType;
import dev.stentik.ctccore.Chips;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.PowderSnowBlock;

/** Boots with a Dust Chip walk on powder snow like leather boots. */
@Mixin(PowderSnowBlock.class)
public class PowderSnowMixin {
    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("HEAD"), cancellable = true, require = 0)
    private static void ctc$dustBoots(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity living && Chips.worn(living, EquipmentSlot.FEET, ChipType.DUST) > 0) cir.setReturnValue(true);
    }
}
