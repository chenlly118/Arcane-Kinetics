# Arcane Kinetics

<img src="src/main/resources/logo.png" alt="Arcane Kinetics Focusing Basin icon" width="128" align="right" />

Starting with 0.5.0, the Arcane Kinetics name and Focusing Basin icon are shown in game.

[简体中文](README.md) · [Changelog](CHANGELOG.md) · [Modpack permission](MODPACK_PERMISSIONS.md)

A **Minecraft 1.21.1 / NeoForge** addon connecting Create with Iron's Spells 'n Spellbooks. Cast mechanical spells, automate spell processing and scroll printing, and power Create networks with a black-hole reactor.

**0.5.0 Beta**: [GitHub Releases](https://github.com/chenlly118/Arcane-Kinetics/releases) · [Modrinth](https://modrinth.com/project/create-spellbook) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/arcane-kinetics) · [Release notes](release/notes/0.5.0.md).

## Features

- A mechanical school with **14 spells** for combat, mining, processing, kinetic power and mechanical companions.
- A Focusing Basin, Enchanted Depot, Spell Sensor, Arcane Spell Emitter and automatic caster; processing integrates with Create depots, belts and sequenced assembly.
- A Scroll Printer that can print batches of scrolls or upgrade existing scrolls.
- A Firework Reactor and paired Portal Funnels that transfer items across dimensions.
- A 5–19 block cubic black-hole reactor with shared resources, kinetic output and instability accidents.
- An Engineer's Handbook, Enchanted Wrench, Arcane Crank, mana containers and materials.
- English and Simplified Chinese localization, Create Ponder tutorials, and optional JEI recipe displays.

## Requirements

Install the same addon version on client and server. Use Java 21, Minecraft 1.21.1, NeoForge 21.1.251+, Create 6.0.9, Iron's Spells 'n Spellbooks 1.21.1-3.16.3, Curios 9.5.1, GeckoLib 4.9.3, Iron's Lib 1.21.1-2.2.0 and Player Animator 2.0.4+1.21.1. JEI 19.64.0.455 is optional. The declared Create range is [6.0.9,6.1), and the Iron's Spells range is [1.21.1-3.16.3,1.21.1-3.17); versions beyond the checked set are not individually tested. Create supplies Ponder, Flywheel and Registrate. Dependencies are installed separately.

Create: Diesel Generators and Create: Wizardry unlock optional integrations. This beta has not completed long factory stress tests, remote multiplayer acceptance or exhaustive third-party spell testing. An unstable reactor can damage the world; try it in a test world and keep backups. See the [recipe format](docs/ARCANE_PROCESSING_RECIPES.md) for customization.

## Building

With JDK 21, run `./gradlew --no-daemon build` (`gradlew.bat` on Windows). Dependencies download from public sources. Validate the main artifact with `python scripts/release_check.py --jar build/libs/create-spellbook-1.21.1-neoforge-0.5.0.jar`.

## Licensing

Original code uses the author-confirmed [MIT license](LICENSE). Third-party material is governed separately; read [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and the [licensing review](docs/LICENSING_REVIEW.md). [Modpacks are welcome](MODPACK_PERMISSIONS.md). Report issues with exact versions, reproduction steps and sanitized logs.
