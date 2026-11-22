exec(open('pxicons.py').read())
def bez(a, c, b, n=60):
    return [((1-t)**2*a[0]+2*(1-t)*t*c[0]+t*t*b[0], (1-t)**2*a[1]+2*(1-t)*t*c[1]+t*t*b[1]) for t in [i/n for i in range(n+1)]]
def line(a, b, n=40): return [(a[0]+(b[0]-a[0])*i/n, a[1]+(b[1]-a[1])*i/n) for i in range(n+1)]
def plot(g, pts, tok, only_empty=False):
    for x, y in pts:
        X, Y = int(round(x)), int(round(y))
        if 0 <= X < 16 and 0 <= Y < 16 and (not only_empty or g[Y][X] is None): g[Y][X] = tok
def bow(kind, pull):
    g = [[None]*16 for _ in range(16)]
    A, B, C = (2, 3), (12, 13), (13.5, 1.5)
    ramp, acc, acc2 = {'titanium': ('ti', 'cu', 'glow'), 'voidsteel': ('vo', 'bone', 'violet')}[kind]
    import math
    O = (5.4, 9.6); r = math.dist(O, A)
    a0 = math.atan2(A[1]-O[1], A[0]-O[0]); a1 = math.atan2(B[1]-O[1], B[0]-O[0])
    if a1 < a0: a1 += 2*math.pi
    arc = [(O[0]+r*math.cos(a0+(a1-a0)*i/60), O[1]+r*math.sin(a0+(a1-a0)*i/60)) for i in range(61)]
    # the arc must pass the top-right side: if it bulges the other way, go round the other side
    if arc[30][0] - arc[30][1] < (A[0]+B[0])/2 - (A[1]+B[1])/2:
        a1 -= 2*math.pi
        arc = [(O[0]+r*math.cos(a0+(a1-a0)*i/60), O[1]+r*math.sin(a0+(a1-a0)*i/60)) for i in range(61)]
    # string first (behind), arrow, then limbs on top
    P = (7.2 - 0.75 * pull, 8.2 + 0.75 * pull)
    plot(g, line(A, P) + line(P, B), 'white.1')
    if pull:
        tip = (P[0] + 8.5 + 0.4 * pull, P[1] - 8.5 - 0.4 * pull)
        plot(g, line(P, tip), 'wood.3')
        plot(g, [tip, (tip[0]-1, tip[1]), (tip[0], tip[1]+1)], 'white.2' if kind == 'titanium' else 'violet.2')
        plot(g, [(P[0]-1, P[1]), (P[0], P[1]+1), (P[0]-1, P[1]+1)], 'white.1')
    for (x, y) in arc:          # limb: 2 px thick: light on the inner (string) side, dark outside
        plot(g, [(x, y)], f'{ramp}.4'); plot(g, [(x + 0.8*(x-O[0])/r, y + 0.8*(y-O[1])/r)], f'{ramp}.2')
    for (x, y) in arc:
        plot(g, [(x + 1.6*(x-O[0])/r, y + 1.6*(y-O[1])/r)], f'{ramp}.0', only_empty=True)
    mid = arc[len(arc)//2]
    for d in (-3, -2, -1, 0, 1, 2, 3):           # grip
        p = arc[len(arc)//2 + d]
        plot(g, [p], f'{acc}.2' if abs(d) < 3 else f'{acc}.1')
    plot(g, [arc[len(arc)//2 - 14], arc[len(arc)//2 + 14]], f'{acc2}.2')   # glowing limb gems
    plot(g, [A, B], f'{ramp}.5')
    return g
def arrow(kind):
    g = [[None]*16 for _ in range(16)]
    a, b = (2, 13), (12.5, 2.5)
    plot(g, line(a, b), 'wood.3'); plot(g, line((a[0]+1, a[1]), (b[0]+1, b[1])), 'wood.1', only_empty=True)
    tipc = {'diamond': ('dia.3', 'dia.2', 'dia.0'), 'void': ('violet.3', 'violet.2', 'vo.1')}[kind]
    plot(g, [(12, 3), (13, 2), (13, 3), (12, 2)], tipc[1]); plot(g, [(13, 1), (14, 2), (11, 2), (13, 4)], tipc[2]); plot(g, [(12, 2), (13, 2)], tipc[0])
    plot(g, [(1, 12), (2, 12), (3, 13), (3, 14), (1, 14), (2, 15), (1, 13)], 'white.2' if kind == 'diamond' else 'violet.1')
    plot(g, [(0, 13), (2, 14), (3, 15), (0, 14)], 'white.0' if kind == 'diamond' else 'violet.0')
    return g
if __name__ == '__main__':
    out = Image.new('RGBA', (16*5, 32), (0,0,0,0))
    for r, k in enumerate(['titanium', 'voidsteel']):
        for p in range(4): out.alpha_composite(render(bow(k, [0, 1.5, 2.6, 3.6][p])), (p*16, r*16))
    out.alpha_composite(render(arrow('diamond')), (64, 0)); out.alpha_composite(render(arrow('void')), (64, 16))
    bg = Image.new('RGBA', out.size, (140,140,150,255)); bg.alpha_composite(out); bg.resize((out.width*8, out.height*8), Image.NEAREST).save('bows16.png')
