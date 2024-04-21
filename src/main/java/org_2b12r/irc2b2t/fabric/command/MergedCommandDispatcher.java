package org_2b12r.irc2b2t.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import net.minecraft.command.CommandSource;
import org_2b12r.irc2b2t.fabric.Config;
import org_2b12r.irc2b2t.fabric.IRC2b2t;
import org_2b12r.irc2b2t.mixin.CommandDispatcherAccessor;

public class MergedCommandDispatcher extends CommandDispatcher<CommandSource> {
    public final CommandDispatcher<CommandSource> serverDispatcher;
    public final CommandDispatcher<CommandSource> ircDispatcher;

    public MergedCommandDispatcher(CommandDispatcher<CommandSource> serverDispatcher, CommandDispatcher<CommandSource> ircDispatcher) {
        super();
        this.serverDispatcher = serverDispatcher;
        this.ircDispatcher = ircDispatcher;
    }

    public ParseResults<CommandSource> parse(final CommandPurpose purpose, final StringReader command, final CommandSource source) {
        final CommandContextBuilder<CommandSource> context = new CommandContextBuilder<>(this, source, getRoot(), command.getCursor());
        return switch (purpose) {
            case ONLY_NORMAL -> parseNodesNormal(command, context);
            case ONLY_IRC -> parseNodes(ircDispatcher, command, context);
            case BOTH -> {
                final ParseResults<CommandSource> ircResults = parseNodes(ircDispatcher, command, context);
                if (!ircResults.getContext().getNodes().isEmpty())
                    yield ircResults;

                yield parseNodesNormal(command, context);
            }
        };
    }

    public ParseResults<CommandSource> parse(final StringReader command, final CommandSource source) {
        return parse(getCommandPurpose(command), command, source);
    }

    private ParseResults<CommandSource> parseNodesNormal(StringReader command, CommandContextBuilder<CommandSource> context) {
        final ParseResults<CommandSource> clientResults = parseNodes(this, command, context);
        if (!clientResults.getContext().getNodes().isEmpty())
            return clientResults;

        return parseNodes(serverDispatcher, command, context);
    }

    public static CommandPurpose getCommandPurpose(StringReader command) {
        final CommandPurpose purpose;
        if (command.getTotalLength() > 0 && command.getString().charAt(0) == Config.sendToIRCPrefix)
            purpose = CommandPurpose.ONLY_IRC; // Only IRC commands if prefixed
        else if (IRC2b2t.sendToIRC)
            purpose = CommandPurpose.BOTH; // All commands if IRC enabled but command isn't prefixed
        else
            purpose = CommandPurpose.ONLY_NORMAL; // Only normal commands if send to IRC is disabled and command isn't prefixed

        return purpose;
    }

    private static ParseResults<CommandSource> parseNodes(CommandDispatcher<CommandSource> dispatcher, StringReader command, CommandContextBuilder<CommandSource> context) {
        return ((CommandDispatcherAccessor) dispatcher).invokeParseNodes(dispatcher.getRoot(), command, context);
    }

    public enum CommandPurpose {
        ONLY_NORMAL,
        ONLY_IRC,
        BOTH
    }
}
