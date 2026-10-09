package dev.afteros.blaster.entity;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.gameplay.BlastProfile;
import dev.afteros.blaster.gameplay.ImpactEffects;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Invisible controller that holds a mob in the air while the owner keeps using Telekinesis, then throws or slams it. */
public class TelekinesisGrip extends Projectile {
    private UUID targetId;
    private int state; // 0 held, 1 thrown, 2 slammed
    private int age;
    private Vec3 flight = Vec3.ZERO;

    public TelekinesisGrip(EntityType<? extends TelekinesisGrip> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static TelekinesisGrip hold(ServerLevel level, ServerPlayer owner, LivingEntity target) {
        TelekinesisGrip grip = new TelekinesisGrip(AfterOSBlaster.TELEKINESIS_GRIP.get(), level);
        grip.setOwner(owner);
        grip.targetId = target.getUUID();
        grip.setPos(target.position());
        return grip;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel server) || this.targetId == null) {
            return;
        }
        if (!(this.getOwner() instanceof ServerPlayer owner) || !owner.isAlive()) {
            this.discard();
            return;
        }
        Entity found = server.getEntity(this.targetId);
        if (!(found instanceof LivingEntity target) || !target.isAlive()) {
            this.discard();
            return;
        }
        this.setPos(target.position());

        if (this.state == 0) {
            boolean stillUsing = owner.isUsingItem() && owner.getUseItem().is(AfterOSBlaster.TELEKINESIS.get());
            if (!stillUsing) { // released: throw
                this.state = 1;
                this.flight = owner.getLookAngle().scale(2.4D).add(0.0D, 0.15D, 0.0D);
                target.setDeltaMovement(this.flight);
                target.hurtMarked = true;
                server.playSound(null, target.getX(), target.getY(), target.getZ(), AfterOSBlaster.FORCE_THROW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                return;
            }
            if (owner.isShiftKeyDown()) { // sneak while holding: slam
                this.state = 2;
                this.flight = new Vec3(0.0D, -3.2D, 0.0D);
                target.setDeltaMovement(this.flight);
                target.hurtMarked = true;
                server.playSound(null, target.getX(), target.getY(), target.getZ(), AfterOSBlaster.FORCE_SLAM.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                return;
            }
            Vec3 hold = owner.getEyePosition().add(owner.getLookAngle().scale(4.5D));
            Vec3 pull = hold.subtract(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)).scale(0.4D);
            if (pull.length() > 1.5D) {
                pull = pull.normalize().scale(1.5D);
            }
            target.setDeltaMovement(pull);
            target.fallDistance = 0.0F;
            target.hurtMarked = true;
            if (this.tickCount % 3 == 0) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        4, 0.4D, 0.5D, 0.4D, 0.1D);
            }
            return;
        }

        this.age++;
        if (this.age <= 6) { // keep the launch velocity from being eaten by mob AI
            target.setDeltaMovement(this.flight);
            target.hurtMarked = true;
        }
        if (this.age > 80) {
            this.discard();
            return;
        }
        if (this.age > 3 && (target.onGround() || target.horizontalCollision || target.verticalCollision)) {
            boolean slam = this.state == 2;
            float damage = (slam ? BlasterConfig.TK_SLAM_DAMAGE : BlasterConfig.TK_THROW_DAMAGE).get().floatValue();
            BlastProfile profile = BlastProfile.custom(damage, slam ? 5.0D : 4.0D, slam ? 3.0D : 2.2D, slam ? 500 : 250, slam ? 40 : 20, 1.2D, false);
            ImpactEffects.blast(server, target.position().add(0.0D, 0.1D, 0.0D), owner, this, profile);
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
