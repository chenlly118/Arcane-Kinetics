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

## Gradle wrapper

Gradle Inc. / Gradle contributors, Apache License 2.0. License text is retained
in `LICENSES/Gradle-Apache-2.0.txt`. The wrapper is a source-build tool and is not
included inside the Mod JAR.

https://github.com/gradle/gradle/blob/master/LICENSE
