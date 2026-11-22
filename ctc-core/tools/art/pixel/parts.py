"""Intermediate items for the 2.0 Create recipes, derived from the finished pixel art so they read as 'not done yet'."""
import json, sys
from PIL import Image
exec(open('pxicons.py').read().split('\nif __name__')[0])
I = '/home/claude/stentik-turnpc/ctc-core/src/main/resources/assets/ctccore/textures/item/'
K = '/home/claude/installer/assets/ctcdata/assets/ctc/textures/item/'

REV = {}
for n, ramp in HEX.items():
    for i, h in enumerate(ramp):
        REV.setdefault(tuple(int(v) for v in rgb(h)), f'{n}.{i}')

def load(path):
    im = Image.open(path).convert('RGBA')
    im = im.crop((0, 0, im.width, im.width))
    g, miss = [], set()
    for y in range(im.height):
        r = []
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] < 128: r.append(None); continue
            t = REV.get(p[:3])
            if t is None:   # not one of our ramps (chip ramps live only in Pixelwright): nearest by distance
                best = min(REV, key=lambda c: sum((a - b) ** 2 for a, b in zip(c, p[:3])))
                t = REV[best]; miss.add(p[:3])
            r.append(t)
        g.append(r)
    return g

def remap(g, m):
    out = []
    for r in g:
        o = []
        for t in r:
            if t is None: o.append(None); continue
            n, i = t.split('.')
            f = m.get(n)
            o.append(f(int(i)) if callable(f) else (f if f else t))
        out.append(o)
    return out

ST = ['st.0', 'st.1', 'st.2', 'st.3', 'st.4', 'st.5']
# unfinished voidsteel: the steel body is cast, the void channels are still empty (dark slots)
UNF = {'vo': lambda i: 'vo.0' if i == 0 else ST[i - 1],
       'violet': lambda i: ['slit.0', 'slit.1', 'slit.2', 'slit.2'][i],
       'dia': lambda i: 'slit.2', 'glow': lambda i: 'slit.2'}
UNF2 = {k: v for k, v in UNF.items() if k != 'vo'}
# incomplete chips: copper board without the gold plating
CU = {'gold': lambda i: ['cu.0', 'cu.1', 'cu.2', 'cu.3'][min(i, 3)]}

def crushed():
    rows = [
        "................",
        "................",
        "................",
        "......00........",
        ".....0430..00...",
        "....042310.0420.",
        "...0423210.0320.",
        "..00332100.0210.",
        ".0430110..000...",
        ".04321100.0420..",
        "..032210.04320..",
        "..0221100032100.",
        "...011000.02210.",
        "....000...00000.",
        "................",
        "................"]
    m = {'0': 'vo.0', '1': 'vo.2', '2': 'vo.3', '3': 'vo.4', '4': 'violet.2'}
    return [[m.get(c) for c in r] for r in rows]

def build():
    out = {}
    out['crushed_voidsteel'] = crushed()
    for p in ['helmet', 'chestplate', 'leggings', 'boots']:
        out[f'unfinished_cracked_voidsteel_{p}'] = remap(load(I + f'cracked_voidsteel_{p}.png'), UNF)
    for w in ['voidsteel_rapier', 'voidsteel_sword', 'voidsteel_battle_axe']:
        out['unfinished_' + w] = remap(load(K + w + '.png'), UNF)
    blank = load(I + 'blank_chip.png')
    out['incomplete_blank_chip'] = remap(blank, CU)
    # incomplete chip: the blank with its core still open (centre 10x10 dark)
    ic = [r[:] for r in blank]
    for y in range(11, 21):
        for x in range(11, 21):
            if ic[y][x]: ic[y][x] = 'slit.1' if (x + y) % 2 else 'slit.2'
    out['incomplete_chip'] = ic
    out['incomplete_chip_socket'] = remap(load(I + 'chip_socket.png'), CU)
    out['incomplete_voidforge_template'] = remap(load(I + 'voidforge_template.png'), UNF2)
    out['incomplete_void_sigil'] = remap(load(I + 'void_sigil.png'), UNF2)
    return out

if __name__ == '__main__':
    out = build()
    json.dump(out, open('parts.json', 'w'))
    if 'save' in sys.argv:
        for n, g in out.items(): render(g).save(K + n + '.png')
    pv = Image.new('RGBA', (len(out) * 72, 72), (60, 60, 60, 255))
    for i, (n, g) in enumerate(out.items()):
        pv.alpha_composite(render(g).resize((64, 64), Image.NEAREST), (i * 72 + 4, 4))
    pv.save('parts_preview.png'); print(list(out))
