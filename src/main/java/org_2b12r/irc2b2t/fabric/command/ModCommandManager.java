package org_2b12r.irc2b2t.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandSource;
import org_2b12r.irc2b2t.fabric.Clearable;
import org_2b12r.irc2b2t.fabric.IRC2b2t;

import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static org_2b12r.irc2b2t.Utils.getMC;
import static org_2b12r.irc2b2t.Utils.print;

public class ModCommandManager {
    private static final CommandDispatcher<CommandSource> ircDispatcher = new CommandDispatcher<>();

    public static CommandDispatcher<CommandSource> addModCommands(CommandDispatcher<CommandSource> serverDispatcher) {
        MergedCommandDispatcher dispatcher = new MergedCommandDispatcher(serverDispatcher, ircDispatcher);
        IRCCommand.register(dispatcher);
        return dispatcher;
    }

    public static void registerCommands(List<String> commands) {
        ((Clearable) ircDispatcher.getRoot()).clear();
        for (final String command : commands)
            ircDispatcher.register(command(command).then(argument("args", greedyString())));
    }

    public static LiteralArgumentBuilder<CommandSource> command(String name) {
        return new LiteralArgumentBuilder<>(name) {
            @Override
            public LiteralCommandNode<CommandSource> build() {
                final ModCommand result = new ModCommand(getLiteral(), getCommand(), getRequirement(), getRedirect(), getRedirectModifier(), isFork());

                for (final CommandNode<CommandSource> argument : getArguments()) {
                    result.addChild(argument);
                }

                return result;
            }
        };
    }

    public static LiteralArgumentBuilder<CommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static <T> RequiredArgumentBuilder<CommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    /**
     * Determines what to do with the command.
     * <h4>Rules</h4>
     * <ul>
     *     <li>If the command starts with the IRC prefix it will never be sent to the server and will be sent to IRC even if the command is invalid.</li>
     *     <li>If the command does not start with the IRC prefix and IRC is enabled, the command will be sent to IRC if it is a valid IRC command.</li>
     *     <li>If the command does not start with the IRC prefix and IRC is disabled, the command will always be sent to the server.</li>
     *     <li>If the command is <code>/irc</code> it will always be handled by the client and will never be sent to the server or IRC.</li>
     * </ul>
     *
     * @param reader The command reader.
     * @return <code>true</code> if the command should not be sent to the server.
     */
    public static boolean handleCommand(StringReader reader) {
        final int commandStart = reader.getCursor();
        final ClientPlayNetworkHandler handler = getMC().getNetworkHandler();
        if (handler == null)
            return true;

        if (!(handler.getCommandDispatcher() instanceof MergedCommandDispatcher commandDispatcher))
            return false;

        final MergedCommandDispatcher.CommandPurpose commandPurpose = MergedCommandDispatcher.getCommandPurpose(reader);
        final ParseResults<CommandSource> parsedCommand = commandDispatcher.parse(commandPurpose, reader, handler.getCommandSource());

        if (parsedCommand.getContext().getNodes().isEmpty()) { // No command was found while parsing.
            if (commandPurpose == MergedCommandDispatcher.CommandPurpose.ONLY_IRC) {
                sendCommandToIRC(reader, commandStart); // Anything starting with the IRC prefix should never be sent to the server
                return true;
            } else {
                return false; // Forward to server if this isn't exclusively meant for IRC.
            }
        }

        final CommandNode<CommandSource> firstNode = parsedCommand.getContext().getNodes().get(0).getNode();
        final boolean isSlashIRC = firstNode instanceof LiteralCommandNode<CommandSource> literal && literal.getLiteral().equals("irc");
        if (firstNode instanceof ModCommand || isSlashIRC) {
            try {
                if (parsedCommand.getContext().getCommand() == null) // Command exists but is not handled by client. Send only to IRC server.
                    sendCommandToIRC(reader, commandStart);
                else
                    commandDispatcher.execute(parsedCommand); // Command is handled by the client
            } catch (CommandSyntaxException e) {
                print(e.getMessage());
            }

            return true;
        }

        return false;
    }

    private static void sendCommandToIRC(StringReader reader, int start) {
        reader.setCursor(start);
        IRC2b2t.sendToIRC("/" + reader.getRemaining());
    }
}
