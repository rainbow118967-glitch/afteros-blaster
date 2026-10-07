# AfterOS // CRT Blaster  v1.1.0  (NeoForge 1.21.1)

Source project. I could not compile or run it where it was written (no JDK/Minecraft libraries
available), so treat the first build as a test: if the compiler complains, send me the error text.

## Build
Requirements: JDK 21 and Gradle 8.10+ (or drop `src/`, `build.gradle`, `gradle.properties`,
`settings.gradle` into the official NeoForge 1.21.1 MDK and use its `gradlew`).

    gradle build            # jar ends up in build/libs/afteros-blaster-1.1.0.jar
    gradle runClient        # test in a dev client

If Gradle can't find the plugin version, bump `net.neoforged.moddev` in `build.gradle` to the
latest 2.0.x. If `neo_version=21.1.256` doesn't exist, use any 21.1.x in `gradle.properties`.

## What's in it
| Item | How it works |
|---|---|
| **CRT Blaster** (`crt_blaster`) | Hold use to charge, release to fire. Shift+right-click switches Bolt / Beam. 1 Phosphor Cell per shot. Impacts now dig a real crater, fling blocks and light fires on the floor. |
| **Floating CRT** (`floating_crt`) | Right-click to deploy (1 cell), right-click again to recall. Hovers beside you and fires every ~1s at the nearest mob (hostiles first, passive mobs too, never you or your tamed pets). Same crater / flying blocks / fire. Lasts 2 minutes by default. |
| **Overclocked CRT** (`super_crt_blaster`) | Hold ~2s, release. 3 cells. A huge beam: 1000 damage to everything in it (one-shots mobs), radius-9 craters pulsing along where it lands, 240 flying blocks, fire. |

Recipes: Floating CRT = `IGI / PEP / ICI` (iron, glass, phosphor cell, ender pearl, copper).
Overclocked CRT = `DND / PBP / DPD` (diamond, nether star, phosphor cell, **a CRT Blaster**).

All three use the new CRT TV model (cabinet, bulging glass, tapered tube, rabbit ears, knobs,
speaker, feet). Overclocked is a red/orange gunmetal recolour, Floating is green/white.

## Terrain rules (all in the server config `afteros_blaster-server.toml`)
- Blocks really are removed. Bedrock, fluids, portals, spawners and the `debris_excluded` tag are never touched.
- Anything with a block entity (chests, furnaces, signs...) is always spared.
- Normal blasts can't break blocks above blast resistance 100 (so no obsidian); the Overclocked CRT can.
- Spawn protection is respected for player-fired shots.
- Flying blocks are visual debris: they never place blocks or drop items. Capped at 500 per dimension.
- `destroyTerrain=false` turns crater digging off entirely; every number above is configurable.

If you already have a world with the old version, delete the old `afteros_blaster-server.toml` once
so the new (stronger) defaults are written.
