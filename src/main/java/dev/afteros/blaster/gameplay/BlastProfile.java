package dev.afteros.blaster.gameplay;

import dev.afteros.blaster.BlasterConfig;

/** Everything a single blast needs to know. Built server-side only (reads server config). */
public record BlastProfile(
        float damage,
        double damageRadius,
        double craterRadius,
        double fireChance,
        int maxBlocks,
        int debris,
        float resistanceCap,
        double knockback,
        boolean oneShot,
        boolean bosses,
        boolean big) {

    /** Charged bolt. power is 0.25 .. 1. */
    public static BlastProfile bolt(float power) {
        double crater = BlasterConfig.IMPACT_RADIUS.get() * (0.55D + 0.45D * power);
        return new BlastProfile(
                (float) (BlasterConfig.DAMAGE.get() * (0.6D + 0.4D * power)),
                crater * 1.6D + 1.0D,
                crater,
                BlasterConfig.FIRE_CHANCE.get(),
                BlasterConfig.MAX_BLOCKS_PER_BLAST.get(),
                (int) (BlasterConfig.DEBRIS_PER_IMPACT.get() * (0.5D + 0.5D * power)),
                BlasterConfig.MAX_BLAST_RESISTANCE.get().floatValue(),
                BlasterConfig.KNOCKBACK.get(),
                false, false, false);
    }

    /** One damage/terrain pulse of the normal beam. */
    public static BlastProfile beamPulse(float power) {
        double crater = BlasterConfig.IMPACT_RADIUS.get() * 0.8D;
        return new BlastProfile(
                (float) (BlasterConfig.DAMAGE.get() * 0.55D),
                crater * 1.5D + 1.0D,
                crater,
                BlasterConfig.FIRE_CHANCE.get(),
                BlasterConfig.MAX_BLOCKS_PER_BLAST.get() / 2,
                BlasterConfig.DEBRIS_PER_IMPACT.get() / 2,
                BlasterConfig.MAX_BLAST_RESISTANCE.get().floatValue(),
                BlasterConfig.KNOCKBACK.get() * 0.5D,
                false, false, false);
    }

    /** Floating CRT shot. */
    public static BlastProfile drone() {
        double crater = BlasterConfig.DRONE_CRATER_RADIUS.get();
        return new BlastProfile(
                BlasterConfig.DRONE_DAMAGE.get().floatValue(),
                crater * 1.6D + 1.5D,
                crater,
                BlasterConfig.FIRE_CHANCE.get(),
                BlasterConfig.MAX_BLOCKS_PER_BLAST.get() / 2,
                BlasterConfig.DEBRIS_PER_IMPACT.get() / 2,
                BlasterConfig.MAX_BLAST_RESISTANCE.get().floatValue(),
                BlasterConfig.KNOCKBACK.get() * 0.6D,
                false, false, false);
    }

    /** Overclocked CRT pulse: massive crater, one-shots mobs. */
    public static BlastProfile superBlast() {
        double crater = BlasterConfig.SUPER_CRATER_RADIUS.get();
        return new BlastProfile(
                BlasterConfig.SUPER_DAMAGE.get().floatValue(),
                crater * 1.5D + 2.0D,
                crater,
                BlasterConfig.SUPER_FIRE_CHANCE.get(),
                BlasterConfig.SUPER_MAX_BLOCKS.get(),
                BlasterConfig.SUPER_DEBRIS.get(),
                3600000.0F,
                BlasterConfig.KNOCKBACK.get() * 2.0D,
                true,
                BlasterConfig.SUPER_ONE_SHOT_BOSSES.get(),
                true);
    }

    /** Subspace Lance impact. big = every 4th basic lance (Space Core). */
    public static BlastProfile lance(boolean big) {
        double crater = BlasterConfig.LANCE_CRATER_RADIUS.get() * (big ? 1.7D : 1.0D);
        return new BlastProfile(
                (float) (BlasterConfig.LANCE_DAMAGE.get() * (big ? 1.6D : 1.0D)),
                crater * 1.8D + 1.0D,
                crater,
                0.0D,
                big ? 500 : 250,
                big ? 30 : 14,
                BlasterConfig.MAX_BLAST_RESISTANCE.get().floatValue(),
                BlasterConfig.KNOCKBACK.get() * 0.7D,
                false, false, false);
    }

    /** Same blast but it never touches terrain (used when the target is right next to the owner). */
    public BlastProfile withoutTerrain() {
        return new BlastProfile(damage, damageRadius, 0.0D, 0.0D, 0, 0, resistanceCap, knockback, oneShot, bosses, big);
    }
}
