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

        b.comment("Herrscher Core of the Void.").push("void_core");
        VOID_BLINK_RANGE = b.defineInRange("blinkRange", 40.0D, 4.0D, 128.0D);
        VOID_LANCE_COUNT = b.defineInRange("lanceCount", 6, 1, 24);
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
