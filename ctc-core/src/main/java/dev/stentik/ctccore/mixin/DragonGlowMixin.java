package dev.stentik.ctccore.mixin;

import java.lang.reflect.Field;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.stentik.ctccore.CtcCore;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/**
 * Ender Dragon, second half: glowing Void/Gravity veins over its wings and body (the Wither gets a render layer,
 * the dragon's renderer has no layers, so this draws once more right after the model, before the pose is popped).
 * The model is found by type, not by field name, and any failure only skips the glow.
 */
@Mixin(EnderDragonRenderer.class)
public abstract class DragonGlowMixin {
    @Unique private static RenderType ctccore$glow;
    @Unique private static Field ctccore$model;
    @Unique private static boolean ctccore$failed;

    @Inject(method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0), require = 0)
    private void ctccore$phaseGlow(EnderDragon dragon, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light, CallbackInfo ci) {
        if (ctccore$failed || dragon.dragonDeathTime > 0 || dragon.getHealth() > dragon.getMaxHealth() * 0.5f) return;
        try {
            if (ctccore$model == null) {
                for (Field f : EnderDragonRenderer.class.getDeclaredFields())
                    if (EntityModel.class.isAssignableFrom(f.getType())) { f.setAccessible(true); ctccore$model = f; break; }
                if (ctccore$model == null) { ctccore$failed = true; return; }
            }
            if (ctccore$glow == null)
                ctccore$glow = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(CtcCore.MODID, "textures/entity/elite/dragon_glow.png"));
            EntityModel<?> model = (EntityModel<?>) ctccore$model.get(this);
            // a slow pulse, brighter right after the switch to the second half
            float t = (dragon.tickCount + partial) * 0.08f;
            int a = (int) (170 + 85 * Mth.sin(t));
            int argb = (a << 24) | (a << 16) | (a << 8) | a;
            model.renderToBuffer(pose, buf.getBuffer(ctccore$glow), 0xF000F0, OverlayTexture.NO_OVERLAY, argb);
        } catch (Throwable e) {
            ctccore$failed = true;
        }
    }
}
