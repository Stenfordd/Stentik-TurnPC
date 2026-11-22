#!/usr/bin/env python3
"""Void Herald (2.0 End boss): writes the Java model (cuboids + UVs) and its two textures from one parts list, so the
UV layout always matches. Look follows David's Gemini reference (tools/../ascended_voidsteel/void_herald.png):
a tall dark Enderman-like figure, crystal horns, an open chest with a floating crystal heart, cracked violet plates.
Run from ctc-core/: python3 tools/gen_void_herald.py"""
import random
from PIL import Image

TEX = 128
random.seed(7)
PLATE, PLATE2, EDGE, SEAM, CRYSTAL, CRYSTAL2, BLACK = (26, 16, 36), (43, 26, 61), (90, 74, 120), (176, 76, 255), (230, 179, 255), (196, 140, 240), (12, 8, 16)

# part, parent, pivot, rotation, boxes: (u, v, x, y, z, w, h, d, style)
PARTS = [
    ("head", "root", (0, -30, 0), (0, 0, 0), [(0, 0, -4, -8, -4, 8, 8, 8, "head")]),
    ("horn_l", "head", (2.5, -8, 0), (-0.5, 0, 0.35), [(32, 0, -1, -4, -1, 2, 4, 2, "horn")]),
    ("horn_l_tip", "horn_l", (0, -4, 0), (-0.6, 0, 0), [(40, 0, -0.5, -3, -0.5, 1, 3, 1, "horn")]),
    ("horn_r", "head", (-2.5, -8, 0), (-0.5, 0, -0.35), [(32, 8, -1, -4, -1, 2, 4, 2, "horn")]),
    ("horn_r_tip", "horn_r", (0, -4, 0), (-0.6, 0, 0), [(40, 8, -0.5, -3, -0.5, 1, 3, 1, "horn")]),
    ("body", "root", (0, -12, 0), (0, 0, 0), [
        (0, 16, -5, -18, -2.5, 10, 6, 5, "chest"),       # upper chest
        (0, 27, -5, -5, -2.5, 10, 5, 5, "plate"),        # belly
        (30, 16, -5, -12, -2.5, 2, 7, 5, "plate"),       # left frame of the chest gap
        (44, 16, 3, -12, -2.5, 2, 7, 5, "plate"),        # right frame
        (0, 37, -7.5, -18.5, -3.5, 5, 4, 7, "shoulder"), # shoulder plates
        (24, 37, 2.5, -18.5, -3.5, 5, 4, 7, "shoulder"),
    ]),
    ("heart", "body", (0, -8.5, 0), (0, 0, 0), [(48, 0, -2, -2, -1, 4, 4, 2, "heart")]),
    ("arm_l", "root", (6.5, -28, 0), (0, 0, 0), [(64, 0, -1.5, -1, -1.5, 3, 34, 3, "limb")]),
    ("arm_r", "root", (-6.5, -28, 0), (0, 0, 0), [(76, 0, -1.5, -1, -1.5, 3, 34, 3, "limb")]),
    ("leg_l", "root", (2.5, -12, 0), (0, 0, 0), [(88, 0, -2, 0, -2, 4, 36, 4, "leg")]),
    ("leg_r", "root", (-2.5, -12, 0), (0, 0, 0), [(104, 0, -2, 0, -2, 4, 36, 4, "leg")]),
]


def faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


tex = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
glow = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
P, G = tex.load(), glow.load()


def fill(rect, col, jitter=6):
    x, y, w, h = [int(round(t)) for t in rect]
    for j in range(max(1, h)):
        for i in range(max(1, w)):
            k = random.randint(-jitter, jitter)
            P[x + i, y + j] = tuple(max(0, min(255, c + k)) for c in col) + (255,)


def border(rect, col, glowing=False):
    x, y, w, h = [int(round(t)) for t in rect]
    for i in range(w):
        for j in (0, h - 1):
            P[x + i, y + j] = col + (255,)
            if glowing: G[x + i, y + j] = col + (255,)
    for j in range(h):
        for i in (0, w - 1):
            P[x + i, y + j] = col + (255,)
            if glowing: G[x + i, y + j] = col + (255,)


for name, parent, pivot, rot, boxes in PARTS:
    for (u, v, x, y, z, w, h, d, style) in boxes:
        w_, h_, d_ = max(1, round(w)), max(1, round(h)), max(1, round(d))
        f = faces(u, v, w_, h_, d_)
        for side, rect in f.items():
            if style == "horn":
                fill(rect, CRYSTAL2 if side in ("left", "back") else CRYSTAL, 10)
            elif style == "heart":
                fill(rect, CRYSTAL, 8)
                rx, ry, rw, rh = rect
                if rw >= 3 and rh >= 3:
                    for j in range(1, rh - 1):
                        for i in range(1, rw - 1):
                            P[rx + i, ry + j] = SEAM + (255,)
                for j in range(rh):
                    for i in range(rw):
                        G[rx + i, ry + j] = P[rx + i, ry + j]
            elif style in ("limb", "leg"):
                fill(rect, BLACK if style == "leg" else PLATE, 4)
                if side in ("front", "back", "left", "right"):
                    rx, ry, rw, rh = rect
                    for band in range(6, rh, 9):          # armour bands down the limb
                        for i in range(rw):
                            P[rx + i, ry + band] = EDGE + (255,)
                    if style == "limb":                   # glowing fingertip seam
                        for i in range(rw):
                            P[rx + i, ry + rh - 2] = SEAM + (255,); G[rx + i, ry + rh - 2] = SEAM + (255,)
            else:
                fill(rect, PLATE2 if style in ("shoulder", "chest") else PLATE, 6)
                border(rect, EDGE)
                if style in ("chest", "shoulder", "plate") and side in ("front", "back", "top"):
                    rx, ry, rw, rh = rect                 # a cracked purple seam across the plate
                    yy = ry + rh // 2
                    for i in range(1, rw - 1):
                        yy2 = yy + (1 if (i // 2) % 2 else 0)
                        if ry < yy2 < ry + rh - 1:
                            P[rx + i, yy2] = SEAM + (255,); G[rx + i, yy2] = SEAM + (255,)
        if style == "head":                               # face: dark visor with a glowing slit
            fx, fy, fw, fh = f["front"]
            for j in range(fh):
                for i in range(fw):
                    P[fx + i, fy + j] = BLACK + (255,)
            for i in range(1, fw - 1):
                P[fx + i, fy + 3] = SEAM + (255,); G[fx + i, fy + 3] = SEAM + (255,)
                P[fx + i, fy + 4] = CRYSTAL + (255,); G[fx + i, fy + 4] = CRYSTAL + (255,)

R = "src/main/resources/assets/ctccore/textures/entity/"
import os
os.makedirs(R, exist_ok=True)
tex.save(R + "void_herald.png")
glow.save(R + "void_herald_glow.png")


def jf(x):
    return f"{float(x)}f"


lines = []
for name, parent, pivot, rot, boxes in PARTS:
    cb = "CubeListBuilder.create()"
    for (u, v, x, y, z, w, h, d, style) in boxes:
        cb += f".texOffs({u}, {v}).addBox({jf(x)}, {jf(y)}, {jf(z)}, {jf(w)}, {jf(h)}, {jf(d)})"
    pose = f"PartPose.offsetAndRotation({jf(pivot[0])}, {jf(pivot[1])}, {jf(pivot[2])}, {jf(rot[0])}, {jf(rot[1])}, {jf(rot[2])})"
    lines.append(f'        PartDefinition {name} = {parent}.addOrReplaceChild("{name}", {cb}, {pose});')
body = "\n".join(lines)

java = f'''package dev.stentik.ctccore.client;

import dev.stentik.ctccore.VoidHerald;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Generated by tools/gen_void_herald.py (cuboids + UVs come from the same list as the texture). */
public class VoidHeraldModel extends HierarchicalModel<VoidHerald> {{
    private final ModelPart root, head, heart, armL, armR, legL, legR;

    public VoidHeraldModel(ModelPart root) {{
        this.root = root;
        this.head = root.getChild("head");
        this.heart = root.getChild("body").getChild("heart");
        this.armL = root.getChild("arm_l");
        this.armR = root.getChild("arm_r");
        this.legL = root.getChild("leg_l");
        this.legR = root.getChild("leg_r");
    }}

    public static LayerDefinition createBodyLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{body}
        return LayerDefinition.create(mesh, {TEX}, {TEX});
    }}

    @Override
    public ModelPart root() {{
        return root;
    }}

    @Override
    public void setupAnim(VoidHerald e, float limbSwing, float amount, float age, float yaw, float pitch) {{
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        float swing = Mth.cos(limbSwing * 0.45f) * 0.7f * amount;
        legL.xRot = swing;
        legR.xRot = -swing;
        armL.xRot = -swing * 0.8f;
        armR.xRot = swing * 0.8f;
        armL.zRot = -0.06f - Mth.sin(age * 0.07f) * 0.04f;
        armR.zRot = 0.06f + Mth.sin(age * 0.07f) * 0.04f;
        if (e.isCasting()) {{ armL.xRot = -2.4f; armR.xRot = -2.4f; }}
        heart.yRot = age * 0.08f;
        heart.y = -8.5f + Mth.sin(age * 0.1f) * 0.6f;
    }}
}}
'''
J = "src/main/java/dev/stentik/ctccore/client/"
os.makedirs(J, exist_ok=True)
open(J + "VoidHeraldModel.java", "w").write(java)
print("void herald model + textures written")
