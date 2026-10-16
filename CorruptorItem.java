package dev.afteros.crt.item;

import dev.afteros.crt.AfterOSBlaster;
import dev.afteros.crt.BlasterConfig;
import dev.afteros.crt.entity.SignalBeam;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** AfterOS CRT Corruptor: a pink-eyed CRT that fires a corruption beam. Charge decides how far the infection spreads. */
public class CorruptorItem extends Item {
    private static final int FULL_CHARGE = 50;

    public CorruptorItem(Item.Properties properties) {
        super(properties);
    }

    /** 5 / 12 / max blocks depending on charge. */
    private static int radiusFor(int charged, int max) {
        float p = Mth.clamp(charged / (float) FULL_CHARGE, 0.0F, 1.0F);
        return Math.min(max, p < 0.34F ? 5 : p < 0.75F ? 12 : 25);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
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
            int radius = radiusFor(charged, BlasterConfig.CORRUPTION_MAX_RADIUS.get());
            CrtBlasterItem.meter(player, "CORRUPT r=" + radius, Math.min(charged, FULL_CHARGE), FULL_CHARGE,
                    ChatFormatting.LIGHT_PURPLE, CrtBlasterItem.countCells(player), "");
        }
        if (charged % 8 == 1) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(), AfterOSBlaster.CHARGE.get(), SoundSource.PLAYERS,
                    0.7F, 0.5F + Math.min(charged, 50) / 50.0F * 0.6F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level.isClientSide || !(user instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) {
            return;
        }
        int charged = getUseDuration(stack, user) - timeLeft;
        if (charged < 8) {
            return;
        }
        fire(server, player, radiusFor(charged, BlasterConfig.CORRUPTION_MAX_RADIUS.get()), 1);
    }

    /** Fires the corruption beam (also used by the keybinding). Returns false if it could not fire. */
    public boolean fire(ServerLevel server, ServerPlayer player, int radius, int cost) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return false;
        }
        if (!CrtBlasterItem.consumeCells(player, cost, BlasterConfig.REQUIRE_AMMO.get())) {
            player.displayClientMessage(Component.translatable("message.afteros_crt.need_cells", cost), true);
            return false;
        }
        int r = Math.min(radius, BlasterConfig.CORRUPTION_MAX_RADIUS.get());
        SignalBeam beam = SignalBeam.held(server, player, 1.5F, 30, false);
        beam.setStyle(3);
        beam.withCorruption(r);
        server.addFreshEntity(beam);
        server.playSound(null, player.getX(), player.getY(), player.getZ(), AfterOSBlaster.FIRE.get(), SoundSource.PLAYERS, 1.2F, 0.6F);
        player.getCooldowns().addCooldown(this, 40);
        return true;
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
        lines.add(Component.translatable("tooltip.afteros_crt.corruptor.flavor").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.afteros_crt.corruptor.charge").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_crt.corruptor.effect").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
