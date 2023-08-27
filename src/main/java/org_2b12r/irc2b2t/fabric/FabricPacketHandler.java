package org_2b12r.irc2b2t.fabric;

import com.mojang.authlib.exceptions.AuthenticationException;
import net.minecraft.network.encryption.NetworkEncryptionException;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.text.Text;
import org_2b12r.irc2b2t.net.*;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;

public class FabricPacketHandler implements IPacketHandler {
    public Connection con;

    @Override
    public void handle(Packets.Disconnect packet) {
        IRC2b2t.reconnectDelayMs = packet.clientReconnectTimer * 1000;
        throw new ConnectionException(Text.Serializer.fromLenientJson(packet.reason));
    }

    @Override
    public void handle(Packets.C2SLogin packet) {

    }

    @Override
    public void handle(Packets.S2CEncryptionRequest packet) {
        try {
            final SecretKey sharedKey = NetworkEncryptionUtils.generateSecretKey();
            final PublicKey publicKey = NetworkEncryptionUtils.decodeEncodedRsaPublicKey(packet.publicKey);
            final String hash = new BigInteger(NetworkEncryptionUtils.computeServerId("", publicKey, sharedKey)).toString(16);

            Utils.getMC().getSessionService().joinServer(Utils.getMC().getSession().getProfile(), Utils.getMC().getSession().getAccessToken(), hash);

            con.sendPacketImmediately(new Packets.C2SEncryptionResponse(NetworkEncryptionUtils.encrypt(publicKey, sharedKey.getEncoded())));
            con.setKey(sharedKey);
        } catch (IOException | AuthenticationException | NetworkEncryptionException e) {
            IRC2b2t.runNextTick(() -> Utils.print("Failed to authenticate: " + e.getMessage()));
            IRC2b2t.reconnectDelayMs = 20000;
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handle(Packets.C2SEncryptionResponse packet) {}

    @Override
    public void handle(Packets.S2CConnected packet) {
        con.state = State.CONNECTED;
        IRC2b2t.runNextTick(() -> {
            Utils.print("Connected to server.");
            IRC2b2t.sendChatState();
        });
    }

    @Override
    public void handle(Packets.S2CCommands packet) {
        IRC2b2t.commands.clear();
        IRC2b2t.commands.addAll(packet.commands);
        IRC2b2t.runNextTick(() -> Utils.print("Commands received: " + packet.commands));
    }

    @Override
    public void handle(Packets.S2CChat packet) {
        IRC2b2t.runNextTick(() -> Utils.sendChatMessage(Text.Serializer.fromLenientJson(packet.json)));
    }

    @Override
    public void handle(Packets.C2SChat packet) {}

    @Override
    public void handle(Packets.S2CPlayerMessage packet) {
        IRC2b2t.runNextTick(() -> Utils.sendChatMessage(Text.Serializer.fromLenientJson(packet.message)));
    }

    @Override
    public void handle(Packets.C2SChatState packet) {}

    @Override
    public void handle(Packets.KeepAlive packet) {
        con.sendPacket(new Packets.KeepAlive(packet.key));
    }

    @Override
    public boolean shouldHandle(Packet packet) {
        return true;
    }
}
