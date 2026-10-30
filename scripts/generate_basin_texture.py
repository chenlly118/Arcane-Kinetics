"""Hand-drawn graphite/brass Focusing Basin atlas and authored cuboid model.

All pixels are drawn below. The user design board is a visual reference only;
neither the board nor dependency bitmaps/models are read by this generator.
"""
from pathlib import Path
import hashlib
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/create_spellbook'
ART = ROOT / 'art/focusing-basin-030'
TILES = {'front': (0, 0), 'side': (1, 0), 'back': (2, 0), 'inner': (3, 0),
         'floor': (0, 1), 'bottom': (1, 1), 'brass': (2, 1), 'rim': (3, 1),
         'post': (0, 2), 'foot': (1, 2), 'clasp': (2, 2), 'trim': (3, 2)}
IRON = ['#242329', '#2c2a31', '#34323b', '#3d3a45', '#494550', '#585260', '#6d6573']
BRASS = ['#493324', '#68462a', '#875a30', '#a87338', '#bf8b48', '#d6a560', '#ecd18c']
VIOLET = ['#34233e', '#49305c', '#674080', '#8d4eb0', '#b06cdf']


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def metal_tile(kind):
    image = Image.new('RGBA', (32, 32), IRON[2])
    d = ImageDraw.Draw(image)
    # Explicit staggered plate courses, in small, low-contrast pixel groups.
    for y, offset in [(0, 0), (8, 7), (16, 2), (24, 11)]:
        d.rectangle((0, y, 31, y + 1), fill=IRON[1])
        d.line((offset, y + 2, offset, min(31, y + 7)), fill=IRON[3])
    for box, shade in [((3, 4, 9, 5), 3), ((17, 3, 25, 4), 4), ((10, 9, 20, 10), 3),
                       ((2, 14, 6, 15), 1), ((22, 13, 29, 15), 3), ((5, 19, 12, 21), 3),
                       ((17, 20, 25, 21), 1), ((9, 27, 17, 28), 3)]:
        d.rectangle(box, fill=IRON[shade])
    if kind in ('front', 'side', 'back'):
        d.rectangle((2, 3, 29, 4), fill=IRON[4])
        d.line((3, 5, 28, 5), fill=IRON[0])
        d.rectangle((2, 24, 29, 26), fill=BRASS[1])
        d.line((3, 24, 28, 24), fill=BRASS[4])
        d.line((3, 27, 28, 27), fill=IRON[0])
        for x in [5, 26]:
            d.rectangle((x, 6, x + 1, 23), fill=IRON[0])
            d.line((x, 7, x, 21), fill=IRON[4])
        # Small embedded arcane inclusions; leave the central panel calm.
        for x, y, length in ([(6, 19, 4), (24, 18, 3)] if kind != 'back' else [(8, 21, 2), (22, 21, 2)]):
            d.rectangle((x, y, x + 1, y + length), fill=VIOLET[2])
            d.point((x, y + 1), fill=VIOLET[4])
        for x, y in [(9, 7), (22, 7), (9, 23), (22, 23)]:
            d.point((x, y), fill=IRON[5])
            d.point((x + 1, y + 1), fill=IRON[0])
    if kind == 'inner':
        d.rectangle((0, 0, 31, 3), fill=IRON[1])
        d.line((0, 4, 31, 4), fill=IRON[4])
        for x, y in [(4, 22), (12, 25), (23, 23), (28, 26)]:
            d.rectangle((x, y, x + 1, y + 2), fill=VIOLET[1])
            d.point((x, y), fill=VIOLET[2])
    if kind == 'floor':
        d.rectangle((3, 3, 28, 28), outline=IRON[0], width=2)
        d.rectangle((5, 5, 26, 26), outline=VIOLET[1])
        # Inlaid stepped diamond rather than a painted imitation liquid.
        points = [(16, 7), (24, 15), (24, 17), (17, 24), (15, 24), (7, 16), (7, 15), (15, 7)]
        d.line(points + [points[0]], fill=VIOLET[2], width=1)
        d.rectangle((13, 13, 18, 18), fill=VIOLET[1])
        d.rectangle((15, 14, 16, 17), fill=VIOLET[3])
        for x, y in [(8, 8), (23, 8), (8, 23), (23, 23)]:
            d.rectangle((x, y, x + 1, y + 1), fill=BRASS[2])
    if kind == 'bottom':
        d.rectangle((4, 4, 27, 27), outline=IRON[0], width=2)
        d.line((6, 6, 25, 25), fill=IRON[1], width=2)
        d.line((25, 6, 6, 25), fill=IRON[1], width=2)
    return image


def brass_tile(kind):
    height = 32 if kind == 'post' else 8
    image = Image.new('RGBA', (8, height), BRASS[3])
    d = ImageDraw.Draw(image)
    d.rectangle((0, 0, 7, height-1), outline=BRASS[1])
    d.line((1, 0, 6, 0), fill=BRASS[6])
    d.line((0, 1, 0, height-2), fill=BRASS[5])
    d.rectangle((1, 1, 2, height-2), fill=BRASS[4])
    d.rectangle((5, 1, 6, height-2), fill=BRASS[2])
    d.line((1, height-1, 7, height-1), fill=BRASS[0])
    d.point((2, 1), fill=BRASS[5])
    if kind == 'post':
        d.rectangle((3, 2, 4, 29), fill=BRASS[2])
        d.line((3, 3, 3, 28), fill=BRASS[3])
        d.rectangle((3, 19, 4, 25), fill=VIOLET[1])
        d.line((3, 20, 3, 24), fill=VIOLET[2])
    if kind in ('foot', 'clasp'):
        d.rectangle((2, 2, 5, 5), fill=BRASS[2])
        d.line((2, 2, 5, 2), fill=BRASS[6])
        d.rectangle((3, 3, 4, 4), fill=IRON[1])
        d.point((3, 3), fill=IRON[6])
    return image.resize((32, 32), Image.Resampling.NEAREST)


def rim_tile():
    image = metal_tile('rim')
    d = ImageDraw.Draw(image)
    d.rectangle((0, 0, 31, 31), outline=IRON[0])
    d.rectangle((1, 1, 30, 30), outline=IRON[5])
    d.rectangle((3, 3, 28, 28), outline=IRON[1])
    d.rectangle((4, 4, 27, 27), fill=IRON[0])
    return image


def main():
    ART.mkdir(parents=True, exist_ok=True)
    atlas = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    for name, (tx, ty) in TILES.items():
        tile = brass_tile(name) if name in ('brass', 'post', 'foot', 'clasp', 'trim') else rim_tile() if name == 'rim' else metal_tile(name)
        atlas.paste(tile, (tx * 32, ty * 32))
    texture = ASSETS / 'textures/block/focusing_basin.png'
    atlas.save(texture)
    metal_tile('side').save(ASSETS / 'textures/block/focusing_basin_particle.png')
    atlas.resize((768, 768), Image.Resampling.NEAREST).save(ART / 'atlas-nearest.png')

    def uv(tile, box=(0, 0, 16, 16)):
        tx, ty = TILES[tile]
        return [tx * 4 + box[0] / 4, ty * 4 + box[1] / 4,
                tx * 4 + box[2] / 4, ty * 4 + box[3] / 4]

    elements = []

    def cube(name, low, high, material='brass', overrides=None):
        faces = {face: {'uv': uv(material), 'texture': '#basin'} for face in ['north', 'south', 'east', 'west', 'up', 'down']}
        if overrides:
            for face, (tile, box) in overrides.items():
                faces[face] = {'uv': uv(tile, box), 'texture': '#basin'}
        elements.append({'name': name, 'from': low, 'to': high, 'faces': faces})

    # Authored four-wall shell, open center and low floor. No inverted upstream mesh.
    cube('floor', [2, 1, 2], [14, 2, 14], 'bottom', {'up': ('floor', (0, 0, 16, 16))})
    for name, low, high, outer, inner, tile in [
        ('front wall', [1, 2, 1], [15, 14, 2], 'north', 'south', 'front'),
        ('rear wall', [1, 2, 14], [15, 14, 15], 'south', 'north', 'back'),
        ('left wall', [1, 2, 2], [2, 14, 14], 'west', 'east', 'side'),
        ('right wall', [14, 2, 2], [15, 14, 14], 'east', 'west', 'side')]:
        cube(name, low, high, 'inner', {outer: (tile, (1, 2, 15, 14)), inner: ('inner', (1, 2, 15, 14))})
    for name, low, high, top_uv in [
        ('front rim', [3, 14, 0], [13, 16, 2], (3, 0, 13, 2)),
        ('rear rim', [3, 14, 14], [13, 16, 16], (3, 14, 13, 16)),
        ('left rim', [0, 14, 3], [2, 16, 13], (0, 3, 2, 13)),
        ('right rim', [14, 14, 3], [16, 16, 13], (14, 3, 16, 13))]:
        cube(name, low, high, 'inner', {'up': ('rim', top_uv),
             'north': ('front', (0, 0, 16, 2)), 'south': ('front', (0, 0, 16, 2)),
             'west': ('side', (0, 0, 16, 2)), 'east': ('side', (0, 0, 16, 2))})
    for x, z in [(0, 0), (13, 0), (0, 13), (13, 13)]:
        cube('brass corner cap', [x, 12.5, z], [x + 3, 16, z + 3], 'brass')
        cube('brass foot', [x, 0, z], [x + 3, 3.5, z + 3], 'foot')
        cube('corner spine', [x + .6, 3.5, z + .6], [x + 2.4, 12.5, z + 2.4], 'post')
    # Raised brass front latch and smaller lateral clips.
    cube('front latch', [6.5, 4, .25], [9.5, 8, 1.25], 'clasp')
    cube('rear latch', [7, 4, 14.75], [9, 7, 15.5], 'brass')
    cube('left clip', [.5, 4, 7], [1.25, 7, 9], 'clasp')
    cube('right clip', [14.75, 4, 7], [15.5, 7, 9], 'clasp')
    model = {'credit': 'Arcane Kinetics: independently hand-drawn pixels and authored cuboids',
             'parent': 'minecraft:block/block', 'ambientocclusion': True,
             'textures': {'basin': 'create_spellbook:block/focusing_basin', 'particle': 'create_spellbook:block/focusing_basin_particle'},
             'elements': elements}
    write(ASSETS / 'models/block/focusing_basin.json', model)
    states = {'variants': {f'facing={facing},powered={str(powered).lower()}': {'model': 'create_spellbook:block/focusing_basin', 'y': rotation}
                          for facing, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]
                          for powered in [False, True]}}
    write(ASSETS / 'blockstates/focusing_basin.json', states)
    atlas_path = ROOT / 'src/main/resources/assets/minecraft/atlases/blocks.json'
    atlas_sources = json.loads(atlas_path.read_text(encoding='utf-8'))
    kept = []
    for source in atlas_sources['sources']:
        if 'textures' in source:
            source['textures'] = [name for name in source['textures'] if name != 'create:block/basin']
            if not source['textures']:
                continue
        kept.append(source)
    atlas_sources['sources'] = kept
    write(atlas_path, atlas_sources)
    manifest = {'atlas': [128, 128], 'tile_pixels': 32, 'elements': len(elements),
                'texture_sha256': hashlib.sha256(texture.read_bytes()).hexdigest(),
                'dependency_images_or_models_read': False, 'reference_is_visual_only': True,
                'method': 'Explicit pixel rectangles/lines and independently authored cuboids',
                'tiles': TILES}
    write(ART / 'manifest.json', manifest)
    print(json.dumps(manifest, indent=2))


if __name__ == '__main__':
    main()
