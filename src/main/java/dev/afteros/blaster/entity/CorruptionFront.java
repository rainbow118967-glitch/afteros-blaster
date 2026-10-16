package dev.afteros.blaster.entity;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.gameplay.CorruptionBlocks;
import dev.afteros.blaster.gameplay.CorruptionData;
import dev.afteros.blaster.gameplay.ImpactEffects;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Drives one expanding wave of corruption (or restoration). Blocks are changed in a queue with a per-tick limit,
 * ring by ring, so the infection visibly crawls outward. The entity is saved with the world, so waves survive reloads.
 */
public class CorruptionFront extends Entity {
    private static final DustParticleOptions PINK = new DustParticleOptions(new Vector3f(1.0F, 0.25F, 0.8F), 1.3F);
    private static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 1.0F), 1.3F);

    private int radius = 5;
    private int current;
    private int creep;
    private int total;
    private int delay;
    private boolean restoring;
    private boolean primed;
    private boolean timed;   // a Corruption Seed wave: huge, paced to finish on schedule
    private int ringDelay;
    private final ArrayDeque<BlockPos> queue = new ArrayDeque<>();

    public CorruptionFront(EntityType<? extends CorruptionFront> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Starts a wave. Returns false if it was refused (disabled, duplicate, or too many active fronts). */
    public static boolean start(ServerLevel level, Vec3 at, int radius, boolean restoring) {
        return start(level, at, radius, restoring, -1);
    }

    /** A Corruption Seed wave: radius may exceed the normal maximum and rings are paced to take about durationTicks in total. */
    public static boolean startTimed(ServerLevel level, Vec3 at, int radius, int durationTicks) {
        if (!BlasterConfig.CORRUPTION_ENABLED.get()) {
            return false;
        }
        int active = level.getEntities(EntityTypeTest.forClass(CorruptionFront.class), x -> true).size();
        if (active >= BlasterConfig.CORRUPTION_MAX_FRONTS.get()) {
            return false;
        }
        CorruptionFront front = new CorruptionFront(AfterOSBlaster.CORRUPTION_FRONT.get(), level);
        front.setPos(at);
        front.radius = Math.max(1, radius);
        front.timed = true;
        front.creep = 0;
        front.ringDelay = Math.max(1, durationTicks / front.radius);
        level.addFreshEntity(front);
        return true;
    }

    /** Stops the seed wave centred at this position. Returns true if one was running. */
    public static boolean cancelTimed(ServerLevel level, Vec3 at) {
        boolean any = false;
        for (CorruptionFront f : level.getEntitiesOfClass(CorruptionFront.class, new AABB(at, at).inflate(1.5D), x -> x.timed)) {
            f.discard();
            any = true;
        }
        return any;
    }

    /** A tiny wave dropped by infected creatures: no creeping phase, so trails stay cheap. */
    public static boolean startVirus(ServerLevel level, Vec3 at, int radius) {
        return start(level, at, radius, false, 0);
    }

    /** creepTicks < 0 uses the config value. */
    public static boolean start(ServerLevel level, Vec3 at, int radius, boolean restoring, int creepTicks) {
        if (!restoring && !BlasterConfig.CORRUPTION_ENABLED.get()) {
            return false;
        }
        int r = restoring ? radius : Math.max(1, Math.min(radius, BlasterConfig.CORRUPTION_MAX_RADIUS.get()));
        if (!restoring) {
            AABB near = new AABB(at, at).inflate(Math.max(2.0D, r * 0.4D));
            if (!level.getEntitiesOfClass(CorruptionFront.class, near, x -> !x.restoring).isEmpty()) {
                return false; // already infected right here
            }
        } else { // restoring cancels any corruption still crawling nearby
            AABB area = new AABB(at, at).inflate(radius + 8.0D);
            for (CorruptionFront old : level.getEntitiesOfClass(CorruptionFront.class, area, x -> !x.restoring)) {
                old.discard();
            }
        }
        int active = level.getEntities(EntityTypeTest.forClass(CorruptionFront.class), x -> true).size();
        if (active >= BlasterConfig.CORRUPTION_MAX_FRONTS.get()) {
            return false;
        }
        CorruptionFront front = new CorruptionFront(AfterOSBlaster.CORRUPTION_FRONT.get(), level);
        front.setPos(at);
        front.radius = r;
        front.restoring = restoring;
        front.creep = restoring ? 0 : (creepTicks >= 0 ? creepTicks : BlasterConfig.CORRUPTION_CREEP_SECONDS.get() * 20);
        level.addFreshEntity(front);
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (this.restoring) {
            restoreTick(server);
        } else {
            corruptTick(server);
        }
    }

    // ------------------------------------------------------------------ corruption

    private void corruptTick(ServerLevel server) {
        CorruptionData data = CorruptionData.get(server);
        if (this.timed && this.tickCount % 20 == 0) {
            progress(server);
        }
        if (this.total > (this.timed ? BlasterConfig.SEED_MAX_BLOCKS.get() : BlasterConfig.CORRUPTION_MAX_BLOCKS.get())) {
            this.discard();
            return;
        }
        if (this.queue.isEmpty()) {
            if (this.delay > 0) {
                this.delay--;
                return;
            }
            if (this.current < this.radius) {
                this.current++;
                buildRing(server);
                this.delay = this.timed ? this.ringDelay : BlasterConfig.CORRUPTION_WAVE_DELAY.get();
                if (this.current % (this.timed ? 8 : 4) == 0) { // pink lightning along the wave
                    Vec3 bolt = this.position().add((server.getRandom().nextDouble() - 0.5D) * this.current * 1.2D, 0.0D,
                            (server.getRandom().nextDouble() - 0.5D) * this.current * 1.2D);
                    if (server.hasChunkAt(BlockPos.containing(bolt))) {
                        ImpactEffects.flash(server, bolt);
                    }
                }
                return;
            }
            if (this.creep > 0) {
                this.creep--;
                creepTick(server, data);
                return;
            }
            this.discard();
            return;
        }

        int limit = BlasterConfig.CORRUPTION_BLOCKS_PER_TICK.get();
        if (this.timed) { // keep the schedule: drain each ring within its time slot, but never more than 2500 blocks a tick
            limit = Math.min(2500, Math.max(limit, this.queue.size() / Math.max(1, this.ringDelay)));
        }
        int done = 0;
        List<BlockPos> shown = new ArrayList<>();
        while (done < limit && !this.queue.isEmpty()) {
            BlockPos pos = this.queue.poll();
            if (convert(server, data, pos)) {
                done++;
                if (shown.size() < 8 && done % 6 == 0) {
                    shown.add(pos);
                }
            }
        }
        for (BlockPos p : shown) {
            server.sendParticles(PINK, p.getX() + 0.5D, p.getY() + 1.1D, p.getZ() + 0.5D, 3, 0.4D, 0.2D, 0.4D, 0.02D);
            server.sendParticles(ParticleTypes.ENCHANT, p.getX() + 0.5D, p.getY() + 1.6D, p.getZ() + 0.5D, 2, 0.4D, 0.4D, 0.4D, 0.4D);
            server.sendParticles(ParticleTypes.ASH, p.getX() + 0.5D, p.getY() + 1.2D, p.getZ() + 0.5D, 2, 0.5D, 0.3D, 0.5D, 0.0D);
        }
        if (done > 0 && this.tickCount % 5 == 0) {
            server.playSound(null, this.getX(), this.getY(), this.getZ(), AfterOSBlaster.CHARGE.get(), SoundSource.BLOCKS, 0.5F,
                    0.5F + server.getRandom().nextFloat() * 0.6F);
        }
    }

    private void progress(ServerLevel server) {
        int pct = this.current * 100 / Math.max(1, this.radius);
        int secondsLeft = (this.radius - this.current) * this.ringDelay / 20;
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(this) < 48.0D * 48.0D) {
                player.displayClientMessage(Component.translatable("message.afteros_blaster.seed_progress", pct, secondsLeft), true);
            }
        }
    }

    /** Collects the shell of blocks at distance [current-1, current) that are exposed and convertible. */
    private void buildRing(ServerLevel server) {
        RandomSource rnd = server.getRandom();
        BlockPos origin = this.blockPosition();
        int r = this.current;
        double outer = (double) r * r;
        double inner = (double) (r - 1) * (r - 1);
        double edge = (double) r / this.radius;
        List<BlockPos> ring = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // walk columns and only the y-range that lies inside the shell, so cost follows the shell's size, not the cube's
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double base = dx * dx + dz * dz;
                if (base >= outer) {
                    continue;
                }
                int yHigh = (int) Math.ceil(Math.sqrt(outer - base)) - 1;
                int yLow = base >= inner ? 0 : (int) Math.ceil(Math.sqrt(inner - base));
                for (int ay = yLow; ay <= yHigh; ay++) {
                    for (int sign = 0; sign < 2; sign++) {
                        if (sign == 1 && ay == 0) {
                            continue;
                        }
                        int dy = sign == 0 ? ay : -ay;
                        if (edge > 0.75D && rnd.nextDouble() < (edge - 0.75D) * 2.4D) {
                            continue; // ragged, organic outer edge
                        }
                        cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                        if (!server.isInWorldBounds(cursor) || !server.hasChunkAt(cursor)) {
                            continue;
                        }
                        BlockState s = server.getBlockState(cursor);
                        if (CorruptionBlocks.convert(s) != null && exposed(server, cursor)) {
                            ring.add(cursor.immutable());
                        }
                    }
                }
            }
        }
        Collections.shuffle(ring, new java.util.Random(rnd.nextLong()));
        this.queue.addAll(ring);
    }

    /** After the main wave, corrupted blocks keep infecting their neighbours for a while. */
    private void creepTick(ServerLevel server, CorruptionData data) {
        RandomSource rnd = server.getRandom();
        int range = this.radius + 6;
        BlockPos origin = this.blockPosition();
        for (int i = 0; i < 30; i++) {
            BlockPos p = origin.offset(rnd.nextInt(range * 2 + 1) - range, rnd.nextInt(range * 2 + 1) - range, rnd.nextInt(range * 2 + 1) - range);
            if (!server.isInWorldBounds(p) || !server.hasChunkAt(p)) {
                continue;
            }
            BlockState s = server.getBlockState(p);
            if (CorruptionBlocks.convert(s) == null || !exposed(server, p) || !touchesCorruption(server, p)) {
                continue;
            }
            if (convert(server, data, p)) {
                server.sendParticles(PINK, p.getX() + 0.5D, p.getY() + 1.1D, p.getZ() + 0.5D, 2, 0.3D, 0.2D, 0.3D, 0.02D);
            }
        }
    }

    private boolean convert(ServerLevel server, CorruptionData data, BlockPos pos) {
        BlockState s = server.getBlockState(pos);
        BlockState to = CorruptionBlocks.convert(s);
        if (to == null || s.hasBlockEntity() || s.getDestroySpeed(server, pos) < 0.0F) {
            return false; // never touch bedrock, chests, spawners...
        }
        data.remember(pos, s);
        server.setBlock(pos, to, Block.UPDATE_CLIENTS);
        BlockPos up = pos.above();
        if (CorruptionBlocks.isPlant(server.getBlockState(up))) {
            server.setBlock(up, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        this.total++;
        return true;
    }

    private static boolean exposed(ServerLevel server, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (server.hasChunkAt(n) && !server.getBlockState(n).isSolidRender(server, n)) {
                return true;
            }
        }
        return false;
    }

    private static boolean touchesCorruption(ServerLevel server, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (server.hasChunkAt(n) && CorruptionBlocks.isCorrupted(server.getBlockState(n))) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ restoration

    private void restoreTick(ServerLevel server) {
        CorruptionData data = CorruptionData.get(server);
        if (!this.primed) {
            this.primed = true;
            this.queue.addAll(data.near(this.position(), this.radius)); // nearest first = outward wave
            server.playSound(null, this.getX(), this.getY(), this.getZ(), AfterOSBlaster.VOID_TRAVEL.get(), SoundSource.PLAYERS, 1.0F, 1.6F);
        }
        if (this.queue.isEmpty()) {
            this.discard();
            return;
        }
        int limit = BlasterConfig.CORRUPTION_BLOCKS_PER_TICK.get() * 2;
        int done = 0;
        while (done < limit && !this.queue.isEmpty()) {
            BlockPos pos = this.queue.poll();
            if (!server.hasChunkAt(pos)) {
                continue; // stays remembered for later
            }
            BlockState original = data.take(pos);
            if (original == null) {
                continue;
            }
            if (CorruptionBlocks.isCorrupted(server.getBlockState(pos))) {
                server.setBlock(pos, original, Block.UPDATE_ALL);
                done++;
                if (done % 5 == 0) {
                    server.sendParticles(WHITE, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 3, 0.4D, 0.3D, 0.4D, 0.02D);
                    server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 1.3D, pos.getZ() + 0.5D, 1, 0.3D, 0.3D, 0.3D, 0.03D);
                }
            }
        }
        if (this.tickCount % 6 == 0) {
            server.playSound(null, this.getX(), this.getY(), this.getZ(), AfterOSBlaster.IMPACT.get(), SoundSource.BLOCKS, 0.4F, 2.0F);
        }
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.radius = Math.max(1, tag.getInt("radius"));
        this.current = tag.getInt("current");
        this.creep = tag.getInt("creep");
        this.total = tag.getInt("total");
        this.restoring = tag.getBoolean("restoring");
        this.timed = tag.getBoolean("timed");
        this.ringDelay = tag.getInt("ringDelay");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("radius", this.radius);
        tag.putInt("current", this.current);
        tag.putInt("creep", this.creep);
        tag.putInt("total", this.total);
        tag.putBoolean("restoring", this.restoring);
        tag.putBoolean("timed", this.timed);
        tag.putInt("ringDelay", this.ringDelay);
    }
}
