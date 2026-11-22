"""16x16 voidsteel block + ore overlays (hand drawn grids), 8-frame pulse animations."""
import json, sys
from PIL import Image
exec(open('pxicons.py').read().split('if __name__')[0])

# ore vein clusters; o = dark rim, a/b/c/d = violet 0..3 core, g = cyan spark. Kept off the edges so faces tile.
ORE = {
 'side': [
  "................",
  "..oo............",
  "..oaoo......oo..",
  "...obao....obo..",
  "....oaco..obco..",
  ".....obo.oaco...",
  "......o..oco....",
  ".........oo.....",
  "..o.............",
  ".oao.......oo...",
  ".obao.....oaco..",
  "..ocdo...obgdo..",
  "...obco.ocao....",
  "....oo..obo.....",
  ".........o......",
  "................"],
 'ew': [
  "................",
  "........oo......",
  ".......obo......",
  "..oo..obco......",
  ".oaco.oao.......",
  "..obao.o.....o..",
  "...ogco.....obo.",
  "....oo.....obco.",
  "..........ocao..",
  "...o.....oaco...",
  "..obo.....oo....",
  "..ocao..........",
  "...obco....oo...",
  "....oco...obdo..",
  ".....o.....oo...",
  "................"],
 'top': [
  "................",
  "................",
  "...oo...........",
  "..obao......o...",
  "...ocdo....obo..",
  "....oo....ocao..",
  "...........oo...",
  "......oo........",
  ".....obgo.......",
  "......oco.......",
  ".......o........",
  "..o.........oo..",
  ".obo.......obco.",
  "..oco.......oo..",
  "...o............",
  "................"],
 'bottom': [
  "................",
  ".....o..........",
  "....obo.....oo..",
  "...ocao....obo..",
  "....oo....ocdo..",
  "...........oo...",
  "................",
  "..oo.....o......",
  ".obco...obo.....",
  "..oao..ocgo.....",
  "...oo...oo......",
  "...........o....",
  "....oo....obo...",
  "...obco...oco...",
  "....oo.....o....",
  "................"],
}
# block: void panel with violet frame and rivets; s = star spots that twinkle
BLOCK = [
  "OOOOOOOOOOOOOOOO",
  "OHhhhhhhhhhhhhRO",
  "Oh1111111111112O",
  "Oh1000000000012O",
  "Oh10s000000s012O",
  "Oh1000000000012O",
  "Oh100000t000012O",
  "Oh1000000000012O",
  "Oh1000s000000r2O",
  "Oh1000000000012O",
  "Oh1000000s00012O",
  "Oh10t00000000r2O",
  "Oh1000000000012O",
  "Oh1222222222222O",
  "OR222222222222RO",
  "OOOOOOOOOOOOOOOO"]
BLOCK = [r if len(r) == 16 else r[:15] + 'O' for r in BLOCK]

def ore_frame(g, f):
    import math
    k = (math.sin(f / 8 * 2 * math.pi) + 1) / 2   # 0..1 pulse
    m = {'o': 'vo.2', 'a': 'violet.0', 'b': 'violet.1', 'c': 'violet.2', 'd': 'violet.3', 'g': 'dia.3' if k > .5 else 'dia.2'}
    if k > .66: m.update(a='violet.1', b='violet.2', c='violet.3')
    elif k < .2: m.update(b='violet.0', c='violet.1', d='violet.2')
    return [[m.get(ch) for ch in row] for row in g]

def block_frame(f):
    m = {'O': 'vo.0', 'H': 'violet.3', 'h': 'violet.1', 'R': 'violet.2', '1': 'vo.3', '2': 'violet.0',
         '0': 'vo.1', 'r': 'violet.2'}
    stars = {'s': ['vo.4', 'violet.2', 'violet.3', 'violet.2', 'vo.4', 'vo.3', 'vo.2', 'vo.3'],
             't': ['vo.2', 'vo.3', 'vo.4', 'dia.2', 'dia.3', 'dia.2', 'vo.4', 'vo.3']}
    out = []
    for y, row in enumerate(BLOCK):
        r = []
        for x, ch in enumerate(row):
            if ch in stars: r.append(stars[ch][(f + x + y) % 8])
            else: r.append(m[ch])
        out.append(r)
    return out

def strip(frames):
    im = Image.new('RGBA', (16, 16 * len(frames)))
    for i, g in enumerate(frames): im.paste(render(g), (0, 16 * i))
    return im

if __name__ == '__main__':
    K = '/home/claude/installer/assets/ctcdata/assets/ctc/textures/block/'
    out = K if 'save' in sys.argv else '/tmp/claude-0/-home-claude/dbb0d9ba-f1cd-5617-86e0-38bb24bf8c87/scratchpad/vb_'
    strip([block_frame(f) for f in range(8)]).save(out + 'voidsteel_block.png')
    for k, n in [('side', ''), ('ew', '_ew'), ('top', '_top'), ('bottom', '_bottom')]:
        strip([ore_frame(ORE[k], f) for f in range(8)]).save(out + 'voidsteel_ore_overlay' + n + '.png')
    # preview: frame 0 of each on an end-stone-ish tan bg, 8x
    pv = Image.new('RGBA', (5 * 136, 136), (40, 40, 40, 255))
    for i, g in enumerate([block_frame(0)] + [ore_frame(ORE[k], 3) for k in ORE]):
        bg = Image.new('RGBA', (16, 16), (219, 222, 158, 255) if i else (0, 0, 0, 0))
        bg.alpha_composite(render(g)); pv.alpha_composite(bg.resize((128, 128), Image.NEAREST), (i * 136, 4))
    pv.save('/tmp/claude-0/-home-claude/dbb0d9ba-f1cd-5617-86e0-38bb24bf8c87/scratchpad/vb_preview.png')
    json.dump({'block': block_frame(0), **{k: ore_frame(ORE[k], 3) for k in ORE}}, open('/tmp/claude-0/-home-claude/dbb0d9ba-f1cd-5617-86e0-38bb24bf8c87/scratchpad/vb.json', 'w'))
    print('ok')
