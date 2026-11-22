#!/usr/bin/env python3
"""3D armor (2.0): extra cubes that stick out of the armor (pauldrons, breastplate, knee guards, crests...) for
steel, titanium, tungsten and VoidSteel. One spec -> client/ArmorModels.java + textures.
The armor layers become 64x64 units (256x256 px): the top half is the normal armor layout (tools/art/armor),
the bottom half holds the extra cubes, painted from Gemini's front view of the same spot (with bevel shading).
Run from ctc-core/: python3 tools/gen_armor3d.py <gemini dir>"""
import os, sys
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from hires_sprites import matte

G = sys.argv[1]
S = 4
ART = 'tools/art/armor/'
OUT = 'src/main/resources/assets/ctccore/textures/models/armor/'
REF = {'voidsteel': f'{G}/worn_front_back.png', 'steel': f'{G}/armor_tiers/steel_front_back.png',
       'titanium': f'{G}/armor_tiers/titanium_front_back.png', 'tungsten': f'{G}/armor_tiers/tungsten_front_back.png'}

# cube: (name, part, layer 1|2, (x, y, z, w, h, d), source patch in front-view units (ux, uy, uw, uh), mirror)
def pauldrons(big=True, second=False):
    w, h, d = (6.0, 3.4, 7.2) if big else (5.4, 2.8, 6.6)
    out = [('pauldron_r', 'right_arm', 1, (-4.9, -3.8, -d / 2, w, h, d), (0, 8, 4, 3), False),
           ('pauldron_l', 'left_arm', 1, (-1.1, -3.8, -d / 2, w, h, d), (12, 8, 4, 3), True)]
    if second:
        out += [('pauldron2_r', 'right_arm', 1, (-4.6, -0.6, -3.4, 5.2, 2.0, 6.8), (0, 10, 4, 2), False),
                ('pauldron2_l', 'left_arm', 1, (-0.6, -0.6, -3.4, 5.2, 2.0, 6.8), (12, 10, 4, 2), True)]
    return out

KNEES = [('knee_r', 'right_leg', 2, (-2.3, 4.2, -3.3, 4.6, 3.0, 0.9), (4, 24, 4, 3), False),
         ('knee_l', 'left_leg', 2, (-2.3, 4.2, -3.3, 4.6, 3.0, 0.9), (8, 24, 4, 3), True)]
BREAST = ('breast', 'body', 1, (-3.6, 0.6, -3.9, 7.2, 6.4, 0.9), (4.4, 8.6, 7.2, 6.4), False)
CREST = ('crest', 'head', 1, (-0.7, -10.8, -4.6, 1.4, 2.0, 9.2), (7.3, 0, 1.4, 2), False)
BRIM = ('brim', 'head', 1, (-4.7, -4.9, -5.9, 9.4, 1.1, 0.9), (4, 3, 8, 1.1), False)
CUFFS = [('cuff_r', 'right_leg', 1, (-3.3, 8.0, -3.3, 6.6, 1.3, 6.6), (4, 28, 4, 1.3), False),
         ('cuff_l', 'left_leg', 1, (-3.3, 8.0, -3.3, 6.6, 1.3, 6.6), (8, 28, 4, 1.3), True)]
GEM = ('gem', 'body', 1, (-1.1, 2.0, -4.7, 2.2, 3.0, 0.9), (7, 10.4, 2, 3), False)

TIERS = {
    'steel': pauldrons(big=False, second=True) + [('ridge', 'body', 1, (-1.0, 0.8, -3.9, 2.0, 8.5, 0.9), (7, 9, 2, 8.5), False), CREST] + KNEES,
    'titanium': pauldrons(big=True) + [BREAST, BRIM] + KNEES,
    'tungsten': pauldrons(big=True, second=True) + [BREAST, CREST] + KNEES + CUFFS,
    'voidsteel': pauldrons(big=True) + [BREAST, GEM] + KNEES,
}

def front_figure(path):
    from scipy import ndimage
    a = matte(np.array(Image.open(path).convert('RGBA')))
    op = a[..., 3] > 128
    lab, k = ndimage.label(ndimage.binary_dilation(op, iterations=6))
    sizes = ndimage.sum(op, lab, range(1, k + 1))
    keep = sorted(np.argsort(sizes)[::-1][:2] + 1, key=lambda i: np.nonzero(lab == i)[1].mean())
    ys, xs = np.nonzero((lab == keep[0]) & op)
    x0, y0, x1, y1 = xs.min(), ys.min(), xs.max() + 1, ys.max() + 1
    # horns above the head (VoidSteel): start the figure where it is 6+ units wide
    est = (y1 - y0) / 32
    w = op[y0:y1, x0:x1].sum(1)
    for yy in range(len(w)):
        if w[yy] > 6 * est: y0 += yy; break
    return a, (x0, y0, x1, y1)

def patch(fig, ux, uy, uw, uh, tw, th):
    a, (x0, y0, x1, y1) = fig
    ppu = (y1 - y0) / 32
    fx0 = (x0 + x1) / 2 - 8 * ppu
    crop = Image.fromarray(a).crop((int(fx0 + ux * ppu), int(y0 + uy * ppu), int(fx0 + (ux + uw) * ppu), int(y0 + (uy + uh) * ppu)))
    p = np.array(crop.resize((max(1, tw), max(1, th)), Image.BOX)).astype(float)
    # solid plates: fill holes with the mean colour
    op = p[..., 3] > 100
    if op.any():
        mean = p[op][:, :3].mean(0)
        p[~op, :3] = mean
    p[..., 3] = 255
    return p

def shade(p, f): q = p.copy(); q[..., :3] = np.clip(q[..., :3] * f, 0, 255); return q

def bevel(p):
    q = p.copy()
    q[0, :, :3] = np.clip(q[0, :, :3] * 1.25 + 10, 0, 255)
    q[:, 0, :3] = np.clip(q[:, 0, :3] * 1.12, 0, 255)
    q[-1, :, :3] *= 0.6
    q[:, -1, :3] *= 0.75
    return q

def faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}

import math
java_parts = {}
for tier, cubes in TIERS.items():
    fig = front_figure(REF[tier])
    tex = {k: np.zeros((64 * S, 64 * S, 4), np.uint8) for k in (1, 2)}
    for k in (1, 2):
        top = np.array(Image.open(f'{ART}{tier}_layer_{k}.png').convert('RGBA').resize((64 * S, 32 * S), Image.NEAREST))
        tex[k][:32 * S] = top
    cursor = {1: [0, 32], 2: [0, 32]}
    rowh = {1: 0, 2: 0}
    order = sorted(cubes, key=lambda c: (-(max(1, round(c[3][4])) + max(1, round(c[3][5]))), c[0].endswith('_l')))
    placed = {}
    for name, part, layer, (x, y, z, w, h, d), src, mirror in order:
        wu, hu, du = max(1, round(w)), max(1, round(h)), max(1, round(d))
        twin = name[:-2] + '_r' if name.endswith('_l') else None
        if twin in placed:                    # left piece: same texture as the right one, mirrored in the model
            u, v = placed[twin]
            java_parts.setdefault(tier, []).append((name, part, layer, u, v, (x, y, z, w, h, d), True))
            continue
        span = 2 * (wu + du)
        if cursor[layer][0] + span > 64:
            cursor[layer][0] = 0; cursor[layer][1] += rowh[layer]; rowh[layer] = 0
        u, v = cursor[layer]
        placed[name] = (u, v)
        cursor[layer][0] += span; rowh[layer] = max(rowh[layer], hu + du)
        assert v + hu + du <= 64, (tier, name)
        fr = patch(fig, *src, wu * S, hu * S)
        if mirror: fr = fr[:, ::-1]
        f = faces(u, v, wu, hu, du)
        def put(rect, arr):
            uu, vv, ww, hh = rect
            img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)).resize((ww * S, hh * S), Image.BOX)
            tex[layer][vv * S:(vv + hh) * S, uu * S:(uu + ww) * S] = np.array(img)
        put(f['front'], bevel(fr))
        put(f['back'], shade(fr, 0.55))
        # the other faces: the same plate resized to fit, lit from above (top brighter, sides darker, bottom darkest)
        put(f['top'], bevel(shade(fr, 1.15)))
        put(f['bottom'], shade(fr, 0.5))
        put(f['right'], bevel(shade(fr, 0.8)))
        put(f['left'], bevel(shade(fr, 0.8)))
        java_parts.setdefault(tier, []).append((name, part, layer, u, v, (x, y, z, w, h, d), False))
    for k in (1, 2):
        Image.fromarray(tex[k]).save(f'{OUT}{tier}_layer_{k}.png')

def jf(x): return f'{float(x)}f'
blocks = []
for tier, parts in java_parts.items():
    for layer in (1, 2):
        lines = []
        for name, part, lay, u, v, (x, y, z, w, h, d), mir in parts:
            if lay != layer: continue
            mm = '.mirror()' if mir else ''
            W, H, Dd = max(1, round(w)), max(1, round(h)), max(1, round(d))
            gx, gy, gz = (w - W) / 2, (h - H) / 2, (d - Dd) / 2      # integer UV size, exact visual size by deformation
            lines.append(f'            root.getChild("{part}").addOrReplaceChild("{name}", CubeListBuilder.create().texOffs({u}, {v}){mm}'
                         f'.addBox({jf(round(x + gx, 3))}, {jf(round(y + gy, 3))}, {jf(round(z + gz, 3))}, {jf(W)}, {jf(H)}, {jf(Dd)}, '
                         f'new CubeDeformation({jf(round(gx, 3))}, {jf(round(gy, 3))}, {jf(round(gz, 3))})), PartPose.ZERO);')
        blocks.append(f'        case "{tier}_{layer}" -> {{\n' + '\n'.join(lines) + '\n        }')
java = '''package dev.stentik.ctccore.client;

import dev.stentik.ctccore.CtcCore;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Generated by tools/gen_armor3d.py: armor with parts that stick out (pauldrons, breastplates, knee guards, crests).
 *  Layer 1 = helmet, chestplate and boots (outer), layer 2 = leggings (inner). Textures are 64x64 units. */
public final class ArmorModels {
    private ArmorModels() {}

    public static final String[] TIERS = { "steel", "titanium", "tungsten", "voidsteel" };

    public static ModelLayerLocation layer(String tier, int layer) {
        return new ModelLayerLocation(CtcCore.id(tier + "_armor"), "layer" + layer);
    }

    public static LayerDefinition create(String tier, int layer) {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(layer == 1 ? 1.0f : 0.5f), 0.0f);
        PartDefinition root = mesh.getRoot();
        switch (tier + "_" + layer) {
''' + '\n'.join(blocks) + '''
            default -> { }
        }
        if (tier.equals("voidsteel") && layer == 1) {   // crystal horns (texture corner u 56..64, v 0..8)
            PartDefinition head = root.getChild("head");
            PartDefinition l = head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(56, 0).addBox(-1f, -4f, -1f, 2f, 4f, 2f),
                    PartPose.offsetAndRotation(3.2f, -8.6f, 0.5f, -0.55f, 0f, 0.4f));
            l.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(56, 0).addBox(-0.5f, -3f, -0.5f, 1f, 3f, 1f),
                    PartPose.offsetAndRotation(0f, -4f, 0f, -0.6f, 0f, 0f));
            PartDefinition r = head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(56, 0).addBox(-1f, -4f, -1f, 2f, 4f, 2f),
                    PartPose.offsetAndRotation(-3.2f, -8.6f, 0.5f, -0.55f, 0f, -0.4f));
            r.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(56, 0).addBox(-0.5f, -3f, -0.5f, 1f, 3f, 1f),
                    PartPose.offsetAndRotation(0f, -4f, 0f, -0.6f, 0f, 0f));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }
}
'''
open('src/main/java/dev/stentik/ctccore/client/ArmorModels.java', 'w').write(java)
print('armor 3d ok')
