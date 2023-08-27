package org_2b12r.irc2b2t.net;

public class PacketException extends RuntimeException {
    public PacketException(String message, Object... args) {
        super(String.format(message, args));
    }
}