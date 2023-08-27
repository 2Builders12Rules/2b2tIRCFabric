package org_2b12r.irc2b2t.net;

import jdk.net.ExtendedSocketOptions;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Connection {
    private final SocketChannel channel;
    private final Selector selector;
    private final IPacketHandler handler;

    public State state = State.AUTHENTICATING;
    public SecretKey key;
    private Cipher encrypt;
    private Cipher decrypt;

    public ConcurrentLinkedQueue<Runnable> writeQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Packet> sendQueue = new ConcurrentLinkedQueue<>();

    public long lastPacketTime = System.currentTimeMillis();

    public Connection(SocketChannel channel, Selector selector, IPacketHandler handler) throws IOException {
        this.channel = channel;
        this.selector = selector;
        this.handler = handler;

        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_CONNECT, this);
        selector.wakeup();
    }

    public void sendPacket(Packet packet) {
        this.sendQueue.add(packet);
        enableWrite();
        selector.wakeup();
    }

    private void enableWrite() {
        try {
            this.channel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, this);
        } catch (ClosedChannelException e) {
            throw new RuntimeException(e);
        }
    }

    private void disableWrite() {
        try {
            this.channel.register(selector, SelectionKey.OP_READ, this);
        } catch (ClosedChannelException e) {
            throw new RuntimeException(e);
        }
    }

    private final DataBuffer tempPacketBuffer = new DataBuffer(4096);
    private final DataBuffer sendBuffer = new DataBuffer(4096);

    public void sendPacketImmediately(Packet packet) throws IOException {
        tempPacketBuffer.writePos = 2; // Leave 2 bytes for packet length
        tempPacketBuffer.writeByte(state.getPacketId(packet.getClass()));
        packet.write(tempPacketBuffer);

        final int pos = tempPacketBuffer.length();
        tempPacketBuffer.writePos = 0;
        tempPacketBuffer.writeShort((short) (pos - 2)); // Write Length
        tempPacketBuffer.writePos = pos;

        encryptData(tempPacketBuffer);

        sendBuffer.writeBytes(tempPacketBuffer.getBytes());
        final int written = channel.write(sendBuffer.toByteBuffer());
        sendBuffer.removeBytes(written);

        if (sendBuffer.available() > 0)
            enableWrite();
        else
            disableWrite();
    }

    private DataBuffer readBuf = new DataBuffer(4096);
    private DataBuffer decryptedBuf = new DataBuffer(4096);

    /**
     * Process the connection by performing various tasks:
     * 1. Read incoming data and store it in a buffer.
     * 2. Decrypt the read data if it was encrypted, skipping decoding if decryption failed.
     * 3. Check if the length of a packet has been received and decode and handle the packet if the entire packet has been received.
     * 4. Remove the decoded bytes from the buffer.
     *
     * @throws Exception if an error occurs during the process.
     */
    public void read() throws Exception {
        // Read incoming data
        final ByteBuffer buf = ByteBuffer.allocate(4096);
        int read;
        while ((read = channel.read(buf)) > 0) {
            readBuf.writeBytes(buf.array(), read);
            buf.clear();
        }

        if (read == -1)
            throw new IOException("Connection closed");

        // Decrypt read data if it was encrypted. Skip decoding if decryption failed.
        if (!this.decryptData(readBuf))
            return;

        decryptedBuf.writeBytes(readBuf.getByteArray(), readBuf.writePos); // Copy decrypted bytes to decrypted buffer
        readBuf.resetWrite(); // Reset read buffer

        while (true) {
            try {
                if (decryptedBuf.available() <= 2)
                    break;

                final short length = decryptedBuf.readShort();
                if (decryptedBuf.available() < length) // Make sure the entire packet has been received before decoding
                    break;

                // Decode and handle packet
                final Packet readPacket = state.readPacket(decryptedBuf);
                if (readPacket != null && handler.shouldHandle(readPacket))
                    readPacket.handle(this);

                // Remove decoded bytes
                decryptedBuf.removeBytes(decryptedBuf.readPos);
            } finally {
                decryptedBuf.resetRead();
            }
        }
    }

    public void write() throws Exception {
        Runnable runnable;
        while ((runnable = writeQueue.poll()) != null)
            runnable.run();

        Packet packet;
        while ((packet = sendQueue.poll()) != null)
            this.sendPacketImmediately(packet);
    }

    public void finishConnect() throws Exception {
        channel.finishConnect();
        channel.register(selector, SelectionKey.OP_READ, this);
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        channel.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, 60);
        channel.setOption(ExtendedSocketOptions.TCP_KEEPINTERVAL, 10);
        channel.setOption(ExtendedSocketOptions.TCP_KEEPCOUNT, 6);
    }

    public void runBeforeWrite(Runnable run) {
        this.writeQueue.offer(run);
        enableWrite();
        selector.wakeup();
    }

    /**
     * Tries to encrypt bytes and write them to the input buffer if {@link Connection#encrypt} isn't null
     *
     * @param buffer the buffer to encrypt and write encrypted bytes to
     * @return false if data is too short to encrypt
     */
    private boolean encryptData(final DataBuffer buffer) {
        if (this.encrypt == null)
            return true;

        final byte[] encryptedBytes = this.encrypt.update(buffer.getBytes());
        if (encryptedBytes == null)
            return false;

        buffer.resetWrite();
        buffer.resetRead();
        buffer.writeBytes(encryptedBytes);
        return true;
    }

    /**
     * Tries to decrypt bytes and write them to the input buffer if {@link Connection#decrypt} isn't null
     *
     * @param buffer the buffer to decrypt and write decrypted bytes to
     * @return false if data is too short to decrypt
     */
    private boolean decryptData(final DataBuffer buffer) {
        if (this.decrypt == null)
            return true;

        final byte[] decryptedBytes = this.decrypt.update(buffer.getBytes());
        if (decryptedBytes == null) // Data is too short to decrypt
            return false;

        buffer.resetWrite();
        buffer.resetRead();
        buffer.writeBytes(decryptedBytes);
        return true;
    }

    public void setKey(SecretKey key) {
        this.key = key;
        try {
            this.encrypt = createCipher(Cipher.ENCRYPT_MODE, key);
            this.decrypt = createCipher(Cipher.DECRYPT_MODE, key);
        } catch (GeneralSecurityException e) {}
    }

    public void close() {
        try {
            if (this.channel != null) {
                channel.close();
                selector.wakeup();
            }
        } catch (IOException ignored) {}
    }

    public boolean isOpen() {
        return this.channel.isOpen();
    }

    public IPacketHandler getHandler() {
        return handler;
    }

    private static Cipher createCipher(int mode, SecretKey key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        cipher.init(mode, key, new IvParameterSpec(key.getEncoded()));
        return cipher;
    }
}
