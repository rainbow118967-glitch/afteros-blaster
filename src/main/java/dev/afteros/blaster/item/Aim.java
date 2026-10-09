package dev.afteros.blaster.item;

import dev.afteros.blaster.gameplay.ImpactEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared look-ray helpers for the arsenal items. */
final class Aim {
    private Aim() {}

    static BlockHitResult block(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        return player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    /** First damageable living entity under the crosshair, or null. */
    static LivingEntity entity(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        BlockHitResult block = block(player, range);
        Vec3 stop = block.getType() == HitResult.Type.MISS ? start.add(player.getLookAngle().scale(range)) : block.getLocation();
        AABB box = player.getBoundingBox().expandTowards(stop.subtract(start)).inflate(1.5D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, start, stop, box,
                e -> e instanceof LivingEntity living && ImpactEffects.canDamage(player, living));
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /** Where the player is pointing: the entity's feet or the block surface or the far end of the ray. */
    static Vec3 point(ServerPlayer player, double range) {
        LivingEntity target = entity(player, range);
        if (target != null) {
            return target.position();
        }
        BlockHitResult block = block(player, range);
        return block.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(player.getLookAngle().scale(range))
                : block.getLocation();
    }
}
