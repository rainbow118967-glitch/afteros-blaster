package dev.afteros.blaster.gameplay;

import dev.afteros.blaster.BlasterConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

/** Honkai energy for the Void Core: refills on its own (absorbed from the air), faster while the Core is held. Server side only. */
public final class VoidEnergy {
    public static final double MAX = 100.0D;
    private static final Map<UUID, double[]> STATE = new HashMap<>(); // {energy, lastGameTime}

    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
    private static final Map<UUID, Long> BAR_SEEN = new HashMap<>();

    private VoidEnergy() {}

    /** Shows / refreshes the Honkai energy bar at the top of the screen (a vanilla boss bar, server driven). */
    public static void showBar(ServerPlayer player, Component label, BossEvent.BossBarColor color) {
        long now = player.level().getGameTime();
        ServerBossEvent bar = BARS.get(player.getUUID());
        if (bar == null) {
            bar = new ServerBossEvent(label, color, BossEvent.BossBarOverlay.NOTCHED_10);
            BARS.put(player.getUUID(), bar);
        }
        if (!bar.getPlayers().contains(player)) {
            bar.removeAllPlayers(); // a reconnecting player is a new object
            bar.addPlayer(player);
        }
        bar.setName(label);
        bar.setColor(color);
        bar.setProgress((float) Math.max(0.0D, Math.min(1.0D, get(player) / MAX)));
        BAR_SEEN.put(player.getUUID(), now);
        for (UUID id : new ArrayList<>(BAR_SEEN.keySet())) { // drop bars nobody is refreshing any more
            if (now - BAR_SEEN.get(id) > 100L) {
                hideBar(id);
            }
        }
    }

    /** Called while a core sits unused in the inventory: hides the bar a couple of seconds after it was last shown. */
    public static void tickIdle(ServerPlayer player) {
        Long seen = BAR_SEEN.get(player.getUUID());
        if (seen != null && player.level().getGameTime() - seen > 40L) {
            hideBar(player.getUUID());
        }
    }

    private static void hideBar(UUID id) {
        ServerBossEvent bar = BARS.remove(id);
        if (bar != null) {
            bar.removeAllPlayers();
        }
        BAR_SEEN.remove(id);
    }

    public static double get(ServerPlayer player) {
        long now = player.level().getGameTime();
        double[] s = STATE.get(player.getUUID());
        if (s == null || now < (long) s[1]) {
            STATE.put(player.getUUID(), new double[] {MAX, now});
            return MAX;
        }
        s[0] = Math.min(MAX, s[0] + (now - (long) s[1]) * BlasterConfig.VOID_REGEN.get() / 20.0D);
        s[1] = now;
        return s[0];
    }

    /** Extra energy radiated into the user while the Core is in hand. */
    public static void absorb(ServerPlayer player, double amount) {
        double e = Math.min(MAX, get(player) + amount);
        STATE.get(player.getUUID())[0] = e;
    }

    public static boolean spend(ServerPlayer player, double cost) {
        if (player.isCreative()) {
            return true;
        }
        double e = get(player);
        if (e < cost) {
            player.displayClientMessage(Component.literal("HONKAI ENERGY // " + (int) e + "%  (needs " + (int) cost + "%)").withStyle(ChatFormatting.RED), true);
            return false;
        }
        STATE.get(player.getUUID())[0] = e - cost;
        return true;
    }
}
