package org_2b12r.irc2b2t.fabric;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org_2b12r.irc2b2t.net.Connection;
import org_2b12r.irc2b2t.net.ConnectionException;
import org_2b12r.irc2b2t.net.Packets;
import org_2b12r.irc2b2t.net.State;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IRC2b2t implements ClientModInitializer {
    public static boolean sendToIRC = false;

    private static Selector selector;
    private static Connection connection;
    public static Set<String> commands = new HashSet<>();

    public static int reconnectDelayMs = 5000;
    public static Text lastDisconnectReason;

    public static String ircPrefix = "§r[§cIRC§r] ";

    public static ConcurrentLinkedQueue<Runnable> tickQueue = new ConcurrentLinkedQueue<>();

    public static void addCompletions(String input, @Nullable CompletableFuture<Suggestions> pendingSuggestions) {
        final StringReader reader = new StringReader(input);
        if (!isConnected())
            return;

        boolean hasPrefix = reader.canRead() && reader.peek() == Config.sendToIRCPrefix;
        if (!hasPrefix && !sendToIRC)
            return;

        if (hasPrefix)
            reader.skip();

        if (reader.canRead() && reader.peek() == '/')
            reader.skip();
        else
            return;

        final int start = reader.getCursor();
        final String cmd = reader.readUnquotedString();
        final int end = reader.getCursor() - 1;

        if (reader.getRemainingLength() > 0)
            return;

        ArrayList<Suggestion> newSuggestions = new ArrayList<>();
        for (String command : commands) {
            if (command.startsWith(cmd))
                newSuggestions.add(new Suggestion(new StringRange(start, end), command));
        }

        if (pendingSuggestions != null && pendingSuggestions.isDone()) {
            final Suggestions suggestions = pendingSuggestions.getNow(null);
            if (suggestions != null)
                suggestions.getList().addAll(newSuggestions);
        }
    }

    public static OrderedText highlightText(String original, int firstCharacterIndex) {
        final StringReader reader = new StringReader(original);
        if (!isConnected())
            return null;

        boolean hasPrefix = reader.canRead() && reader.peek() == Config.sendToIRCPrefix;
        if (!hasPrefix && !sendToIRC)
            return null;

        if (hasPrefix)
            reader.skip();

        if (reader.canRead() && reader.peek() == '/')
            reader.skip();
        else
            return null;

        final int start = reader.getCursor();
        final String cmd = reader.readUnquotedString();
        reader.setCursor(start);

        if (commands.contains(cmd)) {
            final ArrayList<OrderedText> list = Lists.newArrayList();
            list.add(OrderedText.styledForwardsVisitedString(original.substring(0, start), Style.EMPTY.withColor(Formatting.GRAY)));
            if (reader.canRead())
                list.add(OrderedText.styledForwardsVisitedString(reader.getRemaining(), Style.EMPTY.withColor(Formatting.WHITE)));
            return OrderedText.concat(list);
        }
        else
            return null;
    }

    //todo: only try to connect when internet is available
    @Override
    public void onInitializeClient() {
        Config.load();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Config.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        Packets.init();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    selector = Selector.open();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                while (true) {
                    try {
                        if (!isConnected()) {
                            if (!canAutoReconnect()) {
                                this.sleep(1000);
                                continue;
                            }
                            final FabricPacketHandler handler = new FabricPacketHandler();
                            final SocketChannel channel = SocketChannel.open();
                            connection = new Connection(channel, selector, handler);
                            handler.con = connection;
                            channel.connect(new InetSocketAddress("irc.2b12r.org", 7533));
                            reconnectDelayMs = 5000;
                        }

                        selector.select();

                        final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                        while (iterator.hasNext()) {
                            final SelectionKey key = iterator.next();
                            final Connection con = (Connection) key.attachment();
                            if (con == null)
                                continue;

                            if (key.isConnectable()) {
                                runNextTick(() -> Utils.print("Connecting..."));
                                con.finishConnect();
                                connection.sendPacket(new Packets.C2SLogin(1, Utils.getUsername(), Utils.getUUID()));
                            }

                            if (key.isReadable())
                                con.read();

                            if (key.isWritable())
                                con.write();
                            iterator.remove();
                        }
                    } catch (UnresolvedAddressException | ConnectException e) {
                        runNextTick(() -> Utils.print("Failed to connect: " + e));
                        reconnectDelayMs = -1; // Disable auto-reconnect
                    } catch (ConnectionException e) {
                        disconnect();
                        runNextTick(() -> Utils.sendChatMessage(Text.literal(ircPrefix + "Kicked: ").append(e.message)));
                    } catch (Exception e) {
                        disconnect();
                        runNextTick(() -> Utils.print(String.format("Disconnected: %s %s", e.getClass().getName(), e.getMessage())));
                    } finally {
                        if (!isConnected() && canAutoReconnect())
                            this.sleep(reconnectDelayMs);
                    }
                }
            }

            public void sleep(long delay) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
            }

            public void disconnect(){
                if (connection != null)
                    connection.close();
            }
        }, "IRC-Thread").start();
    }

    public static boolean isConnected() {
        return connection != null && connection.isOpen();
    }

    public static boolean canAutoReconnect() {
        return reconnectDelayMs >= 0;
    }

    public static void runNextTick(Runnable runnable) {
        tickQueue.add(runnable);
    }

    public static void onTick() {
        Runnable runnable;
        while ((runnable = tickQueue.poll()) != null) {
            runnable.run();
        }
    }

    public static String lastAccessToken = "";

    public static void onWorldChange() {
        if (!isConnected())
            return;

        final String accessToken = Utils.getMC().getSession().getAccessToken();
        if (!lastAccessToken.equals(accessToken)) { // Detects account change or re-login
            lastAccessToken = accessToken;
            connection.close();
            reconnectDelayMs = 0; // Enable auto-reconnect in case it was disabled due to invalid session
            return;
        }

        if (connection.state != State.CONNECTED)
            return;

        try {
            if (MinecraftClient.getInstance().world == null) {
                connection.sendPacket(new Packets.C2SChatState(Packets.C2SChatState.States.CHAT_DISABLED));
            } else {
                connection.sendPacket(new Packets.C2SChatState(Packets.C2SChatState.States.CHAT_ENABLED));
            }
        } catch (Throwable ignored) {}
    }


    public static boolean onChat(String message) {
        if (message.startsWith("/irc")) {
            final String[] split = message.split(" ");
            if (split.length == 1) {
                sendToIRC = !sendToIRC;
                Utils.print((sendToIRC ? "§aEnabled§r" : "§cDisabled§r") + " sending messages to IRC.");
                if (!isConnected() && !canAutoReconnect()) {
                    reconnectDelayMs = 0; // Enable auto-reconnect
                    Utils.print("§cReconnecting to server...");
                }
            } else if (split.length == 2) {
                Config.sendToIRCPrefix = split[1].charAt(0);
                Utils.print("Set IRC prefix to: " + Config.sendToIRCPrefix);
            }

            return true;
        } else if (!message.isEmpty() && message.charAt(0) == Config.sendToIRCPrefix) {
            sendToIRC(message.substring(1));
            return true;
        }

        if (!sendToIRC) // Sending to IRC is disabled. Allow message to be sent to server.
            return false;

        if (message.startsWith("/")) {
            if (!commands.contains(message.substring(1).split(" ")[0].toLowerCase()))
                return false;
        }

        sendToIRC(message);
        return true;
    }

    /**
     * Tries to send a message or command to the IRC server.
     * @param message The message to send.
     */
    private static void sendToIRC(String message) {
        if (!isConnected()) {
            Utils.print(String.format("§cNot connected to server. %s", canAutoReconnect() ? "Reconnecting..." : "Use /irc command to reconnect to IRC."));
            if (lastDisconnectReason != null)
                Utils.print("Last disconnect reason: " + lastDisconnectReason);

            sendToIRC = false;
        }

        if (connection.state != State.CONNECTED) {
            Utils.print("§cNot connected yet.");
        }

        try {
            connection.sendPacket(new Packets.C2SChat(message));
        } catch (Throwable e) {
            Utils.print("§cFailed to send message: " + e.getMessage());
        }
    }
}
