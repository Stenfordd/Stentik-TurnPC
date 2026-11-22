package dev.stentik.ctccore.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import dev.stentik.ctccore.CtcCore;

/**
 * VoidSteel helmet with two crystal horns as real cubes (they stick out of the head, so they cannot be part of a
 * normal armor texture). The horns take their colour from a corner of the armor layer texture (u 56..64, v 0..8),
 * which the plain helmet never uses.
 */
public final class HornedHelmet {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(CtcCore.id("voidsteel_helmet"), "main");

    private HornedHelmet() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(1.0f), 0.0f);
        PartDefinition head = mesh.getRoot().getChild("head");
        PartDefinition l = head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(56, 0).addBox(-1f, -4f, -1f, 2f, 4f, 2f),
                PartPose.offsetAndRotation(3.2f, -8.6f, 0.5f, -0.55f, 0f, 0.4f));
        l.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(56, 0).addBox(-0.5f, -3f, -0.5f, 1f, 3f, 1f),
                PartPose.offsetAndRotation(0f, -4f, 0f, -0.6f, 0f, 0f));
        PartDefinition r = head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(56, 0).addBox(-1f, -4f, -1f, 2f, 4f, 2f),
                PartPose.offsetAndRotation(-3.2f, -8.6f, 0.5f, -0.55f, 0f, -0.4f));
        r.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(56, 0).addBox(-0.5f, -3f, -0.5f, 1f, 3f, 1f),
                PartPose.offsetAndRotation(0f, -4f, 0f, -0.6f, 0f, 0f));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
