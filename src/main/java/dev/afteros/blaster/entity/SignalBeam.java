package dev.afteros.blaster.entity;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.gameplay.BlastProfile;
import dev.afteros.blaster.gameplay.ImpactEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A sweeping phosphor beam. Either held by a player (damage + crater pulses at the point it
 * lands) or a short visual-only line used by the Floating CRT.
 */
public class SignalBeam extends Projectile {
    private static final EntityDataAccessor<Float> POWER =
            SynchedEntityData.defineId(SignalBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_X =
            SynchedEntityData.defineId(SignalBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Y =
            SynchedEntityData.defineId(SignalBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Z =
            SynchedEntityData.defineId(SignalBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> STYLE =
            SynchedEntityData.defineId(SignalBeam.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION =
            SynchedEntityData.defineId(SignalBeam.class, EntityDataSerializers.INT);

    // server-only state
    private boolean visualOnly;
    private boolean superBeam;

    public SignalBeam(EntityType<? extends SignalBeam> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    /** Beam held by a living owner; it re-aims every tick. */
    public static SignalBeam held(ServerLevel level, LivingEntity owner, float power, int duration, boolean superBeam) {
        SignalBeam beam = new SignalBeam(AfterOSBlaster.SIGNAL_BEAM.get(), level);
        beam.setOwner(owner);
        beam.superBeam = superBeam;
        beam.entityData.set(STYLE, superBeam ? 1 : 0);
        beam.entityData.set(POWER, power);
        beam.entityData.set(DURATION, duration);
        beam.setPos(muzzle(owner, owner.getLookAngle()));
        return beam;
    }

    /** Short decorative line from a to b. Does no damage by itself. */
    public static SignalBeam visual(ServerLevel level, LivingEntity owner, Vec3 from, Vec3 to, float power, int duration, int style) {
        SignalBeam beam = new SignalBeam(AfterOSBlaster.SIGNAL_BEAM.get(), level);
        beam.setOwner(owner);
        beam.visualOnly = true;
        beam.entityData.set(STYLE, style);
        beam.entityData.set(POWER, power);
        beam.entityData.set(DURATION, duration);
        beam.setPos(from);
        beam.setEnd(to.subtract(from));
        return beam;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(POWER, 1.0F);
        builder.define(END_X, 0.0F);
        builder.define(END_Y, 0.0F);
        builder.define(END_Z, 0.0F);
        builder.define(DURATION, 20);
        builder.define(STYLE, 0);
    }

    private void setEnd(Vec3 offset) {
        this.entityData.set(END_X, (float) offset.x);
        this.entityData.set(END_Y, (float) offset.y);
        this.entityData.set(END_Z, (float) offset.z);
    }

    public Vec3 getBeamOffset() {
        return new Vec3(this.entityData.get(END_X), this.entityData.get(END_Y), this.entityData.get(END_Z));
    }

    public float getPower() {
        return this.entityData.get(POWER);
    }

    /** 0 = cyan, 1 = overclocked (red), 2 = floating CRT (green). */
    public int getStyle() {
        return this.entityData.get(STYLE);
    }

    public int getDuration() {
        return this.entityData.get(DURATION);
    }

    private static Vec3 muzzle(LivingEntity owner, Vec3 look) {
        return owner.getEyePosition().add(look.scale(0.8D)).add(0.0D, -0.3D, 0.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount >= getDuration()) {
            this.discard();
            return;
        }
        if (this.visualOnly) {
            return;
        }
        if (!(this.getOwner() instanceof LivingEntity owner) || !owner.isAlive() || !(this.level() instanceof ServerLevel server)) {
            this.discard();
            return;
        }

        Vec3 look = owner.getLookAngle();
        Vec3 start = muzzle(owner, look);
        double range = this.superBeam ? BlasterConfig.SUPER_RANGE.get() : BlasterConfig.BEAM_RANGE.get();
        Vec3 far = start.add(look.scale(range));
        BlockHitResult hit = server.clip(new ClipContext(start, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean blockHit = hit.getType() == HitResult.Type.BLOCK;
        Vec3 end = blockHit ? hit.getLocation() : far;

        this.setPos(start);
        this.setEnd(end.subtract(start));

        int interval = this.superBeam ? 6 : 10;
        if ((this.tickCount - 1) % interval != 0) {
            return;
        }

        BlastProfile profile = this.superBeam ? BlastProfile.superBlast() : BlastProfile.beamPulse(getPower());
        double width = this.superBeam ? 2.4D : 0.9D + 0.4D * getPower();

        // everything standing in the beam
        AABB sweep = new AABB(start, end).inflate(width + 1.0D);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, sweep, t -> ImpactEffects.canDamage(owner, t))) {
            Vec3 c = target.getBoundingBox().getCenter();
            if (distanceToSegment(c, start, end) <= width + target.getBbWidth() * 0.5D) {
                ImpactEffects.strike(server, owner, target, profile, start, 1.0D);
            }
        }

        // the ground where it lands
        if (blockHit && server.hasChunkAt(hit.getBlockPos())) {
            ImpactEffects.blast(server, end.subtract(look.scale(0.1D)), owner, this, profile);
            if (this.superBeam && ((this.tickCount - 1) / interval) % 2 == 0 && BlasterConfig.SUPER_LIGHTNING.get()) {
                ImpactEffects.flash(server, end);
            }
        }
    }

    private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        if (len2 < 1.0E-6D) {
            return p.distanceTo(a);
        }
        double t = Mth.clamp(p.subtract(a).dot(ab) / len2, 0.0D, 1.0D);
        return p.distanceTo(a.add(ab.scale(t)));
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
