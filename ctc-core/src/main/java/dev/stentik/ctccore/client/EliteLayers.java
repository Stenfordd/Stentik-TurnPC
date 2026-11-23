package dev.stentik.ctccore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.stentik.ctccore.ChipType;
import dev.stentik.ctccore.CtcCore;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Elite look: the vanilla mob stays as it is, a chip, glowing eyes and circuit lines are drawn over it
 * (frame lit normally, glow full-bright and tinted with the chip colour; several chips cycle the colour).
 * The client knows the chips from the elite's name ("Ember Kinetic Zombie"), which the server sets.
 * The Wither gets its own pre-coloured layer below half health.
 */
@EventBusSubscriber(modid = CtcCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class EliteLayers {
    private EliteLayers() {}
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    static ResourceLocation tex(String n) {
        return ResourceLocation.fromNamespaceAndPath(CtcCore.MODID, "textures/entity/elite/" + n + ".png");
    }

    /** Chips of an elite, read from its name; empty for everything else. */
    static List<ChipType> chips(LivingEntity e) {
        List<ChipType> out = new ArrayList<>();
        if (e.getCustomName() == null) return out;
        for (String w : e.getCustomName().getString().split(" ")) {
            ChipType c = ChipType.byId(w.toLowerCase(java.util.Locale.ROOT));
            if (c == null) break;   // the chip words come first, then the mob's name
            if (!out.contains(c)) out.add(c);
        }
        return out;
    }

    static class Elite<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
        final RenderType frame, glow;

        Elite(RenderLayerParent<T, M> parent, String kind) {
            super(parent);
            frame = RenderType.entityCutoutNoCull(tex(kind + "_frame"));
            glow = RenderType.eyes(tex(kind + "_glow"));
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buf, int light, T e, float limb, float limbAmount, float partial,
                           float age, float headYaw, float headPitch) {
            List<ChipType> chips = chips(e);
            if (chips.isEmpty() || e.isInvisible()) return;
            int color = chips.get((int) (age / 30) % chips.size()).color;
            // a slow pulse so it reads as alive
            float k = 0.8f + 0.2f * (float) Math.sin(age * 0.12f);
            int r = (int) ((color >> 16 & 255) * k), g = (int) ((color >> 8 & 255) * k), b = (int) ((color & 255) * k);
            getParentModel().renderToBuffer(pose, buf.getBuffer(frame), light, LivingEntityRenderer.getOverlayCoords(e, 0), 0xFFFFFFFF);
            getParentModel().renderToBuffer(pose, buf.getBuffer(glow), 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFF000000 | r << 16 | g << 8 | b);
        }
    }

    static class WitherPhase<M extends EntityModel<WitherBoss>> extends RenderLayer<WitherBoss, M> {
        static final RenderType FRAME = RenderType.entityCutoutNoCull(tex("wither_frame")), GLOW = RenderType.eyes(tex("wither_glow"));

        WitherPhase(RenderLayerParent<WitherBoss, M> parent) { super(parent); }

        @Override
        public void render(PoseStack pose, MultiBufferSource buf, int light, WitherBoss w, float limb, float limbAmount, float partial,
                           float age, float headYaw, float headPitch) {
            if (w.getInvulnerableTicks() > 0 || w.getHealth() > w.getMaxHealth() * 0.5f) return;
            getParentModel().renderToBuffer(pose, buf.getBuffer(FRAME), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            getParentModel().renderToBuffer(pose, buf.getBuffer(GLOW), 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void add(EntityRenderersEvent.AddLayers event, EntityType<?> type, String kind) {
        try {
            if (event.getRenderer((EntityType) type) instanceof LivingEntityRenderer r) r.addLayer(new Elite(r, kind));
        } catch (Throwable t) {
            LOG.warn("CtC: no elite layer for {}: {}", type, t.toString());
        }
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void layers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> t : List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED)) add(event, t, "zombie");
        for (EntityType<?> t : List.of(EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.BOGGED)) add(event, t, "skeleton");
        add(event, EntityType.CREEPER, "creeper");
        add(event, EntityType.ENDERMAN, "enderman");
        try {
            if (event.getRenderer(EntityType.WITHER) instanceof LivingEntityRenderer r) r.addLayer(new WitherPhase(r));
        } catch (Throwable t) {
            LOG.warn("CtC: no wither phase layer: {}", t.toString());
        }
    }
}
