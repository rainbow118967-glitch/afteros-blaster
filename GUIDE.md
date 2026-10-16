# AfterOS // CRT Arsenal: Field Guide (v5.2.0)

Everything in the mod on one page. Item names are as shown in game. Creative tab: Combat (blocks: Building Blocks).
All numbers can be changed in the config file (`afteros_blaster-server.toml` on NeoForge, `config/afteros_crt.properties` on Fabric).

## Crafting recipes
| Item | Pattern (rows) | Key |
|---|---|---|
| AfterOS Corruption Seed | `[OPO]  [PNP]  [OCO]` | O=Obsidian, P=Phosphor Cell, N=Nether Star, C=AfterOS // CRT Corruptor |
| AfterOS // CRT Blaster | `[IGI]  [CAD]  [ RI]` | I=Iron Ingot, G=Glass, C=Copper Ingot, A=Amethyst Shard, D=Diamond, R=Redstone |
| AfterOS // CRT Corruptor | `[APA]  [PBP]  [ACA]` | A=Amethyst Shard, P=Phosphor Cell, B=AfterOS // CRT Blaster, C=Echo Shard |
| Herrscher Core of Flame | `[BPB]  [PNP]  [BPB]` | B=Blaze Rod, P=Phosphor Cell, N=Nether Star |
| AfterOS // Floating CRT | `[IGI]  [PEP]  [ICI]` | I=Iron Ingot, G=Glass, P=Phosphor Cell, E=Ender Pearl, C=Copper Ingot |
| Phosphor Cell | `shapeless` | Amethyst Shard, Redstone |
| AfterOS Restoration Tool | `[ A ]  [APA]  [ S ]` | A=Amethyst Shard, P=Phosphor Cell, S=Stick |
| Soulmine Blades | `[NEN]  [PCP]  [ R ]` | N=Netherite Ingot, E=Echo Shard, P=Diamond Pickaxe, C=Chain, R=Redstone |
| Subspace Lance | `[AEA]  [PRP]  [ R ]` | A=Amethyst Shard, E=Ender Eye, P=Phosphor Cell, R=End Rod |
| AfterOS // OVERCLOCKED CRT | `[DND]  [PBP]  [DPD]` | D=Diamond, N=Nether Star, P=Phosphor Cell, B=AfterOS // CRT Blaster |
| Telekinesis Authority | `shapeless` | Ender Pearl, Amethyst Shard, Echo Shard, Redstone |
| Herrscher Core of the Void | `[ A ]  [ENE]  [ R ]` | A=Amethyst Shard, E=Ender Eye, N=Nether Star, R=Redstone |

## Weapons
- **CRT Blaster**: hold to charge, release to fire. Shift + right-click cycles Bolt / Beam / Scatter. Uses Phosphor Cells and builds heat (100% = overheat).
- **Overclocked CRT**: one huge charged beam, one-shots mobs, craters, lightning. 3 cells.
- **Floating CRT**: right-click deploys a squad of 16 that orbit you; right-click again recalls. Shift + right-click cycles Guard / Hunt / Hold fire / Corrupt.
- **Subspace Lance**: tap = throw (every 4th is a Space Core), hold = barrage, Shift + right-click = Imaginary Space field.
- **Telekinesis Authority**: tap push, Shift + right-click pull, hold on a mob to lift it, release to throw, sneak to slam.
- **Soulmine Blades**: tap = chain whip, hold = Chain Spin, Shift + right-click = orbiting blade rig.

## Herrscher Cores (Honkai energy: refills by itself, faster while held, shown as a boss bar)
Shift + right-click changes ability, right-click uses it. **Hold the Flame Core in your off hand while using the Void Core's Portal Lances** and the lances burn (Herrscher Resonance, +10% energy).
- **Void Core**: Void Step, Portal Gate, Portal Lances, Spatial Collapse, Imaginary Space (a pocket dimension, x8 travel), Spatial Walk (fly), Void Crystals.
- **Flame Core**: Flame Wave, Flame Armor (you are immune to your own fire), Inferno, Heat Lance, Heat Sink, Nuclear Flare (vaporize > lava > magma > glass > ash). Permanent changes: back up your world.

## World corruption
- **CRT Corruptor**: hold to charge (radius 5 / 12 / 25), release to fire. **Restoration Tool** restores (Shift = bigger area) and cures infection.
- **Corruption Seed** (block): place it and everything around corrupts over about 5 minutes. Break it to stop the spread.
- **Data Corruption** infection: infected creatures leave corrupted ground and infect others. Players are immune by default.
- Commands (op): `/afteros corrupt <r>`, `/afteros restore <r>`, `/afteros infect <r>`, `/afteros cure <r>`, `/afteros info`.

## Keys (Options > Controls > AfterOS // CRT Arsenal)
G summon Floating CRTs, H dismiss them, C switch their targeting, Z corruption beam, X fully charged corruption beam, V restore terrain. The matching item just needs to be in your inventory.

## Safety
Bedrock and anything with a block entity (chests, furnaces, spawners...) are never changed. Spawn protection is respected for player-fired blasts.
Corruption, Void Crystals and thermal damage are real changes to the world, so back up before testing.
