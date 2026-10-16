# AfterOS // CRT Blaster  v5.2.0  (NeoForge 1.21.1)

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
| **Floating CRT** (`floating_crt`) | Deploys a squad of 16 (config `droneCount`, 1-40) that orbit you. Each fires at hostile mobs and mobs targeting a player; set `droneTargetPassive=true` to make them shoot every mob. 1 cell per squad. Right-click again to recall. |
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

## Subspace Lance (v1.2.0)
Fan version of the Herrscher of the Void's Subspace Lance (original art, unofficial, no game assets).
Recipe: `AEA / PRP / _R_` = amethyst shard, ender eye, phosphor cell, end rod.
- Tap right-click: a lance is pulled from Imaginary Space and thrown at what you look at. Every 4th one is a bigger "Space Core" burst. Free.
- Hold then release: a barrage of lances (default 4) falls around the target. 1 cell.
- Shift + right-click: opens Imaginary Space at the target. Everything inside is slowed hard and lances rain down for 5 seconds. 2 cells.

## What's new in 2.0.0
- Colored beams: cyan (blaster), red (Overclocked), green (Floating CRT squad).
- Scorched ground: crater floors turn to coarse dirt / glass / blackstone (magma for super blasts). `scorchGround`.
- Shockwave ring and a white flash on big blasts; harmless lightning where the Overclocked beam lands (`superLightning`).
- Action-bar charge meter with Phosphor Cell count on the blaster, Overclocked CRT and Subspace Lance.
- Floating CRT squad: two rings of 8 spinning opposite ways, drones spread their fire instead of all shooting the same mob, and they defend you first (mobs targeting you get priority).
- Subspace Lance: the 4th lance's Space Core now calls down 3 more lances on the same spot.

## What's new in 2.1.0 (ideas borrowed from other mods)
- **Scatter mode** on the CRT Blaster (Shift + right-click cycles Bolt / Beam / Scatter): five small short-range bolts in a cone, a shotgun to go with the long-range modes.
- **Floating CRT modes** (Shift + right-click the item, works live on deployed drones): Guard = hostile mobs only, Hunt = every mob, Hold fire = just follow you.
- Drones never shoot through you, and swing out toward whatever they just shot to get a better angle.

## What's new in 2.2.0 (feel and feedback, borrowed from impact/explosion mods)
- **Camera shake** on big blasts and every Overclocked pulse (`cameraShake`; uses the vanilla hurt tilt, no damage).
- **Hit markers**: a ping sound when your shot lands, a higher one plus a spark burst on a kill.
- **Dust clouds** after blasts and smoke trails on flying blocks.
- **Heat** on the CRT Blaster: each shot adds heat (shown in the action bar). Hit 100% and it overheats and locks for ~4.5 seconds. `heatEnabled`, `heatCoolPerSecond`.

## What's new in 3.0.0: Telekinesis Authority, Soulmine Blades, Herrscher Core of the Void
Rebuilt from scratch for this mod (the originals in the Fabric 1.3.0 jar are compiled-only, so they could not be edited). They reuse that jar's art, sounds and recipes.
- **Telekinesis Authority**: tap = Force Push (cone blast), Shift + right-click = Force Pull, hold on a target = lift it, release = throw (crater on impact), sneak while holding = slam it into the ground.
- **Soulmine Blades**: tap = chain whip (hits and drags a mob to you, or echoes a block into flying shrapnel), hold = Chain Spin (blade ring pulses around you), Shift + right-click = summon / dismiss two orbiting blades that cut hostile mobs (1 Phosphor Cell, 45 s).
- **Herrscher Core of the Void**: tap = Void Step (portal blink where you look), hold + release = rain Subspace Lances (1 cell), Shift + right-click = Void Collapse, a black hole that drags everything in then detonates (3 cells).
Recipes are the same as in the Fabric jar. All numbers are in the config (`telekinesis`, `soulmine_blades`, `void_core` sections).

## What's new in 4.0.0: World Corruption (from the "AfterOS: CRT Corruption" spec)
- **AfterOS // CRT Corruptor** (pink-eyed CRT). Hold to charge, release to fire a pink corruption beam. Charge sets the radius: 5 / 12 / 25 blocks (`maxRadius`). 1 Phosphor Cell.
- **Terrain really transforms**: grass, dirt, stone, sand, deepslate, logs and leaves become Corrupted Grass / Dirt / Stone / Sand / Deepslate / Wood and Glitched Leaves
  (animated black, purple and pink circuitry textures, they glow a little and drop sensible items). Water is not converted (see below).
- **Animated wave, queued**: corruption spreads ring by ring, `blocksPerTick` blocks at a time, over hills, caves and walls (only exposed surfaces change). After the main wave it keeps creeping to neighbouring blocks for `creepSeconds` (0 = no continuous spread).
- **Ground impact**: the beam also craters, flings blocks and knocks mobs back; pink lightning flashes along the wave; shockwave, camera shake, dust and static particles.
- **Corrupted ground** slows and hurts mobs; mobs on it glow and hit harder. Players are immune by default (`corruptPlayers`).
- **AfterOS Restoration Tool**: right-click restores outward from where you look, Shift + right-click a 3x larger area. Original blocks are saved with the world, so it works after reloads.
- **Floating CRT** gets a 4th mode, **Corrupt** (Shift + right-click cycles): every shot also starts a small corruption wave, and zones from many drones merge.
- **Commands** (op): `/afteros corrupt <radius>`, `/afteros restore <radius>`, `/afteros info`.
- Safe by design: bedrock and anything with a block entity (chests, furnaces, spawners...) are never touched; `maxActiveFronts`, `maxBlocksPerFront` and `enabled` limit load.
Not included: corrupted digital water (needs a custom fluid), real per-mob "corrupted versions" with custom glowing-eye models, and configurable keybindings (those need custom networking per loader; use the items' right-click controls).

## What's new in 4.1.0: configurable keybindings (custom network packet)
Rebind in Options > Controls > "AfterOS // CRT Arsenal". Keys work from the inventory; the matching item just has to be in it. Works in singleplayer and multiplayer (the server does the work).
- G summon Floating CRT squad, H dismiss them, C switch their targeting mode (Guard / Hunt / Hold fire / Corrupt)
- Z fire a corruption beam (radius 12), X fire a fully charged one (max radius, 2 cells), V restore terrain where you look (sneak = bigger area)
(Charging by holding a key is not included; use the CRT Corruptor itself for a hold-to-charge shot.)

## What's new in 4.2.0: Data Corruption, the mob virus
- New status effect **Data Corruption**. Infected creatures are slowed (-30%), hit harder (+50%), take damage over time, glitch with pink particles,
  **leave a trail of corrupted ground wherever they walk**, and **infect other creatures that stand near them** (every half second, `virusSpreadChance`, within `virusRange`).
- You catch it from: the CRT Corruptor beam, Floating CRTs in Corrupt mode (the target they shoot), and standing on corrupted ground.
- Players are immune by default (`corruptPlayers`). The infection runs out after `virusSeconds` unless it keeps being passed on in a crowd. `virusEnabled=false` turns the whole thing off.
- **Cure**: the Restoration Tool cures creatures in its radius while it restores the ground. `/afteros infect <radius>` and `/afteros cure <radius>` for testing.
Not included: pink glowing eyes / custom models on infected mobs (that needs a custom renderer layer per mob type).

## What's new in 4.3.0: AfterOS Corruption Seed
A block. Place it and a huge corruption wave starts, paced to take about **five minutes** (`seedSeconds`, default 300) and spreading up to 96 blocks (`seedRadius`, only loaded chunks change). Progress shows in the action bar for nearby players.
- **Break the seed to stop it.** What it already corrupted stays until the Restoration Tool (or /afteros restore) puts it back.
- Obsidian-tough (blast resistance 1200); mine it with a pickaxe to get it back.
- Recipe: obsidian x4, phosphor cell x3, nether star, CRT Corruptor (`OPO / PNP / OCO`).
- Infected mobs and corrupted ground work as before. Hard cap `seedMaxBlocks`; up to 2500 blocks per tick so the schedule holds.
Heads up: corrupted blocks are real changes to your world. Back up before testing, or use `/afteros restore`.

## What's new in 5.0.0: the Void Core, rebuilt from its lore (Herrscher Core of the Void)
- **Look**: a small faceted orange-red crystal with a white-orange glowing heart, radiating faint orange Honkai sparks while you hold it.
- **Honkai energy**: every ability runs on a 0-100% energy bar the Core absorbs from the air by itself (`energyRegenPerSecond`), faster while held. No Phosphor Cells needed. Shown in the action bar.
- **Shift + right-click** changes ability, **right-click** uses it:
  1. **Void Step**: instant blink along your line of sight, through walls (finds the farthest free space). 10%
  2. **Portal Gate**: a linked pair of big oval portals (purple rim, near-black centre, glowing purple star, swirling edge). One in front of you, one where you look. Walk through; arrows and fireballs get redirected too. 15%
  3. **Portal Lances**: opens a ring of portals (default 16, up to 200) around the target; each fires an incorporeal Subspace Lance that passes through defences and solidifies on the target for a Honkai explosion. 30%
  4. **Spatial Collapse**: crushes the space around the target (black hole, then detonation). 60%
  5. **Imaginary Space**: enter a real pocket dimension (endless starry void, giant floating black cubes, you can fly). Every block you move there is 8 in the Overworld, so it is a shortcut; use the Core again to return to the Overworld. 25%
  6. **Spatial Walk**: toggle. Hover and fly with semi-transparent purple discs under your feet; drains a little energy each second. 5% to start
  7. **Void Crystals**: raises a cluster of black crystal spires, the tallest up to 24-40 blocks (`crystalHeight`). 35%
- Honest limits: walking through solid blocks on foot is not possible (the client decides your movement); Void Step phases you through walls instead. Portals do not follow you around. Redirected attacks are arrows, fireballs and thrown items.
- New data: a custom dimension (`afteros_blaster:imaginary_space`). Back up your world before first use, as always.

## What's new in 5.1.0: Herrscher Core of Flame (Gem of Destruction)
Same crystal as the Void Core but deep crimson, glowing from within and about 45% bigger. Runs on the same Honkai energy bar as the Void Core (absorbs from the air, shown in the action bar). **You are immune to your own fire** (fire resistance is kept up automatically, you are never hurt by your own blasts).
Shift + right-click changes ability, right-click uses it:
1. **Flame Wave** (12%): a wave of thermal energy rolls out 13 blocks, burning and knocking back everything and lighting the ground.
2. **Flame Armor** (8% + 3%/s): toggle. Wrapped in flames; hostile things that get close are scorched.
3. **Inferno** (25%): enormous fire around the target, everything in reach ignited.
4. **Heat Lance** (20%): heat concentrated into a point. Incinerates a target (250 damage, burning) or bores a small molten pit into what it hits.
5. **Heat Sink** (free, recharges you): absorbs thermal energy: puts out fire, cools lava to obsidian/cobblestone and magma to stone.
6. **Nuclear Flare** (80%, 30 s cooldown): a nuclear-scale explosion at the target. Expands ring by ring over 4 s (`nukeSeconds`) out to 24 blocks (`nukeRadius`): vaporized core, then a lava lining, a magma lining, a layer of fused glass, then ash and fire. Soil, rock and structures are transformed; water is boiled away; everything in range is hit (1000 damage at the core, falling off outward).
- Safe by design: bedrock and block entities (chests, furnaces...) are never touched; `thermalEnabled`, `thermalMaxBlocks`, `thermalBlocksPerTick` limit it.
- **These changes are permanent** (the Restoration Tool only undoes corruption). Back up your world, and keep the nuke away from your base.
- Recipe: blaze rod x4, phosphor cell x4, nether star (`BPB / PNP / BPB`).
Honest limits: "seen from space" is represented by a huge flash, sound and camera shake for players within 80 blocks; only loaded chunks change.

## What's new in 5.2.0
- **Honkai energy boss bar**: holding the Void Core or Flame Core shows an energy meter at the top of the screen (purple / red) with the current ability.
- **Herrscher Resonance**: hold the Flame Core in your off hand and use the Void Core's Portal Lances: every lance carries Flame (bigger craters, more damage, fires). Costs 10% extra energy.
- **GUIDE.md**: one-page field guide generated from the real recipes.
