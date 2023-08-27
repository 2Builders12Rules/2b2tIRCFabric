package org_2b12r.irc2b2t.net;

public interface IPacketHandler {
    void handle(Packets.Disconnect packet);

    // Authenticating
    void handle(Packets.C2SLogin packet);
    void handle(Packets.S2CEncryptionRequest packet);
    void handle(Packets.C2SEncryptionResponse packet);
    void handle(Packets.S2CConnected packet);

    // Connected
    void handle(Packets.S2CCommands packet);
    void handle(Packets.S2CChat packet);
    void handle(Packets.C2SChat packet);
    void handle(Packets.S2CPlayerMessage packet);
    void handle(Packets.C2SChatState packet);
    void handle(Packets.KeepAlive packet);

    boolean shouldHandle(Packet packet);
}
