"""Run by hand when mods change: reads ore worldgen out of mod jars and writes tools/where_auto.json for gen_where.py.
usage: python3 tools/extract_ores.py <mods folder>"""
import zipfile, json, sys, os, re
mods = sys.argv[1]
PLANET = [('lunar', 'moon'), ('moon', 'moon'), ('mars', 'mars'), ('martian', 'mars'), ('venus', 'venus'), ('mercury', 'mercury')]
def height(pl):
    cnt = None
    for p in pl:
        if p['type'] == 'minecraft:count': cnt = p['count']
        if p['type'] == 'minecraft:height_range':
            h = p['height']; t = h.get('type', 'minecraft:uniform').split(':')[1]
            def v(a):
                if 'absolute' in a: return a['absolute']
                if 'above_bottom' in a: return -64 + a['above_bottom']
                if 'below_top' in a: return 320 - a['below_top']
            lo, hi = v(h['min_inclusive']), v(h['max_inclusive'])
            peak = (lo + hi) // 2 if t == 'trapezoid' else None
            return max(lo, -64), min(hi, 320), peak
    return None
out = []
for jar in os.listdir(mods):
    if not jar.lower().startswith('northstar'): continue
    z = zipfile.ZipFile(os.path.join(mods, jar)); names = set(z.namelist())
    def j(path): return json.loads(z.read(path)) if path in names else None
    found = {}
    for n in names:
        m = re.match(r'data/northstar/worldgen/biome/(.+)\.json$', n)
        if not m: continue
        planet = next((p for k, p in PLANET if m.group(1).startswith(k)), None)
        if not planet: continue
        for step in j(n).get('features', []):
            for f in (step if isinstance(step, list) else [step]):
                if not isinstance(f, str) or not f.startswith('northstar:'): continue
                pf = j('data/northstar/worldgen/placed_feature/%s.json' % f.split(':')[1])
                if not pf: continue
                cf = pf['feature'] if isinstance(pf['feature'], dict) else j('data/northstar/worldgen/configured_feature/%s.json' % pf['feature'].split(':')[1])
                if not cf or cf['type'] not in ('minecraft:ore', 'minecraft:scattered_ore'): continue
                hr = height(pf['placement'])
                if not hr: continue
                for t in cf['config']['targets']:
                    block = t['state']['Name']
                    if not re.search(r'_ore$', block): continue
                    lt = j('data/northstar/loot_table/blocks/%s.json' % block.split(':')[1])
                    drop = None
                    if lt:
                        items = re.findall(r'"name":\s*"([a-z0-9_:]+)"', json.dumps(lt))
                        drop = next((i for i in items if i != block), None)
                    key = (planet, drop or block)
                    e = found.setdefault(key, {"blocks": set(), "ymin": hr[0], "ymax": hr[1], "best": hr[2]})
                    e["blocks"].add(block); e["ymin"] = min(e["ymin"], hr[0]); e["ymax"] = max(e["ymax"], hr[1])
                    if hr[2] is not None and e["best"] is None: e["best"] = hr[2]
    for (planet, drop), e in sorted(found.items()):
        best = e["best"]
        if best is not None: best = min(max(best, e["ymin"]), e["ymax"])
        out.append({"planet": planet, "items": sorted(e["blocks"]) + ([drop] if drop and drop not in e["blocks"] else []),
                    "ymin": e["ymin"], "ymax": e["ymax"], "best": best})
json.dump(out, open('tools/where_auto.json', 'w'), indent=1)
print(len(out), 'ore entries'); [print(o['planet'], o['items'][-1], o['ymin'], o['ymax'], o['best']) for o in out]
