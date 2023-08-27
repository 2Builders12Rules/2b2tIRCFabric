package org_2b12r.irc2b2t.net;

import java.util.List;
import java.util.UUID;

public class Packets {
    public static void init() {
        State.AUTHENTICATING.registerPacket(C2SLogin.class, C2SLogin::new);
        State.AUTHENTICATING.registerPacket(S2CEncryptionRequest.class, S2CEncryptionRequest::new);
        State.AUTHENTICATING.registerPacket(C2SEncryptionResponse.class, C2SEncryptionResponse::new);
        State.AUTHENTICATING.registerPacket(S2CConnected.class, S2CConnected::new);
        State.AUTHENTICATING.registerPacket(Disconnect.class, Disconnect::new);

        State.CONNECTED.registerPacket(Disconnect.class, Disconnect::new);
        State.CONNECTED.registerPacket(KeepAlive.class, KeepAlive::new);
        State.CONNECTED.registerPacket(S2CCommands.class, S2CCommands::new);
        State.CONNECTED.registerPacket(S2CChat.class, S2CChat::new);
        State.CONNECTED.registerPacket(C2SChat.class, C2SChat::new);
        State.CONNECTED.registerPacket(C2SChatState.class, C2SChatState::new);
        State.CONNECTED.registerPacket(S2CPlayerMessage.class, S2CPlayerMessage::new);
    }

    public static class C2SLogin implements Packet {
        public int version;
        public String username;

        public C2SLogin(DataBuffer buf) {
            this.version = buf.readShort();
            this.username = buf.readString(16);
        }

        public C2SLogin(int version, String username) {
            this.version = version;
            this.username = username;
        }

        @Override
        public void write(DataBuffer buf) {
            buf.writeShort((short) version);
            buf.writeString(this.username);
        }

        @Override
        public void handle(Connection con) {
            con.getHandler().handle(this);
        }
    }

    public static class S2CEncryptionRequest implements Packet {
        public byte[] publicKey;

        public S2CEncryptionRequest(DataBuffer buf) {
            this.publicKey = buf.readByteArrayLimited(1024);
        }

        public S2CEncryptionRequest(byte[] publicKey) {
            this.publicKey = publicKey;
        }

        @Override
        public void write(DataBuffer buf) {
            buf.writeByteArray(publicKey);
        }

        @Override
        public void handle(Connection con) {
            con.getHandler().handle(this);
        }
    }

    public static class C2SEncryptionResponse implements Packet {
        public byte[] sharedKey;

        public C2SEncryptionResponse(DataBuffer buf) {
            this.sharedKey = buf.readByteArrayLimited(1024);
        }

        public C2SEncryptionResponse(byte[] sharedKey) {
            this.sharedKey = sharedKey;
        }

        @Override
        public void write(DataBuffer buf) {
            buf.writeByteArray(sharedKey);
        }

        @Override
        public void handle(Connection con) {
            con.getHandler().handle(this);
        }
    }

    public static class S2CConnected implements Packet {
        public S2CConnected(DataBuffer buf) {}

        public S2CConnected() {}

        @Override
        public void write(DataBuffer buf) {}

        @Override
        public void handle(Connection con) {
            con.getHandler().handle(this);
        }
    }

    public static class Disconnect implements Packet {
        public int clientReconnectTimer;
        public String reason;

        public Disconnect(DataBuffer buf) {
            this.clientReconnectTimer = buf.readVarInt();
            this.reason = buf.readString(32767);
        }

        public Disconnect(String reason, int clientReconnectTimer) {
            this.clientReconnectTimer = clientReconnectTimer;
            this.reason = reason;
        }

        @Override
        public void write(DataBuffer buf) {
            buf.writeVarInt(clientReconnectTimer);
            buf.writeString(reason);
        }

        @Override
        public void handle(Connection con) {
            con.getHandler().handle(this);
        }
    }


    public static class S2CCommands implements Packet {
        public List<String> commands;

        public S2CCommands(DataBuffer buf) {
            this.commands = buf.readList(buf::readString);
        }

        public S2CCommands(List<String> commands) {
            this.commands = commands;
        }

        @Override
        public void write(DataBuffer buf) {
            buf.writeList(commands, buf::writeString);
        }

        @Override
        public void handle(Connection con) {
            con.getHandler().handle(this);
        }
    }

    public static class S2CChat implements Packet {
        public byte type;
        public String json;

        public S2CChat(DataBuffer buf) {
            this.type = buf.readByte();
            this.json = buf.readString(32767);
        }

        public S2CChat(byte type, String json) {
            this.type = type;
            this.json = json;
        }

        @Override
        public void write(DataBuffer buf) {
            buf.writeByte(type);
            buf.writeString(json);
        }

        @Override
        public void handle(Connection con) {
            con.getHandler().handle(this);
        }
    }

    public static class C2SChat implements Packet {
        public String message;

        public C2SChat(DataBuffer buf) {
            this.message = buf.readString(4096);
        }

        public C2SChat(String message) {
            this.message = message;
        }

        @Override
        public void write(DataBuffer buf) {
            buf.writeString(message);
        }

        @Override
        public void handle(Connection con) {
            con.getHandler().handle(this);
        }
    }

    public static class S2CPlayerMessage implements Packet {
        public UUID sender;
        public String message;

        public S2CPlayerMessage(DataBuffer buf) {
            this.sender = buf.readUUID();
            this.message = buf.readString(32767);
        }

        public S2CPlayerMessage(UUID sender, String message) {
            this.sender = sender;
            this.message = message;
        }

        @Override
        public void write(DataBuffer buf) {
            buf.writeUUID(sender);
            buf.writeString(message);
        }

        @Override
        public void handle(Connection con) {
            con.getHandler().handle(this);
        }
    }

    public static class C2SChatState implements Packet {
        public States state;
        public C2SChatState(DataBuffer buf) {
            this.state = States.values()[buf.readVarInt()];
        }

        public C2SChatState(States state) {
            this.state = state;
        }

        @Override
        public void write(DataBuffer buf) {
            buf.writeVarInt(this.state.ordinal());
        }

        @Override
        public void handle(Connection con) {
            con.getHandler().handle(this);
        }

        public enum States {
            CHAT_ENABLED,
            CHAT_DISABLED,
        }
    }

    public static class KeepAlive implements Packet {
        public long key;

        public KeepAlive(DataBuffer buf) {
            this.key = buf.readLong();
        }

        public KeepAlive(long key) {
            this.key = key;
        }

        @Override
        public void write(DataBuffer buf) {
            buf.writeLong(key);
        }

        @Override
        public void handle(Connection con) {
            con.getHandler().handle(this);
        }
    }
}
