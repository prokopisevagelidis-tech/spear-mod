package dev.spearevolution;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public final class SpearCommand {
    private SpearCommand() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spear")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("check")
                        .then(Commands.argument("player", EntityArgument.player()).executes(SpearCommand::check)))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("tier", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(SpearTier.values()).map(SpearTier::key), b))
                                        .executes(SpearCommand::set))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player()).executes(SpearCommand::reset)))
                .then(Commands.literal("reload").executes(SpearCommand::reload)));
    }

    private static int check(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        SpearTier t = SpearManager.data().getTier(p.getUUID());
        int kills = SpearManager.data().getKills(p.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + ": " + t.displayName()
                + " (tier " + (t.index() + 1) + "/" + SpearTier.values().length + ", " + kills + " kills, damage "
                + t.totalDamage() + ")"), false);
        return t.index() + 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        String raw = StringArgumentType.getString(ctx, "tier");
        SpearTier t = SpearTier.parse(raw);
        if (t == null) {
            throw new SimpleCommandExceptionType(Component.literal("Unknown tier '" + raw + "'. Use wooden..god or 1-10.")).create();
        }
        SpearManager.data().setTier(p.getUUID(), t);
        SpearManager.syncSpear(p);
        SpearManager.data().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Set " + p.getName().getString() + " to " + t.displayName()), true);
        return t.index() + 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        SpearManager.data().reset(p.getUUID());
        GodSpearAbility.clear(p.getUUID());
        SpearManager.syncSpear(p);
        SpearManager.data().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Reset spear progression of " + p.getName().getString()), true);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        boolean ok = SpearConfig.load();
        ctx.getSource().sendSuccess(() -> Component.literal(ok ? "Spear Evolution config reloaded."
                : "Reload failed - see the server log; previous settings kept."), true);
        return ok ? 1 : 0;
    }
}
