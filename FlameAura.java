package dev.afteros.crt.entity;

import dev.afteros.crt.AfterOSBlaster;
import dev.afteros.crt.gameplay.BlastProfile;
import dev.afteros.crt.gameplay.ImpactEffects;
import dev.afteros.crt.gameplay.VoidEnergy;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/** Flame Armor: surrounds the owner in flames. The owner cannot be burned; anything hostile that gets close is scorched. */
public class FlameAura extends Projectile {
    public FlameAura(EntityType<? extends FlameAura> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static FlameAura start(ServerLevel level, ServerPlayer owner) {
        FlameAura aura = new FlameAura(AfterOSBlaster.FLAME_AURA.get(), level);
        aura.setOwner(owner);
        aura.setPos(owner.position());
        return aura;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (!(this.getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || owner.level() != this.level()) {
            this.discard();
            return;
        }
        this.setPos(owner.position());
        owner.clearFire();
        if (this.tickCount % 20 == 1) {
            if (this.tickCount > 1 && !VoidEnergy.spend(owner, 3.0D)) {
                owner.displayClientMessage(Component.translatable("message.afteros_crt.flame_armor_off"), true);
                this.discard();
                return;
            }
            owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0, false, false));
        }
        RandomSource rnd = server.getRandom();
        for (int i = 0; i < 5; i++) {
            double a = rnd.nextDouble() * Math.PI * 2.0D;
            server.sendParticles(ParticleTypes.FLAME, owner.getX() + Math.cos(a) * 1.1D, owner.getY() + rnd.nextDouble() * 1.9D, owner.getZ() + Math.sin(a) * 1.1D, 1, 0.0D, 0.05D, 0.0D, 0.01D);
        }
        if (this.tickCount % 5 == 0) {
            server.sendParticles(ParticleTypes.LAVA, owner.getX(), owner.getY() + 1.0D, owner.getZ(), 1, 0.8D, 0.6D, 0.8D, 0.0D);
        }
        if (this.tickCount % 10 == 0) {
            BlastProfile scorch = BlastProfile.thermal(6.0F, 0.8D);
            for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(3.5D), t -> ImpactEffects.canDamage(owner, t))) {
                if (target.distanceToSqr(owner) <= 3.5D * 3.5D) {
                    ImpactEffects.strike(server, owner, target, scorch, owner.position(), 1.0D);
                }
            }
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
