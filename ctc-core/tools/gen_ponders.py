#!/usr/bin/env python3
"""2.0 Ponder schematics (voidsteel processing, chip assembly, voidsteel gear) + their lang lines, read from VoidsteelScenes.java."""
import gzip, json, os, re, struct
R = os.path.join(os.path.dirname(__file__), '../src/main/')
A = R + 'resources/assets/ctccore/'

def nbt_str(v): b = v.encode(); return struct.pack('>H', len(b)) + b
def tag(t, name, payload): return bytes([t]) + nbt_str(name) + payload
def compound(items): return b''.join(items) + b'\x00'
def int_list(vals): return bytes([3]) + struct.pack('>i', len(vals)) + b''.join(struct.pack('>i', v) for v in vals)
def comp_list(comps): return bytes([10]) + struct.pack('>i', len(comps)) + b''.join(compound(c) for c in comps)
def schematic(name, size, blocks):
    palette = []
    def st(n, props):
        key = (n, tuple(sorted((props or {}).items())))
        if key not in palette: palette.append(key)
        return palette.index(key)
    base = [(x, 0, z, 'minecraft:snow_block' if (x + z) % 2 else 'minecraft:smooth_stone', None) for x in range(size[0]) for z in range(size[2])]
    bl = [(x, y, z, st(n, pr)) for x, y, z, n, pr in base + blocks]
    pal = []
    for n, props in palette:
        items = [tag(8, 'Name', nbt_str(n))]
        if props: items.append(tag(10, 'Properties', compound([tag(8, k, nbt_str(v)) for k, v in props])))
        pal.append(items)
    root = compound([tag(3, 'DataVersion', struct.pack('>i', 3955)), tag(9, 'size', int_list(list(size))), tag(9, 'palette', comp_list(pal)),
                     tag(9, 'blocks', comp_list([[tag(9, 'pos', int_list([x, y, z])), tag(3, 'state', struct.pack('>i', s))] for x, y, z, s in bl])),
                     tag(9, 'entities', bytes([0]) + struct.pack('>i', 0))])
    with gzip.open(A + f'ponder/{name}.nbt', 'wb') as f: f.write(bytes([10]) + nbt_str('') + root)

def mixer(x, blaze):
    return [(x, 1, 3, 'create:blaze_burner', {'blaze': blaze, 'facing': 'north'}), (x, 2, 3, 'create:basin', {'facing': 'down'}),
            (x, 4, 3, 'create:mechanical_mixer', None), (x, 5, 3, 'create:creative_motor', {'facing': 'down'})]
schematic('voidsteel_processing', (9, 6, 7),
          [(1, 2, 3, 'create:crushing_wheel', {'axis': 'z'}), (3, 2, 3, 'create:crushing_wheel', {'axis': 'z'}), (2, 1, 3, 'create:depot', None)]
          + mixer(5, 'kindled') + mixer(7, 'seething'))
schematic('chip_assembly', (7, 5, 5),
          [(x, 1, 2, 'create:depot', None) for x in (1, 3, 5)]
          + [(1, 3, 2, 'create:deployer', {'facing': 'down', 'axis_along_first': 'true'}), (3, 3, 2, 'create:spout', None),
             (5, 3, 2, 'create:mechanical_press', {'facing': 'north'})])
schematic('voidsteel_finishing', (7, 5, 5),
          [(x, y, 2, 'create:mechanical_crafter', {'facing': 'south', 'pointing': 'up' if x == 1 else 'left'}) for x in (1, 2, 3) for y in (1, 2, 3)]
          + [(5, 1, 2, 'create:depot', None), (5, 3, 2, 'create:spout', None)])
old = A + 'ponder/voidsteel_melting.nbt'
if os.path.exists(old): os.remove(old)

# lang: titles and texts in the order they appear in each method
src = open(R + 'java/dev/stentik/ctccore/ponder/VoidsteelScenes.java').read()
lp = A + 'lang/en_us.json'
lang = json.load(open(lp))
lang = {k: v for k, v in lang.items() if not k.startswith('ctccore.ponder.voidsteel_melting')}
for body in re.split(r'public static void \w+\(', src)[1:]:
    t = re.search(r'scene\.title\("(\w+)", "([^"]+)"\)', body)
    if not t: continue
    scene = t.group(1)
    lang[f'ctccore.ponder.{scene}.header'] = t.group(2)
    for i, line in enumerate(re.findall(r'\.text\("((?:[^"\\]|\\.)*)"\)', body), 1):
        lang[f'ctccore.ponder.{scene}.text_{i}'] = line
json.dump(lang, open(lp, 'w'), indent=2, ensure_ascii=False)
print('ponders ok')
