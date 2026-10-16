package dev.afteros.blaster;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-authoritative tuning. NOTE: SERVER config is not loaded on a remote client, so
 * these values must only ever be read from server-side code paths.
 */
public final class BlasterConfig {
    public static final ModConfigSpec SPEC;

    // ---- weapon
    public static final ModConfigSpec.DoubleValue DAMAGE;
    public static final ModConfigSpec.DoubleValue IMPACT_RADIUS;
    public static final ModConfigSpec.IntValue CHARGE_TICKS;
    public static final ModConfigSpec.IntValue COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue PROJECTILE_SPEED;
    public static final ModConfigSpec.DoubleValue BEAM_RANGE;
    public static final ModConfigSpec.IntValue BEAM_TICKS;
    public static final ModConfigSpec.DoubleValue KNOCKBACK;
    public static final ModConfigSpec.BooleanValue REQUIRE_AMMO;
    public static final ModConfigSpec.BooleanValue HURT_PLAYERS;
    public static final ModConfigSpec.BooleanValue HEAT_ENABLED;
    public static final ModConfigSpec.IntValue HEAT_COOL_PER_SECOND;

    // ---- terrain / effects
    public static final ModConfigSpec.BooleanValue DESTROY_TERRAIN;
    public static final ModConfigSpec.IntValue MAX_BLOCKS_PER_BLAST;
    public static final ModConfigSpec.DoubleValue MAX_BLAST_RESISTANCE;
    public static final ModConfigSpec.DoubleValue FIRE_CHANCE;
    public static final ModConfigSpec.BooleanValue SCORCH_GROUND;
    public static final ModConfigSpec.BooleanValue CAMERA_SHAKE;
    public static final ModConfigSpec.BooleanValue ENABLE_DEBRIS;
    public static final ModConfigSpec.IntValue DEBRIS_PER_IMPACT;
    public static final ModConfigSpec.IntValue MAX_ACTIVE_DEBRIS;
    public static final ModConfigSpec.IntValue DEBRIS_LIFETIME;
    public static final ModConfigSpec.IntValue PARTICLE_COUNT;

    // ---- floating CRT
    public static final ModConfigSpec.DoubleValue DRONE_DAMAGE;
    public static final ModConfigSpec.DoubleValue DRONE_CRATER_RADIUS;
    public static final ModConfigSpec.IntValue DRONE_COUNT;
    public static final ModConfigSpec.IntValue DRONE_INTERVAL;
    public static final ModConfigSpec.DoubleValue DRONE_RANGE;
    public static final ModConfigSpec.IntValue DRONE_LIFETIME;
    public static final ModConfigSpec.BooleanValue DRONE_TARGET_PASSIVE;
    public static final ModConfigSpec.BooleanValue DRONE_REQUIRE_AMMO;

    // ---- subspace lance
    public static final ModConfigSpec.DoubleValue LANCE_DAMAGE;
    public static final ModConfigSpec.DoubleValue LANCE_CRATER_RADIUS;
    public static final ModConfigSpec.IntValue LANCE_BARRAGE_COUNT;
    public static final ModConfigSpec.DoubleValue SPACE_RADIUS;
    public static final ModConfigSpec.IntValue SPACE_TICKS;
    public static final ModConfigSpec.IntValue SPACE_LANCE_INTERVAL;
    public static final ModConfigSpec.IntValue LANCE_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue BARRAGE_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue SPACE_COOLDOWN_TICKS;

    // ---- world corruption
    public static final ModConfigSpec.BooleanValue CORRUPTION_ENABLED;
    public static final ModConfigSpec.IntValue CORRUPTION_MAX_RADIUS;
    public static final ModConfigSpec.IntValue CORRUPTION_BLOCKS_PER_TICK;
    public static final ModConfigSpec.IntValue CORRUPTION_WAVE_DELAY;
    public static final ModConfigSpec.IntValue CORRUPTION_CREEP_SECONDS;
    public static final ModConfigSpec.IntValue CORRUPTION_MAX_FRONTS;
    public static final ModConfigSpec.IntValue CORRUPTION_MAX_BLOCKS;
    public static final ModConfigSpec.BooleanValue CORRUPTION_MOB_EFFECTS;
    public static final ModConfigSpec.BooleanValue CORRUPT_PLAYERS;
    public static final ModConfigSpec.IntValue RESTORE_RADIUS;
    public static final ModConfigSpec.IntValue SEED_SECONDS;
    public static final ModConfigSpec.IntValue SEED_RADIUS;
    public static final ModConfigSpec.IntValue SEED_MAX_BLOCKS;
    public static final ModConfigSpec.BooleanValue VIRUS_ENABLED;
    public static final ModConfigSpec.IntValue VIRUS_SECONDS;
    public static final ModConfigSpec.DoubleValue VIRUS_CHANCE;
    public static final ModConfigSpec.DoubleValue VIRUS_RANGE;

    // ---- telekinesis / soulmine blades / void core
    public static final ModConfigSpec.DoubleValue TK_RANGE;
    public static final ModConfigSpec.DoubleValue TK_PUSH_DAMAGE;
    public static final ModConfigSpec.DoubleValue TK_THROW_DAMAGE;
    public static final ModConfigSpec.DoubleValue TK_SLAM_DAMAGE;
    public static final ModConfigSpec.DoubleValue SOUL_WHIP_DAMAGE;
    public static final ModConfigSpec.DoubleValue SOUL_WHIP_RANGE;
    public static final ModConfigSpec.DoubleValue SOUL_SPIN_DAMAGE;
    public static final ModConfigSpec.DoubleValue SOUL_SPIN_RADIUS;
    public static final ModConfigSpec.IntValue SOUL_ORBIT_TICKS;
    public static final ModConfigSpec.DoubleValue SOUL_ORBIT_DAMAGE;
    public static final ModConfigSpec.IntValue SOUL_ORBIT_COST;
    public static final ModConfigSpec.DoubleValue VOID_BLINK_RANGE;
    public static final ModConfigSpec.IntValue VOID_LANCE_COUNT;
    public static final ModConfigSpec.BooleanValue THERMAL_ENABLED;
    public static final ModConfigSpec.IntValue NUKE_RADIUS;
    public static final ModConfigSpec.IntValue NUKE_SECONDS;
    public static final ModConfigSpec.IntValue THERMAL_MAX_BLOCKS;
    public static final ModConfigSpec.IntValue THERMAL_PER_TICK;
    public static final ModConfigSpec.DoubleValue VOID_REGEN;
    public static final ModConfigSpec.IntValue VOID_PORTAL_SECONDS;
    public static final ModConfigSpec.IntValue VOID_PORTAL_MAX;
    public static final ModConfigSpec.IntValue VOID_CRYSTAL_HEIGHT;
    public static final ModConfigSpec.DoubleValue VOID_COLLAPSE_RADIUS;
    public static final ModConfigSpec.IntValue VOID_COLLAPSE_TICKS;
    public static final ModConfigSpec.DoubleValue VOID_COLLAPSE_DAMAGE;
    public static final ModConfigSpec.IntValue VOID_COLLAPSE_COST;

    // ---- overclocked (super) CRT
    public static final ModConfigSpec.DoubleValue SUPER_DAMAGE;
    public static final ModConfigSpec.DoubleValue SUPER_CRATER_RADIUS;
    public static final ModConfigSpec.DoubleValue SUPER_RANGE;
    public static final ModConfigSpec.IntValue SUPER_BEAM_TICKS;
    public static final ModConfigSpec.IntValue SUPER_CHARGE_TICKS;
    public static final ModConfigSpec.IntValue SUPER_CELL_COST;
    public static final ModConfigSpec.IntValue SUPER_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue SUPER_MAX_BLOCKS;
    public static final ModConfigSpec.IntValue SUPER_DEBRIS;
    public static final ModConfigSpec.DoubleValue SUPER_FIRE_CHANCE;
    public static final ModConfigSpec.BooleanValue SUPER_ONE_SHOT_BOSSES;
    public static final ModConfigSpec.BooleanValue SUPER_LIGHTNING;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("CRT Blaster tuning.").push("weapon");
        DAMAGE = b.comment("Direct/splash damage of a fully charged bolt, in health points (2 = one heart).")
                .defineInRange("damage", 14.0D, 0.0D, 10000.0D);
        IMPACT_RADIUS = b.comment("Crater radius of a fully charged bolt, in blocks.")
                .defineInRange("impactRadius", 3.5D, 0.5D, 24.0D);
        CHARGE_TICKS = b.comment("Ticks for full charge; minimum fire charge is one quarter of this.")
                .defineInRange("chargeTicks", 30, 4, 200);
        COOLDOWN_TICKS = b.defineInRange("cooldownTicks", 20, 0, 1200);
        PROJECTILE_SPEED = b.defineInRange("projectileSpeed", 2.4D, 0.5D, 8.0D);
        BEAM_RANGE = b.defineInRange("beamRange", 64.0D, 8.0D, 256.0D);
        BEAM_TICKS = b.comment("Beam duration. Damage and terrain pulses every 10 ticks.")
                .defineInRange("beamTicks", 40, 10, 200);
        KNOCKBACK = b.defineInRange("knockback", 1.2D, 0.0D, 10.0D);
        REQUIRE_AMMO = b.comment("Consume Phosphor Cells (creative mode never consumes).")
                .define("requireAmmo", true);
        HEAT_ENABLED = b.comment("Each CRT Blaster shot adds heat; at 100% it overheats and locks for a few seconds.")
                .define("heatEnabled", true);
        HEAT_COOL_PER_SECOND = b.comment("Heat points lost per second (shots add roughly 15-35).")
                .defineInRange("heatCoolPerSecond", 12, 1, 200);
        HURT_PLAYERS = b.comment("Player damage still respects server PVP and teams.")
                .define("hurtPlayers", false);
        b.pop();

        b.comment("Terrain destruction and visual effects. Bedrock, fluids, portals and the debris_excluded tag are never touched, "
                + "and blocks with block entities (chests, furnaces, spawners...) are always spared.").push("effects");
        DESTROY_TERRAIN = b.comment("Master switch: blasts really remove blocks.")
                .define("destroyTerrain", true);
        MAX_BLOCKS_PER_BLAST = b.comment("Hard cap on blocks removed by one normal blast.")
                .defineInRange("maxBlocksPerBlast", 900, 0, 20000);
        MAX_BLAST_RESISTANCE = b.comment("Normal blasts cannot break blocks with a blast resistance above this (obsidian = 1200).")
                .defineInRange("maxBlastResistance", 100.0D, 0.0D, 3600000.0D);
        FIRE_CHANCE = b.comment("Chance that each exposed crater-floor block gets set on fire.")
                .defineInRange("fireChance", 0.35D, 0.0D, 1.0D);
        CAMERA_SHAKE = b.comment("Big blasts jolt the camera of nearby players (uses the vanilla hurt tilt, no damage).")
                .define("cameraShake", true);
        SCORCH_GROUND = b.comment("Crater floors get scorched: dirt becomes coarse dirt, sand becomes glass, stone becomes blackstone (magma for super blasts).")
                .define("scorchGround", true);
        ENABLE_DEBRIS = b.comment("Flying block debris. Debris is visual only: it cannot place blocks or drop items.")
                .define("enableDebris", true);
        DEBRIS_PER_IMPACT = b.defineInRange("debrisPerImpact", 48, 0, 1000);
        MAX_ACTIVE_DEBRIS = b.comment("Hard cap of debris entities per dimension; prevents rapid-fire buildup.")
                .defineInRange("maxActiveDebris", 500, 0, 5000);
        DEBRIS_LIFETIME = b.defineInRange("debrisLifetime", 120, 20, 1200);
        PARTICLE_COUNT = b.comment("Impact particles. Zero disables all blaster particles.")
                .defineInRange("particleCount", 40, 0, 400);
        b.pop();

        b.comment("Floating CRT (hovering turret that targets every mob).").push("floating_crt");
        DRONE_DAMAGE = b.defineInRange("droneDamage", 10.0D, 0.0D, 10000.0D);
        DRONE_CRATER_RADIUS = b.defineInRange("droneCraterRadius", 2.5D, 0.0D, 16.0D);
        DRONE_COUNT = b.comment("How many Floating CRTs one deployment creates.")
                .defineInRange("droneCount", 16, 1, 40);
        DRONE_INTERVAL = b.comment("Ticks between shots PER drone (shots are staggered across the squad).")
                .defineInRange("droneInterval", 40, 4, 400);
        DRONE_RANGE = b.defineInRange("droneRange", 28.0D, 4.0D, 96.0D);
        DRONE_LIFETIME = b.comment("Ticks a deployed Floating CRT stays online (2400 = 2 minutes).")
                .defineInRange("droneLifetime", 2400, 100, 1728000);
        DRONE_TARGET_PASSIVE = b.comment("false = only hostile mobs and mobs currently targeting a player. true = every mob.")
                .define("droneTargetPassive", false);
        DRONE_REQUIRE_AMMO = b.comment("Deploying costs one Phosphor Cell.")
                .define("droneRequireAmmo", true);
        b.pop();

        b.comment("Subspace Lance. Basic cast is free; barrage costs 1 Phosphor Cell, Imaginary Space costs 2.").push("subspace_lance");
        LANCE_DAMAGE = b.defineInRange("lanceDamage", 10.0D, 0.0D, 10000.0D);
        LANCE_CRATER_RADIUS = b.defineInRange("lanceCraterRadius", 2.0D, 0.0D, 16.0D);
        LANCE_BARRAGE_COUNT = b.comment("Lances dropped by a charged cast.").defineInRange("barrageCount", 4, 1, 16);
        SPACE_RADIUS = b.defineInRange("imaginarySpaceRadius", 7.0D, 2.0D, 24.0D);
        SPACE_TICKS = b.defineInRange("imaginarySpaceTicks", 100, 20, 600);
        SPACE_LANCE_INTERVAL = b.comment("Ticks between lances falling inside Imaginary Space.").defineInRange("imaginarySpaceInterval", 4, 1, 40);
        LANCE_COOLDOWN_TICKS = b.defineInRange("lanceCooldown", 6, 0, 200);
        BARRAGE_COOLDOWN_TICKS = b.defineInRange("barrageCooldown", 50, 0, 1200);
        SPACE_COOLDOWN_TICKS = b.defineInRange("imaginarySpaceCooldown", 260, 0, 6000);
        b.pop();

        b.comment("World corruption: the CRT Corruptor (and Floating CRTs in Corrupt mode) overwrite terrain. Original blocks are remembered "
                + "and can be put back with the Restoration Tool or /afteros restore. Bedrock and block entities are never touched.").push("corruption");
        CORRUPTION_ENABLED = b.comment("Master switch for all corruption.").define("enabled", true);
        CORRUPTION_MAX_RADIUS = b.comment("Largest radius a wave can have (a full charge asks for 25).").defineInRange("maxRadius", 25, 1, 64);
        CORRUPTION_BLOCKS_PER_TICK = b.comment("Per-tick limit on blocks changed by one wave. Lower = slower spread, less lag.")
                .defineInRange("blocksPerTick", 200, 10, 2000);
        CORRUPTION_WAVE_DELAY = b.comment("Ticks between rings of the wave.").defineInRange("waveDelayTicks", 2, 0, 40);
        CORRUPTION_CREEP_SECONDS = b.comment("After the main wave, corrupted blocks keep infecting neighbours for this long. 0 = no continuous spreading.")
                .defineInRange("creepSeconds", 8, 0, 120);
        CORRUPTION_MAX_FRONTS = b.comment("Most corruption waves active at once.").defineInRange("maxActiveFronts", 24, 1, 200);
        CORRUPTION_MAX_BLOCKS = b.comment("Hard cap of blocks one wave may change.").defineInRange("maxBlocksPerFront", 30000, 100, 500000);
        CORRUPTION_MOB_EFFECTS = b.comment("Corrupted ground slows and hurts mobs; mobs on it glow and hit harder.").define("mobEffects", true);
        CORRUPT_PLAYERS = b.comment("If false (default) players are immune to corrupted ground.").define("corruptPlayers", false);
        RESTORE_RADIUS = b.comment("Restoration Tool radius (x3 while sneaking).").defineInRange("restoreRadius", 20, 4, 128);
        SEED_SECONDS = b.comment("Corruption Seed: how long the whole wave takes (300 = five minutes).").defineInRange("seedSeconds", 300, 10, 3600);
        SEED_RADIUS = b.comment("Corruption Seed: how far the wave spreads (only loaded chunks are changed).").defineInRange("seedRadius", 96, 8, 256);
        SEED_MAX_BLOCKS = b.comment("Corruption Seed: hard cap of blocks it may change.").defineInRange("seedMaxBlocks", 500000, 1000, 5000000);
        VIRUS_ENABLED = b.comment("Data Corruption infection: infected creatures leave corrupted ground and infect others nearby.").define("virusEnabled", true);
        VIRUS_SECONDS = b.comment("How long an infection lasts. Infecting others restarts the clock for them, so it can keep going in a crowd.")
                .defineInRange("virusSeconds", 30, 5, 600);
        VIRUS_CHANCE = b.comment("Chance per half second that an infected creature infects each other creature in range.")
                .defineInRange("virusSpreadChance", 0.35D, 0.0D, 1.0D);
        VIRUS_RANGE = b.defineInRange("virusRange", 3.5D, 1.0D, 12.0D);
        b.pop();

        b.comment("Telekinesis Authority.").push("telekinesis");
        TK_RANGE = b.defineInRange("range", 24.0D, 4.0D, 64.0D);
        TK_PUSH_DAMAGE = b.defineInRange("pushDamage", 4.0D, 0.0D, 10000.0D);
        TK_THROW_DAMAGE = b.defineInRange("throwDamage", 14.0D, 0.0D, 10000.0D);
        TK_SLAM_DAMAGE = b.defineInRange("slamDamage", 18.0D, 0.0D, 10000.0D);
        b.pop();

        b.comment("Soulmine Blades.").push("soulmine_blades");
        SOUL_WHIP_DAMAGE = b.defineInRange("whipDamage", 9.0D, 0.0D, 10000.0D);
        SOUL_WHIP_RANGE = b.defineInRange("whipRange", 24.0D, 4.0D, 64.0D);
        SOUL_SPIN_DAMAGE = b.comment("Damage per spin pulse (every half second while held).").defineInRange("spinDamage", 5.0D, 0.0D, 10000.0D);
        SOUL_SPIN_RADIUS = b.defineInRange("spinRadius", 4.5D, 1.0D, 16.0D);
        SOUL_ORBIT_TICKS = b.comment("How long the orbiting dual blade rig lasts (900 = 45 seconds).").defineInRange("orbitTicks", 900, 100, 72000);
        SOUL_ORBIT_DAMAGE = b.defineInRange("orbitDamage", 7.0D, 0.0D, 10000.0D);
        SOUL_ORBIT_COST = b.comment("Phosphor Cells to summon the rig.").defineInRange("orbitCost", 1, 0, 64);
        b.pop();

        b.comment("Herrscher Core of Flame (Gem of Destruction). Thermal terrain changes are permanent; bedrock and block entities are never touched.").push("flame_core");
        THERMAL_ENABLED = b.comment("Master switch for terrain melting / vaporizing (Heat Lance and Nuclear Flare).").define("thermalEnabled", true);
        NUKE_RADIUS = b.comment("Nuclear Flare radius in blocks.").defineInRange("nukeRadius", 24, 6, 48);
        NUKE_SECONDS = b.comment("How long the fireball takes to expand to full size.").defineInRange("nukeSeconds", 4, 2, 30);
        THERMAL_MAX_BLOCKS = b.comment("Hard cap of blocks one thermal explosion may change.").defineInRange("thermalMaxBlocks", 150000, 1000, 2000000);
        THERMAL_PER_TICK = b.comment("Blocks changed per tick (raised automatically to keep the schedule, capped at 4000).").defineInRange("thermalBlocksPerTick", 1500, 100, 4000);
        b.pop();

        b.comment("Herrscher Core of the Void.").push("void_core");
        VOID_BLINK_RANGE = b.defineInRange("blinkRange", 40.0D, 4.0D, 128.0D);
        VOID_LANCE_COUNT = b.comment("Portals (and lances) opened by Portal Lances. Hundreds are possible but heavy.").defineInRange("lanceCount", 16, 1, 200);
        VOID_REGEN = b.comment("Honkai energy the Core absorbs per second (max 100). Holding it adds more.").defineInRange("energyRegenPerSecond", 6.0D, 0.0D, 100.0D);
        VOID_PORTAL_SECONDS = b.comment("How long a Portal Gate stays open.").defineInRange("portalSeconds", 30, 5, 600);
        VOID_PORTAL_MAX = b.comment("Most linked portals one player can keep open.").defineInRange("maxPortals", 12, 2, 100);
        VOID_CRYSTAL_HEIGHT = b.comment("Height of the tallest Void Crystal (blocks, max 40).").defineInRange("crystalHeight", 24, 4, 40);
        VOID_COLLAPSE_RADIUS = b.defineInRange("collapseRadius", 12.0D, 3.0D, 32.0D);
        VOID_COLLAPSE_TICKS = b.comment("Ticks the black hole pulls before it detonates.").defineInRange("collapseTicks", 60, 10, 400);
        VOID_COLLAPSE_DAMAGE = b.defineInRange("collapseDamage", 60.0D, 0.0D, 100000.0D);
        VOID_COLLAPSE_COST = b.defineInRange("collapseCost", 3, 0, 64);
        b.pop();

        b.comment("Overclocked CRT (super blaster).").push("overclocked");
        SUPER_DAMAGE = b.comment("Damage applied to everything caught in the beam. High enough to one-shot mobs.")
                .defineInRange("superDamage", 1000.0D, 0.0D, 1000000.0D);
        SUPER_CRATER_RADIUS = b.defineInRange("superCraterRadius", 9.0D, 1.0D, 32.0D);
        SUPER_RANGE = b.defineInRange("superRange", 160.0D, 16.0D, 512.0D);
        SUPER_BEAM_TICKS = b.defineInRange("superBeamTicks", 50, 10, 400);
        SUPER_CHARGE_TICKS = b.defineInRange("superChargeTicks", 40, 4, 200);
        SUPER_CELL_COST = b.defineInRange("superCellCost", 3, 0, 64);
        SUPER_COOLDOWN_TICKS = b.defineInRange("superCooldownTicks", 80, 0, 6000);
        SUPER_MAX_BLOCKS = b.comment("Hard cap on blocks removed by one super pulse.")
                .defineInRange("superMaxBlocks", 5000, 0, 60000);
        SUPER_DEBRIS = b.defineInRange("superDebris", 240, 0, 2000);
        SUPER_FIRE_CHANCE = b.defineInRange("superFireChance", 0.6D, 0.0D, 1.0D);
        SUPER_LIGHTNING = b.comment("Harmless visual lightning where the super beam lands.")
                .define("superLightning", true);
        SUPER_ONE_SHOT_BOSSES = b.comment("If true the Ender Dragon and Wither are force-killed too.")
                .define("oneShotBosses", false);
        b.pop();

        SPEC = b.build();
    }

    private BlasterConfig() {}
}
