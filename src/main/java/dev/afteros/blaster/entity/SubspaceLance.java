package dev.afteros.blaster.entity;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.gameplay.BlastProfile;
import dev.afteros.blaster.gameplay.ImpactEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A lance pulled out of Imaginary Space. Either shot straight, or dropped from the sky after a short delay. */
public class SubspaceLance extends Projectile {
    private static final EntityDataAccessor<Boolean> ARMED =
            SynchedEntityData.defineId(SubspaceLance.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CORE =
            SynchedEntityData.defineId(SubspaceLance.class, EntityDataSerializers.BOOLEAN);

    // server-only
    private int delay;
    private Vec3 launch = Vec3.ZERO;

    public SubspaceLance(EntityType<? extends SubspaceLance> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Summoned beside the owner and thrown at the target. core = the 4th lance of a combo (bigger blast). */
    public static SubspaceLance straight(ServerLevel level, LivingEntity owner, Vec3 target, boolean core) {
        SubspaceLance lance = new SubspaceLance(AfterOSBlaster.SUBSPACE_LANCE_ENTITY.get(), level);
        lance.setOwner(owner);
        lance.entityData.set(CORE, core);
        lance.entityData.set(ARMED, true);
        double yaw = Math.toRadians(owner.getYRot());
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
        double side = (level.getRandom().nextDouble() - 0.5D) * 2.4D;
        Vec3 start = owner.getEyePosition().add(right.scale(side)).add(0.0D, 0.4D + level.getRandom().nextDouble() * 0.6D, 0.0D)
                .add(owner.getLookAngle().scale(0.6D));
        lance.setPos(start);
        lance.launch = target.subtract(start).normalize().scale(2.8D);
        lance.setDeltaMovement(lance.launch);
        lance.aim();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, start.x, start.y, start.z, 14, 0.2D, 0.2D, 0.2D, 0.1D);
        return lance;
    }

    /** Appears high above the target after delay ticks and plunges straight down. */
    public static SubspaceLance skyfall(ServerLevel level, LivingEntity owner, Vec3 target, int delay) {
        SubspaceLance lance = new SubspaceLance(AfterOSBlaster.SUBSPACE_LANCE_ENTITY.get(), level);
        lance.setOwner(owner);
        lance.delay = Math.max(1, delay);
        BlockHitResult ceiling = level.clip(new ClipContext(target.add(0.0D, 0.6D, 0.0D), target.add(0.0D, 18.0D, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        Vec3 start = ceiling.getType() == HitResult.Type.MISS
                ? target.add(0.0D, 18.0D, 0.0D)
                : ceiling.getLocation().subtract(0.0D, 0.6D, 0.0D);
        lance.setPos(start);
        lance.launch = new Vec3(0.0D, -2.8D, 0.0D);
        lance.setYRot(0.0F);
        lance.setXRot(90.0F);
        return lance;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ARMED, false);
        builder.define(CORE, false);
    }

    public boolean isArmed() {
        return this.entityData.get(ARMED);
    }

    public boolean isCore() {
        return this.entityData.get(CORE);
    }

    private void aim() {
        Vec3 m = this.getDeltaMovement();
        double horizontal = Math.sqrt(m.x * m.x + m.z * m.z);
        this.setYRot((float) (Mth.atan2(-m.x, m.z) * (180.0D / Math.PI)));
        this.setXRot((float) (-Mth.atan2(m.y, horizontal) * (180.0D / Math.PI)));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (isArmed()) {
                this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
            return;
        }
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (!isArmed()) {
            if (--this.delay <= 0) {
                this.entityData.set(ARMED, true);
                this.setDeltaMovement(this.launch);
                server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 16, 0.3D, 0.3D, 0.3D, 0.1D);
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
            return;
        }
        if (this.tickCount > 200 || !server.hasChunkAt(this.blockPosition())) {
            this.discard();
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            detonate(server, hit.getLocation());
            return;
        }
        Vec3 m = this.getDeltaMovement();
        this.setPos(getX() + m.x, getY() + m.y, getZ() + m.z);
        this.aim();
    }

    private void detonate(ServerLevel server, Vec3 at) {
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        Vec3 dir = this.getDeltaMovement();
        Vec3 center = dir.lengthSqr() > 1.0E-6D ? at.subtract(dir.normalize().scale(0.15D)) : at;
        ImpactEffects.blast(server, center, owner, this, BlastProfile.lance(isCore()));
        int n = isCore() ? 60 : 24;
        server.sendParticles(ParticleTypes.PORTAL, center.x, center.y + 0.5D, center.z, n, 0.8D, 0.8D, 0.8D, 0.6D);
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.5D, center.z, n, 0.6D, 0.6D, 0.6D, 0.2D);
        if (isCore() && owner != null) { // Space Core: a few more lances rain on the same spot
            for (int i = 0; i < 3; i++) {
                double a = server.getRandom().nextDouble() * Math.PI * 2.0D;
                double r = 1.5D + server.getRandom().nextDouble() * 2.5D;
                Vec3 point = new Vec3(center.x + Math.cos(a) * r, center.y, center.z + Math.sin(a) * r);
                server.addFreshEntity(SubspaceLance.skyfall(server, owner, point, 8 + i * 4));
            }
        }
        this.discard();
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
