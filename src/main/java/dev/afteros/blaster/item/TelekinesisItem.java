package dev.afteros.blaster.item;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.entity.TelekinesisGrip;
import dev.afteros.blaster.gameplay.BlastProfile;
import dev.afteros.blaster.gameplay.ImpactEffects;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Telekinesis Authority.
 * Tap: Force Push. Shift + right-click: Force Pull.
 * Hold on a target: lift it; release to throw it, sneak to slam it into the ground.
 */
public class TelekinesisItem extends Item {
    private static final int GRAB_TICKS = 8;

    public TelekinesisItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (player.isShiftKeyDown()) {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
                cone(server, sp, true);
                sp.getCooldowns().addCooldown(this, 20);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide || !(user instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) {
            return;
        }
        int charged = getUseDuration(stack, user) - remaining;
        if (charged == GRAB_TICKS) {
            LivingEntity target = Aim.entity(player, BlasterConfig.TK_RANGE.get());
            if (target != null) {
                server.addFreshEntity(TelekinesisGrip.hold(server, player, target));
                server.playSound(null, target.getX(), target.getY(), target.getZ(), AfterOSBlaster.FORCE_GRAB.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level.isClientSide || !(user instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) {
            return;
        }
        int charged = getUseDuration(stack, user) - timeLeft;
        if (charged < GRAB_TICKS) { // a tap is a push
            cone(server, player, false);
            player.getCooldowns().addCooldown(this, 20);
        } else {
            player.getCooldowns().addCooldown(this, 10);
        }
    }

    /** Cone in front of the player: push (damage + knockback) or pull (drag toward the player). */
    private static void cone(ServerLevel server, ServerPlayer player, boolean pull) {
        double range = Math.min(18.0D, BlasterConfig.TK_RANGE.get());
        Vec3 look = player.getLookAngle();
        Vec3 origin = player.getEyePosition();
        BlastProfile push = BlastProfile.custom(BlasterConfig.TK_PUSH_DAMAGE.get().floatValue(), 0.0D, 0.0D, 0, 0, 2.2D, false);
        AABB box = player.getBoundingBox().inflate(range);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, box, t -> ImpactEffects.canDamage(player, t))) {
            Vec3 d = target.getBoundingBox().getCenter().subtract(origin);
            double dist = d.length();
            if (dist > range || dist < 0.01D) {
                continue;
            }
            Vec3 dir = d.scale(1.0D / dist);
            if (dir.dot(look) < 0.55D) {
                continue;
            }
            if (pull) {
                target.setDeltaMovement(dir.scale(-1.6D).add(0.0D, 0.3D, 0.0D));
                target.hurtMarked = true;
            } else {
                ImpactEffects.strike(server, player, target, push, origin, 1.0D);
            }
        }
        for (int i = 1; i <= 4; i++) {
            Vec3 p = origin.add(look.scale(i * 2.5D));
            server.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        server.playSound(null, player.getX(), player.getY(), player.getZ(),
                pull ? AfterOSBlaster.FORCE_PULL.get() : AfterOSBlaster.FORCE_PUSH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.afteros_blaster.tk.flavor").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.afteros_blaster.tk.tap").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.tk.hold").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.tk.pull").withStyle(ChatFormatting.DARK_AQUA));
    }
}
