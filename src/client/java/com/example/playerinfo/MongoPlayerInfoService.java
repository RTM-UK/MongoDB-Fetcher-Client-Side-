package com.raffe.playerinfo;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MongoPlayerInfoService implements AutoCloseable {
    private final InfoConfig config;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Player Info MongoDB");
        thread.setDaemon(true);
        return thread;
    });

    private MongoClient client;

    public MongoPlayerInfoService(InfoConfig config) {
        this.config = config;
    }

    public CompletableFuture<Optional<PlayerRecord>> findPlayer(String playerName) {
        return CompletableFuture.supplyAsync(() -> findPlayerBlocking(playerName), executor);
    }

    private Optional<PlayerRecord> findPlayerBlocking(String playerName) {
        try {
            Document document = collection()
                    .find(Filters.eq(config.nameField, playerName))
                    .first();

            if (document == null) {
                return Optional.empty();
            }

            String storedName = document.getString(config.nameField);
            String region = document.getString(config.regionField);
            String tier = document.getString(config.tierField);

            return Optional.of(new PlayerRecord(
                    storedName == null ? playerName : storedName,
                    region == null ? "Unknown" : region,
                    tier == null ? "Unknown" : tier
            ));
        } catch (MongoException exception) {
            throw new PlayerInfoLookupException("MongoDB lookup failed: " + exception.getMessage(), exception);
        }
    }

    private MongoCollection<Document> collection() {
        if (client == null) {
            client = MongoClients.create(config.connectionString);
        }

        return client.getDatabase(config.database).getCollection(config.collection);
    }

    @Override
    public void close() {
        executor.shutdownNow();

        if (client != null) {
            client.close();
        }
    }
}
