# Third-party notices — Arcane Kinetics 0.3.0

The author confirmed asset redistribution rights and approved the MIT public
release on 2026-10-30. The MIT file covers material the author is entitled to
license. Upstream dependencies and runtime resources retain their own terms;
this document does not relicense them. See `docs/LICENSING_REVIEW.md`.

## Create — The Create Team / The Creators of Create

Code: MIT. Assets under the upstream `src/main/resources/assets/`: All Rights
Reserved. Create 6.0.9 is installed separately, with its bundled Ponder,
Flywheel and Registrate libraries. The addon uses Create APIs, parent models,
native partials, connected textures and runtime palette/resource references.

The current Focusing Basin replaces the former baked Create recolor. Its
graphite/brass atlas, purple inlays, particle tile and 25-cuboid open shell are
independently drawn/authored in `scripts/generate_basin_texture.py`, guided by
the user's retired-machine design board. The generator does not read upstream
textures or models or sample pixels from the reference board. The old derived
PNG is kept only in local backups and is not distributed.

https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md

## Iron's Spells 'n Spellbooks — Iron431, Lab3 and contributors

All Rights Reserved. The upstream license allows modpack use and independently
written addons depending on the mod; it separately restricts asset use and
redistribution. Iron's Spells 1.21.1-3.16.3 is installed separately. The addon
uses its spell lifecycle, SpellBook and renderer APIs, sounds, runtime scroll
and book sprites, and black-hole rendering resources. No upstream mod JAR is
bundled. Upstream runtime resources remain in the separately installed mod
and are not covered by this addon's MIT license.

https://github.com/iron431/irons-spells-n-spellbooks/blob/1.21/LICENSE.md

## Separately installed libraries and optional integrations

- Curios 9.5.1: TheIllusiveC4 and contributors, LGPL-3.0-or-later as declared
  in the checked JAR; used for equipment integration.
- GeckoLib 4.9.3: bernie-g and contributors, MIT as declared in the checked JAR.
- Player Animator 2.0.4: KosmX and contributors, MIT as declared in the checked JAR.
- Iron's Lib 1.21.1-2.2.0: Iron431, All Rights Reserved as declared in the checked JAR.
- JEI 19.64.0.455: mezz and contributors, MIT; optional public API integration.
- Create: Wizardry 0.5.1-pre1: TTZPlayz, All Rights Reserved; optional resource
  identifier and registry-alias compatibility. See `LICENSES/CREATE_WIZARDRY_NOTICE.md`.
- Create: Enchantment Industry 2.3.0: RaymondBlaze / DragonsPlus, LGPL-3.0;
  historically inspected for printer arrangement. No CEI geometry, code or
  textures are incorporated according to the retained development records.

These dependency JARs are not distributed inside the addon.

## Supplied and project-authored assets

The project icon (`src/main/resources/logo.png`) is the author's supplied
in-game Focusing Basin image, copied without redrawing or changing its pixels.
It depicts the independently authored basin described above.

The user supplied the spell emitter and auto-caster geometry/atlases and the
Engineer's Handbook model/material archive. Liquid Divine Soul textures and
animation metadata came from the user's original modpack. The author confirmed
that these supplied files are original or have public redistribution permission
and will provide source details. Any third-party terms and attribution must be
preserved; this confirmation does not automatically relicense those assets as MIT.

Spell icons and a handbook cover have project image-generation records; core
sprites and their animation are drawn in `scripts/generate_reactor_core_resources.py`.
Other addon geometry, palettes and Ponder schematics have authored generation
records. Raw references and supplied archives remain outside the public source
allowlist. No new image generation was used for this release preparation.

Minecraft and NeoForge textures are referenced by resource identifier at
runtime; the addon does not redistribute the Minecraft game or its JARs.

The 0.4.0 utility-device sprites are hand-drawn 16-by-16 pixel assets made by
`scripts/generate_utility_resources.py`. The firework reactor reuses the
user-supplied auto-caster body geometry and atlas, with independently authored
emerald launch details; it does not incorporate an upstream machine model.
The healing station and portal funnel geometry and palettes are authored in
the same script. The healing station references Create's shaft partial at
runtime. Invisibility compatibility mixins are independently written against
the installed Flywheel and optional Sodium 0.8.13 APIs; no dependency source,
bytecode, or textures are bundled. No image generation was used for these assets.

In 0.4.3 the healing station is replaced by an independently authored treatment
lamp, visually referencing the tiers and sight chamber of Create 6.0.9's fluid
tank, its bearing socket, and Minecraft's beacon. Neither upstream geometry nor
texture pixels are incorporated. Its seven 16-by-16 sprites and the new handheld
concealment-lens icon are hand-pixelled in
`scripts/generate_utility_polish_resources.py`. The firework reactor now retains
the supplied auto-caster's exact 12-element body and four-element crystal. Only
the six amethyst palette shades of its supplied atlas are recolored to emerald;
the brass and blue regions are preserved. No image generation is used.

In 0.4.6 the healing station follows Minecraft 1.21.1's beacon silhouette,
vertically fitted above Create's existing rotating shaft partial. Glass and
obsidian are referenced by their vanilla resource identifiers. The original
beacon pattern is recolored to gold at runtime using the existing vanilla
paletted-permutations atlas system; only color-key samples and the target gold
palette are included. Its violet base trim is an independently drawn 16-pixel
sprite. No vanilla texture image, Create shaft image, dependency code or game
JAR is redistributed. `scripts/generate_healing_beacon.py` records this process.

Starting with 0.4.1, the portal funnel references Create 6.0.9's brass funnel
block, item and flap models by resource identifier. Its atlas source reads the
separately installed Create textures at resource reload and recolors brass
pixels using the arcane-ingot palette in memory. The original PNGs and geometry
are not redistributed. Create's native FilteringBehaviour, list/attribute
filters, value settings, slot positioning and flap rendering APIs are reused.
Create authors retain credit for the referenced models, textures and APIs under
the Create license linked above.

Starting with 0.4.4, portal funnels subclass the installed Create funnel blocks
and FunnelBlockEntity. Native belt insertion, extraction, placement, wrenching,
filtering and flap animation are reused. A narrowly scoped accessor binds only
the addon's subclass to its own portal inventory, because Create 6.0.9 has no
public factory for the three privately held inventory/filter behaviours. Native
Create funnels retain their original behaviours. All 112 wall/belt state models
reference the installed Create geometry and recolor sprites at runtime; no
upstream Java classes, model geometry or texture images are bundled. The
utility Ponder storyboards, layouts, captions and compatibility code are authored
for this addon using the installed Ponder/Create APIs.

The Invisibility Device is removed in 0.4.4, including its item, recipe, assets,
positional-mask storage handler, synchronization payload and eight rendering
mixins. Historical notices above describe earlier releases. No invisibility
device functionality or compatibility hook is retained in the current addon.

The Crimson Executioner housing, seven 16-by-16 sprites and cuboid model are
independently hand-authored in `scripts/generate_crimson_executioner_resources.py`.
The supplied crimson-spike design board was used only as a visual reference;
its pixels and geometry are not incorporated. Shaft rendering and fluid-window
tiling use the installed Create APIs. Blood Needles visuals are drawn by the
installed Iron's Spells renderer using a client-only, unspawned visual proxy;
no Iron's Spells texture or geometry is copied into this addon. Damage sources
and the blood fluid reference that mod's existing registrations.

The Healing Station and Crimson Executioner entries above describe historical
releases. Both machines and their assets were removed in 0.4.12.

## Gradle wrapper

Gradle Inc. / Gradle contributors, Apache License 2.0. License text is retained
in `LICENSES/Gradle-Apache-2.0.txt`. The wrapper is a source-build tool and is not
included inside the Mod JAR.

https://github.com/gradle/gradle/blob/master/LICENSE
