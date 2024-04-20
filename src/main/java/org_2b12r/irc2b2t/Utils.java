package org_2b12r.irc2b2t;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;

import java.util.UUID;

public class Utils {
    public static final MessageIndicator IRC2B2T_MESSAGE_INDICATOR = new MessageIndicator(0x87ed7b, null, Text.of("This message is from the 2b2t IRC mod."), "2b2t IRC");
    public static final Gson GSON = new GsonBuilder().registerTypeHierarchyAdapter(Text.class, new Text.Serializer()).create();

    public static MinecraftClient getMC() {
        return MinecraftClient.getInstance();
    }

    public static String getUsername() {
        return getMC().getSession().getUsername();
    }

    public static void sendChatMessage(Text message) {
        if (getMC().inGameHud != null)
            getMC().inGameHud.getChatHud().addMessage(message, null, IRC2B2T_MESSAGE_INDICATOR);
    }

    public static void print(String message) {
        sendChatMessage(Text.of(message));
    }

    public static UUID getUUID() {
        return getMC().getSession().getUuidOrNull();
    }
}
