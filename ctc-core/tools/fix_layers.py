"""Make projected armor layers fully closed: every face of every armor part is solid (no visor holes, no gaps between
legs, no white background stripes). Optional: replace the body back (e.g. drop wings Gemini drew)."""
import sys
import numpy as np
from PIL import Image
from scipy import ndimage

S = 4
def faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}
HEAD, BODY, ARM, LEG = faces(0, 0, 8, 8, 8), faces(16, 16, 8, 12, 4), faces(40, 16, 4, 12, 4), faces(0, 16, 4, 12, 4)

def solid(a):
    rgb = a[..., :3].astype(int)
    white = (rgb.min(-1) > 225)
    a = a.copy(); a[white] = 0
    op = a[..., 3] > 0
    if op.sum() < 4:
        return a
    idx = ndimage.distance_transform_edt(~op, return_distances=False, return_indices=True)
    return a[idx[0], idx[1]]

def fix(path, parts):
    t = np.array(Image.open(path).convert('RGBA'))
    for part in parts:
        for name, (u, v, w, h) in part.items():
            sl = (slice(v * S, (v + h) * S), slice(u * S, (u + w) * S))
            t[sl] = solid(t[sl])
    return t

def boots(t, rows=5):
    """Boots (layer 1 legs): only the lowest `rows` model rows, closed; the rest stays empty so leggings show."""
    for name, (u, v, w, h) in LEG.items():
        if name in ('top', 'bottom'):
            continue
        y0 = (v + h - rows) * S
        t[v * S:y0, u * S:(u + w) * S] = 0
        sl = (slice(y0, (v + h) * S), slice(u * S, (u + w) * S))
        t[sl] = solid(t[sl])
    u, v, w, h = LEG['bottom']
    sl = (slice(v * S, (v + h) * S), slice(u * S, (u + w) * S)); t[sl] = solid(t[sl])
    return t

def plain_back(t, plate=None):
    """Body back = front body without its center decoration (crystal/emblem) -> plain plate."""
    fu, fv, fw, fh = BODY['front']; bu, bv, bw, bh = BODY['back']
    front = t[fv * S:(fv + fh) * S, fu * S:(fu + fw) * S].copy()
    lum = front[..., :3].astype(int).mean(-1)
    bright = lum > 150
    if bright.any():
        front[bright, 3] = 0
        front = solid(front)
    t[bv * S:(bv + bh) * S, bu * S:(bu + bw) * S] = front
    return t

if __name__ == '__main__':
    name = sys.argv[1]; nowings = len(sys.argv) > 2 and sys.argv[2] == 'plainback'
    l1 = fix(f'worn/{name}_layer_1.png', [HEAD, BODY, ARM])
    l1 = boots(l1)
    if nowings:
        l1 = plain_back(l1)
    l2 = fix(f'worn/{name}_layer_2.png', [BODY, LEG])
    Image.fromarray(l1).save(f'worn/{name}_layer_1.png'); Image.fromarray(l2).save(f'worn/{name}_layer_2.png')
