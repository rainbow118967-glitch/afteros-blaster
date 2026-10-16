package dev.afteros.crt.item;

import dev.afteros.crt.BlasterConfig;
import dev.afteros.crt.entity.CorruptionFront;
import dev.afteros.crt.gameplay.Virus;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** AfterOS Restoration Tool: puts the original blocks back, outward from where you look. */
public class RestorationToolItem extends Item {
    public RestorationToolItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            restore(server, sp, sp.isShiftKeyDown());
            sp.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Starts a restoration wave where the player is looking (also used by the keybinding). */
    public static void restore(ServerLevel server, ServerPlayer player, boolean large) {
        Vec3 at = player.pick(64.0D, 1.0F, false).getLocation();
        int radius = BlasterConfig.RESTORE_RADIUS.get() * (large ? 3 : 1);
        CorruptionFront.start(server, at, radius, true);
        Virus.cureAround(server, at, Math.min(radius, 48)); // restoring also cures the infection
        player.displayClientMessage(Component.translatable("message.afteros_crt.restoring", radius), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.afteros_crt.restoration.flavor").withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.afteros_crt.restoration.use").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
