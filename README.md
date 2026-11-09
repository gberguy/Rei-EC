# Rei

Rei is a Minecraft 1.12.2 companion mod for the Eternal Confluence modpack. Equipping the Rei bauble displays an animated fairy near the player's shoulder and enables a saved, player-specific feeding bonus for Thaumcraft Runic Shielding.

## Installation

- Minecraft 1.12.2 with Forge 14.23.5.2859; Java 8 is the build target.
- Baubles 1.12-1.5.2, or the compatible BaublesEX 1.12.2-2.3.7 implementation.
- Optional: Thaumcraft 1.12.2-6.1.BETA26 for Runic Shielding integration.

Rei is currently pre-launch; CurseForge is not live. Its first official public release will be All Rights Reserved.

For local testing, place an approved `Rei_EC-0.1.1.jar` build in the instance's `mods` directory. Install the same Rei version on the client and server, with the required Baubles implementation. Use one Baubles implementation at a time. GeckoLib and Cleanroom are not required.

## Using Rei

Rei is available in the Miscellaneous creative tab, or through an operator command:

```text
/give @p rei:rei 1
```

Equip her in a compatible Baubles slot. Rei stacks to one, prevents a second copy from being equipped through normal equipment validation, and remains removable and transferable. The companion is a visual attachment, without a separate entity or hitbox. Its animated model, glow mask, item icon, and small wing particles are included; particles last approximately three seconds at 20 ticks per second.

The feeding bonus belongs to the player, persists through death and world saves, and is clamped to 0-127. Operators with permission level 2 can manage an online player's value:

```text
/ecrei <player> get
/ecrei <player> set <amount>
/ecrei <player> add <amount>
/ecrei <player> sub <amount>
```

With Thaumcraft installed, an equipped Rei adds the saved bonus once to the player's native Runic Shielding maximum, alongside ordinary item infusion. Thaumcraft retains control of absorption and recharge. Unequipping Rei removes her bonus from the maximum without deleting the saved value. This version provides command-driven progression; it does not add automatic feeding mechanics or crafting recipes.

The server controls feeding values and equipped-companion visibility. Relevant state is synchronized to clients and refreshed across player lifecycle changes. Multiplayer requires matching Rei versions. Client preferences in `config/rei-visuals.cfg` control companion placement, size, render distance, glow, particles, and animation settings; they do not change shielding or progression.

## Building

Use a Java 8 JDK selected through `JAVA_HOME`. The included wrapper uses Gradle 5.6.4, ForgeGradle 3.0.197, Forge 14.23.5.2859, and snapshot mappings `20171003-1.12`. No separately installed Gradle is needed.

Place these exact reference JARs in the ignored `dev-libs/mods/` directory:

- `Baubles-1.12-1.5.2.jar` for compilation and tests.
- `BaublesEX-1.12.2-2.3.7.jar` for equipment compatibility tests.
- `Thaumcraft-1.12.2-6.1.BETA26.jar` for integration bytecode tests.

Run:

```powershell
.\gradlew.bat build
```

The build runs the automated tests and produces the reobfuscated mod at `build/libs/Rei_EC-0.1.1.jar`. The JAR includes the All Rights Reserved notice in `META-INF/LICENSE` and matching license metadata in `mcmod.info` and its manifest. Reference mod JARs are not bundled into Rei. Automated checks cover equipment validation, progression, networking, class-loading boundaries, the Thaumcraft integration point, and visual model behavior. They do not replace in-game testing.

## Updating the companion model

The runtime model is `src/main/resources/assets/rei/companion/rei.bbmodel`. To replace it, use a free-format Blockbench project with embedded textures and the idle animation expected by the runtime loader:

```powershell
.\gradlew.bat importReiModel build "-PreiModel=dev-libs/your-model.bbmodel"
```

Keep `left_wing` and `right_wing` groups for wing particle placement. The source model is copied into the runtime resource; no exported Java model or GeckoLib dependency is needed. The imported runtime model is tracked, while local source files in `dev-libs/` remain ignored.

## Development status and license

The author's tests have passed so far. Broader modpack, multiplayer, and playthrough testing remains ongoing during Eternal Confluence development.

See [CHANGELOG.md](CHANGELOG.md) for project changes and [LICENSE](LICENSE) for the All Rights Reserved notice covering Rei's own code, documentation, and original assets. Permission from gberguy is required for copying, modification, redistribution, sublicensing, or sale, except as permitted by applicable law. Unrelated third-party notices remain applicable.

GitHub is intended for source hosting only. No GitHub Releases, tags, or JAR publications are planned at this stage.
