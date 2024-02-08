package org_2b12r.irc2b2t.fabric;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;

public class Config {
    private static final File FILE = new File("config/irc2b2t.json");
    private static final Gson GSON = new Gson();

    public static char sendToIRCPrefix = '!';

    public static void load() {
        try {
            final JsonObject config = GSON.fromJson(new FileReader(FILE), JsonObject.class);
            sendToIRCPrefix = config.get("sendToIRCPrefix").getAsString().charAt(0);
        } catch (Throwable ignored) {}
    }

    public static void save() throws IOException {
        final JsonObject config = new JsonObject();
        config.addProperty("sendToIRCPrefix", sendToIRCPrefix);

        FILE.getParentFile().mkdirs();
        FILE.createNewFile();
        try (FileWriter writer = new FileWriter(FILE)) {
            writer.write(new Gson().toJson(config));
        }
    }
}
