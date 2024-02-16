package org_2b12r.irc2b2t.fabric;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;

import java.util.UUID;

public class Utils {
    public static final MessageIndicator IRC2B2T_MESSAGE_INDICATOR = new MessageIndicator(0x87ed7b, null, Text.of("This message is from the 2b2t IRC mod."), "2b2t IRC");

    public static MinecraftClient getMC() {
        return MinecraftClient.getInstance();
    }

    public static String getUsername() {
        return getMC().getSession().getProfile().getName();
    }

    public static void sendChatMessage(Text message) {
        if (getMC().inGameHud != null)
            getMC().inGameHud.getChatHud().addMessage(message, null, IRC2B2T_MESSAGE_INDICATOR);
    }

    public static void print(String message) {
        sendChatMessage(Text.of(message));
    }

    public static UUID getUUID() {
        return getMC().getSession().getProfile().getId();
    }
}
