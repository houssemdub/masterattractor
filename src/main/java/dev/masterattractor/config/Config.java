package dev.masterattractor.config;

import com.google.gson.*;
import dev.masterattractor.MasterAttractor;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Config {
	public static Path getConfigPath() {
		try {
			var loader = FabricLoader.getInstance();
			if (loader != null) {
				Path dir = loader.getConfigDir();
				if (dir != null) {
					return dir.resolve("masterattractor.json");
				}
			}
		} catch (Throwable ignored) {}
		return Path.of("config", "masterattractor.json");
	}

	public static final Path CONFIG_PATH = getConfigPath();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public enum FilterMode {
		NONE,
		BLACKLIST,
		WHITELIST
	}

	public enum AnimationMode {
		ARC_HOP,
		SPIRAL,
		MAGNETIC,
		FLOAT
	}

	public enum ParticleTrail {
		PORTAL,
		ENCHANTED_HIT,
		FLAME,
		SOUL_FIRE_FLAME,
		ELECTRIC_SPARK,
		GLOW,
		END_ROD,
		HEART,
		NAUTILUS,
		TOTEM_OF_UNDYING,
		CHERRY_LEAVES,
		BUBBLE,
		NONE
	}

	public enum TrailDensity {
		OFF,
		LOW,
		MEDIUM,
		HIGH,
		ULTRA
	}

	// 1. Core
	public static boolean enabled = true;
	public static double radius = 16.0;
	public static double strength = 1.25;
	public static int tickInterval = 1;

	// 2. Flight & Animation
	public static AnimationMode animationMode = AnimationMode.ARC_HOP;
	public static double flightSpeed = 0.65;
	public static double jumpStrength = 0.5;
	public static boolean enableCeilingClamping = true;
	public static boolean targetPocket = true;
	public static boolean smoothArrival = true;
	public static boolean instantVacuum = false;

	// 3. Visual Trails & Effects
	public static ParticleTrail particleTrail = ParticleTrail.PORTAL;
	public static TrailDensity trailDensity = TrailDensity.MEDIUM;
	public static boolean arrivalParticles = true;
	public static boolean showRadiusOverlay = false;
	public static int overlayColor = 0x00FF00;
	public static double overlayOpacity = 0.3;

	// 4. Audio & Sound
	public static boolean playSoundOnPull = true;
	public static boolean playArrivalChime = true;
	public static double soundVolume = 0.25;
	public static double soundPitch = 1.2;

	// 5. Targets
	public static boolean attractItems = true;
	public static boolean attractXpOrbs = true;
	public static boolean attractProjectiles = true;
	public static boolean attractMobs = false;
	public static boolean attractPlayers = false;
	public static boolean attractBoats = false;
	public static boolean attractMinecarts = false;
	public static boolean attractFallingBlocks = false;

	// 6. Item Filtering & Inventory
	public static FilterMode filterMode = FilterMode.NONE;
	public static List<String> filteredItems = new ArrayList<>(List.of(
		"minecraft:cobblestone",
		"minecraft:dirt",
		"minecraft:andesite",
		"minecraft:diorite",
		"minecraft:granite",
		"minecraft:rotten_flesh"
	));
	private static Set<String> filteredItemsSet = new HashSet<>(filteredItems);

	public static boolean isItemFiltered(String id) {
		return filteredItemsSet.contains(id);
	}

	public static void updateFilteredItems(List<String> items) {
		filteredItems = new ArrayList<>(items);
		filteredItemsSet = new HashSet<>(items);
	}

	public static boolean checkInventorySpace = true;
	public static boolean vacuumMode = false;

	// 7. Behavior
	public static boolean ignorePlayersWithGodMode = true;
	public static boolean onlyPullToNearestPlayer = false;
	public static boolean respectPickupDelay = false;

	public static void load() {
		try {
			if (!Files.exists(CONFIG_PATH)) {
				save();
				return;
			}
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();

				if (obj.has("enabled")) enabled = obj.get("enabled").getAsBoolean();
				if (obj.has("radius")) radius = Math.clamp(obj.get("radius").getAsDouble(), 1.0, 2048.0);
				if (obj.has("strength")) strength = Math.clamp(obj.get("strength").getAsDouble(), 0.1, 10.0);
				if (obj.has("tickInterval")) tickInterval = Math.max(1, obj.get("tickInterval").getAsInt());

				if (obj.has("animationMode")) {
					try {
						animationMode = AnimationMode.valueOf(obj.get("animationMode").getAsString().toUpperCase(Locale.ROOT));
					} catch (Exception ignored) {
						animationMode = AnimationMode.ARC_HOP;
					}
				}
				if (obj.has("flightSpeed")) flightSpeed = Math.clamp(obj.get("flightSpeed").getAsDouble(), 0.1, 3.0);
				if (obj.has("jumpStrength")) jumpStrength = Math.clamp(obj.get("jumpStrength").getAsDouble(), 0.0, 2.0);
				if (obj.has("enableCeilingClamping")) enableCeilingClamping = obj.get("enableCeilingClamping").getAsBoolean();
				if (obj.has("targetPocket")) targetPocket = obj.get("targetPocket").getAsBoolean();
				if (obj.has("smoothArrival")) smoothArrival = obj.get("smoothArrival").getAsBoolean();
				if (obj.has("instantVacuum")) instantVacuum = obj.get("instantVacuum").getAsBoolean();

				if (obj.has("particleTrail")) {
					try {
						particleTrail = ParticleTrail.valueOf(obj.get("particleTrail").getAsString().toUpperCase(Locale.ROOT));
					} catch (Exception ignored) {
						particleTrail = ParticleTrail.PORTAL;
					}
				}
				if (obj.has("trailDensity")) {
					try {
						trailDensity = TrailDensity.valueOf(obj.get("trailDensity").getAsString().toUpperCase(Locale.ROOT));
					} catch (Exception ignored) {
						trailDensity = TrailDensity.MEDIUM;
					}
				}
				if (obj.has("arrivalParticles")) arrivalParticles = obj.get("arrivalParticles").getAsBoolean();

				if (obj.has("playSoundOnPull")) playSoundOnPull = obj.get("playSoundOnPull").getAsBoolean();
				if (obj.has("playArrivalChime")) playArrivalChime = obj.get("playArrivalChime").getAsBoolean();
				if (obj.has("soundVolume")) soundVolume = Math.clamp(obj.get("soundVolume").getAsDouble(), 0.0, 1.0);
				if (obj.has("soundPitch")) soundPitch = Math.clamp(obj.get("soundPitch").getAsDouble(), 0.5, 2.0);

				if (obj.has("attractItems")) attractItems = obj.get("attractItems").getAsBoolean();
				if (obj.has("attractXpOrbs")) attractXpOrbs = obj.get("attractXpOrbs").getAsBoolean();
				if (obj.has("attractProjectiles")) attractProjectiles = obj.get("attractProjectiles").getAsBoolean();
				if (obj.has("attractMobs")) attractMobs = obj.get("attractMobs").getAsBoolean();
				if (obj.has("attractPlayers")) attractPlayers = obj.get("attractPlayers").getAsBoolean();
				if (obj.has("attractBoats")) attractBoats = obj.get("attractBoats").getAsBoolean();
				if (obj.has("attractMinecarts")) attractMinecarts = obj.get("attractMinecarts").getAsBoolean();
				if (obj.has("attractFallingBlocks")) attractFallingBlocks = obj.get("attractFallingBlocks").getAsBoolean();

				if (obj.has("filterMode")) {
					try {
						filterMode = FilterMode.valueOf(obj.get("filterMode").getAsString().toUpperCase(Locale.ROOT));
					} catch (Exception ignored) {
						filterMode = FilterMode.NONE;
					}
				}

				if (obj.has("filteredItems") && obj.get("filteredItems").isJsonArray()) {
					List<String> list = new ArrayList<>();
					for (JsonElement el : obj.getAsJsonArray("filteredItems")) {
						list.add(el.getAsString());
					}
					updateFilteredItems(list);
				} else {
					updateFilteredItems(filteredItems);
				}

				if (obj.has("checkInventorySpace")) checkInventorySpace = obj.get("checkInventorySpace").getAsBoolean();
				if (obj.has("vacuumMode")) vacuumMode = obj.get("vacuumMode").getAsBoolean();

				if (obj.has("ignorePlayersWithGodMode")) ignorePlayersWithGodMode = obj.get("ignorePlayersWithGodMode").getAsBoolean();
				if (obj.has("onlyPullToNearestPlayer")) onlyPullToNearestPlayer = obj.get("onlyPullToNearestPlayer").getAsBoolean();
				if (obj.has("respectPickupDelay")) respectPickupDelay = obj.get("respectPickupDelay").getAsBoolean();

				if (obj.has("showRadiusOverlay")) showRadiusOverlay = obj.get("showRadiusOverlay").getAsBoolean();
				if (obj.has("overlayColor")) overlayColor = obj.get("overlayColor").getAsInt();
				if (obj.has("overlayOpacity")) overlayOpacity = obj.get("overlayOpacity").getAsDouble();
			}
		} catch (Exception e) {
			MasterAttractor.LOGGER.warn("Failed to load MasterAttractor config, writing defaults", e);
			save();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			JsonObject obj = new JsonObject();
			obj.addProperty("enabled", enabled);
			obj.addProperty("radius", radius);
			obj.addProperty("strength", strength);
			obj.addProperty("tickInterval", tickInterval);

			obj.addProperty("animationMode", animationMode.name());
			obj.addProperty("flightSpeed", flightSpeed);
			obj.addProperty("jumpStrength", jumpStrength);
			obj.addProperty("enableCeilingClamping", enableCeilingClamping);
			obj.addProperty("targetPocket", targetPocket);
			obj.addProperty("smoothArrival", smoothArrival);
			obj.addProperty("instantVacuum", instantVacuum);

			obj.addProperty("particleTrail", particleTrail.name());
			obj.addProperty("trailDensity", trailDensity.name());
			obj.addProperty("arrivalParticles", arrivalParticles);

			obj.addProperty("playSoundOnPull", playSoundOnPull);
			obj.addProperty("playArrivalChime", playArrivalChime);
			obj.addProperty("soundVolume", soundVolume);
			obj.addProperty("soundPitch", soundPitch);

			obj.addProperty("attractItems", attractItems);
			obj.addProperty("attractXpOrbs", attractXpOrbs);
			obj.addProperty("attractProjectiles", attractProjectiles);
			obj.addProperty("attractMobs", attractMobs);
			obj.addProperty("attractPlayers", attractPlayers);
			obj.addProperty("attractBoats", attractBoats);
			obj.addProperty("attractMinecarts", attractMinecarts);
			obj.addProperty("attractFallingBlocks", attractFallingBlocks);

			obj.addProperty("filterMode", filterMode.name());
			JsonArray arr = new JsonArray();
			for (String item : filteredItems) {
				arr.add(item);
			}
			obj.add("filteredItems", arr);

			obj.addProperty("checkInventorySpace", checkInventorySpace);
			obj.addProperty("vacuumMode", vacuumMode);

			obj.addProperty("ignorePlayersWithGodMode", ignorePlayersWithGodMode);
			obj.addProperty("onlyPullToNearestPlayer", onlyPullToNearestPlayer);
			obj.addProperty("respectPickupDelay", respectPickupDelay);

			obj.addProperty("showRadiusOverlay", showRadiusOverlay);
			obj.addProperty("overlayColor", overlayColor);
			obj.addProperty("overlayOpacity", overlayOpacity);

			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(obj, writer);
			}
		} catch (Exception e) {
			MasterAttractor.LOGGER.warn("Failed to save MasterAttractor config", e);
		}
	}
}