package org_2b12r.irc2b2t.net;

public interface Packet {
    void write(DataBuffer buf);
    void handle(Connection con);
}
