package dev.afteros.crt.entity;

import dev.afteros.crt.AfterOSBlaster;
import dev.afteros.crt.BlasterConfig;
import dev.afteros.crt.gameplay.BlastProfile;
import dev.afteros.crt.gameplay.ImpactEffects;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A thermal explosion that expands ring by ring. From the centre outwards the ground becomes:
 * vaporized (air) -> lava lining -> magma lining -> fused glass -> ash and fire. Blocks change in a queue with a per-tick limit.
 * Never touches bedrock or block entities. The owner is made fire-proof.
 */
public class ThermalFront extends Entity {
    private UUID ownerId;
    private int radius = 8;
    private int current;
    private int ringDelay = 1;
    private int delay;
    private int total;
    private int originY;
    private boolean detonated;
    private final ArrayDeque<BlockPos> queue = new ArrayDeque<>();

    public ThermalFront(EntityType<? extends ThermalFront> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static boolean start(ServerLevel level, LivingEntity owner, Vec3 at, int radius, int durationTicks) {
        if (!BlasterConfig.THERMAL_ENABLED.get()) {
            return false;
        }
        ThermalFront front = new ThermalFront(AfterOSBlaster.THERMAL_FRONT.get(), level);
        front.setPos(at);
        front.ownerId = owner == null ? null : owner.getUUID();
        front.radius = Math.max(2, radius);
        front.ringDelay = Math.max(1, durationTicks / front.radius);
        level.addFreshEntity(front);
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    private LivingEntity owner(ServerLevel server) {
        return this.ownerId != null && server.getEntity(this.ownerId) instanceof LivingEntity living ? living : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (!this.detonated) {
            this.detonated = true;
            this.originY = this.blockPosition().getY();
            detonate(server);
        }
        if (this.total > BlasterConfig.THERMAL_MAX_BLOCKS.get()) {
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
                this.delay = this.ringDelay;
                ringEffects(server);
                return;
            }
            this.discard();
            return;
        }
        int limit = Math.min(4000, Math.max(BlasterConfig.THERMAL_PER_TICK.get(), this.queue.size() / Math.max(1, this.ringDelay)));
        RandomSource rnd = server.getRandom();
        for (int i = 0; i < limit && !this.queue.isEmpty(); i++) {
            if (melt(server, this.queue.poll(), rnd)) {
                this.total++;
            }
        }
    }

    /** The flash: damage everything in range (the owner is exempt) and make the owner fire-proof. */
    private void detonate(ServerLevel server) {
        LivingEntity owner = owner(server);
        Vec3 c = this.position();
        if (owner != null) {
            owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0, false, false));
            owner.clearFire();
        }
        BlastProfile profile = BlastProfile.thermal(1000.0F, 3.0D);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(this.radius * 1.2D), t -> ImpactEffects.canDamage(owner, t))) {
            double f = target.position().distanceTo(c) / this.radius;
            if (f > 1.2D) {
                continue;
            }
            double falloff = f < 0.3D ? 1.0D : Math.max(0.03D, (1.0D - f) * 0.3D);
            ImpactEffects.strike(server, owner, target, profile, c, falloff);
        }
        server.sendParticles(ParticleTypes.FLASH, c.x, c.y + 1.0D, c.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1.0D, c.z, Math.min(12, 2 + this.radius / 3), this.radius * 0.15D, 1.0D, this.radius * 0.15D, 0.0D);
        server.playSound(null, c.x, c.y, c.z, AfterOSBlaster.BOOM.get(), SoundSource.PLAYERS, 8.0F, 0.5F);
        server.playSound(null, c.x, c.y, c.z, AfterOSBlaster.VOID_COLLAPSE_SOUND.get(), SoundSource.PLAYERS, 6.0F, 1.4F);
        if (BlasterConfig.CAMERA_SHAKE.get()) {
            for (ServerPlayer near : server.players()) {
                if (near.distanceToSqr(c) <= 80.0D * 80.0D) {
                    near.connection.send(new ClientboundHurtAnimationPacket(near.getId(), server.getRandom().nextFloat() * 360.0F));
                }
            }
        }
        ImpactEffects.flash(server, c);
    }

    private void buildRing(ServerLevel server) {
        RandomSource rnd = server.getRandom();
        BlockPos origin = this.blockPosition();
        int r = this.current;
        double outer = (double) r * r;
        double inner = (double) (r - 1) * (r - 1);
        List<BlockPos> ring = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
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
                        cursor.set(origin.getX() + dx, origin.getY() + (sign == 0 ? ay : -ay), origin.getZ() + dz);
                        if (!server.isInWorldBounds(cursor) || !server.hasChunkAt(cursor) || server.getBlockState(cursor).isAir()) {
                            continue;
                        }
                        ring.add(cursor.immutable());
                    }
                }
            }
        }
        Collections.shuffle(ring, new java.util.Random(rnd.nextLong()));
        this.queue.addAll(ring);
    }

    private void ringEffects(ServerLevel server) {
        Vec3 c = this.position();
        double r = this.current;
        int n = Math.min(60, 12 + this.current * 2);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0D * i / n;
            double x = c.x + Math.cos(a) * r;
            double z = c.z + Math.sin(a) * r;
            if (server.hasChunkAt(BlockPos.containing(x, c.y, z))) {
                server.sendParticles(ParticleTypes.FLAME, x, c.y + 1.0D, z, 0, Math.cos(a), 0.3D, Math.sin(a), 0.4D);
                if (i % 3 == 0) {
                    server.sendParticles(ParticleTypes.LAVA, x, c.y + 1.0D, z, 1, 0.5D, 0.5D, 0.5D, 0.0D);
                }
            }
        }
    }

    /** Applies the zone for this block's distance from the centre. */
    private boolean melt(ServerLevel server, BlockPos pos, RandomSource rnd) {
        BlockState s = server.getBlockState(pos);
        if (s.isAir() || s.hasBlockEntity() || s.getDestroySpeed(server, pos) < 0.0F) {
            return false;
        }
        double frac = Math.sqrt(pos.distToCenterSqr(this.getX(), this.getY(), this.getZ())) / this.radius;
        boolean water = s.getFluidState().is(FluidTags.WATER);
        boolean flammable = s.ignitedByLava() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced();
        boolean ground = pos.getY() <= this.originY;
        BlockState to = null;
        if (frac < 0.30D) {
            to = Blocks.AIR.defaultBlockState();                                         // vaporized
        } else if (frac < 0.38D) {
            to = ground && !water ? Blocks.LAVA.defaultBlockState() : Blocks.AIR.defaultBlockState();   // molten lining
        } else if (frac < 0.55D) {
            to = ground && !water && !flammable ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
        } else if (frac < 0.75D) {
            to = water || flammable ? Blocks.AIR.defaultBlockState() : Blocks.GLASS.defaultBlockState();  // fused to glass
        } else if (flammable || water) {
            to = Blocks.AIR.defaultBlockState();                                         // ash
        } else if (s.is(BlockTags.DIRT)) {
            to = Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (to == null) {
            if (rnd.nextFloat() < 0.10F && server.getBlockState(pos.above()).isAir()) {
                server.setBlock(pos.above(), BaseFireBlock.getState(server, pos.above()), Block.UPDATE_CLIENTS);
            }
            return false;
        }
        server.setBlock(pos, to, Block.UPDATE_CLIENTS);
        if (to.isAir() && rnd.nextInt(40) == 0) {
            server.sendParticles(ParticleTypes.ASH, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 3, 0.4D, 0.4D, 0.4D, 0.02D);
        }
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
