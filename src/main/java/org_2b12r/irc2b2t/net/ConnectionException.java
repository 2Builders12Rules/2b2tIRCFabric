package org_2b12r.irc2b2t.net;

import net.minecraft.text.Text;

public class ConnectionException extends RuntimeException {
    public Text message;

    public ConnectionException(Text message) {
        this.message = message;
    }
}
