package dev.stentik.ctccore.client;

import dev.stentik.ctccore.CtcCore;
import dev.stentik.ctccore.VoidHerald;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

public class VoidHeraldRenderer extends MobRenderer<VoidHerald, VoidHeraldModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(CtcCore.id("void_herald"), "main");
    private static final ResourceLocation TEX = CtcCore.id("textures/entity/void_herald.png");
    private static final RenderType GLOW = RenderType.eyes(CtcCore.id("textures/entity/void_herald_glow.png"));

    public VoidHeraldRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VoidHeraldModel(ctx.bakeLayer(LAYER)), 1.0f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(VoidHerald e) {
        return TEX;
    }
}
