package org_2b12r.irc2b2t.net;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;

import java.util.function.Function;

public enum State {
    AUTHENTICATING,
    CONNECTED;

    private final Int2ObjectArrayMap<Function<DataBuffer, Packet>> id2Packet = new Int2ObjectArrayMap<>();
    private final Object2IntArrayMap<Class<?>> packet2Id = new Object2IntArrayMap<>();

    private int nextId = 0;

    public void registerPacket(Class<?> packetClass, Function<DataBuffer, Packet> reader) {
        id2Packet.put(nextId, reader);
        packet2Id.put(packetClass, nextId);
        ++nextId;
    }

    public Packet readPacket(DataBuffer buffer) {
        final int id = buffer.readByte();
        final Function<DataBuffer, Packet> reader = id2Packet.get(id);
        if (reader != null)
            return reader.apply(buffer);

        return null;
    }

    public byte getPacketId(Class<?> packet) {
        return (byte) packet2Id.getInt(packet);
    }
}
