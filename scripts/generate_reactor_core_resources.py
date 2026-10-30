"""Hand-authored core pixel art and recessed frames; no generated-image input.

Each 64px face is drawn from the palette, rectangles and pixel paths below.
Writes the two existing core models, eight faces and one crystal edge sprite.
Motion is authored as vanilla texture frames; the mechanical frame stays fixed.
"""
from pathlib import Path
import itertools
import json
import math
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/create_spellbook'
ART = ROOT / 'art/reactor-cores-024'
CORES = ('black_hole_reaction_core', 'black_hole_energy_core')
FACES = ('front', 'side', 'top', 'bottom')
# The neutral train-casing metal and existing arcane casing's exact trim ramp.
STEEL = ('#17191c', '#1e2126', '#303639', '#353b3e', '#444b4c', '#52595a')
TRIM = ('#2a3157', '#384575', '#4c5496', '#6456b6', '#ac84ef', '#d8b4ff')
VIOLET = ('#111424', '#192039', '#2a3157', '#384575', '#6456b6', '#8368d8', '#ac84ef', '#d8b4ff')
VORTEX_FRAMES = 24
SHIMMER_FRAMES = 16
CRYSTAL_EDGE = 'create_spellbook:block/reactor_core/energy_crystal_edge'


def pixel_path(draw, points, color, width=1):
    draw.line(points, fill=color, width=width, joint='curve')


def bracket():
    """One deliberately drawn L-shaped arcane corner, rotated for the others."""
    image = Image.new('RGBA', (13, 13))
    d = ImageDraw.Draw(image)
    d.polygon([(0, 0), (12, 0), (12, 5), (5, 5), (5, 12), (0, 12)], fill=TRIM[1])
    d.polygon([(1, 1), (11, 1), (11, 4), (4, 4), (4, 11), (1, 11)], fill=TRIM[3])
    d.line([(1, 11), (1, 1), (11, 1)], fill=TRIM[5])
    d.line([(2, 10), (2, 2), (10, 2)], fill=TRIM[4])
    d.line([(5, 12), (5, 5), (12, 5)], fill=TRIM[0])
    d.line([(3, 10), (3, 3), (10, 3)], fill=TRIM[2])
    d.rectangle((6, 6, 8, 8), fill=VIOLET[4])
    d.point((6, 6), fill=VIOLET[7])
    return image


def frame(image):
    d = ImageDraw.Draw(image)
    # Four continuous steel rails; authored broad facets, without random noise.
    for box in [(0, 0, 63, 7), (0, 56, 63, 63), (0, 8, 7, 55), (56, 8, 63, 55)]:
        d.rectangle(box, fill=STEEL[2])
    d.line([(0, 63), (0, 0), (63, 0)], fill=STEEL[4])
    d.line([(1, 62), (1, 1), (62, 1)], fill=STEEL[3])
    d.line([(1, 63), (63, 63), (63, 1)], fill=STEEL[0])
    for y in (2, 57):
        for x, width, shade in [(13, 8, 3), (22, 7, 2), (34, 9, 3), (44, 7, 2)]:
            d.rectangle((x, y, x + width, y + 3), fill=STEEL[shade])
            d.line((x, y, x + width, y), fill=STEEL[min(5, shade + 1)])
            d.line((x + width, y + 1, x + width, y + 4), fill=STEEL[1])
    for x in (2, 57):
        for y, height, shade in [(13, 9, 3), (23, 6, 2), (34, 8, 3), (43, 8, 2)]:
            d.rectangle((x, y, x + 3, y + height), fill=STEEL[shade])
            d.line((x, y, x, y + height), fill=STEEL[min(5, shade + 1)])
            d.line((x + 4, y, x + 4, y + height), fill=STEEL[1])
    d.rectangle((7, 7, 56, 56), outline=STEEL[0], width=2)
    d.line([(9, 54), (9, 9), (54, 9)], fill=STEEL[3])
    d.line([(10, 55), (55, 55), (55, 10)], fill=STEEL[1])
    # Small violet inlays, four matching connectors, not an all-purple frame.
    for x, y, vertical in [(30, 1, True), (30, 57, True), (1, 30, False), (57, 30, False)]:
        box = (x, y, x + (3 if vertical else 5), y + (5 if vertical else 3))
        d.rectangle(box, fill=VIOLET[3])
        d.line((x, y + 1, box[2], y + 1), fill=VIOLET[6])
        d.rectangle((x + 1, y + 2, box[2] - 1, box[3] - 1), fill=VIOLET[4])
    corner = bracket()
    for angle, pos in [(0, (0, 0)), (90, (0, 51)), (180, (51, 51)), (270, (51, 0))]:
        tile = corner.rotate(angle)
        image.alpha_composite(tile, pos)


def cavity():
    image = Image.new('RGBA', (64, 64), '#101319')
    d = ImageDraw.Draw(image)
    # Intentional broad recess facets, restrained compared with the focal art.
    for box, color in [((9, 9, 18, 21), '#191d24'), ((45, 12, 53, 26), '#171b22'),
                       ((12, 44, 24, 53), '#171b23'), ((44, 42, 54, 54), '#202431'),
                       ((16, 10, 21, 14), '#242938'), ((48, 30, 54, 36), '#252838')]:
        d.rectangle(box, fill=color)
    for points in [[(13, 15), (19, 21)], [(49, 15), (43, 21)],
                   [(14, 49), (21, 42)], [(49, 49), (42, 42)]]:
        pixel_path(d, points, STEEL[1], 4)
        pixel_path(d, points, STEEL[3], 1)
    return image


def vortex(image, alternate=False, phase=0):
    d = ImageDraw.Draw(image)
    outer = [(11, 28), (14, 20), (20, 14), (28, 11), (37, 12), (45, 16), (51, 24),
             (53, 33), (50, 43), (43, 50), (34, 53), (25, 51), (17, 46), (12, 38)]
    d.polygon(outer, fill=VIOLET[1])
    pixel_path(d, outer + [outer[0]], VIOLET[2], 3)
    # Four hand-placed, stepped spiral ribbons and their short light edges.
    ribbons = [
        [(13, 34), (14, 26), (18, 20), (24, 16), (31, 15), (38, 18), (41, 22)],
        [(28, 13), (36, 14), (43, 18), (48, 24), (49, 31), (47, 37), (43, 40)],
        [(50, 29), (49, 37), (45, 44), (39, 48), (32, 49), (26, 46), (23, 42)],
        [(36, 50), (28, 49), (21, 45), (16, 39), (15, 32), (18, 26), (22, 23)],
    ]
    if alternate:
        ribbons = [[(63 - y, x) for x, y in path] for path in ribbons]
    angle = math.tau * phase / VORTEX_FRAMES
    cosine, sine = math.cos(angle), math.sin(angle)
    def turn(point):
        x, y = point[0] - 31.5, point[1] - 31.5
        return round(31.5 + x * cosine - y * sine), round(31.5 + x * sine + y * cosine)
    ribbons = [[turn(point) for point in path] for path in ribbons]
    for path in ribbons:
        pixel_path(d, path, VIOLET[3], 6)
        pixel_path(d, path, VIOLET[4], 4)
        pixel_path(d, path[1:5], VIOLET[5], 2)
        pixel_path(d, path[2:4], VIOLET[6], 1)
    # An asymmetric deep central void, not a flat uniformly colored disc.
    aperture = [(23, 23), (29, 20), (36, 21), (42, 26), (44, 33), (41, 40),
                (35, 44), (27, 43), (21, 38), (19, 31)]
    d.polygon(aperture, fill='#0b0613')
    pixel_path(d, aperture + [aperture[0]], VIOLET[2], 2)
    d.ellipse((23, 23, 40, 41), fill='#030206')
    d.rectangle((30, 29, 31, 30), fill=VIOLET[7])
    for point, color in [((15, 41), VIOLET[6]), ((44, 16), VIOLET[5]),
                         ((48, 39), VIOLET[7]), ((20, 11), VIOLET[3])]:
        x, y = turn(point)
        d.rectangle((x, y, x + 1, y + 1), fill=color)


def conduits(d):
    for points in [[(31, 10), (31, 20)], [(43, 31), (53, 31)],
                   [(31, 43), (31, 53)], [(10, 31), (20, 31)]]:
        pixel_path(d, points, VIOLET[2], 5)
        pixel_path(d, points, VIOLET[4], 2)
        d.point(points[0], fill=VIOLET[6])


def crystal(image, alternate=False):
    d = ImageDraw.Draw(image)
    d.ellipse((14, 14, 49, 49), fill=VIOLET[1])
    d.ellipse((17, 17, 46, 46), fill=VIOLET[2])
    conduits(d)
    # Deliberately square crystal, its bevels read as a solid amethyst insert.
    d.rectangle((20, 20, 43, 43), fill=VIOLET[2])
    d.rectangle((21, 21, 42, 42), fill=VIOLET[5])
    d.polygon([(21, 21), (42, 21), (38, 25), (25, 25)], fill=VIOLET[7])
    d.polygon([(21, 22), (25, 26), (25, 38), (21, 42)], fill=VIOLET[6])
    d.polygon([(42, 22), (38, 26), (38, 38), (42, 42)], fill=VIOLET[3])
    d.polygon([(22, 42), (26, 38), (38, 38), (42, 42)], fill=VIOLET[4])
    d.rectangle((26, 26, 37, 37), fill=VIOLET[5])
    d.polygon([(27, 27), (35, 27), (32, 30), (27, 33)], fill=VIOLET[6])
    d.rectangle((32, 33, 37, 37), fill='#7566c4')
    d.line([(22, 40), (22, 22), (40, 22)], fill=VIOLET[7])
    if alternate:
        d.polygon([(29, 29), (35, 29), (35, 32), (32, 35), (29, 35)], fill=VIOLET[6])
    else:
        d.polygon([(28, 35), (32, 32), (36, 35), (36, 36), (28, 36)], fill=VIOLET[6])


def diamond(image, dark=False, phase=None):
    d = ImageDraw.Draw(image)
    for start, end in [((14, 14), (25, 25)), ((49, 14), (38, 25)),
                       ((49, 49), (38, 38)), ((14, 49), (25, 38))]:
        pixel_path(d, [start, end], STEEL[0], 7)
        pixel_path(d, [start, end], STEEL[3], 4)
        pixel_path(d, [(start[0], start[1] - 1), (end[0], end[1] - 1)], STEEL[4], 1)
        d.rectangle((start[0] - 2, start[1] - 2, start[0] + 1, start[1] + 1), fill=TRIM[2])
        d.point((start[0] - 2, start[1] - 2), fill=TRIM[4])
    conduits(d)
    for radius, color in [(14, VIOLET[1]), (12, VIOLET[3]), (9, VIOLET[4]),
                           (6, VIOLET[5]), (3, VIOLET[7] if not dark else VIOLET[4])]:
        d.polygon([(31, 31 - radius), (31 + radius, 31), (31, 31 + radius),
                   (31 - radius, 31)], fill=color)
    d.line([(23, 31), (31, 23), (38, 30)], fill=VIOLET[6])
    if dark:
        d.polygon([(31, 26), (36, 31), (31, 36), (26, 31)], fill=VIOLET[1])
        d.rectangle((31, 30, 32, 31), fill=VIOLET[5])
    if phase is not None:
        # Gentle pulse plus an explicitly positioned traveling rim sparkle.
        gleams = [(31, 19), (34, 22), (37, 25), (40, 28), (43, 31), (40, 34),
                  (37, 37), (34, 40), (31, 43), (28, 40), (25, 37), (22, 34),
                  (19, 31), (22, 28), (25, 25), (28, 22)]
        phase %= SHIMMER_FRAMES
        pulse = (0, 0, 0, 1, 1, 1, 2, 2, 1, 1, 1, 0, 0, 0, 0, 0)[phase]
        if pulse:
            d.polygon([(31, 26), (36, 31), (31, 36), (26, 31)], fill=VIOLET[5 + pulse])
        # Pixel glint crosses the cut facets, contained within the diamond.
        sweep = phase * 2 - 15
        for y in range(23, 40):
            for x in range(23, 40):
                if abs(x - 31) + abs(y - 31) <= 8 and abs(x + y - 62 - sweep) <= 1:
                    d.point((x, y), fill=VIOLET[7])
        x, y = gleams[phase]
        d.line((x - 1, y, x + 1, y), fill=VIOLET[6])
        d.line((x, y - 1, x, y + 1), fill=VIOLET[6])
        d.point((x, y), fill=VIOLET[7])


def crystal_edge():
    """Opaque sidewall facets for the four physically raised crystal inserts."""
    image = Image.new('RGBA', (16, 16), VIOLET[4])
    d = ImageDraw.Draw(image)
    d.polygon([(0, 0), (15, 0), (12, 3), (3, 3)], fill=VIOLET[6])
    d.polygon([(0, 1), (3, 4), (3, 12), (0, 15)], fill=VIOLET[5])
    d.polygon([(15, 1), (12, 4), (12, 12), (15, 15)], fill=VIOLET[3])
    d.polygon([(1, 15), (4, 12), (12, 12), (15, 15)], fill=VIOLET[2])
    d.rectangle((4, 4, 11, 11), fill=VIOLET[4])
    d.line((1, 1, 14, 1), fill=VIOLET[7])
    d.line((4, 11, 11, 4), fill=VIOLET[5])
    return image


def uv_for(direction, a, b):
    x, y, z = a; X, Y, Z = b
    return {'north': [16-X, 16-Y, 16-x, 16-y], 'south': [x, 16-Y, X, 16-y],
            'west': [z, 16-Y, Z, 16-y], 'east': [16-Z, 16-Y, 16-z, 16-y],
            'up': [x, z, X, Z], 'down': [x, 16-Z, X, 16-z]}[direction]


def model(name):
    textures = {face: f'create_spellbook:block/reactor_core/{name}_{face}' for face in FACES}
    textures['particle'] = textures['front']
    elements = []
    directions = {'north': 'front', 'south': 'front', 'east': 'side', 'west': 'side',
                  'up': 'top', 'down': 'bottom'}

    def box(label, a, b):
        faces = {}
        for direction, texture in directions.items():
            face = {'texture': '#' + texture, 'uv': uv_for(direction, a, b)}
            axis, boundary = {'west': (0, 0), 'east': (0, 16), 'down': (1, 0),
                              'up': (1, 16), 'north': (2, 0), 'south': (2, 16)}[direction]
            if (a if boundary == 0 else b)[axis] == boundary:
                face['cullface'] = direction
            faces[direction] = face
        elements.append({'name': label, 'from': a, 'to': b, 'faces': faces})

    box('recessed_energy_chamber', [2, 2, 2], [14, 14, 14])
    # Twelve short rails terminate at the eight protectors; no coplanar overlays.
    for axis in range(3):
        other = [i for i in range(3) if i != axis]
        for first, second in itertools.product((0, 14), repeat=2):
            a = [0, 0, 0]; b = [0, 0, 0]; a[axis] = 3; b[axis] = 13
            for i, v in zip(other, (first, second)):
                a[i] = v; b[i] = v + 2
            box('steel_rail_' + str(len(elements)), a, b)
    for corner in itertools.product((0, 13), repeat=3):
        box('arcane_guard_' + str(len(elements)), list(corner), [v + 3 for v in corner])
    if name == CORES[1]:
        textures['crystal_edge'] = CRYSTAL_EDGE
        # 2 model units proud of the recessed chamber, flush with the outer rim.
        # Back faces terminate inside the chamber and are deliberately omitted.
        for direction, back, a, b in [
                ('north', 'south', [5, 5, 0], [11, 11, 2.25]),
                ('south', 'north', [5, 5, 13.75], [11, 11, 16]),
                ('west', 'east', [0, 5, 5], [2.25, 11, 11]),
                ('east', 'west', [13.75, 5, 5], [16, 11, 11])]:
            box('raised_crystal_' + direction, a, b)
            faces = elements[-1]['faces']
            faces.pop(back)
            for side, face in faces.items():
                if side != direction:
                    face['texture'] = '#crystal_edge'
                    face['uv'] = [0, 0, 16, 16]
    return {'parent': 'minecraft:block/block', 'ambientocclusion': True,
            'textures': textures, 'elements': elements}


def generate():
    target = ASSETS / 'textures/block/reactor_core'; target.mkdir(parents=True, exist_ok=True)
    ART.mkdir(parents=True, exist_ok=True)
    tiles = []
    animations = []
    for name in CORES:
        for face in FACES:
            frames = VORTEX_FRAMES if face in ('front', 'side') and name == CORES[0] else SHIMMER_FRAMES if face == 'top' else 1
            images = []
            for phase in range(frames):
                image = cavity()
                if face in ('top', 'bottom'):
                    diamond(image, dark=face == 'bottom' and name == CORES[0],
                            phase=(phase + (5 if name == CORES[1] else 0)) % SHIMMER_FRAMES if face == 'top' else None)
                elif name == CORES[0]:
                    vortex(image, alternate=face == 'side', phase=phase)
                else:
                    crystal(image, alternate=face == 'side')
                frame(image)
                images.append(image)
            sheet = Image.new('RGBA', (64, 64 * frames))
            for phase, image in enumerate(images):
                sheet.paste(image, (0, 64 * phase))
            path = target / f'{name}_{face}.png'; sheet.save(path)
            metadata = path.with_suffix('.png.mcmeta')
            if frames > 1:
                metadata.write_text(json.dumps({'animation': {'width': 64, 'height': 64,
                    'frametime': 2 if frames == VORTEX_FRAMES else 3, 'interpolate': True,
                    'frames': list(range(frames))}}, indent=2) + '\n', encoding='utf8')
                animations.append((name, face, images, 100 if frames == VORTEX_FRAMES else 150))
            elif metadata.exists():
                metadata.unlink()
            tiles.append(images[0])
        (ASSETS / 'models/block' / f'{name}.json').write_text(
            json.dumps(model(name), indent=2) + '\n', encoding='utf8')
    preview = Image.new('RGB', (1024, 512), '#17151d')
    for i, tile in enumerate(tiles):
        preview.paste(tile.resize((256, 256), Image.Resampling.NEAREST), ((i % 4) * 256, (i // 4) * 256))
    preview.save(ART / 'hand-painted-faces.png')
    crystal_edge().save(target / 'energy_crystal_edge.png')
    (ART / 'palette.json').write_text(json.dumps({'steel': STEEL, 'arcane_trim': TRIM, 'violet': VIOLET}, indent=2) + '\n', encoding='utf8')
    # Editable-face preview only; actual in-game model screenshots are separate.
    preview_frames = []
    for phase in range(48):
        image = Image.new('RGB', (768, 256), '#101319')
        for i, (_, _, frames, duration) in enumerate([animations[0], animations[2], animations[3]]):
            index = (phase // (duration // 50)) % len(frames)
            image.paste(frames[index].resize((256, 256), Image.Resampling.NEAREST), (i * 256, 0))
        preview_frames.append(image)
    preview_frames[0].save(ART / 'hand-painted-animation.gif', save_all=True,
                          append_images=preview_frames[1:], duration=50, loop=0, disposal=2)
    print('Generated hand-authored arcane core faces: 24-frame vortex, 16-frame top gleams, four raised crystals.')


if __name__ == '__main__':
    generate()
