package org_2b12r.irc2b2t.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import org_2b12r.irc2b2t.fabric.Config;
import org_2b12r.irc2b2t.fabric.IRC2b2t;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static org_2b12r.irc2b2t.Utils.print;

public class IRCCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
            ModCommandManager.literal("irc")
                .executes(ctx -> {
                    IRC2b2t.sendToIRC = !IRC2b2t.sendToIRC;
                    print((IRC2b2t.sendToIRC ? "§aEnabled§r" : "§cDisabled§r") + " sending messages to IRC.");
                    if (!IRC2b2t.isConnected() && !IRC2b2t.canAutoReconnect()) {
                        IRC2b2t.reconnectDelayMs = 0; // Enable auto-reconnect
                        print("§cReconnecting to server...");
                    }

                    return 1;
                })
                .then(
                    ModCommandManager.literal("connect")
                        .executes(ctx -> {
                            if (IRC2b2t.isConnected())
                                print("You are already connected to IRC.");
                            else {
                                IRC2b2t.reconnectDelayMs = 0;
                                print("Connecting to IRC");
                            }

                            return 1;
                        })
                ).then(
                    ModCommandManager.literal("disconnect")
                        .executes(ctx -> {
                            if (!IRC2b2t.isConnected())
                                print("You are not connected to IRC.");
                            else {
                                IRC2b2t.reconnectDelayMs = -1;
                                IRC2b2t.connection.close();
                                print("Disconnected from IRC.");
                            }

                            return 1;
                        })
                ).then(
                    ModCommandManager.literal("reconnect")
                        .executes(ctx -> {
                            IRC2b2t.reconnectDelayMs = 0;
                            IRC2b2t.connection.close();
                            print("Reconnecting to IRC");

                            return 1;
                        })
                ).then(ModCommandManager.literal("prefix")
                    .then(ModCommandManager.argument("prefix", greedyString())
                        .executes(ctx -> {
                            Config.sendToIRCPrefix = getString(ctx, "prefix").charAt(0);
                            print("Set IRC prefix to '" + Config.sendToIRCPrefix + "'.");

                            return 1;
                        })
                    )
                )
        );
    }
}
