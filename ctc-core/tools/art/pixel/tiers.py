import json, re
exec(open('canvas.py').read())
src = open('pw_titanium.py').read()
ns = {}
exec(src.split("for o,r in W1")[0].replace("L1.save","#"), ns)
W1, W2 = ns['W1'], ns['W2']
def grid(writes):
    c = C(64, 64)
    for (ox, oy), rows in writes:
        for j, r in enumerate(rows):
            r = r.replace('|', '')
            for i, ch in enumerate(r):
                if ch != '.': c.g[oy + j][ox + i] = ch
    return c
TOK = {
 'steel': dict(zip('abcdefpqrngh i'.replace(' ',''), ['st.0','st.1','st.2','st.3','st.4','st.5','leather.1','leather.2','leather.3','slit.2','slit.2','slit.1','slit.0'])),
 'tungsten': dict(zip('abcdefpqrnghi', ['tu.0','tu.1','tu.2','tu.3','tu.4','tu.5','hot.1','hot.2','hot.3','hot.1','hot.2','hot.3','hot.4'])),
 'voidsteel': dict(zip('abcdefpqrnghi', ['vo.0','vo.1','vo.2','vo.3','vo.4','vo.5','bone.0','bone.1','bone.2','violet.0','violet.1','violet.2','violet.3'])),
}
def clear(c, x, y, w, h):
    for j in range(h):
        for i in range(w): c.g[y + j][x + i] = '.'
def faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}
def plate(c, rect, light=0, edge=None):
    """top-left lit plate in letters a..f"""
    x0, y0, w, h = rect; L = 'abcdef'
    for j in range(h):
        for i in range(w):
            k = 3 + light
            if j == 0 or i == 0: k += 1
            if j == h - 1 or i == w - 1: k -= 1
            if j == h - 1 and h > 2: k -= 1
            c.g[y0 + j][x0 + i] = L[max(0, min(5, k))]
def box(c, u, v, w, h, d, light=0):
    f = faces(u, v, w, h, d)
    plate(c, f['top'], light + 1); plate(c, f['bottom'], light - 2); plate(c, f['front'], light)
    plate(c, f['back'], light - 1); plate(c, f['right'], light - 1); plate(c, f['left'], light - 1)
    return f
def row(c, rect, y, s):
    x0, y0, w, h = rect
    for i, ch in enumerate(s):
        if ch != ' ': c.g[y0 + y][x0 + i] = ch

def build(tier):
    c1, c2 = grid(W1), grid(W2)
    # clear titanium's 3D parts area, redraw per tier
    clear(c1, 0, 32, 64, 32); clear(c2, 0, 32, 64, 32)
    HF = (8, 8, 8, 8); HR = (0, 8, 8, 8); HL = (16, 8, 8, 8)
    CF = (20, 20, 8, 10)
    if tier == 'steel':
        c1.put(8, 8, ["deeeeeed", "deddeddc", "nhhhhhhn", "cddeddcc", "cddeddcc", "cdhehdcb", "ddcecccb", "bbbbbbba"])
        c1.put(0, 8, ["deeeeeed", "dddddddd", "cddddddh", "cddeddh ", "cdddddhc", "cddddddc", "cccccccc", "bbbbbbbb".replace(' ','b')])
        c1.put(16, 8, ["deeeeeed", "dddddddd", "hddddddc", " hddeddc", "chdddddc", "cddddddc", "cccccccc", "bbbbbbbb"])
        c1.put(24, 8, [None] and ["deeeeeed", "dddddddc", "cdddddcc", "cddddccb", "ccddcccb", "ccccccbb", "bccccbbb", "bbbbbbba"])
        c1.put(20, 20, ["dqddddqc", "eqfeedqc", "eqeddcqc", "dqddddqb", "cqdddcqb", "cdddcccb", "cddcccbb", "cccccbbb", "pqqrrqqp", "bcccccba"])
        c1.put(32, 20, ["dqddddqc", "dqddddqc", "dqcddcqc", "dqddddqb", "cqddccqb", "cddcccbb", "cdccccbb", "cccccbbb", "pqqqqqqp", "bccccbba"])
        for y, s in ((29, "qrrq"), (29, "qrrq")):
            row(c1, (44, 20, 4, 12), 9, "qrrq"); row(c1, (40, 20, 4, 12), 9, "qrrq"); row(c1, (48, 20, 4, 12), 9, "pqqp"); row(c1, (52, 20, 4, 12), 9, "pqqp")
        for x in (40, 44, 48, 52):
            row(c1, (x, 20, 4, 12), 3, "cbbb" if x != 44 else "dccb"); row(c1, (x, 20, 4, 12), 7, "cbbb" if x != 44 else "dccb")
        # 3D: pauldron (0,32) 6x3x8, second plate (28,44) 5x2x8, ridge (42,32) 2x8x1, crest (0,44) 2x2x10
        f = box(c1, 0, 32, 6, 3, 8)
        for k in ('front', 'back', 'right', 'left'): row(c1, f[k], f[k][3] - 1, 'b' * f[k][2])
        row(c1, f['top'], 3, " c   "); row(c1, f['top'], 5, " c   ")
        f = box(c1, 28, 44, 5, 2, 8, -1)
        f = box(c1, 42, 32, 2, 8, 1, 1)
        f = box(c1, 0, 44, 2, 2, 10, 1)
        c2_knee = 'steel'
    if tier == 'tungsten':
        c1.put(8, 8, ["qrrrrrrq", "dddddddc", "cihhhhic", "cbbihbbc", "cddihddc", "cddghddb", "ddcccccb", "pqqqqqqp"])
        c1.put(0, 8, ["qrrrrrrr", "dddddddd", "cddddddc", "cdqqqqdh", "cddddddg", "cdqqqqdc", "cccccccc", "pqqqqqqq"])
        c1.put(16, 8, ["rrrrrrrq", "dddddddd", "cddddddc", "hdqqqqdc", "gddddddc", "cdqqqqdc", "cccccccc", "qqqqqqqp"])
        c1.put(20, 20, ["qrrrrrrq", "eddeeddc", "ddhiihdc", "ddhiihcc", "qdccccdq", "qdddccdq", "qrrrrrrq", "cdddcccb", "pqqrrqqp", "bcccccba"])
        c1.put(32, 20, ["qrrrrrrq", "dddddddc", "dcqqqqcc", "dcddddcb", "dcddddcb", "dcqqqqcb", "ddcccccb", "cdddcccb", "pqqqqqqp", "bccccbba"])
        # 3D: pauldron, second plate, breast (28,32), crest (0,44), cuffs (40,54) 6x1x6
        f = box(c1, 0, 32, 6, 3, 8)
        row(c1, f['top'], 0, "rrrrrr"); row(c1, f['top'], 7, "qqqqqq")
        for k in ('front', 'back', 'right', 'left'): row(c1, f[k], f[k][3] - 1, 'q' * f[k][2])
        f = box(c1, 28, 44, 5, 2, 8, -1)
        for k in ('front', 'back', 'right', 'left'): row(c1, f[k], 1, 'p' * f[k][2])
        f = box(c1, 28, 32, 6, 6, 1)
        c1.put(f['front'][0], f['front'][1], ["qrrrrq", "edhhdc", "dhiihc", "dhiihc", "ddhhcc", "qqqqqp"])
        f = box(c1, 0, 44, 2, 2, 10, 1)
        row(c1, f['top'], 0, "rr"); 
        for j in range(10): row(c1, f['top'], j, "rq")
        f = box(c1, 40, 54, 6, 1, 6)
        for k in ('front', 'back', 'right', 'left'): row(c1, f[k], 0, 'r' * f[k][2])
    if tier == 'voidsteel':
        c1.put(8, 8, ["drrrrrrd", "caaaaaac", "caaaaaac", "cahiihac", "caaaaaac", "cbaaaabb", "ddcccccb", "pqqqqqqp"])
        c1.put(0, 8, ["deeeeeed", "dddddddd", "cddddddc", "cdhddddh", "cddhdddg", "cdddhddc", "cccccccc", "pqqqqqqq"])
        c1.put(16, 8, ["deeeeeed", "dddddddd", "cddddddc", "hddddhdc", "gdddhddc", "cddhdddc", "cccccccc", "qqqqqqqp"])
        c1.put(20, 20, ["qrddddrq", "eqrddrqc", "eddhhddc", "ddhiihcc", "cdhiihcb", "cddhhccb", "cgdddcgb", "cdgccgcb", "pqqrrqqp", "bcccccba"])
        c1.put(32, 20, ["dddddddc", "dhddddhc", "ddhddhcc", "dddhhdcb", "dddhhdcb", "ddhddhcb", "dhccccgb", "cdddcccb", "pqqqqqqp", "bccccbba"])
        for x in (40, 44, 48, 52): row(c1, (x, 20, 4, 12), 9, "ghhg" if x != 44 else "hiih")
        # horns (56,0): crystal
        for j in range(6):
            for i in range(8): c1.g[j][56 + i] = 'i' if (i + j) % 3 == 0 else 'h'
        # 3D: pauldron with bone rim and violet studs, breast, gem (42,32)
        f = box(c1, 0, 32, 6, 3, 8)
        for k in ('front', 'back', 'right', 'left'): row(c1, f[k], f[k][3] - 1, 'q' * f[k][2])
        row(c1, f['top'], 0, "rrrrrr"); row(c1, f['top'], 2, " h  h "); row(c1, f['top'], 5, "  h   ")
        f = box(c1, 28, 32, 6, 6, 1)
        c1.put(f['front'][0], f['front'][1], ["qrrrrq", "dddddc", "ddhhdc", "dhiihc", "ddhhcc", "cccbbb"])
        f = box(c1, 42, 32, 2, 3, 1)
        for k in f:
            x0, y0, w, h = f[k]
            for j in range(h):
                for i in range(w): c1.g[y0 + j][x0 + i] = 'i' if (i + j) % 2 == 0 else 'h'
    # knees (layer 2, (0,32) 4x3x1) per tier
    f = box(c2, 0, 32, 4, 3, 1)
    row(c2, f['front'], 0, "qrrq"); row(c2, f['front'], 1, "rfdq" if tier != 'voidsteel' else "rhhq"); row(c2, f['front'], 2, "pqqp")
    if tier == 'steel':
        row(c2, f['front'], 0, "deec"); row(c2, f['front'], 1, "dfdc"); row(c2, f['front'], 2, "bccb")
    return c1, c2
