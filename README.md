# Rei

Rei is a Minecraft 1.12.2 companion mod for the Eternal Confluence modpack. Equipping the Rei bauble displays an animated fairy near the player's shoulder and enables a saved, player-specific feeding bonus for Thaumcraft Runic Shielding.

## Installation

- Minecraft 1.12.2 with Forge 14.23.5.2859.
- Baubles or a compatible fork.
- Optional: Thaumcraft 1.12.2-6.1.BETA26 for Runic Shielding integration.

## Using Rei

Equip her in a compatible Baubles slot. Rei stacks to one, prevents a second copy from being equipped through normal equipment validation, and remains removable and transferable. The companion is a visual attachment, without a separate entity or hitbox.

The feeding bonus belongs to the player, persists through death and world saves, and is clamped to 0-127.

```text
/ecrei <player> get
/ecrei <player> set <amount>
/ecrei <player> add <amount>
/ecrei <player> sub <amount>
```

With Thaumcraft installed, an equipped Rei adds the saved bonus once to the player's native Runic Shielding maximum, alongside ordinary item infusion. Thaumcraft retains control of absorption and recharge. Unequipping Rei removes her bonus from the maximum without deleting the saved value. This version provides command-driven progression; it does not add automatic feeding mechanics or crafting recipes.

The server controls feeding values and equipped-companion visibility. Relevant state is synchronized to clients and refreshed across player lifecycle changes. Multiplayer requires matching Rei versions. Client preferences in `config/rei-visuals.cfg` control companion placement, size, render distance, glow, particles, and animation settings; they do not change shielding or progression.

## License

See [CHANGELOG.md](CHANGELOG.md) for project changes and [LICENSE](LICENSE) for the All Rights Reserved notice covering Rei's own code, documentation, and original assets. Permission from gberguy is required for copying, modification, redistribution, sublicensing, or sale, except as permitted by applicable law. Unrelated third-party notices remain applicable.
