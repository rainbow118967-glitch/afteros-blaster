package dev.afteros.blaster.gameplay;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.entity.SignalDebris;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-side blast logic shared by bolts, beams, the floating CRT and the overclocked CRT. */
public final class ImpactEffects {
    private static final TagKey<Block> EXCLUDED = TagKey.create(Registries.BLOCK, AfterOSBlaster.id("debris_excluded"));

    private ImpactEffects() {}

    // ------------------------------------------------------------------ targeting

    public static boolean canDamage(LivingEntity owner, LivingEntity target) {
        if (!target.isAlive() || target.isSpectator() || target instanceof ArmorStand) {
            return false;
        }
        if (owner != null) {
            if (target == owner || target.isAlliedTo(owner)) {
                return false;
            }
            if (target instanceof OwnableEntity pet && owner.getUUID().equals(pet.getOwnerUUID())) {
                return false;
            }
        }
        if (target instanceof Player victim) {
            if (!BlasterConfig.HURT_PLAYERS.get() || victim.isCreative()) {
                return false;
            }
            if (owner instanceof Player attacker && !attacker.canHarmPlayer(victim)) {
                return false;
            }
        }
        return true;
    }

    public static DamageSource source(ServerLevel level, LivingEntity owner) {
        if (owner instanceof Player player) {
            return level.damageSources().playerAttack(player);
        }
        if (owner != null) {
            return level.damageSources().mobAttack(owner);
        }
        return level.damageSources().generic();
    }

    /** Damage + knockback + burning for one entity. falloff is 0..1. */
    public static void strike(ServerLevel level, LivingEntity owner, LivingEntity target,
                              BlastProfile p, Vec3 origin, double falloff) {
        DamageSource src = source(level, owner);
        if (p.oneShot()) {
            target.invulnerableTime = 0; // ignore hurt immunity so every pulse lands
        }
        target.hurt(src, (float) (p.damage() * falloff));

        boolean boss = target instanceof EnderDragon || target instanceof WitherBoss;
        if (p.oneShot() && target.isAlive() && (!boss || p.bosses())) {
            target.kill();
        }
        if (!target.isAlive()) {
            return;
        }
        Vec3 away = target.position().subtract(origin);
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() > 1.0E-4D) {
            away = away.normalize();
        } else {
            away = Vec3.ZERO;
        }
        double kb = p.knockback() * falloff;
        target.setDeltaMovement(target.getDeltaMovement().add(away.x * kb, 0.25D + 0.2D * Math.min(kb, 2.0D), away.z * kb));
        target.hurtMarked = true;
        if (p.fireChance() > 0.0D) {
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
        }
    }

    // ------------------------------------------------------------------ the blast

    public static void blast(ServerLevel level, Vec3 center, LivingEntity owner, Entity source, BlastProfile p) {
        RandomSource rnd = level.getRandom();

        // 1) entities
        AABB box = new AABB(center, center).inflate(p.damageRadius());
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, t -> canDamage(owner, t))) {
            double dist = target.getBoundingBox().getCenter().distanceTo(center);
            if (dist > p.damageRadius()) {
                continue;
            }
            double falloff = p.oneShot() ? 1.0D : Mth.clamp(1.15D - dist / p.damageRadius(), 0.25D, 1.0D);
            strike(level, owner, target, p, center, falloff);
        }

        // 2) terrain: real crater
        List<Removed> removed = List.of();
        if (BlasterConfig.DESTROY_TERRAIN.get() && p.craterRadius() > 0.0D && p.maxBlocks() > 0) {
            removed = carve(level, center, owner, p, rnd);
        }

        // 3) blocks fly
        if (!removed.isEmpty()) {
            launchDebris(level, center, removed, p, rnd);
        }

        // 4) fire on the crater floor
        if (!removed.isEmpty() && p.fireChance() > 0.0D) {
            for (Removed r : removed) {
                if (rnd.nextDouble() > p.fireChance()) {
                    continue;
                }
                BlockPos pos = r.pos();
                BlockPos below = pos.below();
                if (!level.getBlockState(pos).isAir()) {
                    continue;
                }
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    continue;
                }
                level.setBlock(pos, BaseFireBlock.getState(level, pos), Block.UPDATE_ALL);
            }
        }

        // 5) fx
        effects(level, center, p);
    }

    // ------------------------------------------------------------------ terrain

    private record Removed(BlockPos pos, BlockState state, double dist) {}

    private static boolean canDestroy(ServerLevel level, BlockPos pos, BlockState state, BlastProfile p) {
        if (state.is(EXCLUDED)) {
            return false;
        }
        if (state.hasBlockEntity()) {
            return false; // never delete chests, furnaces, spawners, signs...
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false; // bedrock & friends
        }
        return state.getBlock().getExplosionResistance() <= p.resistanceCap();
    }

    private static List<Removed> carve(ServerLevel level, Vec3 c, LivingEntity owner, BlastProfile p, RandomSource rnd) {
        double radius = p.craterRadius();
        int r = (int) Math.ceil(radius * 1.25D);
        BlockPos origin = BlockPos.containing(c);
        ServerPlayer player = owner instanceof ServerPlayer sp ? sp : null;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        List<Removed> candidates = new ArrayList<>();

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    // bowl shaped: full depth below the impact, shallower above it
                    double ey = dy > 0 ? dy / 0.6D : dy;
                    double dist = Math.sqrt(dx * dx + ey * ey + dz * dz);
                    double limit = radius * (0.78D + 0.44D * rnd.nextDouble()); // ragged edge
                    if (dist > limit) {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.isInWorldBounds(cursor) || !level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir() || !canDestroy(level, cursor, state, p)) {
                        continue;
                    }
                    if (player != null && !level.mayInteract(player, cursor)) {
                        continue; // spawn protection etc.
                    }
                    candidates.add(new Removed(cursor.immutable(), state, dist));
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(Removed::dist));
        List<Removed> picked = candidates.size() > p.maxBlocks()
                ? new ArrayList<>(candidates.subList(0, p.maxBlocks()))
                : candidates;

        int flags = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
        for (Removed removed : picked) {
            level.setBlock(removed.pos(), removed.state().getFluidState().createLegacyBlock(), flags);
        }
        return picked;
    }

    private static void launchDebris(ServerLevel level, Vec3 center, List<Removed> removed, BlastProfile p, RandomSource rnd) {
        if (!BlasterConfig.ENABLE_DEBRIS.get() || p.debris() <= 0) {
            return;
        }
        int live = level.getEntities(EntityTypeTest.forClass(SignalDebris.class), d -> true).size();
        int room = Math.max(0, BlasterConfig.MAX_ACTIVE_DEBRIS.get() - live);
        int count = Math.min(Math.min(p.debris(), room), removed.size());
        if (count <= 0) {
            return;
        }
        List<Removed> pool = new ArrayList<>(removed);
        Collections.shuffle(pool, new java.util.Random(rnd.nextLong()));

        double boost = p.big() ? 1.7D : 1.0D;
        int life = BlasterConfig.DEBRIS_LIFETIME.get();
        for (int i = 0; i < count; i++) {
            Removed r = pool.get(i);
            Vec3 pos = Vec3.atCenterOf(r.pos());
            Vec3 dir = pos.subtract(center);
            double len = dir.length();
            dir = len < 1.0E-3D ? new Vec3(0.0D, 1.0D, 0.0D) : dir.scale(1.0D / len);
            double speed = (0.30D + rnd.nextDouble() * 0.55D) * boost;
            Vec3 vel = new Vec3(
                    dir.x * speed + (rnd.nextDouble() - 0.5D) * 0.2D,
                    (0.45D + rnd.nextDouble() * 0.75D) * boost,
                    dir.z * speed + (rnd.nextDouble() - 0.5D) * 0.2D);
            level.addFreshEntity(new SignalDebris(level, r.state(), pos, vel, life + rnd.nextInt(40)));
        }
    }

    // ------------------------------------------------------------------ fx

    private static void effects(ServerLevel level, Vec3 c, BlastProfile p) {
        double spread = Math.max(1.0D, p.craterRadius() * 0.45D);
        int n = BlasterConfig.PARTICLE_COUNT.get();
        if (n > 0) {
            level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, p.big() ? 10 : 3, spread, spread * 0.5D, spread, 0.0D);
            if (p.big()) {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1.0D, c.z, 3, spread * 0.5D, 0.5D, spread * 0.5D, 0.0D);
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 0.5D, c.z, p.big() ? n * 4 : n, spread, spread * 0.7D, spread, 0.6D);
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.5D, c.z, n / 2, spread, spread * 0.7D, spread, 0.25D);
            level.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.3D, c.z, n / 2, spread, spread * 0.4D, spread, 0.12D);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.5D, c.z, n / 3, spread, spread * 0.5D, spread, 0.05D);
        }
        level.playSound(null, c.x, c.y, c.z, AfterOSBlaster.BOOM.get(), SoundSource.PLAYERS, p.big() ? 5.0F : 2.0F, p.big() ? 0.6F : 1.0F);
        level.playSound(null, c.x, c.y, c.z, AfterOSBlaster.IMPACT.get(), SoundSource.PLAYERS, p.big() ? 2.5F : 1.2F, 0.8F);
    }
}
