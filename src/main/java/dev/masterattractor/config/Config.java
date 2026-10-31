package dev.masterattractor.config;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Config {
	public static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("masterattractor.json");

	// Core
	public static boolean enabled = true;
	public static double radius = 10.0;
	public static double strength = 1.25;
	public static int tickInterval = 1; // every N ticks (1 = every tick)

	// What to attract
	public static boolean attractItems = true;
	public static boolean attractXpOrbs = true;
	public static boolean attractProjectiles = true;
	public static boolean attractMobs = false;
	public static boolean attractPlayers = false;
	public static boolean attractBoats = false;
	public static boolean attractMinecarts = false;
	public static boolean attractFallingBlocks = false;

	// Behavior
	public static boolean ignorePlayersWithGodMode = true;
	public static boolean onlyPullToNearestPlayer = false;
	public static boolean respectPickupDelay = false;
	public static boolean playSoundOnPull = true;
	public static boolean spawnParticlesOnPull = true;

	// Visual
	public static boolean showRadiusOverlay = true;
	public static int overlayColor = 0x00FF00; // green
	public static double overlayOpacity = 0.3;

	public static void load() {
		try {
			if (!Files.exists(CONFIG_PATH)) {
				save();
				return;
			}
			String json = Files.readString(CONFIG_PATH);
			Map<String, Object> values = parseJson(json);

			enabled = asBoolean(values.getOrDefault("enabled", true), true);
			radius = asDouble(values.getOrDefault("radius", 10.0), 10.0);
			strength = asDouble(values.getOrDefault("strength", 1.25), 1.25);
			tickInterval = asInt(values.getOrDefault("tickInterval", 1), 1);

			attractItems = asBoolean(values.getOrDefault("attractItems", true), true);
			attractXpOrbs = asBoolean(values.getOrDefault("attractXpOrbs", true), true);
			attractProjectiles = asBoolean(values.getOrDefault("attractProjectiles", true), true);
			attractMobs = asBoolean(values.getOrDefault("attractMobs", false), false);
			attractPlayers = asBoolean(values.getOrDefault("attractPlayers", false), false);
			attractBoats = asBoolean(values.getOrDefault("attractBoats", false), false);
			attractMinecarts = asBoolean(values.getOrDefault("attractMinecarts", false), false);
			attractFallingBlocks = asBoolean(values.getOrDefault("attractFallingBlocks", false), false);

			ignorePlayersWithGodMode = asBoolean(values.getOrDefault("ignorePlayersWithGodMode", true), true);
			onlyPullToNearestPlayer = asBoolean(values.getOrDefault("onlyPullToNearestPlayer", false), false);
			respectPickupDelay = asBoolean(values.getOrDefault("respectPickupDelay", false), false);
			playSoundOnPull = asBoolean(values.getOrDefault("playSoundOnPull", true), true);
			spawnParticlesOnPull = asBoolean(values.getOrDefault("spawnParticlesOnPull", true), true);

			showRadiusOverlay = asBoolean(values.getOrDefault("showRadiusOverlay", true), true);
			overlayColor = asInt(values.getOrDefault("overlayColor", 0x00FF00), 0x00FF00);
			overlayOpacity = asDouble(values.getOrDefault("overlayOpacity", 0.3), 0.3);

		} catch (Exception e) {
			dev.masterattractor.MasterAttractor.LOGGER.warn("Failed to load MasterAttractor config, using defaults", e);
			save();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			StringBuilder sb = new StringBuilder();
			sb.append("{\n");
			sb.append(quote("enabled")).append(": ").append(enabled).append(",\n");
			sb.append(quote("radius")).append(": ").append(radius).append(",\n");
			sb.append(quote("strength")).append(": ").append(strength).append(",\n");
			sb.append(quote("tickInterval")).append(": ").append(tickInterval).append(",\n");
			sb.append(quote("attractItems")).append(": ").append(attractItems).append(",\n");
			sb.append(quote("attractXpOrbs")).append(": ").append(attractXpOrbs).append(",\n");
			sb.append(quote("attractProjectiles")).append(": ").append(attractProjectiles).append(",\n");
			sb.append(quote("attractMobs")).append(": ").append(attractMobs).append(",\n");
			sb.append(quote("attractPlayers")).append(": ").append(attractPlayers).append(",\n");
			sb.append(quote("attractBoats")).append(": ").append(attractBoats).append(",\n");
			sb.append(quote("attractMinecarts")).append(": ").append(attractMinecarts).append(",\n");
			sb.append(quote("attractFallingBlocks")).append(": ").append(attractFallingBlocks).append(",\n");
			sb.append(quote("ignorePlayersWithGodMode")).append(": ").append(ignorePlayersWithGodMode).append(",\n");
			sb.append(quote("onlyPullToNearestPlayer")).append(": ").append(onlyPullToNearestPlayer).append(",\n");
			sb.append(quote("respectPickupDelay")).append(": ").append(respectPickupDelay).append(",\n");
			sb.append(quote("playSoundOnPull")).append(": ").append(playSoundOnPull).append(",\n");
			sb.append(quote("spawnParticlesOnPull")).append(": ").append(spawnParticlesOnPull).append(",\n");
			sb.append(quote("showRadiusOverlay")).append(": ").append(showRadiusOverlay).append(",\n");
			sb.append(quote("overlayColor")).append(": ").append(overlayColor).append(",\n");
			sb.append(quote("overlayOpacity")).append(": ").append(overlayOpacity).append("\n");
			sb.append("}\n");
			Files.writeString(CONFIG_PATH, sb.toString());
		} catch (IOException e) {
			dev.masterattractor.MasterAttractor.LOGGER.warn("Failed to save MasterAttractor config", e);
		}
	}

	private static Map<String, Object> parseJson(String json) {
		Map<String, Object> map = new LinkedHashMap<>();
		json = json.trim();
		if (!json.startsWith("{") || !json.endsWith("}")) return map;
		json = json.substring(1, json.length() - 1).trim();

		Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*([^,}]+)");
		Matcher matcher = pattern.matcher(json);
		while (matcher.find()) {
			String key = matcher.group(1);
			String val = matcher.group(2).trim();
			map.put(key, parseValue(val));
		}
		return map;
	}

	private static Object parseValue(String val) {
		if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) return Boolean.parseBoolean(val);
		if (val.matches("-?\\d+")) return Integer.parseInt(val);
		if (val.matches("-?\\d+\\.\\d+")) return Double.parseDouble(val);
		if (val.startsWith("\"") && val.endsWith("\"")) return val.substring(1, val.length() - 1);
		return val;
	}

	private static boolean asBoolean(Object v, boolean d) {
		if (v instanceof Boolean b) return b;
		if (v instanceof String s) return Boolean.parseBoolean(s);
		return d;
	}

	private static int asInt(Object v, int d) {
		if (v instanceof Number n) return n.intValue();
		if (v instanceof String s) {
			try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
		}
		return d;
	}

	private static double asDouble(Object v, double d) {
		if (v instanceof Number n) return n.doubleValue();
		if (v instanceof String s) {
			try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
		}
		return d;
	}

	private static String quote(String s) {
		return "\"" + s + "\"";
	}
}
