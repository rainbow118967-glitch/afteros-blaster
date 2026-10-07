package dev.afteros.blaster.entity;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.gameplay.ImpactEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A field of Imaginary Space: time crawls inside it while subspace lances rain down. Invisible; drawn with particles. */
public class ImaginarySpace extends Projectile {
    private int life;
    private int duration;
    private double radius;
    private int interval;

    public ImaginarySpace(EntityType<? extends ImaginarySpace> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static ImaginarySpace open(ServerLevel level, LivingEntity owner, Vec3 center) {
        ImaginarySpace space = new ImaginarySpace(AfterOSBlaster.IMAGINARY_SPACE.get(), level);
        space.setOwner(owner);
        space.setPos(center);
        space.duration = BlasterConfig.SPACE_TICKS.get();
        space.radius = BlasterConfig.SPACE_RADIUS.get();
        space.interval = BlasterConfig.SPACE_LANCE_INTERVAL.get();
        level.playSound(null, center.x, center.y, center.z, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.8F, 1.4F);
        return space;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (++this.life > this.duration) {
            this.discard();
            return;
        }
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;

        // the rim of the field
        if (this.life % 2 == 0) {
            for (int i = 0; i < 28; i++) {
                double a = Math.PI * 2.0D * i / 28.0D + this.life * 0.05D;
                server.sendParticles(ParticleTypes.PORTAL, getX() + Math.cos(a) * radius, getY() + 0.15D, getZ() + Math.sin(a) * radius,
                        1, 0.05D, 0.4D, 0.05D, 0.02D);
            }
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.3D, getZ(), 6, radius * 0.5D, 0.2D, radius * 0.5D, 0.02D);
        }

        // time slows for everything inside
        if (this.life % 5 == 1) {
            AABB box = new AABB(getX() - radius, getY() - 4.0D, getZ() - radius, getX() + radius, getY() + 10.0D, getZ() + radius);
            for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, box, t -> ImpactEffects.canDamage(owner, t))) {
                double dx = target.getX() - getX();
                double dz = target.getZ() - getZ();
                if (dx * dx + dz * dz <= radius * radius) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 14, 4, false, false));
                }
            }
        }

        // lances rain down
        if (owner != null && this.life % this.interval == 0) {
            double a = server.getRandom().nextDouble() * Math.PI * 2.0D;
            double r = Math.sqrt(server.getRandom().nextDouble()) * radius;
            Vec3 point = new Vec3(getX() + Math.cos(a) * r, getY(), getZ() + Math.sin(a) * r);
            server.addFreshEntity(SubspaceLance.skyfall(server, owner, point, 4));
        }
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
