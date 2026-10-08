package dev.afteros.blaster.entity;

import dev.afteros.blaster.AfterOSBlaster;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A flying chunk of terrain. Purely visual: it never places a block or drops an item. */
public class SignalDebris extends Entity {
    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(SignalDebris.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME =
            SynchedEntityData.defineId(SignalDebris.class, EntityDataSerializers.INT);

    public SignalDebris(EntityType<? extends SignalDebris> type, Level level) {
        super(type, level);
    }

    public SignalDebris(ServerLevel level, BlockState state, Vec3 pos, Vec3 velocity, int lifetime) {
        this(AfterOSBlaster.SIGNAL_DEBRIS.get(), level);
        this.setPos(pos);
        this.setDeltaMovement(velocity);
        this.entityData.set(STATE, Block.getId(state));
        this.entityData.set(LIFETIME, lifetime);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, Block.getId(Blocks.STONE.defaultBlockState()));
        builder.define(LIFETIME, 100);
    }

    public BlockState getBlockState() {
        return Block.stateById(this.entityData.get(STATE));
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    public boolean hasExpired() {
        return this.tickCount >= getLifetime();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.onGround() && this.tickCount < 40 && this.tickCount % 3 == 0) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, this.getX(), this.getY() + 0.25D, this.getZ(), 0.0D, 0.0D, 0.0D);
        }
        if (!this.level().isClientSide && (hasExpired() || this.getY() < this.level().getMinBuildHeight())) {
            this.discard();
            return;
        }
        Vec3 motion = this.getDeltaMovement().add(0.0D, -0.04D, 0.0D);
        this.move(MoverType.SELF, motion);
        Vec3 after = this.getDeltaMovement();
        double x = after.x;
        double y = after.y;
        double z = after.z;
        if (this.onGround()) {
            x *= 0.6D;
            z *= 0.6D;
            y = motion.y < -0.2D ? -motion.y * 0.3D : 0.0D; // little bounce
        } else {
            x *= 0.99D;
            z *= 0.99D;
            y *= 0.99D;
        }
        this.setDeltaMovement(x, y, z);
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
