package dev.masterattractor;

import dev.masterattractor.attraction.AttractionManager;
import dev.masterattractor.command.AttractorCommand;
import dev.masterattractor.config.Config;
import dev.masterattractor.network.SyncStatePayload;
import dev.masterattractor.network.TogglePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MasterAttractor implements ModInitializer {
	public static final String MOD_ID = "masterattractor";
	public static final Logger LOGGER = LoggerFactory.getLogger("MasterAttractor");

	@Override
	public void onInitialize() {
		Config.load();

		// Register packet codecs
		PayloadTypeRegistry.playC2S().register(TogglePayload.TYPE, TogglePayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SyncStatePayload.TYPE, SyncStatePayload.STREAM_CODEC);

		// Register incoming C2S toggle handler
		ServerPlayNetworking.registerGlobalReceiver(TogglePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			boolean enabled = AttractionManager.togglePlayer(player);
			ServerPlayNetworking.send(player, new SyncStatePayload(enabled));
			Component status = Component.translatable(enabled ? "text.masterattractor.activated" : "text.masterattractor.deactivated");
			player.sendSystemMessage(
				Component.translatable("text.masterattractor.prefix").append(status),
				true
			);
		});

		// Sync state on player join
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			boolean enabled = AttractionManager.isPlayerAttracting(player);
			ServerPlayNetworking.send(player, new SyncStatePayload(enabled));
		});

		// Clean up state on disconnect to avoid memory leak
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			AttractionManager.onPlayerDisconnect(handler.getPlayer());
		});

		// Server tick
		ServerTickEvents.END_SERVER_TICK.register(MasterAttractor::onServerTick);

		// Commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			AttractorCommand.register(dispatcher);
		});

		LOGGER.info("MasterAttractor has initialized — coils are live.");
	}

	private static void onServerTick(MinecraftServer server) {
		AttractionManager.tick(server);
	}
}
