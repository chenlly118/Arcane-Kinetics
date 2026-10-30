"""Generate the Ponder schematics for Create: Spellbook.

The schematics are authored here instead of being exported from a world so the
layout can be regenerated deterministically and reviewed in source control.
Run: python scripts/generate_ponder_schematics.py
"""
from pathlib import Path
import json
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'scripts'))
from ponder_nbt import NBT, TAG_COMPOUND, TAG_INT, TAG_LIST, TAG_STRING

OUT = ROOT / 'src/main/resources/assets/create_spellbook/ponder'

MC = 'minecraft:'
CS = 'create_spellbook:'
CREATE = 'create:'

WHITE = MC + 'white_concrete'
SNOW = MC + 'snow_block'

CASING = CS + 'arcane_casing'
GLASS = CS + 'arcane_reinforced_glass'
CONTROLLER = CS + 'reactor_controller'
MANA_IF = CS + 'reactor_mana_interface'
STAB_IF = CS + 'reactor_port'
OUTPUT_IF = CS + 'reactor_output'
REACTION_CORE = CS + 'black_hole_reaction_core'
ENERGY_CORE = CS + 'black_hole_energy_core'
REACTOR_SHAFT = CS + 'black_hole_reaction_shaft'

ARM = CS + 'arcane_casting_arm'
BASIN = CS + 'focusing_basin'
DEPOT = CS + 'arcane_depot'
PRINTER = CS + 'scroll_printer'
AUTO_CASTER = CS + 'arcane_reactor'

SHAFT = CREATE + 'shaft'
COGWHEEL = CREATE + 'cogwheel'
MOTOR = CREATE + 'creative_motor'
TANK = CREATE + 'fluid_tank'
PIPE = CREATE + 'fluid_pipe'
GLASS_PIPE = CREATE + 'glass_fluid_pipe'
PUMP = CREATE + 'mechanical_pump'
LEVER = MC + 'lever'


class Structure:
    def __init__(self, sx, sy, sz):
        self.size = (sx, sy, sz)
        self.palette = []
        self.palette_index = {}
        self.blocks = {}

    def _state(self, name, props):
        props = tuple(sorted((props or {}).items()))
        key = (name, props)
        if key not in self.palette_index:
            entry = {'Name': name}
            if props:
                entry['Properties'] = dict(props)
            self.palette_index[key] = len(self.palette)
            self.palette.append(entry)
        return self.palette_index[key]

    def set(self, pos, name, props=None, nbt=None):
        x, y, z = pos
        sx, sy, sz = self.size
        assert 0 <= x < sx and 0 <= y < sy and 0 <= z < sz, (pos, self.size)
        self.blocks[(x, y, z)] = (self._state(name, props), nbt)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                for x in range(min(x0, x1), max(x0, x1) + 1):
                    self.set((x, y, z), name, props)

    def plate(self, size, x0=0, z0=0):
        for x in range(x0, x0 + size):
            for z in range(z0, z0 + size):
                self.set((x, 0, z), WHITE if (x + z) % 2 == 0 else SNOW)

    def save(self, path):
        palette = self.palette
        blocks = []
        for (x, y, z) in sorted(self.blocks, key=lambda p: (p[1], p[2], p[0])):
            state, nbt = self.blocks[(x, y, z)]
            entry = {
                'pos': (TAG_LIST, (TAG_INT, [x, y, z])),
                'state': state,
            }
            if nbt is not None:
                entry['nbt'] = nbt
            blocks.append(entry)
        root = {
            'size': (TAG_LIST, (TAG_INT, list(self.size))),
            'entities': (TAG_LIST, (0, [])),
            'blocks': (TAG_LIST, (TAG_COMPOUND, blocks)),
            'palette': (TAG_LIST, (TAG_COMPOUND, palette)),
            'DataVersion': 3955,
        }
        path.parent.mkdir(parents=True, exist_ok=True)
        NBT.save(root, path)
        print('wrote', path.relative_to(ROOT), 'blocks', len(blocks), 'palette', len(palette))


def pipe(conn, glass=False):
    """conn is a set of 'north','east','south','west','up','down'."""
    props = {d: ('true' if d in conn else 'false') for d in ('north', 'east', 'south', 'west', 'up', 'down')}
    props['waterlogged'] = 'false'
    return GLASS_PIPE if glass else PIPE, props


def glass_pipe(axis):
    return GLASS_PIPE, {'waterlogged': 'false', 'alt': 'false', 'axis': axis}


def tank_nbt():
    # A one-block Create tank controller needs its structural keys before the
    # tank capacity is usable; PonderLevel does not run the connectivity update.
    return {'id': 'create:fluid_tank', 'Window': True, 'Size': 1, 'Height': 1, 'Luminosity': 0}


def kinetic(nbt_id, speed=64.0):
    return {'id': nbt_id, 'Speed': speed, 'NeedsSpeedUpdate': 1}


def reactor_schematic():
    """9^3 reactor, controller on the south face, external pump in front."""
    s = Structure(13, 11, 13)
    s.plate(13)
    ox, oy, oz = 10, 1, 8  # local (0,0,0); +X local is west, +Z local is north

    def at(x, y, z):
        return ox - x, oy + y, oz - z

    n = 9
    # Edges are always casing.
    for y in range(n):
        for z in range(n):
            for x in range(n):
                boundary = (1 if x in (0, n - 1) else 0) + (1 if y in (0, n - 1) else 0) + (1 if z in (0, n - 1) else 0)
                if boundary >= 2:
                    s.set(at(x, y, z), CASING)
    # Face panels.
    for y in range(1, n - 1):
        for z in range(1, n - 1):
            # x faces
            for x in (0, n - 1):
                s.set(at(x, y, z), GLASS if 2 <= y <= 7 else CASING)
        for x in range(1, n - 1):
            # z=0 is the back, z=n-1 is the front.
            s.set(at(x, y, 0), CASING if y in (1, 8) or x in (1, n - 2) else GLASS)
            s.set(at(x, y, n - 1), GLASS)
    # Roof and floor, with the two cores replacing the centre panels.
    for z in range(1, n - 1):
        for x in range(1, n - 1):
            s.set(at(x, 0, z), CASING)
            s.set(at(x, n - 1, z), CASING)
    s.set(at(4, 4, 0), CONTROLLER, {'facing': 'south', 'formed': 'false'})
    s.set(at(0, 4, 4), MANA_IF)
    s.set(at(6, 4, 0), STAB_IF)
    s.set(at(4, 2, 0), OUTPUT_IF)
    s.set(at(4, 8, 4), REACTION_CORE)
    s.set(at(4, 0, 4), ENERGY_CORE)
    s.set(at(8, 4, 4), REACTOR_SHAFT, {'facing': 'west'})

    # Create shaft and cogwheel on the reactor's west face.
    s.set((1, 5, 4), SHAFT, {'waterlogged': 'false', 'axis': 'x'}, kinetic('create:simple_kinetic'))
    s.set((0, 5, 4), COGWHEEL, {'waterlogged': 'false', 'axis': 'x'}, kinetic('create:simple_kinetic'))

    # External mana supply on the east face: tank -> vertical run -> pump -> interface.
    s.set((12, 1, 4), TANK, {'shape': 'window', 'top': 'true', 'bottom': 'true'}, tank_nbt())
    s.set((12, 2, 4), *glass_pipe('y'))
    s.set((12, 3, 4), *glass_pipe('y'))
    s.set((12, 4, 4), *glass_pipe('y'))
    s.set((12, 5, 4), *pipe({'down', 'west'}))
    s.set((11, 5, 4), PUMP, {'waterlogged': 'false', 'facing': 'west'}, kinetic('create:mechanical_pump'))

    # Named anchors used by the storyboard; exported for a cross-check.
    anchors = {
        'controller': at(4, 4, 0),
        'mana': at(0, 4, 4),
        'stabilizer': at(6, 4, 0),
        'output': at(4, 2, 0),
        'reaction_core': at(4, 8, 4),
        'energy_core': at(4, 0, 4),
        'shaft': at(8, 4, 4),
        'tank': (12, 1, 4),
        'pump': (11, 5, 4),
        'origin': (ox, oy, oz),
    }
    s.save(OUT / 'black_hole_reactor.nbt')
    return anchors


def casting_arm_schematic():
    """Arm on a vertical shaft, mana from the west, depot ahead for processing."""
    s = Structure(9, 7, 11)
    s.plate(9)
    s.set((4, 3, 5), ARM, {'facing': 'south'}, {'id': ARM})
    s.set((4, 2, 5), SHAFT, {'waterlogged': 'false', 'axis': 'y'}, kinetic('create:simple_kinetic'))
    s.set((4, 1, 5), MOTOR, {'facing': 'up'}, {'id': 'create:motor', 'ScrollValue': 64, 'Speed': 64.0, 'NeedsSpeedUpdate': 1})
    # Redstone input for pulse mode, attached to the top of the arm.
    s.set((4, 4, 5), LEVER, {'face': 'floor', 'facing': 'south', 'powered': 'false'}, {'id': 'minecraft:lever'})

    # Mana tank west of the arm, through a pump into the arm's west face.
    s.set((0, 1, 5), TANK, {'shape': 'window', 'top': 'true', 'bottom': 'true'}, tank_nbt())
    s.set((0, 2, 5), *glass_pipe('y'))
    s.set((0, 3, 5), *pipe({'up', 'east'}))
    s.set((1, 3, 5), *glass_pipe('x'))
    s.set((2, 3, 5), PUMP, {'waterlogged': 'false', 'facing': 'east'}, kinetic('create:mechanical_pump', 64.0))
    s.set((3, 3, 5), *glass_pipe('x'))

    # Processing target in front of the arm, one block lower on a small plinth.
    s.set((4, 1, 8), CASING)
    s.set((4, 2, 8), DEPOT, {}, {'id': DEPOT})

    anchors = {
        'arm': (4, 3, 5),
        'motor': (4, 1, 5),
        'shaft': (4, 2, 5),
        'lever': (4, 4, 5),
        'tank': (0, 1, 5),
        'pump': (2, 3, 5),
        'depot': (4, 2, 8),
        'muzzle': (4.5, 4.0, 6.6),
    }
    s.save(OUT / 'arcane_casting_arm.nbt')
    return anchors


def basin_schematic():
    """Basin plus a pump and tank on its east side."""
    s = Structure(9, 6, 9)
    s.plate(9)
    s.set((4, 1, 4), BASIN, {'facing': 'north', 'powered': 'false'}, {'id': BASIN})
    s.set((5, 1, 4), *glass_pipe('x'))
    s.set((6, 1, 4), PUMP, {'waterlogged': 'false', 'facing': 'west'}, kinetic('create:mechanical_pump'))
    s.set((7, 1, 4), *glass_pipe('x'))
    s.set((8, 1, 4), TANK, {'shape': 'window', 'top': 'true', 'bottom': 'true'}, tank_nbt())
    anchors = {
        'basin': (4, 1, 4),
        'pump': (6, 1, 4),
        'tank': (8, 1, 4),
        'impact': (4.5, 3.0, 4.5),
    }
    s.save(OUT / 'focusing_basin.nbt')
    return anchors


def depot_schematic():
    """Depot in front of a powered, mana-fed casting arm."""
    s = Structure(11, 7, 11)
    s.plate(11)
    s.set((5, 1, 5), CASING)
    s.set((5, 2, 5), DEPOT, {}, {'id': DEPOT})
    s.set((5, 3, 8), ARM, {'facing': 'north'}, {'id': ARM})
    s.set((5, 2, 8), SHAFT, {'waterlogged': 'false', 'axis': 'y'}, kinetic('create:simple_kinetic'))
    s.set((5, 1, 8), MOTOR, {'facing': 'up'}, {'id': 'create:motor', 'ScrollValue': 64, 'Speed': 64.0, 'NeedsSpeedUpdate': 1})
    s.set((5, 4, 8), LEVER, {'face': 'floor', 'facing': 'north', 'powered': 'false'}, {'id': 'minecraft:lever'})

    # Mana enters the arm's east face (clockwise of north).
    s.set((6, 3, 8), *glass_pipe('x'))
    s.set((7, 3, 8), PUMP, {'waterlogged': 'false', 'facing': 'west'}, kinetic('create:mechanical_pump'))
    s.set((8, 3, 8), *glass_pipe('x'))
    s.set((9, 3, 8), *pipe({'west', 'down'}))
    s.set((9, 2, 8), *glass_pipe('y'))
    s.set((9, 1, 8), TANK, {'shape': 'window', 'top': 'true', 'bottom': 'true'}, tank_nbt())
    anchors = {
        'depot': (5, 2, 5),
        'arm': (5, 3, 8),
        'motor': (5, 1, 8),
        'lever': (5, 4, 8),
        'tank': (9, 1, 8),
        'pump': (7, 3, 8),
        'muzzle': (5.5, 4.0, 7.0),
    }
    s.save(OUT / 'arcane_depot.nbt')
    return anchors


def printer_schematic():
    """Printer two blocks above a depot, with ink fed into its top face."""
    s = Structure(9, 7, 9)
    s.plate(9)
    s.set((4, 3, 4), PRINTER, {'facing': 'south'}, {'id': PRINTER})
    s.set((4, 1, 4), DEPOT, {}, {'id': DEPOT})
    # Ink reservoir at ground level, then up and across to the printer's top.
    s.set((6, 1, 4), TANK, {'shape': 'window', 'top': 'true', 'bottom': 'true'}, tank_nbt())
    s.set((6, 2, 4), *glass_pipe('y'))
    s.set((6, 3, 4), *glass_pipe('y'))
    s.set((6, 4, 4), *pipe({'down', 'west'}))
    s.set((5, 4, 4), *glass_pipe('x'))
    s.set((4, 4, 4), *pipe({'east'}))
    anchors = {
        'printer': (4, 3, 4),
        'depot': (4, 1, 4),
        'tank': (6, 1, 4),
        'head': (4.5, 2.7, 4.5),
    }
    s.save(OUT / 'scroll_printer.nbt')
    return anchors


def auto_caster_schematic():
    """Autonomous caster facing east, mana tank behind it and a target ahead."""
    s = Structure(9, 7, 9)
    s.plate(9)
    s.set((4, 1, 4), CASING)
    s.set((4, 2, 4), AUTO_CASTER, {'facing': 'east'}, {'id': AUTO_CASTER})
    # West face is the rear inlet; route mana up from a tank on the ground.
    s.set((3, 1, 4), TANK, {'shape': 'window', 'top': 'true', 'bottom': 'true'}, tank_nbt())
    s.set((3, 2, 4), *pipe({'down', 'east'}))
    anchors = {
        'caster': (4, 2, 4),
        'tank': (3, 1, 4),
        'muzzle': (4.5, 3.37, 4.5),
        'target': (7.0, 2.5, 4.5),
    }
    s.save(OUT / 'arcane_reactor.nbt')
    return anchors


def main():
    anchors = {
        'reactor': reactor_schematic(),
        'arm': casting_arm_schematic(),
        'basin': basin_schematic(),
        'depot': depot_schematic(),
        'printer': printer_schematic(),
        'auto_caster': auto_caster_schematic(),
    }
    report = ROOT / 'build/reports/ponder-layout.json'
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(anchors, indent=2), encoding='utf-8')
    print(json.dumps(anchors, indent=2))


if __name__ == '__main__':
    main()
