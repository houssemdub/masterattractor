package dev.masterattractor.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.masterattractor.MasterAttractor;
import dev.masterattractor.network.SyncStatePayload;
import dev.masterattractor.network.TogglePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class MasterAttractorClient implements ClientModInitializer {
	private static KeyMapping toggleKey;
	public static boolean isAttractorActive = false;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MasterAttractor.MOD_ID, "key_category"));

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.masterattractor.toggle",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_V,
			category
		));

		ClientPlayNetworking.registerGlobalReceiver(SyncStatePayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				isAttractorActive = payload.enabled();
				if (context.client().gui != null) {
					Component status = Component.translatable(isAttractorActive ? "text.masterattractor.on" : "text.masterattractor.off");
					context.client().gui.setOverlayMessage(
						Component.translatable("text.masterattractor.prefix").append(status),
						false
					);
				}
			});
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				ClientPlayNetworking.send(TogglePayload.INSTANCE);
			}
		});
	}
}
