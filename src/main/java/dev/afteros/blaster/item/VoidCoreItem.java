package dev.afteros.blaster.item;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.entity.SubspaceLance;
import dev.afteros.blaster.entity.VoidCollapse;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Herrscher Core of the Void.
 * Tap: Void Step (portal blink). Hold + release: rain Subspace Lances. Shift + right-click: Void Collapse (black hole).
 */
public class VoidCoreItem extends Item {
    private static final int LANCE_TICKS = 12;

    public VoidCoreItem(Item.Properties properties) {
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
                int cost = BlasterConfig.VOID_COLLAPSE_COST.get();
                if (!CrtBlasterItem.consumeCells(sp, cost, BlasterConfig.REQUIRE_AMMO.get())) {
                    sp.displayClientMessage(Component.translatable("message.afteros_blaster.need_cells", cost), true);
                    return InteractionResultHolder.fail(stack);
                }
                server.addFreshEntity(VoidCollapse.open(server, sp, Aim.point(sp, 48.0D)));
                sp.getCooldowns().addCooldown(this, 400);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide || !(user instanceof ServerPlayer player)) {
            return;
        }
        int charged = getUseDuration(stack, user) - remaining;
        if (charged % 4 == 0) {
            CrtBlasterItem.meter(player, charged >= LANCE_TICKS ? "VOID LANCES" : "VOID STEP", Math.min(charged, LANCE_TICKS), LANCE_TICKS,
                    ChatFormatting.DARK_PURPLE, CrtBlasterItem.countCells(player), "");
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level.isClientSide || !(user instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) {
            return;
        }
        int charged = getUseDuration(stack, user) - timeLeft;
        if (charged < LANCE_TICKS) {
            blink(server, player);
            return;
        }
        if (!CrtBlasterItem.consumeCells(player, 1, BlasterConfig.REQUIRE_AMMO.get())) {
            player.displayClientMessage(Component.translatable("message.afteros_blaster.no_ammo"), true);
            return;
        }
        Vec3 target = Aim.point(player, 64.0D);
        int n = BlasterConfig.VOID_LANCE_COUNT.get();
        for (int i = 0; i < n; i++) {
            double a = server.getRandom().nextDouble() * Math.PI * 2.0D;
            double r = i == 0 ? 0.0D : 1.5D + server.getRandom().nextDouble() * 3.5D;
            Vec3 point = new Vec3(target.x + Math.cos(a) * r, target.y, target.z + Math.sin(a) * r);
            server.addFreshEntity(SubspaceLance.skyfall(server, player, point, 4 + i * 3));
        }
        server.playSound(null, player.getX(), player.getY(), player.getZ(), AfterOSBlaster.VOID_LANCE.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        player.getCooldowns().addCooldown(this, 60);
    }

    /** Void Step: open a portal and appear where you are looking. */
    private void blink(ServerLevel server, ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = BlasterConfig.VOID_BLINK_RANGE.get();
        BlockHitResult hit = Aim.block(player, range);
        Vec3 end = hit.getType() == HitResult.Type.MISS
                ? start.add(look.scale(range))
                : hit.getLocation().subtract(look.scale(0.9D));
        Vec3 feet = new Vec3(end.x, end.y - player.getEyeHeight(), end.z);
        Vec3 dest = null;
        for (int up = 0; up <= 2 && dest == null; up++) {
            Vec3 f = feet.add(0.0D, up, 0.0D);
            AABB moved = player.getBoundingBox().move(f.subtract(player.position()));
            if (server.noCollision(player, moved)) {
                dest = f;
            }
        }
        if (dest == null) {
            player.displayClientMessage(Component.translatable("message.afteros_blaster.void_blocked"), true);
            return;
        }
        Vec3 from = player.position();
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1.0D, from.z, 40, 0.4D, 0.8D, 0.4D, 0.2D);
        server.playSound(null, from.x, from.y, from.z, AfterOSBlaster.VOID_OPEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.connection.teleport(dest.x, dest.y, dest.z, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
        server.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0D, dest.z, 60, 0.4D, 0.8D, 0.4D, 0.5D);
        server.playSound(null, dest.x, dest.y, dest.z, AfterOSBlaster.VOID_TRAVEL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.getCooldowns().addCooldown(this, 25);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.afteros_blaster.void.flavor").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.afteros_blaster.void.step").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.void.lances").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.void.collapse").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
