package dev.afteros.crt.gameplay;

import dev.afteros.crt.AfterOSBlaster;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Imaginary Space: a void dimension with a starry sky and giant floating black cubes.
 * Every block you walk there is 8 blocks in the Overworld, so it is a shortcut between far places.
 */
public final class ImaginaryTravel {
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, AfterOSBlaster.id("imaginary_space"));

    private ImaginaryTravel() {}

    public static boolean isInside(ServerPlayer player) {
        return player.level().dimension().equals(DIM);
    }

    /** Enters Imaginary Space, or leaves it if already inside. Returns true if the player moved. */
    public static boolean toggle(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        if (isInside(player)) {
            leave(server, player);
            return true;
        }
        ServerLevel dim = server.getLevel(DIM);
        if (dim == null) {
            player.displayClientMessage(Component.translatable("message.afteros_crt.void_no_dimension"), true);
            return false;
        }
        int ax = Mth.floor(player.getX() / 8.0D);
        int az = Mth.floor(player.getZ() / 8.0D);
        int ay = 100;
        buildScenery(dim, ax, ay, az);
        player.teleportTo(dim, ax + 0.5D, ay, az + 0.5D, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
        if (!player.isCreative() && !player.isSpectator() && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true; // you can fly in the Void
            player.onUpdateAbilities();
        }
        player.displayClientMessage(Component.translatable("message.afteros_crt.void_in"), false);
        return true;
    }

    private static void leave(MinecraftServer server, ServerPlayer player) {
        ServerLevel dest = server.overworld();
        double tx = player.getX() * 8.0D;
        double tz = player.getZ() * 8.0D;
        BlockPos clamped = dest.getWorldBorder().clampToBounds(tx, 64.0D, tz);
        tx = clamped.getX() + 0.5D;
        tz = clamped.getZ() + 0.5D;
        BlockPos column = BlockPos.containing(tx, 64.0D, tz);
        dest.getChunkAt(column); // load / generate the destination
        int y = dest.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        player.teleportTo(dest, tx, y + 1.0D, tz, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        player.displayClientMessage(Component.translatable("message.afteros_crt.void_out"), true);
    }

    /** A landing platform plus a scatter of huge floating black cubes (the same ones every time for a given spot). */
    private static void buildScenery(ServerLevel dim, int ax, int ay, int az) {
        if (dim.getBlockState(new BlockPos(ax, ay - 1, az)).isAir()) {
            fill(dim, ax - 4, ay - 3, az - 4, 9, 3, 9, new Random(ax * 31L + az));
        }
        Random rnd = new Random(ax * 341873128712L + az * 132897987541L);
        for (int i = 0; i < 16; i++) {
            int sx = 3 + rnd.nextInt(10);
            int sy = 3 + rnd.nextInt(10);
            int sz = 3 + rnd.nextInt(10);
            int x = ax + rnd.nextInt(161) - 80;
            int y = 40 + rnd.nextInt(130);
            int z = az + rnd.nextInt(161) - 80;
            if (Math.abs(x - ax) < 8 && Math.abs(z - az) < 8) {
                continue; // keep the landing area clear
            }
            if (dim.getBlockState(new BlockPos(x, y, z)).isAir()) {
                fill(dim, x, y, z, sx, sy, sz, rnd);
            }
        }
    }

    private static void fill(ServerLevel dim, int x0, int y0, int z0, int sx, int sy, int sz, Random rnd) {
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    boolean edge = x == 0 || y == 0 || z == 0 || x == sx - 1 || y == sy - 1 || z == sz - 1;
                    BlockState state = edge && rnd.nextInt(7) == 0 ? Blocks.AMETHYST_BLOCK.defaultBlockState()
                            : rnd.nextInt(5) == 0 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
                    dim.setBlock(new BlockPos(x0 + x, y0 + y, z0 + z), state, Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
