exec(open('pxicons.py').read())
G='/mnt/user-data/outputs/ascended_voidsteel'; K='/home/claude/installer/assets/ctcdata/assets/ctc/textures/item/'
vs=[lighten(b,1.5,10) for b in blobs(f'{G}/tools/D_voidsteel_tools.png',gap=1)]
st=[lighten(b,1.35,10) for b in blobs(f'{G}/tools/Fa2_steel_tools.png',gap=1)]
ti=blobs(f'{G}/tools/Fb_titanium.png',gap=1)
W = {
 'steel_saber': (st[0], ['st','leather','wood'], 1),
 'titanium_blade': (ti[0], ['ti','cu','glow','wood'], 1),
 'voidsteel_sword': (vs[0], ['vo','violet','wood','bone'], 1),
 'voidsteel_battle_axe': (vs[5], ['vo','violet','wood','bone'], 1),
 'martian_glaive': (Image.open(K+'martian_glaive.png'), ['mars','gold','wood'], 0),
 'tungsten_hammer': (Image.open(K+'tungsten_hammer.png'), ['tu','hot','wood'], 0),
 'witherite_cleaver': (Image.open(K+'witherite_cleaver.png'), ['st','blood','slit'], 0),
 'voidsteel_rapier': (Image.open(K+'voidsteel_rapier.png'), ['vo','glow','white'], 0),
}
grids = {n: light(pixelize(img, 32, ramps, pad=pad)) for n, (img, ramps, pad) in W.items()}
if __name__ == '__main__':
    out = Image.new('RGBA', (32*4, 64), (0,0,0,0))
    for i, n in enumerate(W): out.alpha_composite(render(grids[n]), ((i%4)*32, (i//4)*32))
    bg = Image.new('RGBA', out.size, (140,140,150,255)); bg.alpha_composite(out); bg.resize((1024, 512), Image.NEAREST).save('weap32.png')
