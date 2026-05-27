package com.example.playerinfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class InfoConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String ENV_MONGO_URI = "PLAYER_INFO_MONGO_URI";

    public String connectionString = "mongodb://localhost:27017";
    public String database = "Test";
    public String collection = "Test";
    public String nameField = "ign";
    public String regionField = "region";
    public String tierField = "tier";

    public static InfoConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("player-info-client.json");
        InfoConfig config = readOrCreate(configPath);
        String envConnectionString = System.getenv(ENV_MONGO_URI);

        if (envConnectionString != null && !envConnectionString.isBlank()) {
            config.connectionString = envConnectionString;
        }

        return config;
    }

    private static InfoConfig readOrCreate(Path configPath) {
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                InfoConfig config = GSON.fromJson(reader, InfoConfig.class);
                return config == null ? new InfoConfig() : config;
            } catch (IOException exception) {
                PlayerInfoClientMod.LOGGER.warn("Failed to read {}, using defaults", configPath, exception);
                return new InfoConfig();
            }
        }

        InfoConfig config = new InfoConfig();
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            PlayerInfoClientMod.LOGGER.warn("Failed to create default config at {}", configPath, exception);
        }
        return config;
    }
}
