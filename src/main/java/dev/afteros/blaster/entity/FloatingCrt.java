package dev.afteros.blaster.entity;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.gameplay.BlastProfile;
import dev.afteros.blaster.gameplay.ImpactEffects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A CRT that hovers beside its owner and fires at the nearest mob (hostile ones first).
 * Every shot craters the ground, flings blocks and starts fires, just like the handheld blaster.
 */
public class FloatingCrt extends Projectile {
    private static final EntityDataAccessor<Integer> FIRE_AGE =
            SynchedEntityData.defineId(FloatingCrt.class, EntityDataSerializers.INT);

    /** Mob id -> game time a drone last claimed it, so the squad spreads its fire. */
    private static final Map<UUID, Long> CLAIMS = new HashMap<>();

    // server-only state
    private int ticksLeft;
    private int cooldown = 10;
    private int index;
    private int mode; // 0 guard (hostile only), 1 hunt (every mob), 2 hold fire
    private Vec3 engagePoint = Vec3.ZERO;
    private int engageTicks;
    private int count = 1;

    public FloatingCrt(EntityType<? extends FloatingCrt> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static FloatingCrt deploy(ServerLevel level, ServerPlayer owner, int index, int count, int mode) {
        FloatingCrt crt = new FloatingCrt(AfterOSBlaster.FLOATING_CRT_ENTITY.get(), level);
        crt.setOwner(owner);
        crt.index = index;
        crt.mode = mode;
        crt.count = Math.max(1, count);
        crt.cooldown = 5 + (index * BlasterConfig.DRONE_INTERVAL.get()) / crt.count; // stagger the squad
        crt.ticksLeft = BlasterConfig.DRONE_LIFETIME.get();
        crt.setPos(crt.hoverPosition(owner));
        crt.setYRot(owner.getYRot());
        return crt;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FIRE_AGE, 100);
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return this.mode;
    }

    /** Ticks since the last shot, capped at 100 (used by the renderer for recoil). */
    public int getFireAge() {
        return this.entityData.get(FIRE_AGE);
    }

    private Vec3 hoverPosition(ServerPlayer owner) {
        if (this.count <= 1) {
            double yaw = Math.toRadians(owner.getYRot());
            Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
            Vec3 right = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
            double bob = Math.sin(this.tickCount * 0.1D) * 0.12D;
            return owner.position()
                    .add(right.scale(1.25D))
                    .add(forward.scale(-0.1D))
                    .add(0.0D, owner.getBbHeight() + 0.35D + bob, 0.0D);
        }
        // squad: rings of 8 around the owner, alternate rings spin the other way
        int ringSize = 8;
        int ring = this.index / ringSize;
        int inRing = Math.max(1, Math.min(ringSize, this.count - ring * ringSize));
        int slot = this.index % ringSize;
        double spin = (ring % 2 == 0 ? 1.0D : -1.0D) * this.tickCount * 0.02D;
        double angle = Math.PI * 2.0D * slot / inRing + spin + ring * 0.4D;
        double radius = 2.3D + ring * 1.2D;
        double height = owner.getBbHeight() + 0.5D + ring * 0.45D + (slot % 2) * 0.4D
                + Math.sin(this.tickCount * 0.1D + this.index) * 0.12D;
        return owner.position().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0D * 96.0D;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (getFireAge() < 3) {
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.0D, 0.0D);
            }
            return;
        }
        if (!(this.getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || owner.level() != this.level()
                || !(this.level() instanceof ServerLevel server)) {
            this.discard();
            return;
        }
        if (--this.ticksLeft <= 0) {
            server.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.5D, this.getZ(), 12, 0.3D, 0.3D, 0.3D, 0.05D);
            this.discard();
            return;
        }

        // age counter for the renderer (only changes for a few ticks after each shot)
        int age = getFireAge();
        if (age < 100) {
            this.entityData.set(FIRE_AGE, age + 1);
        }

        // hover beside the owner
        Vec3 want = hoverPosition(owner);
        if (this.engageTicks > 0) { // swing out toward whatever we just shot, to get a better angle
            this.engageTicks--;
            Vec3 toward = this.engagePoint.subtract(owner.position());
            double len = toward.length();
            if (len > 0.5D) {
                want = want.add(toward.scale(Math.min(4.0D, len * 0.35D) / len));
            }
        }
        Vec3 delta = want.subtract(this.position());
        if (delta.lengthSqr() > 32.0D * 32.0D) {
            this.setPos(want);
        } else {
            this.setPos(this.position().add(delta.scale(0.22D)));
        }
        this.setDeltaMovement(Vec3.ZERO);

        // pick a target and shoot
        Mob target = null;
        if (--this.cooldown <= 0) {
            target = this.mode == 2 ? null : findTarget(server, owner);
            if (target != null) {
                fireAt(server, owner, target);
                this.cooldown = BlasterConfig.DRONE_INTERVAL.get();
            } else {
                this.cooldown = 5;
            }
        }

        // face the target (or whatever the owner is looking at)
        float wantYaw = owner.getYRot();
        if (target != null) {
            Vec3 d = target.getBoundingBox().getCenter().subtract(this.position());
            wantYaw = (float) (Mth.atan2(-d.x, d.z) * (180.0D / Math.PI));
        }
        this.setYRot(Mth.rotLerp(0.3F, this.getYRot(), wantYaw));
    }

    private Mob findTarget(ServerLevel server, ServerPlayer owner) {
        double range = BlasterConfig.DRONE_RANGE.get();
        boolean passive = this.mode == 1;
        List<Mob> mobs = server.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(range), m ->
                ImpactEffects.canDamage(owner, m)
                        && !m.isInvulnerable()
                        && (passive || m instanceof Enemy || m.getTarget() instanceof Player)
                        && m.distanceToSqr(this) <= range * range
                        && hasSight(server, m)
                        && !ownerInLine(owner, m));
        long now = server.getGameTime();
        Mob best = null;
        double bestScore = Double.MAX_VALUE;
        for (Mob mob : mobs) {
            double score = mob.distanceToSqr(this) + (mob instanceof Enemy ? 0.0D : 400.0D); // hostiles first
            Long claimed = CLAIMS.get(mob.getUUID());
            if (claimed != null && now - claimed < 14L) {
                score += 1500.0D; // another drone just fired at this one
            }
            if (mob.getTarget() == owner) {
                score -= 300.0D; // defend the owner first
            } else if (mob.getTarget() instanceof Player) {
                score -= 100.0D;
            }
            if (score < bestScore) {
                bestScore = score;
                best = mob;
            }
        }
        if (best != null) {
            CLAIMS.put(best.getUUID(), now);
            if (CLAIMS.size() > 512) {
                CLAIMS.values().removeIf(t -> now - t > 40L);
            }
        }
        return best;
    }

    /** Never shoot through the owner. */
    private boolean ownerInLine(ServerPlayer owner, Mob mob) {
        Vec3 from = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        Vec3 to = mob.getBoundingBox().getCenter();
        return owner.getBoundingBox().inflate(0.25D).clip(from, to).isPresent();
    }

    private boolean hasSight(ServerLevel server, Mob mob) {
        Vec3 from = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        Vec3 to = mob.getBoundingBox().getCenter();
        return server.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    private void fireAt(ServerLevel server, ServerPlayer owner, Mob target) {
        Vec3 muzzle = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        Vec3 aim = target.getBoundingBox().getCenter();
        server.addFreshEntity(SignalBeam.visual(server, owner, muzzle, aim, 1.0F, 8, 2));

        BlastProfile profile = BlastProfile.drone();
        if (target.distanceToSqr(owner) < 3.5D * 3.5D) {
            profile = profile.withoutTerrain(); // don't dig a hole under the owner's feet
        }
        ImpactEffects.blast(server, target.position().add(0.0D, 0.05D, 0.0D), owner, this, profile);

        server.playSound(null, this.getX(), this.getY(), this.getZ(), AfterOSBlaster.FIRE.get(), SoundSource.PLAYERS, 0.9F, 1.5F);
        this.entityData.set(FIRE_AGE, 0);
        this.engagePoint = target.position();
        this.engageTicks = 20;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }
}
