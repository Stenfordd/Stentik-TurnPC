exec(open('sheets.py').read().split("out = {}")[0])
I='/home/claude/stentik-turnpc/ctc-core/src/main/resources/assets/ctccore/textures/item/'
K='/home/claude/installer/assets/ctcdata/assets/ctc/textures/item/'
n=0
for name, g in armor.items():
    tier, piece = name.split('_')
    fn = (f'cracked_voidsteel_{piece}' if tier == 'cracked' else f'{tier}_{piece}')
    render(g).save(I + fn + '.png'); n += 1
for name, g in tools.items(): render(g).save(I + name + '.png'); n += 1
for name, g in bowg.items(): render(g).save(I + name + '.png'); n += 1
for name, g in grids.items(): render(g).save(K + name + '.png'); n += 1
print('icons written', n)
