package com.raffe.playerinfo;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class PlayerInfoClientMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("player-info-client");

    private MongoPlayerInfoService playerInfoService;

    @Override
    public void onInitializeClient() {
        InfoConfig config = InfoConfig.load();
        playerInfoService = new MongoPlayerInfoService(config);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("info")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(context -> executeInfo(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "name")
                                )))));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> playerInfoService.close());
    }

    private int executeInfo(FabricClientCommandSource source, String playerName) {
        source.sendFeedback(Component.literal("Looking up " + playerName + "..."));

        playerInfoService.findPlayer(playerName).whenComplete((result, throwable) ->
                Minecraft.getInstance().execute(() -> sendLookupResult(source, playerName, result, throwable)));

        return Command.SINGLE_SUCCESS;
    }

    private void sendLookupResult(
            FabricClientCommandSource source,
            String playerName,
            Optional<PlayerRecord> result,
            Throwable throwable
    ) {
        if (throwable != null) {
            LOGGER.warn("Failed to look up {}", playerName, throwable);
            source.sendError(Component.literal("Could not look up " + playerName + ": " + rootMessage(throwable)));
            return;
        }

        if (result.isEmpty()) {
            source.sendFeedback(Component.literal("No info found for " + playerName + "."));
            return;
        }

        PlayerRecord player = result.get();
        source.sendFeedback(Component.literal(player.name() + " | Region: " + player.region() + " | Tier: " + player.tier()));
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
