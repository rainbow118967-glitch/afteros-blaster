# AfterOS // CRT Arsenal (Fabric) 5.2.0  -  Minecraft 1.21.1

Same mod as the NeoForge 5.2.0 version, ported to Fabric. Mod id `afteros_crt`, package `dev.afteros.crt`,
so it will NOT clash with the older `afteros_blaster` Fabric jar - they can sit in the same mods folder
(you'd just have two sets of blasters). Needs Fabric Loader 0.16+ and Fabric API.

Build: push to GitHub (the Actions workflow is included) or run `gradle build` with JDK 21 + Gradle 9.2.
Jar: build/libs/afteros-crt-fabric-5.2.0.jar (not the -sources one).

Items: CRT Blaster, Overclocked CRT, Floating CRT (squad of 16, hostile-only by default), Subspace Lance, Phosphor Cell.
Settings: config/afteros_crt.properties (droneCount, droneTargetPassive, terrain/fire/debris options...).

## 5.2.0
Beam colors per weapon, scorched crater floors, shockwave + flash, lightning on Overclocked hits, action-bar charge meter,
two-ring Floating CRT squad that spreads fire and defends you first, bigger Space Core, Scatter mode, Floating CRT modes
(Guard / Hunt / Hold fire via Shift + right-click), drones never shoot through you, camera shake, hit markers, dust, heat, and the new Telekinesis Authority, Soulmine Blades and Herrscher Core of the Void (rebuilt, reusing the Fabric 1.3.0 art and sounds). World corruption (CRT Corruptor, Restoration Tool, corrupted blocks, /afteros commands, Floating CRT Corrupt mode) configurable keybindings with a custom network packet, and the Data Corruption infection (mobs spread corruption like a virus; /afteros infect|cure) the timed Corruption Seed block, and the rebuilt Void Core (portals, Imaginary Space dimension, Spatial Walk, Void Crystals), and the Herrscher Core of Flame (Heat Lance, Nuclear Flare...), an energy boss bar, and Herrscher Resonance (both cores). Settings: `cameraShake`, `heatEnabled`, `scorchGround`, `superLightning`, `corruption.*`.
