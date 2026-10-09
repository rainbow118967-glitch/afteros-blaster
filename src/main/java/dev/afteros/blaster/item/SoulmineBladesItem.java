package dev.afteros.blaster.item;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.entity.SoulmineOrbit;
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
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Soulmine Blades.
 * Tap: chain whip (drags a mob to you, or echoes a block into flying shrapnel).
 * Hold: Chain Spin. Shift + right-click: summon / dismiss the orbiting dual blade rig.
 */
public class SoulmineBladesItem extends Item {
    private static final int SPIN_TICKS = 10;

    public SoulmineBladesItem(Item.Properties properties) {
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
                List<? extends SoulmineOrbit> mine = server.getEntities(EntityTypeTest.forClass(SoulmineOrbit.class),
                        d -> d.getOwner() != null && d.getOwner().getUUID().equals(sp.getUUID()));
                if (!mine.isEmpty()) {
                    mine.forEach(SoulmineOrbit::discard);
                    sp.displayClientMessage(Component.translatable("message.afteros_blaster.orbit_off"), true);
                } else if (CrtBlasterItem.consumeCells(sp, BlasterConfig.SOUL_ORBIT_COST.get(), BlasterConfig.REQUIRE_AMMO.get())) {
                    server.addFreshEntity(SoulmineOrbit.summon(server, sp));
                    server.playSound(null, sp.getX(), sp.getY(), sp.getZ(), AfterOSBlaster.SOUL_CHARGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    sp.displayClientMessage(Component.translatable("message.afteros_blaster.orbit_on"), true);
                } else {
                    sp.displayClientMessage(Component.translatable("message.afteros_blaster.no_ammo"), true);
                    return InteractionResultHolder.fail(stack);
                }
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
        if (charged < SPIN_TICKS || (charged - SPIN_TICKS) % 10 != 0) {
            return;
        }
        // Chain Spin pulse: a ring of blades around the player
        double radius = BlasterConfig.SOUL_SPIN_RADIUS.get();
        BlastProfile spin = BlastProfile.custom(BlasterConfig.SOUL_SPIN_DAMAGE.get().floatValue(), 0.0D, 0.0D, 0, 0, 1.0D, false);
        AABB box = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, box, t -> ImpactEffects.canDamage(player, t))) {
            if (target.distanceToSqr(player) <= radius * radius) {
                ImpactEffects.strike(server, player, target, spin, player.position(), 1.0D);
            }
        }
        for (int i = 0; i < 20; i++) {
            double a = Math.PI * 2.0D * i / 20.0D;
            server.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + Math.cos(a) * radius * 0.8D, player.getY() + 1.0D,
                    player.getZ() + Math.sin(a) * radius * 0.8D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        server.playSound(null, player.getX(), player.getY(), player.getZ(), AfterOSBlaster.CHAIN_SWING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level.isClientSide || !(user instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) {
            return;
        }
        int charged = getUseDuration(stack, user) - timeLeft;
        if (charged >= SPIN_TICKS) {
            player.getCooldowns().addCooldown(this, 16);
            return;
        }

        // chain whip
        double range = BlasterConfig.SOUL_WHIP_RANGE.get();
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.5D)).add(0.0D, -0.3D, 0.0D);
        LivingEntity target = Aim.entity(player, range);
        if (target != null) {
            Vec3 end = target.getBoundingBox().getCenter();
            chain(server, start, end);
            BlastProfile whip = BlastProfile.custom(BlasterConfig.SOUL_WHIP_DAMAGE.get().floatValue(), 0.0D, 0.0D, 0, 0, 0.0D, false);
            ImpactEffects.strike(server, player, target, whip, player.position(), 1.0D);
            if (target.isAlive()) { // reel it in
                Vec3 reel = player.position().subtract(target.position());
                if (reel.lengthSqr() > 1.0E-4D) {
                    target.setDeltaMovement(reel.normalize().scale(1.4D).add(0.0D, 0.3D, 0.0D));
                    target.hurtMarked = true;
                }
            }
            server.playSound(null, player.getX(), player.getY(), player.getZ(), AfterOSBlaster.CHAIN_THROW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            server.playSound(null, end.x, end.y, end.z, AfterOSBlaster.SOUL_IMPACT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            BlockHitResult hit = Aim.block(player, range);
            if (hit.getType() == HitResult.Type.BLOCK) { // block echo: the blade bites the ground and throws it back as shrapnel
                Vec3 at = hit.getLocation();
                chain(server, start, at);
                BlastProfile echo = BlastProfile.custom(6.0F, 5.0D, 2.2D, 250, 18, 1.0D, false);
                ImpactEffects.blast(server, at, player, null, echo);
                server.playSound(null, at.x, at.y, at.z, AfterOSBlaster.BLOCK_LAUNCH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                server.playSound(null, player.getX(), player.getY(), player.getZ(), AfterOSBlaster.CHAIN_SWING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        player.getCooldowns().addCooldown(this, 14);
    }

    private static void chain(ServerLevel server, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.1D) {
            return;
        }
        Vec3 step = d.scale(0.6D / len);
        Vec3 p = from;
        for (double t = 0.0D; t <= len; t += 0.6D) {
            server.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            p = p.add(step);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.afteros_blaster.soul.flavor").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.afteros_blaster.soul.tap").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.soul.hold").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.soul.orbit").withStyle(ChatFormatting.DARK_GRAY));
    }
}
