# Spear Evolution (Fabric, Minecraft 26.2)

Every kill evolves your spear: Wooden -> Copper -> Iron -> Gold -> Lapis -> Redstone -> Emerald -> Diamond -> Netherite -> God.

## Build & install
Requirements: **JDK 25**, **Gradle 9.3+** (Minecraft 26.x ships on Java 25 and is unobfuscated; no mappings are used).
```
gradle wrapper        # once, optional
gradle build          # jar -> build/libs/spear-evolution-1.0.0.jar
```
Put the jar in `mods/` of a 26.2 server **and** every client, together with Fabric Loader >= 0.19.3 and Fabric API (26.2 build).
If `gradle.properties` versions are rejected, set `loom_version` / `fabric_api_version` to the newest published
(see maven.fabricmc.net) - the source has no other version-specific build settings.

## Damage (total = base + 4 HP bonus + sharpness bonus 0.5*S+0.5)
| Tier | Base | Sharp | Lunge | Total HP |
|---|---|---|---|---|
| Wooden | 1 | 0 | 0 | 5.0 |
| Copper | 2 | 1 | 0.5 | 7.0 |
| Iron | 3 | 2 | 1 | 8.5 |
| Gold | 3.5 | 3 | 1.5 | 9.5 |
| Lapis | 3.5 | 4 | 2 | 10.0 |
| Redstone | 3.5 | 5 | 2.5 | 10.5 |
| Emerald | 4 | 5 | 3 | 11.0 |
| Diamond | 4.5 | 5 | 3 | 11.5 |
| Netherite | 5 | 5.5 | 3.5 | 12.25 |
| God | 5 | 8 | 4 | 13.5 |
Sharpness (incl. 5.5 and VIII) is baked into the item's attack-damage attribute, since the enchantment registry can't express it.
Lunge = right-click dash, power 0.35 + 0.30 x Lunge, 1.5 s cooldown.

## Special mechanics
* **Shield breaking**: Netherite and God spear hits stop the target's blocking and disable the shield (10 s; config `shieldDestroy` destroys it instead).
* **God Spear (rainbow, animated)**: hold it, sneak, press swap-hand (F). Next hit on another player = visual lightning + 4 extra hearts. 30 s cooldown, action-bar feedback.
* **God ban**: killing a player with the God Spear runs the vanilla `/ban` on the victim (persists in banned-players.json; `/pardon` works) and logs `[GOD SPEAR BAN] killer (uuid) slew victim (uuid)`. Disable with `banOnGodKill=false`.
* **Progression** is stored per UUID in `<world>/spear_evolution/players.json`; one tier per kill, no skipping. With `strictInventory` the
  player always holds exactly one spear of their tier (dropped/lost/dead -> re-issued; higher or duplicate spears removed).

## Commands (op level 2)
`/spear check <player>` | `/spear set <player> <tier|1-10>` | `/spear reset <player>` | `/spear reload`

## Config: config/spear_evolution.json
banOnGodKill, banReason, countMobKills, killsPerUpgrade, strictInventory, godAbilityCooldownSeconds, godAbilityWindowSeconds, godAbilityExtraHearts, shieldDisableSeconds, shieldDestroy.

`tools/gen_textures.py` regenerates all textures (Pillow).
