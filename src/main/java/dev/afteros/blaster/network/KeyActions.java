package dev.afteros.blaster.network;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.item.CorruptorItem;
import dev.afteros.blaster.item.FloatingCrtItem;
import dev.afteros.blaster.item.RestorationToolItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Server-side handling of the keybindings. Every action works from the inventory (no need to hold the item). */
public final class KeyActions {
    public static final int SUMMON = 0;
    public static final int DISMISS = 1;
    public static final int FIRE = 2;
    public static final int CHARGED_FIRE = 3;
    public static final int SWITCH_TARGET = 4;
    public static final int RESTORE = 5;

    private KeyActions() {}

    private static ItemStack find(ServerPlayer player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void missing(ServerPlayer player, Item item) {
        player.displayClientMessage(Component.translatable("message.afteros_blaster.key_missing", item.getDescription()), true);
    }

    public static void handle(ServerPlayer player, int action) {
        if (!(player.level() instanceof ServerLevel server) || !player.isAlive()) {
            return;
        }
        switch (action) {
            case SUMMON -> {
                Item item = AfterOSBlaster.FLOATING_CRT.get();
                ItemStack stack = find(player, item);
                if (stack.isEmpty()) {
                    missing(player, item);
                } else if (!FloatingCrtItem.drones(server, player).isEmpty()) {
                    player.displayClientMessage(Component.translatable("message.afteros_blaster.drone_deployed"), true);
                } else if (!player.getCooldowns().isOnCooldown(item) && FloatingCrtItem.deploy(server, player, stack)) {
                    player.getCooldowns().addCooldown(item, 20);
                }
            }
            case DISMISS -> FloatingCrtItem.recall(server, player);
            case SWITCH_TARGET -> {
                Item item = AfterOSBlaster.FLOATING_CRT.get();
                ItemStack stack = find(player, item);
                if (stack.isEmpty()) {
                    missing(player, item);
                } else {
                    FloatingCrtItem.cycleMode(server, player, stack);
                }
            }
            case FIRE, CHARGED_FIRE -> {
                CorruptorItem item = AfterOSBlaster.CRT_CORRUPTOR.get();
                if (find(player, item).isEmpty()) {
                    missing(player, item);
                } else if (action == FIRE) {
                    item.fire(server, player, 12, 1);
                } else {
                    item.fire(server, player, BlasterConfig.CORRUPTION_MAX_RADIUS.get(), 2);
                }
            }
            case RESTORE -> {
                Item item = AfterOSBlaster.RESTORATION_TOOL.get();
                if (find(player, item).isEmpty()) {
                    missing(player, item);
                } else if (!player.getCooldowns().isOnCooldown(item)) {
                    RestorationToolItem.restore(server, player, player.isShiftKeyDown());
                    player.getCooldowns().addCooldown(item, 20);
                }
            }
            default -> { }
        }
    }
}
