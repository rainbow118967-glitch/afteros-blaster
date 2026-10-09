package dev.afteros.blaster.entity;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.gameplay.BlastProfile;
import dev.afteros.blaster.gameplay.ImpactEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Two Soulmine blades orbiting the owner, cutting any hostile mob that gets close. The blades are drawn by the renderer. */
public class SoulmineOrbit extends Projectile {
    private int ticksLeft;

    public SoulmineOrbit(EntityType<? extends SoulmineOrbit> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static SoulmineOrbit summon(ServerLevel level, ServerPlayer owner) {
        SoulmineOrbit rig = new SoulmineOrbit(AfterOSBlaster.SOULMINE_ORBIT.get(), level);
        rig.setOwner(owner);
        rig.ticksLeft = BlasterConfig.SOUL_ORBIT_TICKS.get();
        rig.setPos(owner.getX(), owner.getY() + owner.getBbHeight() * 0.5D, owner.getZ());
        return rig;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0D * 96.0D;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (!(this.getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || owner.level() != this.level() || --this.ticksLeft <= 0) {
            this.discard();
            return;
        }
        this.setPos(owner.getX(), owner.getY() + owner.getBbHeight() * 0.5D, owner.getZ());
        if (this.tickCount % 8 != 0) {
            return;
        }
        BlastProfile profile = BlastProfile.custom(BlasterConfig.SOUL_ORBIT_DAMAGE.get().floatValue(), 0.0D, 0.0D, 0, 0, 0.9D, false);
        AABB box = owner.getBoundingBox().inflate(3.4D);
        boolean hit = false;
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, box, t -> ImpactEffects.canDamage(owner, t))) {
            boolean hostile = target instanceof Enemy || (target instanceof Mob mob && mob.getTarget() instanceof Player);
            if (hostile && target.distanceToSqr(owner) <= 3.4D * 3.4D) {
                ImpactEffects.strike(server, owner, target, profile, owner.position(), 1.0D);
                server.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                hit = true;
            }
        }
        if (hit) {
            server.playSound(null, owner.getX(), owner.getY(), owner.getZ(), AfterOSBlaster.SOUL_IMPACT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
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
