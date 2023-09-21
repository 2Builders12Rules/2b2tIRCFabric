package org_2b12r.irc2b2t.fabric;

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
    public static Text lastDisconnectReason;

    public static String ircPrefix = "§r[§cIRC§r] ";

    public static ConcurrentLinkedQueue<Runnable> tickQueue = new ConcurrentLinkedQueue<>();
    //todo: only try to connect when internet is available
    @Override
    public void onInitializeClient() {
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
                                this.sleep(1000);
                                connection.sendPacket(new Packets.C2SLogin(1, Utils.getUsername()));
                            }

                            if (key.isReadable())
                                con.read();

                            if (key.isWritable())
                                con.write();
                            iterator.remove();
                        }
                    } catch (UnresolvedAddressException | ConnectException e) {
                        runNextTick(() -> Utils.print("Failed to connect: " + e));
                        reconnectDelayMs = -1; // disable auto reconnect
                    } catch (ConnectionException e){
                        disconnect();
                        runNextTick(() -> Utils.sendChatMessage(Text.literal(ircPrefix + "Kicked: ").append(e.message)));
                    }
                    catch (Exception e) {
                        disconnect();
                        runNextTick(() -> Utils.print(String.format("Disconnected: %s %s", e.getClass().getName(), e.getMessage())));
                    }
                    finally {
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

    private static boolean isConnected() {
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

    public static void sendChatState() {
        if (connection == null || connection.state != State.CONNECTED)
            return;
        try {
            if (MinecraftClient.getInstance().world == null) {
                connection.sendPacket(new Packets.C2SChatState(Packets.C2SChatState.States.CHAT_DISABLED));
            } else {
                connection.sendPacket(new Packets.C2SChatState(Packets.C2SChatState.States.CHAT_ENABLED));
            }
        }catch (Throwable ignored){}
    }


    public static boolean onChat(String message) {
        if (message.startsWith("/irc")) {
            sendToIRC = !sendToIRC;
            Utils.sendChatMessage(Text.of((sendToIRC ? "§aEnabled§r" : "§cDisabled§r") + " sending messages to IRC."));
            if (!isConnected() && !canAutoReconnect()) {
                reconnectDelayMs = 0; // enable auto reconnect
                Utils.sendChatMessage(Text.of("§cReconnecting to IRC server..."));
            }
            return true;
        }

        if (!sendToIRC)
            return false;

        if (isConnected() && connection.state == State.CONNECTED) {
            if (message.startsWith("/")) {
                if (!commands.contains(message.substring(1).split(" ")[0].toLowerCase()))
                    return false;
            }
            try {
                connection.sendPacket(new Packets.C2SChat(message));
            } catch (Throwable e) {
                Utils.sendChatMessage(Text.of("§cFailed to send message to IRC server: " + e.getMessage()));
            }
        } else {
            Utils.sendChatMessage(Text.of(String.format("§cNot connected to IRC server. %s", canAutoReconnect() ? "Reconnecting..." : "Use /irc command to reconnect to IRC.")));
            if(!canAutoReconnect() && lastDisconnectReason != null)
                Utils.sendChatMessage(Text.literal("Last Disconnect Reason: ").append(lastDisconnectReason));
        }

        Utils.getMC().inGameHud.getChatHud().addToMessageHistory(message);

        return true;
    }
}
