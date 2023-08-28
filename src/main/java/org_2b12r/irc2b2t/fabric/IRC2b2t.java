package org_2b12r.irc2b2t.fabric;

import org_2b12r.irc2b2t.net.Connection;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IRC2b2t implements ClientModInitializer {
    private static boolean sendToIRC = false;

    private static Selector selector;
    private static Connection connection;
    public static Set<String> commands = new HashSet<>();

    public static int reconnectDelayMs = 5000;

    public static ConcurrentLinkedQueue<Runnable> tickQueue = new ConcurrentLinkedQueue<>();
    //todo: only try to connect when internet is available
    @Override
    public void onInitializeClient() {
        Packets.init();
        new Thread(() -> {
            try {
                selector = Selector.open();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            while (true) {
                try {
                    if (!isConnected()) {
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
                            Thread.sleep(1000);
                            connection.sendPacket(new Packets.C2SLogin(0, Utils.getUsername()));
                        }

                        if (key.isReadable())
                            con.read();

                        if (key.isWritable())
                            con.write();
                        iterator.remove();
                    }
                } catch (UnresolvedAddressException | ConnectException e) {
                    reconnectDelayMs = 30000;

                    try {
                        Thread.sleep(reconnectDelayMs);
                    } catch (InterruptedException ignored) {}
                } catch (Exception e) {
                    if (connection != null)
                        connection.close();

                    runNextTick(() -> Utils.print("Disconnected: " + e));

                    try {
                        Thread.sleep(reconnectDelayMs);
                    } catch (InterruptedException ignored) {}
                }
            }
        }, "IRC-Thread").start();
    }

    private static boolean isConnected() {
        return connection != null && connection.isOpen();
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

    public static void sendChatState() {
        if (connection == null || connection.state != State.CONNECTED)
            return;
        if (MinecraftClient.getInstance().world == null) {
            connection.sendPacket(new Packets.C2SChatState(Packets.C2SChatState.States.CHAT_DISABLED));
        } else {
            connection.sendPacket(new Packets.C2SChatState(Packets.C2SChatState.States.CHAT_ENABLED));
        }
    }


    public static boolean onChat(String message) {
        if (message.startsWith("/irc")) {
            sendToIRC = !sendToIRC;
            Utils.sendChatMessage(Text.of((sendToIRC ? "§aEnabled§r" : "§cDisabled§r") + " sending messages to IRC."));
            return true;
        }

        if (!sendToIRC)
            return false;

        if (connection == null || connection.state != State.CONNECTED) {
            Utils.sendChatMessage(Text.of("§cNot connected to IRC server."));
        } else {
            if (message.startsWith("/")) {
                if (!commands.contains(message.substring(1).split(" ")[0].toLowerCase()))
                    return false;
            }
            try {
                connection.sendPacket(new Packets.C2SChat(message));
            } catch (Exception e) {
                Utils.sendChatMessage(Text.of("§cFailed to send message to IRC server: " + e.getMessage()));
            }
        }

        Utils.getMC().inGameHud.getChatHud().addToMessageHistory(message);

        return true;
    }
}
