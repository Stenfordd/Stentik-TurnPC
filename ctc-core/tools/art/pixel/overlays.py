"""Hand-placed elite overlays on the vanilla UV layout. Only these pixels are ours; the mob underneath stays vanilla.
The vanilla textures in vanilla_ref/ are a local reference for the preview only (never shipped)."""
import sys
from PIL import Image
sys.argv = [sys.argv[0]]
exec(open('mobs3d.py').read().split("\nif __name__")[0])

REF = 'vanilla_ref/'

def ov(w, h): return Skin(w, h)

def chip4(s, x, y, chip):
    for dy in range(4):
        for dx in range(4):
            s.put(x + dx, y + dy, chip_px(x + dx, y + dy, x, y, chip))

def trace(s, pts, chip, step=2):
    for i, (x, y) in enumerate(pts):
        s.put(x, y, f'{chip}.{step}')

def eyes(s, pairs, chip):
    for (x, y), inner in pairs:
        s.put(x, y, f'{chip}.{4 if inner else 3}')

def zombie_ov(chip):
    s = ov(64, 64)
    eyes(s, [((9, 12), False), ((10, 12), True), ((13, 12), True), ((14, 12), False)], chip)
    chip4(s, 22, 22, chip)
    trace(s, [(22, 26), (22, 27), (21, 27), (21, 28)], chip)               # circuit traces out of the chip
    trace(s, [(26, 23), (27, 23)], chip)
    trace(s, [(25, 26), (25, 27), (26, 28)], chip, 1)
    trace(s, [(45, 23), (45, 24), (45, 25), (46, 25)], chip)               # right arm
    trace(s, [(5, 21), (5, 22), (6, 22)], chip, 1)                         # leg
    return s.im

def skeleton_ov(chip):
    s = ov(64, 32)
    eyes(s, [((9, 12), False), ((10, 12), True), ((13, 12), True), ((14, 12), False)], chip)
    chip4(s, 22, 22, chip)
    trace(s, [(14, 11), (15, 10)], chip, 1)                                # crack over the eye
    trace(s, [(42, 23), (42, 24)], chip, 1)                                # arm
    return s.im

def creeper_ov(chip):
    s = ov(64, 32)
    eyes(s, [((9, 10), True), ((10, 10), False), ((13, 10), False), ((14, 10), True),
             ((9, 11), False), ((14, 11), False)], chip)
    chip4(s, 22, 23, chip)
    trace(s, [(23, 27), (23, 28), (24, 28), (24, 29)], chip)
    trace(s, [(26, 24), (27, 24)], chip, 1)
    trace(s, [(5, 21), (5, 22)], chip, 1)
    return s.im

def enderman_ov(chip):
    s = ov(64, 32)
    for x in (8, 9, 10, 13, 14, 15): s.put(x, 12, f'{chip}.{4 if x in (9, 14) else 3}')
    chip4(s, 38, 22, chip)
    trace(s, [(39, 26), (39, 27), (40, 27), (40, 28)], chip)
    trace(s, [(58, 9), (58, 10), (58, 14), (59, 15)], chip, 1)
    return s.im

def wither_ov():
    s = ov(64, 64)
    eyes(s, [((9, 12), False), ((10, 12), True), ((13, 12), True), ((14, 12), False)], 'ember')
    chip4(s, 10, 8, 'ember')
    for cx, chip in ((40, 'acid'),):   # side heads share one texture
        s.put(cx, 6, 'gold.2'); s.put(cx + 1, 6, 'gold.2'); s.put(cx, 7, f'{chip}.4'); s.put(cx + 1, 7, f'{chip}.2')
    eyes(s, [((38, 8), False), ((39, 8), True), ((42, 8), True), ((43, 8), False)], 'acid')
    for x in range(3, 23):                                                # Kinetic plating along the shoulders
        if x % 3 != 0: s.put(x, 19, 'kinetic.2')
        else: s.put(x, 19, 'kinetic.4')
    for x, y in ((27, 25), (30, 24), (33, 25), (35, 24)): s.put(x, y, 'acid.3')   # acid between the ribs
    return s.im

if __name__ == '__main__':
    cast = [('zombie', 'zombie', zombie_ov('ember'), 'Ember Zombie'), ('zombie', 'zombie', zombie_ov('acid'), 'Acid Zombie'),
            ('skeleton', 'skeleton', skeleton_ov('kinetic'), 'Kinetic Skeleton'), ('creeper', 'creeper', creeper_ov('lunar'), 'Lunar Creeper'),
            ('enderman', 'enderman', enderman_ov('void'), 'Void Enderman'), ('zombie', 'zombie', None, 'Zombie (vanilla)'),
            ('wither', 'wither', None, 'Wither'), ('wither', 'wither', wither_ov(), 'Wither, phase 2')]
    from PIL import ImageDraw
    sheet = Image.new('RGBA', (4 * 300, 2 * 440), (34, 36, 48, 255)); d = ImageDraw.Draw(sheet)
    for i, (model, ref, o, name) in enumerate(cast):
        base = Image.open(REF + ref + '.png').convert('RGBA')
        if model == 'enderman':   # vanilla's eyes are a separate glowing layer; show them on the base
            pass
        skin = base.copy()
        if o is not None:
            skin.alpha_composite(o)
            o.save(f'mob3d/{model}_{name.split()[0].lower()}_overlay.png')
        sc = 6 if model == 'enderman' else 8 if model == 'wither' else 9
        img = render(model, skin, scale=sc, size=(300, 410))
        x, y = (i % 4) * 300, (i // 4) * 440
        sheet.alpha_composite(img, (x, y))
        d.text((x + 10, y + 415), name, fill=(230, 220, 190, 255))
    sheet.save('elite_overlays.png'); print('ok')
