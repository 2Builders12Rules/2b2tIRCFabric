package org_2b12r.irc2b2t.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org_2b12r.irc2b2t.Utils;
import org_2b12r.irc2b2t.net.Connection;
import org_2b12r.irc2b2t.net.ConnectionException;
import org_2b12r.irc2b2t.net.Packets;
import org_2b12r.irc2b2t.net.State;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org_2b12r.irc2b2t.Utils.*;

public class IRC2b2t implements ClientModInitializer {
    public static boolean sendToIRC = false;

    private static Selector selector;
    public static Connection connection;
    public static int reconnectDelayMs = 5000;
    public static Text lastDisconnectReason;

    public static ConcurrentLinkedQueue<Runnable> tickQueue = new ConcurrentLinkedQueue<>();

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
//                            channel.connect(new InetSocketAddress("irc.2b12r.org", 7533));
                            channel.connect(new InetSocketAddress("localhost", 7533));
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
                                runNextTick(() -> print("Connecting..."));
                                con.finishConnect();
                                connection.sendPacket(new Packets.C2SLogin(1, getUsername(), getUUID()));
                            }

                            if (key.isReadable())
                                con.read();

                            if (key.isWritable())
                                con.write();
                            iterator.remove();
                        }
                    } catch (UnresolvedAddressException | ConnectException e) {
                        runNextTick(() -> print("Failed to connect: " + e));
                        reconnectDelayMs = -1; // Disable auto-reconnect
                    } catch (ConnectionException e) {
                        disconnect();
                        runNextTick(() -> sendChatMessage(Text.literal("Kicked: ").append(e.message)));
                    } catch (Exception e) {
                        disconnect();
                        runNextTick(() -> print(String.format("Disconnected: %s %s", e.getClass().getName(), e.getMessage())));
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
                if (connection != null) {
                    connection.close();
                    connection = null;
                }
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
        while ((runnable = tickQueue.poll()) != null)
            runnable.run();
    }

    public static String lastAccessToken = "";

    public static void onWorldChange() {
        if (!isConnected())
            return;

        final String accessToken = getMC().getSession().getAccessToken();
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
        if (!message.isEmpty() && message.charAt(0) == Config.sendToIRCPrefix) {
            sendToIRC(message.substring(1));
            return true;
        }

        if (!sendToIRC) // Sending to IRC is disabled. Allow message to be sent to server.
            return false;

        sendToIRC(message);
        return true;
    }

    /**
     * Tries to send a message or command to the IRC server.
     * @param message The message to send.
     */
    public static void sendToIRC(String message) {
        if (!isConnected()) {
            print(String.format("§cNot connected to server. %s", canAutoReconnect() ? "Reconnecting..." : "Use /irc command to reconnect to IRC."));
            if (lastDisconnectReason != null)
                sendChatMessage(Text.literal("Last disconnect reason: ").append(lastDisconnectReason));

            sendToIRC = false;
            Utils.sendChatMessage(Text.of("§cDisabled§r sending messages to IRC because you are not connected."));
            return;
        }

        if (connection.state != State.CONNECTED) {
            print("§cNot connected yet.");
        }

        try {
            connection.sendPacket(new Packets.C2SChat(message));
        } catch (Throwable e) {
            print("§cFailed to send message: " + e.getMessage());
        }
    }
}
