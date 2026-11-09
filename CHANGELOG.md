# Changelog

## Unreleased

### Licensing

- Prepared All Rights Reserved licensing for Rei-owned code, documentation, and assets, including its first official public release.
- Added consistent ARR project metadata and JAR manifest licensing; packaged the ARR notice.
- Preserved applicable unrelated third-party notices.
- GitHub is intended for source hosting only; no GitHub Releases, tags, or JAR publications are planned at this stage.

## 0.1.1 - Pre-launch build - 2026-11-09

### Added

- The `rei:rei` Baubles trinket, duplicate equip validation, wearable enchantment support, and a custom 128 x 128 item icon.
- A saved per-player feeding bonus, clamped to 0-127, with server-side `/ecrei` set, add, sub, and get commands.
- Optional Thaumcraft 6.1.BETA26 integration that adds the equipped player's feeding bonus once to the native Runic Shielding maximum while preserving item infusion, absorption, and recharge.
- Client synchronization for feeding tooltips and equipped-companion visibility, including tracking, respawn, dimension changes, and connection cleanup.
- An animated shoulder companion using an embedded Blockbench model, glow mask, configurable placement, and wing particles with a three-second lifetime.
- Direct Blockbench model import support, the updated Rei model, and project documentation.
- Automated tests for equipment restrictions, persistence, commands, networking, client/server boundaries, Thaumcraft integration, model loading, rendering lifecycle, placement, and particles.

### Fixed

- Companion startup loading now defers texture uploads until Minecraft's texture manager is available.
- Companion clearance and wing particle placement now follow model size and animated wing geometry.

### Release

- Set the mod version to 0.1.1 and the distribution filename to `Rei_EC-0.1.1.jar`.
- Include the software license in the distributable JAR.
- Retained Minecraft 1.12.2, Forge 14.23.5.2859, ForgeGradle 3.0.197, Gradle 5.6.4, and Java 8 compatibility.
- The author's tests have passed so far. In-depth modpack and playthrough testing remains ongoing; automated verification does not establish complete in-game compatibility.

## 0.1.0 - Development scaffold

- Initialized the Forge 1.12.2 project and Gradle wrapper.
- Migrated from ForgeGradle 2.3 to ForgeGradle 3.0.197 with Gradle 5.6.4 to use Forge 14.23.5.2859's `userdev3` development artifact.
