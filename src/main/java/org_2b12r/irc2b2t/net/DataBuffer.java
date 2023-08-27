package org_2b12r.irc2b2t.net;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DataBuffer {
    private byte[] buffer;
    public int writePos;
    public int readPos;

    public DataBuffer(int size) {
        this.buffer = new byte[size];
        this.writePos = 0;
    }

    public DataBuffer(byte[] bytes) {
        this.buffer = bytes;
        this.writePos = bytes.length;
    }

    public void writeByte(int b) {
        this.ensureCapacity(this.writePos + 1);
        this.buffer[this.writePos++] = (byte) b;
    }

    public byte readByte() {
        return this.readPos < this.writePos ? this.buffer[this.readPos++] : 0;
    }

    public void writeUnsignedByte(int b) {
        this.writeByte(b);
    }

    public int readUnsignedByte() {
        return this.readByte() & 0xFF;
    }

    public void writeBytes(byte[] b) {
        this.ensureCapacity(this.writePos + b.length);
        System.arraycopy(b, 0, this.buffer, this.writePos, b.length);
        this.writePos += b.length;
    }

    public void writeBytes(byte[] b, int length) {
        final int newLength = Math.min(b.length, length);
        this.ensureCapacity(this.writePos + newLength);
        System.arraycopy(b, 0, this.buffer, this.writePos, newLength);
        this.writePos += newLength;
    }

    public int readBytes(byte[] buffer) {
        final int read = Math.min(this.available(), buffer.length);
        System.arraycopy(this.buffer, this.readPos, buffer, 0, read);

        this.readPos += read;
        return read;
    }

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public byte[] readBytes(int limit) {
        if (this.readPos >= this.writePos || limit == 0)
            return EMPTY_BYTE_ARRAY;

        final byte[] buffer = new byte[limit];
        this.readBytes(buffer);

        return buffer;
    }

    public byte[] readBytes() {
        return this.readBytes(this.available());
    }

    public void writeString(String string) {
        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);

        this.writeVarInt(bytes.length);
        this.writeBytes(bytes);
    }

    // TODO: Remove and replace code that uses this with writeList
    public void writeStrings(String[] input) {
        for (int i = 0; i < input.length; i++)
            writeString(input[i]);
    }

    public String readString() {
        return readString(32767);
    }

    public String readString(int limit) {
        final int length = this.readVarInt();
        if (length > limit || length < 0)
            throw new PacketException("Invalid string length: %d. Max Length: %d.", length, limit);
        return new String(this.readBytes(length));
    }

    // TODO: Remove and replace code that uses this with readList
    public String[] readStrings(int length) {
        final String[] strings = new String[length];
        for (int i = 0; i < length; i++)
            strings[i] = this.readString();
        return strings;
    }

    public void writeShort(short input) {
        this.ensureCapacity(this.writePos + 2);
        this.buffer[this.writePos++] = (byte)(input >>> 8);
        this.buffer[this.writePos++] = (byte)(input);
    }

    public short readShort() {
        return (short) (((this.readByte() & 0xFF) << 8) | (this.readByte() & 0xFF));
    }

    public void writeInt(int input) {
        this.ensureCapacity(this.writePos + 4);
        this.buffer[this.writePos++] = (byte)(input >>> 24);
        this.buffer[this.writePos++] = (byte)(input >>> 16);
        this.buffer[this.writePos++] = (byte)(input >>> 8);
        this.buffer[this.writePos++] = (byte)(input);
    }

    public int readInt() {
        return ((this.readByte() & 0xFF) << 24) | ((this.readByte() & 0xFF) << 16) | ((this.readByte() & 0xFF) << 8) | (this.readByte() & 0xFF);
    }

    public void writeLong(long input) {
        this.ensureCapacity(this.writePos + 8);
        this.buffer[this.writePos++] = (byte)(input >>> 56);
        this.buffer[this.writePos++] = (byte)(input >>> 48);
        this.buffer[this.writePos++] = (byte)(input >>> 40);
        this.buffer[this.writePos++] = (byte)(input >>> 32);
        this.buffer[this.writePos++] = (byte)(input >>> 24);
        this.buffer[this.writePos++] = (byte)(input >>> 16);
        this.buffer[this.writePos++] = (byte)(input >>> 8);
        this.buffer[this.writePos++] = (byte)(input);
    }

    public void writeLongs(long[] input) {
        for (int i = 0; i < input.length; i++)
            writeLong(input[i]);
    }

    public long readLong() {
        return ((long) (this.readByte() & 0xFF) << 56) | ((long) (this.readByte() & 0xFF) << 48) | ((long) (this.readByte() & 0xFF) << 40) | ((long) (this.readByte() & 0xFF) << 32) | ((long) (this.readByte() & 0xFF) << 24) | ((long) (this.readByte() & 0xFF) << 16) | ((long) (this.readByte() & 0xFF) << 8) | (long) (this.readByte() & 0xFF);
    }

    public long[] readLongs(int length) {
        final long[] longs = new long[length];
        for (int i = 0; i < length; i++)
            longs[i] = this.readLong();
        return longs;
    }

    public void writeFloat(float input) {
        this.writeInt(Float.floatToRawIntBits(input));
    }

    public float readFloat() {
        return Float.intBitsToFloat(this.readInt());
    }

    public void writeDouble(double input) {
        this.writeLong(Double.doubleToRawLongBits(input));
    }

    public double readDouble() {
        return Double.longBitsToDouble(this.readLong());
    }

    public void writeBoolean(boolean b) {
        this.writeByte(b ? 1 : 0);
    }

    public boolean readBoolean() {
        return this.readByte() == 1;
    }

    public void writeUUID(UUID uuid) {
        this.writeLong(uuid.getMostSignificantBits());
        this.writeLong(uuid.getLeastSignificantBits());
    }

    public UUID readUUID() {
        return new UUID(this.readLong(), this.readLong());
    }

    public void writeVarInt(int input) {
        do {
            byte temp = (byte) (input & 0b01111111);
            input >>>= 7;
            if (input != 0) {
                temp |= 0b10000000;
            }
            this.writeByte(temp);
        }
        while (input != 0);
        /*for (int i = 0; i < 5; i++) {
            final int currentByte = input & 0b1111111;
            final boolean hasNext = (input >>>= 7) > 0;

            this.writeByte(hasNext ? 0b10000000 | currentByte : currentByte);

            if (!hasNext)
                break;
        }*/
    }

    public void writeVarInts(int[] input) {
        for (int i = 0; i < input.length; i++)
            writeVarInt(input[i]);
    }

    public int readVarInt() {
        int out = 0;
        for (int i = 0; i < 5; i++) {
            final byte read = this.readByte();

            out |= ((read & 0b01111111) << (7 * i)); // Add 7 bits to output VarInt

            if ((read & 0b10000000) == 0) // Break if first bit is 0 (Next byte is not part of VarInt)
                break;
        }

        return out;
    }

    public int[] readVarInts(int length) {
        final int[] ints = new int[length];
        for (int i = 0; i < length; i++)
            ints[i] = readVarInt();
        return ints;
    }

    public void writeVarLong(long input) {
        do {
            byte temp = (byte) (input & 0b01111111);
            input >>>= 7;
            if (input != 0) {
                temp |= 0b10000000;
            }
            this.writeByte(temp);
        }
        while (input != 0);
    }

    public void writeVarLongs(long[] input) {
        for (int i = 0; i < input.length; i++)
            writeVarLong(input[i]);
    }

    public long readVarLong() {
        long out = 0;
        for (int i = 0; i < 10; i++) {
            final byte read = this.readByte();

            out |= ((long) (read & 0b01111111) << (7 * i)); // Add 7 bits to output VarInt

            if ((read & 0b10000000) == 0) // Break if first bit is 0 (Next byte is not part of VarInt)
                break;
        }

        return out;
    }

    public long[] readVarLongs(int length) {
        final long[] longs = new long[length];
        for (int i = 0; i < length; i++)
            longs[i] = readVarLong();
        return longs;
    }

    public void writeBitSet(BitSet bitSet) {
        final long[] longArray = bitSet.toLongArray();
        this.writeVarInt(longArray.length);
        this.writeLongs(longArray);
    }

    public BitSet readBitSet() {
        final int length = this.readVarInt();
        return BitSet.valueOf(this.readLongs(length));
    }

    public <T> void writeList(List<T> input, Consumer<T> writer) {
        final Iterator<T> iterator = input.iterator();
        final int size = input.size();
        this.writeVarInt(size);
        for (int i = 0; i < size; i++) {
            writer.accept(iterator.next());
        }
    }

    public <T> ArrayList<T> readList(Supplier<T> reader) {
        final int length = this.readVarInt();
        final ArrayList<T> out = new ArrayList<>(Math.min(length, 64)); // Prevent packets from pre-allocating a huge array

        for (int i = 0; i < length; i++) {
            out.add(reader.get());
        }

        return out;
    }

    public void writeByteArray(byte[] b) {
        this.writeVarInt(b.length);
        this.writeBytes(b);
    }

    public byte[] readByteArray() {
        return this.readByteArrayLimited(this.available());
    }

    public byte[] readByteArrayLimited(int limit) {
        final int length = this.readVarInt();
        if (length > limit) {
            throw new PacketException("Invalid byte array length: %d. Max Length: %d.", length, limit);
        } else {
            byte[] bytes = new byte[length];
            this.readBytes(bytes);
            return bytes;
        }
    }

    public int varIntSize(int input) {
        return (31 - Integer.numberOfLeadingZeros(input)) / 7 + 1;
    }

    public int varLongSize(int input) {
        return (63 - Long.numberOfLeadingZeros(input)) / 7 + 1;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity - this.buffer.length > 0) {
            this.grow(minCapacity);
        }
    }

    private void grow(int minCapacity) {
        final int oldCapacity = this.buffer.length;
        int newCapacity = oldCapacity << 1;

        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }

        this.buffer = Arrays.copyOf(this.buffer, newCapacity);
    }

    public int length() {
        return this.writePos;
    }

    public void resetWrite() {
        this.writePos = 0;
    }

    public void resetRead() {
        this.readPos = 0;
    }

    public void removeBytes(int count) {
        if (this.writePos - count < 0)
            return;
        this.buffer = Arrays.copyOfRange(this.buffer, count, this.buffer.length);
        this.writePos = this.writePos - count;
    }

    public int available() {
        return this.writePos - this.readPos;
    }

    public byte[] getBytes() {
        return Arrays.copyOf(this.buffer, this.writePos);
    }

    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(this.buffer, 0, this.writePos);
    }

    public byte[] getByteArray() {
        return this.buffer;
    }

    @Override
    public DataBuffer clone() {
        final DataBuffer newBuffer = new DataBuffer(this.length());
        System.arraycopy(this.buffer, 0, newBuffer.buffer, 0, this.length());
        newBuffer.writePos = this.writePos;
        newBuffer.readPos = this.readPos;
        return newBuffer;
    }
}
