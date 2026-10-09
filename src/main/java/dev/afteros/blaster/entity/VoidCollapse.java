package dev.afteros.blaster.entity;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.gameplay.BlastProfile;
import dev.afteros.blaster.gameplay.ImpactEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A black hole: drags everything nearby toward its centre, grinds it down, then detonates. Drawn with particles. */
public class VoidCollapse extends Projectile {
    private int life;
    private int duration;
    private double radius;

    public VoidCollapse(EntityType<? extends VoidCollapse> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static VoidCollapse open(ServerLevel level, LivingEntity owner, Vec3 center) {
        VoidCollapse hole = new VoidCollapse(AfterOSBlaster.VOID_COLLAPSE.get(), level);
        hole.setOwner(owner);
        hole.duration = BlasterConfig.VOID_COLLAPSE_TICKS.get();
        hole.radius = BlasterConfig.VOID_COLLAPSE_RADIUS.get();
        hole.setPos(center.add(0.0D, 1.5D, 0.0D));
        level.playSound(null, center.x, center.y, center.z, AfterOSBlaster.VOID_OPEN.get(), SoundSource.PLAYERS, 2.0F, 0.8F);
        return hole;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        this.life++;
        Vec3 c = this.position();

        // pull everything in
        AABB box = new AABB(c.x - radius, c.y - radius, c.z - radius, c.x + radius, c.y + radius, c.z + radius);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, box, t -> ImpactEffects.canDamage(owner, t))) {
            Vec3 toward = c.subtract(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
            double dist = toward.length();
            if (dist > radius || dist < 0.05D) {
                continue;
            }
            Vec3 pull = toward.scale(1.0D / dist).scale(0.2D + 0.25D * (this.life / (double) this.duration));
            target.setDeltaMovement(target.getDeltaMovement().scale(0.85D).add(pull));
            target.fallDistance = 0.0F;
            target.hurtMarked = true;
            if (this.life % 10 == 0) {
                ImpactEffects.strike(server, owner, target, BlastProfile.custom(4.0F, 0.0D, 0.0D, 0, 0, 0.0D, false), c, 1.0D);
            }
        }

        // swirling particles, drawn inward
        double ring = radius * (1.0D - (this.life % 20) / 20.0D);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2.0D * i / 12.0D + this.life * 0.35D;
            double px = c.x + Math.cos(a) * ring;
            double pz = c.z + Math.sin(a) * ring;
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, px, c.y + (i % 3) * 0.4D, pz, 0, c.x - px, 0.0D, c.z - pz, 0.12D);
        }
        server.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 3, 0.3D, 0.3D, 0.3D, 0.01D);

        if (this.life >= this.duration) {
            BlastProfile finale = BlastProfile.custom(BlasterConfig.VOID_COLLAPSE_DAMAGE.get().floatValue(), radius * 1.2D, radius * 0.65D, 4000, 220, 2.5D, true);
            ImpactEffects.blast(server, c.subtract(0.0D, 1.0D, 0.0D), owner, this, finale);
            server.playSound(null, c.x, c.y, c.z, AfterOSBlaster.VOID_COLLAPSE_SOUND.get(), SoundSource.PLAYERS, 4.0F, 1.0F);
            this.discard();
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
