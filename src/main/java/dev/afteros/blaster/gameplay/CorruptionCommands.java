package dev.afteros.blaster.gameplay;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.afteros.blaster.entity.CorruptionFront;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

/** /afteros corrupt <radius>, /afteros restore <radius>, /afteros info */
public final class CorruptionCommands {
    private CorruptionCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("afteros")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("corrupt").then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "radius"), false))))
                .then(Commands.literal("restore").then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
                        .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "radius"), true))))
                .then(Commands.literal("infect").then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> infect(ctx, IntegerArgumentType.getInteger(ctx, "radius"), true))))
                .then(Commands.literal("cure").then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> infect(ctx, IntegerArgumentType.getInteger(ctx, "radius"), false))))
                .then(Commands.literal("info").executes(CorruptionCommands::info)));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, int radius, boolean restore) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        Vec3 at = player.pick(64.0D, 1.0F, false).getLocation();
        boolean ok = CorruptionFront.start(level, at, radius, restore);
        if (ok) {
            source.sendSuccess(() -> Component.literal((restore ? "Restoring" : "Corrupting") + " radius " + radius + " at "
                    + (int) at.x + " " + (int) at.y + " " + (int) at.z), true);
            return 1;
        }
        source.sendFailure(Component.literal("Refused: corruption disabled, too many active fronts, or one is already there."));
        return 0;
    }

    /** Infect (or cure) every mob within radius of the caller. */
    private static int infect(CommandContext<CommandSourceStack> ctx, int radius, boolean infect) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = ctx.getSource().getLevel();
        int count = 0;
        if (infect) {
            for (net.minecraft.world.entity.Mob mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, player.getBoundingBox().inflate(radius))) {
                if (Virus.infect(mob)) {
                    count++;
                }
            }
        } else {
            count = Virus.cureAround(level, player.position(), radius);
        }
        int n = count;
        ctx.getSource().sendSuccess(() -> Component.literal((infect ? "Infected " : "Cured ") + n + " creatures"), true);
        return n;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        int fronts = level.getEntities(EntityTypeTest.forClass(CorruptionFront.class), x -> true).size();
        int blocks = CorruptionData.get(level).size();
        ctx.getSource().sendSuccess(() -> Component.literal("AfterOS corruption: " + blocks + " remembered blocks, " + fronts + " active fronts"), false);
        return 1;
    }
}
