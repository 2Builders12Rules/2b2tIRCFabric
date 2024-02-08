package org_2b12r.irc2b2t.fabric;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.UUID;

public class Utils {
    public static MinecraftClient getMC() {
        return MinecraftClient.getInstance();
    }

    public static String getUsername() {
        return getMC().getSession().getProfile().getName();
    }

    public static void sendChatMessage(Text message) {
        if (getMC().inGameHud != null)
            getMC().inGameHud.getChatHud().addMessage(message);
    }

    public static void print(String message) {
        sendChatMessage(Text.of(IRC2b2t.ircPrefix + message));
    }

    public static UUID getUUID() {
        return getMC().getSession().getProfile().getId();
    }
}
