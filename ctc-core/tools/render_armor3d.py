#!/usr/bin/env python3
"""Rough 3D preview of an armor set (player model + armor boxes + the ArmorModels extra cubes), several angles.
render_armor3d.py out.png tier [tier...]   (reads textures from resources and cubes from client/ArmorModels.java)"""
import math, re, sys
import numpy as np
from PIL import Image, ImageDraw
RES = 1
TEX = 'src/main/resources/assets/ctccore/textures/models/armor/'
J = open('src/main/java/dev/stentik/ctccore/client/ArmorModels.java').read()
PIV = {'head': (0, 0, 0), 'body': (0, 0, 0), 'right_arm': (-5, 2, 0), 'left_arm': (5, 2, 0), 'right_leg': (-1.9, 12, 0), 'left_leg': (1.9, 12, 0)}
BASE = {'head': (0, 0, -4, -8, -4, 8, 8, 8, False), 'body': (16, 16, -4, 0, -2, 8, 12, 4, False),
        'right_arm': (40, 16, -3, -2, -2, 4, 12, 4, False), 'left_arm': (40, 16, -1, -2, -2, 4, 12, 4, True),
        'right_leg': (0, 16, -2, 0, -2, 4, 12, 4, False), 'left_leg': (0, 16, -2, 0, -2, 4, 12, 4, True)}

def extras(tier, layer):
    m = re.search(r'case "%s_%d" -> \{(.*?)\n        \}' % (tier, layer), J, re.S)
    out = []
    if not m: return out
    for line in m.group(1).splitlines():
        r = re.search(r'getChild\("(\w+)"\).*texOffs\((\d+), (\d+)\)(\.mirror\(\))?\.addBox\(([-\d.]+)f, ([-\d.]+)f, ([-\d.]+)f, ([\d.]+)f, ([\d.]+)f, ([\d.]+)f, new CubeDeformation\(([-\d.]+)f(?:, ([-\d.]+)f, ([-\d.]+)f)?\)', line)
        if r:
            g = r.groups()
            gr = [float(g[10])] * 3 if g[11] is None else [float(x) for x in g[10:13]]
            out.append((g[0], int(g[1]), int(g[2]), [float(x) for x in g[4:10]], gr, bool(g[3])))
    return out

def cube_faces(u, v, x, y, z, w, h, d, grow, mirror):
    """Yields (corners[4] in 3D as (a,b)->point function, uv rect, flip_u)."""
    gx, gy, gz = grow
    x0, y0, z0, x1, y1, z1 = x - gx, y - gy, z - gz, x + w + gx, y + h + gy, z + d + gz
    W, H, D = w, h, d
    F = []
    # (uv rect (u, v, du, dv)), point(s, t) for s,t in [0,1] over the texture rect
    F.append(((u + D, v + D, W, H), lambda s, t: (x0 + s * (x1 - x0), y0 + t * (y1 - y0), z0)))            # north / front
    F.append(((u + 2 * D + W, v + D, W, H), lambda s, t: (x1 - s * (x1 - x0), y0 + t * (y1 - y0), z1)))    # south / back
    F.append(((u, v + D, D, H), lambda s, t: (x0, y0 + t * (y1 - y0), z1 - s * (z1 - z0))))                # west (-x)
    F.append(((u + D + W, v + D, D, H), lambda s, t: (x1, y0 + t * (y1 - y0), z0 + s * (z1 - z0))))        # east (+x)
    F.append(((u + D, v, W, D), lambda s, t: (x0 + s * (x1 - x0), y0, z1 - t * (z1 - z0))))                # up
    F.append(((u + D + W, v, W, D), lambda s, t: (x0 + s * (x1 - x0), y1, z0 + t * (z1 - z0))))            # down
    return F

def render(tier, yaw, pitch, scale=7):
    quads = []
    ims = {k: Image.open(f'{TEX}{tier}_layer_{k}.png').convert('RGBA') for k in (1, 2)}
    tw = {k: ims[k].width / 64 for k in (1, 2)}
    th = {k: ims[k].height / (64 if ims[k].height == ims[k].width else 32) for k in (1, 2)}
    px = {k: np.array(ims[k]) for k in (1, 2)}
    cy, sy, cp, sp = math.cos(yaw), math.sin(yaw), math.cos(pitch), math.sin(pitch)
    def proj(p):
        x, y, z = p
        x2, z2 = x * cy - z * sy, x * sy + z * cy
        y2, z3 = y * cp - z2 * sp, y * sp + z2 * cp
        return x2, y2, z3
    boxes = []
    for part in PIV:      # grey body underneath
        u, v, x, y, z, w, h, d, m = BASE[part]
        boxes.append((None, part, u, v, (x, y, z, w, h, d), (0, 0, 0), m))
    for layer, parts, g in ((2, ['body', 'right_leg', 'left_leg'], 0.5), (1, list(PIV), 1.0)):
        for part in parts:
            u, v, x, y, z, w, h, d, m = BASE[part]
            boxes.append((layer, part, u, v, (x, y, z, w, h, d), (g, g, g), m))
        for part, u, v, (x, y, z, w, h, d), grow, m in extras(tier, layer):
            boxes.append((layer, part, u, v, (x, y, z, w, h, d), tuple(grow), m))
    for layer, part, u, v, (x, y, z, w, h, d), grow, mirror in boxes:
        P = PIV[part]
        if layer == 1 and part in ('right_leg', 'left_leg'):   # boots only: lower third of the leg
            pass
        for (fu, fv, fw, fh), pt in cube_faces(u, v, x, y, z, w, h, d, grow, mirror):
            n = max(1, int(round(fw * RES))), max(1, int(round(fh * RES)))
            for i in range(n[0]):
                for j in range(n[1]):
                    s0, s1, t0, t1 = i / n[0], (i + 1) / n[0], j / n[1], (j + 1) / n[1]
                    if layer is None:
                        col = (150, 120, 100, 255)
                    else:
                        tu = fu + (fw - (i + 0.5) / RES if mirror else (i + 0.5) / RES)
                        tv = fv + (j + 0.5) / RES
                        a = px[layer]
                        X, Y = int(tu * tw[layer]), int(tv * th[layer])
                        if not (0 <= X < a.shape[1] and 0 <= Y < a.shape[0]): continue
                        col = tuple(int(c) for c in a[Y, X])
                        if col[3] < 128: continue
                    cs = [proj(tuple(P[k] + q[k] for k in range(3))) for q in (pt(s0, t0), pt(s1, t0), pt(s1, t1), pt(s0, t1))]
                    depth = sum(c[2] for c in cs) / 4 + (0 if layer is None else -0.01 * (3 - (layer or 0)))
                    quads.append((depth, [(c[0], c[1]) for c in cs], col))
    quads.sort(key=lambda q: -q[0])
    img = Image.new('RGBA', (int(28 * scale), int(44 * scale)), (60, 64, 76, 255)); dr = ImageDraw.Draw(img)
    for _, cs, col in quads:
        dr.polygon([((x + 14) * scale, (y + 11) * scale) for x, y in cs], fill=col[:3] + (255,))
    return img

tiers = sys.argv[2:]
views = [(0.0, 0.0), (0.6, 0.25), (-0.9, 0.2), (math.pi, 0.0)]
tiles = [render(t, yw, pt) for t in tiers for yw, pt in views]
W = tiles[0].width
out = Image.new('RGBA', (W * len(views), tiles[0].height * len(tiers)), (40, 40, 48, 255))
for k, im in enumerate(tiles): out.alpha_composite(im, ((k % len(views)) * W, (k // len(views)) * im.height))
out.save(sys.argv[1])
