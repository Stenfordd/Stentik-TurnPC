import json, sys
exec(open('armor_icons.py').read().split("if __name__")[0])
exec(open('bows.py').read().split("if __name__")[0])
exec(open('weapons.py').read().split("if __name__")[0])
G='/mnt/user-data/outputs/ascended_voidsteel'
# --- armor icons (+ voidsteel horns, + cracked row)
TIERS['cracked'] = ('st', 'slit.1', 'st.2')
def horns(g):
    for (x, y, t) in [(3,1,'violet.2'),(3,2,'violet.1'),(4,2,'violet.2'),(12,1,'violet.2'),(12,2,'violet.1'),(11,2,'violet.2')]: g[y][x] = t
    return g
CRACK = {'helmet': [(6,4),(7,5),(7,6),(8,7)], 'chestplate': [(7,3),(8,4),(7,5),(8,6),(7,7),(6,8),(7,9)], 'leggings': [(6,4),(7,5),(6,6),(5,7)], 'boots': [(4,6),(5,8),(4,9),(4,10)]}
armor = {}
for tier in ['steel','titanium','tungsten','voidsteel','cracked']:
    for p in ['helmet','chestplate','leggings','boots']:
        g = armor_grid(p, tier)
        if tier in ('voidsteel', 'cracked') and p == 'helmet': g = horns(g)
        if tier == 'cracked':
            if p == 'helmet':
                for (x, y) in [(3,1),(3,2),(4,2),(12,1),(12,2),(11,2)]: g[y][x] = 'st.2'
            for (x, y) in CRACK[p]: g[y][x] = 'slit.0'
        armor[f'{tier}_{p}'] = g
# --- tools
sheets={'voidsteel':([lighten(b,1.5,10) for b in blobs(f'{G}/tools/D_voidsteel_tools.png',gap=1)],['vo','violet','hot','wood']),
 'steel':([lighten(b,1.35,10) for b in blobs(f'{G}/tools/Fa2_steel_tools.png',gap=1)],['st','leather','wood']),
 'titanium':(blobs(f'{G}/tools/Fb_titanium.png',gap=1),['ti','cu','glow','wood']),
 'tungsten':([lighten(b,1.25,8) for b in blobs(f'{G}/tools/Fc_tungsten.png',gap=1)],['tu','hot','wood'])}
tools = {}
for tier, (sp, ramps) in sheets.items():
    for i, t in enumerate(['pickaxe','axe','shovel','hoe']): tools[f'{tier}_{t}'] = light(pixelize(sp[i+1], 16, ramps))
# --- bows/arrows
bowg = {}
for k in ('titanium','voidsteel'):
    bowg[f'{k}_bow'] = bow(k, 0)
    for p, v in enumerate([1.5, 2.6, 3.6]): bowg[f'{k}_bow_pulling_{p}'] = bow(k, v)
bowg['diamond_tipped_arrow'] = arrow('diamond'); bowg['void_tipped_arrow'] = arrow('void')
def sheet(items, cols, size):
    names = list(items); rows = (len(names) + cols - 1) // cols
    G = [[None] * (cols * size) for _ in range(rows * size)]; pos = {}
    for i, n in enumerate(names):
        ox, oy = (i % cols) * size, (i // cols) * size; pos[n] = (ox, oy)
        for y in range(size):
            for x in range(size): G[oy + y][ox + x] = items[n][y][x]
    return G, pos
CH = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*+=?<>~-_:;/()[]{},`'
def to_rows(G):
    toks = sorted({t for r in G for t in r if t}); key = {t: CH[i] for i, t in enumerate(toks)}
    rows = [''.join(key[t] if t else '.' for t in r) for r in G]
    rows = ['|'.join(r[i:i+8] for i in range(0, len(r), 8)) for r in rows]
    keys = {v: k for k, v in key.items()}; keys['.'] = 'transparent'
    return keys, rows
out = {}
for name, items, cols, size in (('armor_icons', armor, 4, 16), ('tool_icons', tools, 4, 16), ('bows', bowg, 5, 16), ('weapons', grids, 4, 32)):
    G, pos = sheet(items, cols, size)
    parts = []
    for y0 in range(0, len(G), 40 if size == 16 else 32):
        keys, rows = to_rows(G[y0:y0 + (40 if size == 16 else 32)]); parts.append({'origin': [0, y0], 'keys': keys, 'rows': rows})
    out[name] = {'parts': parts, 'pos': pos, 'size': size, 'w': len(G[0]), 'h': len(G)}
    render([r + [None] * 0 for r in G]) if False else None
json.dump(out, open('sheets.json', 'w'))
for n, d in out.items(): print(n, d['w'], d['h'], [len(p['keys']) for p in d['parts']])
