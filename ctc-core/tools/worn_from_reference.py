"""Project a front+back Gemini reference of a Minecraft player in armor onto 128x64 armor layer textures."""
import sys, colorsys
from PIL import Image
import numpy as np
from collections import Counter
from scipy import ndimage
src, name = sys.argv[1], sys.argv[2]; S = int(sys.argv[3]) if len(sys.argv) > 3 else 2           # texels per model unit
im = np.array(Image.open(src).convert('RGB')).astype(int); H, W, _ = im.shape
light = (im.min(2) > 200) & ((im.max(2) - im.min(2)) < 30)
lab, _ = ndimage.label(light); edge = set(lab[0]) | set(lab[-1]) | set(lab[:, 0]) | set(lab[:, -1]); edge.discard(0)
bg = np.isin(lab, list(edge))
def skin(c):
    r, g, b = c; h, s, v = colorsys.rgb_to_hsv(r/255, g/255, b/255)
    if v > .85 and s < .12: return True                      # white halo / eye whites
    return 0.025 < h < 0.11 and 0.2 < s < 0.75 and 0.07 < v < 0.86 and r - b > 25 and not (s > .7 and v > .8)   # skin, hair, beard; keeps the bright gem
PAD = len(sys.argv) > 4 and sys.argv[4] == 'pad'
NOSKIN = PAD or (len(sys.argv) > 4 and sys.argv[4] == 'noskin')
def pad(c):                        # neutral grey undershirt Gemini drew under the plates: let the player's skin show
    r, g, b = c; lum = (r + g + b) / 3
    return max(c) - min(c) < 18 and 55 < lum < 150
def figure(x_lo, x_hi):
    m = ~bg[:, x_lo:x_hi]; ys, xs = np.where(m)
    y0, y1 = ys.min(), ys.max() + 1; ppu = (y1 - y0) / 32
    cx = (xs.min() + xs.max()) / 2 + x_lo
    return y0, cx - 8 * ppu, ppu
def sample(fig, ux, uy, uw, uh, mirror=False):
    """Region of the drawn figure in model units -> (uw*S x uh*S) RGBA texels."""
    y0, x0, ppu = fig; c = ppu / S; out = np.zeros((uh * S, uw * S, 4), np.uint8)
    for j in range(uh * S):
        for i in range(uw * S):
            ya, xa = y0 + (uy * S + j) * c, x0 + (ux * S + i) * c
            blk = im[int(ya + c * .2):int(ya + c * .8) + 1, int(xa + c * .2):int(xa + c * .8) + 1].reshape(-1, 3)
            m = bg[int(ya + c * .2):int(ya + c * .8) + 1, int(xa + c * .2):int(xa + c * .8) + 1].reshape(-1)
            if len(blk) == 0 or m.mean() > .5: continue
            fg = blk[~m]; q = Counter(map(tuple, fg // 12)).most_common(1)[0][0]
            col = fg[np.all(fg // 12 == q, axis=1)].mean(0)
            if (not NOSKIN and skin(col)) or (PAD and pad(col)): continue
            out[j, i, :3] = col; out[j, i, 3] = 255
    return out[:, ::-1] if mirror else out
F = figure(0, W // 2); B = figure(W // 2, W)
def faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}
def put(tex, rect, arr):
    u, v, w, h = rect; tex[v * S:(v + h) * S, u * S:(u + w) * S] = arr[:h * S, :w * S]
def fill_solid(arr):
    """top/bottom caps: dominant opaque colour of a face"""
    op = arr[..., 3] > 0
    if not op.any(): return arr
    c = Counter(map(tuple, arr[op])).most_common(1)[0][0]; out = np.zeros_like(arr); out[...] = c; return out
L1 = np.zeros((32 * S, 64 * S, 4), np.uint8); L2 = np.zeros_like(L1)
# head (helmet): front with the face cut out, back, sides from the back view
def solidify(a):                       # helmet back/sides: no holes where Gemini drew hair
    op = a[..., 3] > 0
    if op.any():
        idx = ndimage.distance_transform_edt(~op, return_distances=False, return_indices=True)
        a = a[idx[0], idx[1]]
    return a
hf = faces(0, 0, 8, 8, 8)
def visor(a):                          # big near-black areas on the helmet front = eye slit: let the face show through
    lum = a[..., :3].astype(int).mean(2); rgb = a[..., :3].astype(int); dark = (lum < 38) & ((rgb.max(2) - rgb.min(2)) < 14) & (a[..., 3] > 0)
    core = ndimage.binary_opening(dark, structure=np.ones((3, 3)))
    a = a.copy(); a[ndimage.binary_dilation(core) & dark] = 0
    eyes = (rgb[..., 2] > rgb[..., 0] + 30) & (rgb[..., 2] > rgb[..., 1] + 30) & (lum > 60) & (rgb[..., 0] > rgb[..., 1] + 10)   # Steve's eyes from the reference
    a[eyes] = 0; return a
def dehair(a):
    a = a.copy(); rgb = a[..., :3].astype(int)
    hair = (rgb[..., 0] > rgb[..., 2] + 15) & (rgb.max(2) < 110) & ((rgb.max(2) - rgb.min(2)) < 0.65 * rgb.max(2)) & (a[..., 3] > 0)
    a[hair] = 0; return a
def fill_specks(a):                   # small holes in the helmet plate (not the eye slit) get the neighbour colour
    holes = a[..., 3] == 0; lab, n = ndimage.label(holes)
    small = np.zeros_like(holes)
    for k in range(1, n + 1):
        m = lab == k
        if m.sum() < S * S: small |= m
    if small.any():
        idx = ndimage.distance_transform_edt(small | holes & ~small, return_distances=False, return_indices=True)
        filled = a[idx[0], idx[1]]; a = a.copy(); a[small] = filled[small]
    return a
def crown(a, rows=1):                  # top of the helmet: Gemini's outline/hair there, redo it as plain plate + dark rim
    a = a.copy(); band = a[rows * S:(rows + 1) * S].reshape(-1, 4); band = band[band[:, 3] > 0]
    if len(band) == 0: return a
    rgb = band[:, :3].astype(int); sat = rgb.max(1) - rgb.min(1)
    base = Counter(map(tuple, band[sat <= np.percentile(sat, 50)])).most_common(1)[0][0]
    a[:rows * S] = base; a[0, :, :3] = (np.array(base[:3]) * 0.55).astype(np.uint8)
    a[1, :, :3] = np.minimum(255, np.array(base[:3]) * 1.2).astype(np.uint8); return a
def forehead(a):
    top = dehair(a[:3 * S])
    a = a.copy(); a[:3 * S] = solidify(top); return a
if NOSKIN and not PAD:                # closed helmet: keep the visor glow as drawn
    put(L1, hf['front'], sample(F, 4, 0, 8, 8)); put(L1, hf['back'], solidify(sample(B, 4, 0, 8, 8)))
else:
    put(L1, hf['front'], crown(fill_specks(forehead(visor(sample(F, 4, 0, 8, 8)))))); put(L1, hf['back'], crown(solidify(dehair(sample(B, 4, 0, 8, 8))), 2))
side = sample(B, 4, 0, 8, 8)
side = crown(solidify(dehair(side)))
put(L1, hf['right'], side); put(L1, hf['left'], side[:, ::-1])
put(L1, hf['top'], fill_solid(side)); 
# body (chestplate rows 0-9 on layer 1, belt rows 8-12 on layer 2)
bf = faces(16, 16, 8, 12, 4)
fr, bk = sample(F, 4, 8, 8, 12), sample(B, 4, 8, 8, 12)
for a in (fr, bk):
    a[:S, :S] = 0; a[:S, 7 * S:] = 0
c1f, c1b = fr.copy(), bk.copy(); c1f[10 * S:] = 0; c1b[10 * S:] = 0
put(L1, bf['front'], c1f); put(L1, bf['back'], c1b)
sd = bk[:, :4 * S].copy(); sd1 = sd.copy(); sd1[10 * S:] = 0
put(L1, bf['right'], sd1); put(L1, bf['left'], sd1[:, ::-1])
belt_f, belt_b, belt_s = fr.copy(), bk.copy(), sd.copy()
for a in (belt_f, belt_b, belt_s): a[:8 * S] = 0
put(L2, bf['front'], belt_f); put(L2, bf['back'], belt_b); put(L2, bf['right'], belt_s); put(L2, bf['left'], belt_s[:, ::-1])
# right arm (viewer's left on the front view, viewer's right on the back view)
af = faces(40, 16, 4, 12, 4)
afr, abk = sample(F, 0, 8, 4, 12), sample(B, 12, 8, 4, 12)
def open_rows(a):                      # rows of the arm that are mostly skin (between pauldron and bracer): fully open, no dots
    a = a.copy(); frac = (a[..., 3] > 0).mean(1)
    a[frac < 0.45] = 0; return a
if PAD: afr, abk = open_rows(afr), open_rows(abk)
put(L1, af['front'], afr); put(L1, af['back'], abk); put(L1, af['right'], afr); put(L1, af['left'], abk)
put(L1, af['top'], fill_solid(afr[:4 * S]))
# right leg: boots (rows 8-12) on layer 1, leggings (rows 0-8) on layer 2
lf = faces(0, 16, 4, 12, 4)
lfr, lbk = sample(F, 4, 20, 4, 12), sample(B, 8, 20, 4, 12)
for layer, rows in ((L1, slice(8 * S, 12 * S)), (L2, slice(0, 8 * S))):
    for face, a in (('front', lfr), ('back', lbk), ('right', lfr), ('left', lbk)):
        b = np.zeros_like(a); b[rows] = a[rows]; put(layer, lf[face], b)
put(L1, lf['bottom'], fill_solid(lfr[8 * S:]))
def despeckle(t):                      # lone dots left after removing skin/undershirt: clear them (< 3/4 of a model pixel)
    lab, n = ndimage.label(t[..., 3] > 0)
    if n:
        sizes = ndimage.sum(np.ones(lab.shape), lab, range(1, n + 1))
        for k in np.where(sizes < S * S * 2.5)[0]: t[lab == k + 1] = 0
    return t
L1, L2 = despeckle(L1), despeckle(L2)
Image.fromarray(L1).save(f'worn/{name}_layer_1.png'); Image.fromarray(L2).save(f'worn/{name}_layer_2.png')
# flat front/back preview on a Steve-coloured dummy
def view(tex1, tex2, back=False):
    pv = Image.new('RGBA', (16 * S, 32 * S), (0, 0, 0, 0)); body = (150, 110, 80, 255)
    base = np.zeros((32 * S, 16 * S, 4), np.uint8); base[...] = body; base[:, :4*S][:8*S] = 0; base[:, 12*S:][:8*S] = 0
    base[20*S:, 4*S:12*S] = (60, 60, 140, 255); base[8*S:20*S, 4*S:12*S] = (0, 160, 170, 255)
    canvas = Image.fromarray(base)
    k = 'back' if back else 'front'
    for tex in (tex2, tex1):
        t = Image.fromarray(tex)
        def crop(r): u, v, w, h = r; return t.crop((u * S, v * S, (u + w) * S, (v + h) * S))
        canvas.alpha_composite(crop(hf[k]), (4 * S, 0)); canvas.alpha_composite(crop(bf[k]), (4 * S, 8 * S))
        a = crop(af[k]); canvas.alpha_composite(a, (0, 8 * S)); canvas.alpha_composite(a.transpose(Image.FLIP_LEFT_RIGHT), (12 * S, 8 * S))
        l = crop(lf[k]); canvas.alpha_composite(l, (4 * S, 20 * S)); canvas.alpha_composite(l.transpose(Image.FLIP_LEFT_RIGHT), (8 * S, 20 * S))
    return canvas
pv = Image.new('RGBA', (40 * S, 34 * S), (139, 139, 139, 255))
pv.alpha_composite(view(L1, L2), (2 * S, S)); pv.alpha_composite(view(L1, L2, True), (22 * S, S))
pv.resize((pv.width * 6, pv.height * 6), 0).save(f'worn/{name}_preview.png')
