package dev.afteros.blaster.entity;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.gameplay.BlastProfile;
import dev.afteros.blaster.gameplay.ImpactEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Charged phosphor bolt: flies straight, then craters whatever it hits. */
public class SignalBolt extends Projectile {
    private static final EntityDataAccessor<Float> POWER =
            SynchedEntityData.defineId(SignalBolt.class, EntityDataSerializers.FLOAT);

    public SignalBolt(EntityType<? extends SignalBolt> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    private int maxAge = 120;

    public SignalBolt(ServerLevel level, LivingEntity owner, float power) {
        this(level, owner, power, owner.getLookAngle());
    }

    public SignalBolt(ServerLevel level, LivingEntity owner, float power, Vec3 dir) {
        this(AfterOSBlaster.SIGNAL_BOLT.get(), level);
        this.setOwner(owner);
        this.entityData.set(POWER, power);
        this.setPos(owner.getEyePosition().add(dir.scale(0.8D)).add(0.0D, -0.2D, 0.0D));
        this.setDeltaMovement(dir.scale(BlasterConfig.PROJECTILE_SPEED.get()));
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
    }

    /** Server-side range limit in ticks (scatter shots use a short one). */
    public SignalBolt withMaxAge(int ticks) {
        this.maxAge = ticks;
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(POWER, 1.0F);
    }

    public float getPower() {
        return this.entityData.get(POWER);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = this.getDeltaMovement();
        if (!this.level().isClientSide) {
            if (this.tickCount > this.maxAge || !this.level().hasChunkAt(this.blockPosition())) {
                this.discard();
                return;
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                detonate(hit.getLocation());
                return;
            }
        } else {
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }

    private void detonate(Vec3 at) {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        Vec3 travel = this.getDeltaMovement();
        Vec3 center = travel.lengthSqr() > 1.0E-6D ? at.subtract(travel.normalize().scale(0.15D)) : at;
        ImpactEffects.blast(server, center, owner, this, BlastProfile.bolt(getPower()));
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
