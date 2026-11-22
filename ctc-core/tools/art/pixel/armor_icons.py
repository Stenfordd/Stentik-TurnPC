exec(open('pxicons.py').read())
A = {}
A['helmet'] = [
"................",
"................",
"................",
"....oooooooo....",
"...o55544443o...",
"..o5544443332o..",
"..o5443333322o..",
"..o4xxxyyxxx2o..",
"..o433oooo321o..",
"..o32o....o21o..",
"..o32o....o21o..",
"..o21o....o11o..",
"..oooo....oooo..",
"................",
"................",
"................"]
A['chestplate'] = [
"................",
"..oooo....oooo..",
".o5544oooo3322o.",
".o54443333332oo.",
".oo44433333322o.",
"..oo443yy3322oo.",
"...o44xyyx322o..",
"...o4433x3322o..",
"...o44333322o...",
"...o43333322o...",
"...oxxxxxxxxo...",
"...o43333221o...",
"...o33322211o...",
"...oooooooooo...",
"................",
"................"]
A['leggings'] = [
"................",
"................",
"...oooooooooo...",
"...oxxxyyxxxo...",
"...o54443322o...",
"...o54433322o...",
"...o544o3322o...",
"...o54o.o322o...",
"...o54o.o32oo...",
"...oyyo.oyyo....",
"...o43o.o21o....",
"...o43o.o21o....",
"...o32o.o11o....",
"...oooo.oooo....",
"................",
"................"]
A['boots'] = [
"................",
"................",
"................",
"................",
"................",
"...oooo..oooo...",
"...o54o..o32o...",
"...oxyo..oxxo...",
"...o54o..o32o...",
"...o443o.o322o..",
"..o54432oo33221o",
"..o44332oo32211o",
"..oooooooooooooo",
"................",
"................",
"................"]
TIERS = {'steel': ('st', 'leather.2', 'leather.3'), 'titanium': ('ti', 'cu.2', 'glow.2'), 'tungsten': ('tu', 'hot.1', 'hot.3'), 'voidsteel': ('vo', 'bone.2', 'violet.2')}
def armor_grid(piece, tier):
    ramp, x, y = TIERS[tier]; t = A[piece]; g = []
    for r in t:
        assert len(r) == 16, (piece, r)
        row = []
        for ch in r:
            if ch == '.': row.append(None)
            elif ch == 'o': row.append(ramp + '.0')
            elif ch in '12345': row.append(f'{ramp}.{ch}')
            elif ch == 'x': row.append(x)
            elif ch == 'y': row.append(y)
        g.append(row)
    return g
if __name__ == '__main__':
    out = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    for r, tier in enumerate(TIERS):
        for c, p in enumerate(['helmet', 'chestplate', 'leggings', 'boots']):
            out.alpha_composite(render(armor_grid(p, tier)), (c * 16, r * 16))
    bg = Image.new('RGBA', out.size, (140, 140, 150, 255)); bg.alpha_composite(out); bg.resize((512, 512), Image.NEAREST).save('armor16.png')
