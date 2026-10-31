package dev.masterattractor.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.masterattractor.config.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public final class MasterAttractorModMenu implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("MasterAttractor Configuration"));

			builder.setSavingRunnable(Config::save);

			ConfigEntryBuilder entry = builder.entryBuilder();

			// General Category
			ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

			general.addEntry(entry.startBooleanToggle(Component.literal("Mod Enabled"), Config.enabled)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Global master toggle for MasterAttractor."))
				.setSaveConsumer(val -> Config.enabled = val)
				.build());

			general.addEntry(entry.startDoubleField(Component.literal("Attraction Radius"), Config.radius)
				.setDefaultValue(10.0)
				.setMin(1.0)
				.setMax(128.0)
				.setTooltip(Component.literal("Radius (in blocks) around the player to pull entities from."))
				.setSaveConsumer(val -> Config.radius = val)
				.build());

			general.addEntry(entry.startDoubleField(Component.literal("Pull Strength"), Config.strength)
				.setDefaultValue(1.25)
				.setMin(0.1)
				.setMax(10.0)
				.setTooltip(Component.literal("Multiplier for the attraction pull force."))
				.setSaveConsumer(val -> Config.strength = val)
				.build());

			general.addEntry(entry.startIntField(Component.literal("Tick Interval"), Config.tickInterval)
				.setDefaultValue(1)
				.setMin(1)
				.setMax(20)
				.setTooltip(Component.literal("Run pull logic every N ticks (1 = every tick for maximum smoothness)."))
				.setSaveConsumer(val -> Config.tickInterval = val)
				.build());

			// Targets Category
			ConfigCategory targets = builder.getOrCreateCategory(Component.literal("Targets"));

			targets.addEntry(entry.startBooleanToggle(Component.literal("Attract Dropped Items"), Config.attractItems)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Pull dropped items on the ground."))
				.setSaveConsumer(val -> Config.attractItems = val)
				.build());

			targets.addEntry(entry.startBooleanToggle(Component.literal("Attract XP Orbs"), Config.attractXpOrbs)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Pull experience orbs."))
				.setSaveConsumer(val -> Config.attractXpOrbs = val)
				.build());

			targets.addEntry(entry.startBooleanToggle(Component.literal("Attract Projectiles"), Config.attractProjectiles)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Pull arrows, tridents, and throwables."))
				.setSaveConsumer(val -> Config.attractProjectiles = val)
				.build());

			targets.addEntry(entry.startBooleanToggle(Component.literal("Attract Mobs"), Config.attractMobs)
				.setDefaultValue(false)
				.setTooltip(Component.literal("Pull living mobs toward the player."))
				.setSaveConsumer(val -> Config.attractMobs = val)
				.build());

			targets.addEntry(entry.startBooleanToggle(Component.literal("Attract Boats"), Config.attractBoats)
				.setDefaultValue(false)
				.setTooltip(Component.literal("Pull boats."))
				.setSaveConsumer(val -> Config.attractBoats = val)
				.build());

			targets.addEntry(entry.startBooleanToggle(Component.literal("Attract Minecarts"), Config.attractMinecarts)
				.setDefaultValue(false)
				.setTooltip(Component.literal("Pull minecarts."))
				.setSaveConsumer(val -> Config.attractMinecarts = val)
				.build());

			targets.addEntry(entry.startBooleanToggle(Component.literal("Attract Falling Blocks"), Config.attractFallingBlocks)
				.setDefaultValue(false)
				.setTooltip(Component.literal("Pull falling sand, gravel, anvils, etc."))
				.setSaveConsumer(val -> Config.attractFallingBlocks = val)
				.build());

			// Behavior & FX Category
			ConfigCategory behavior = builder.getOrCreateCategory(Component.literal("Behavior & Effects"));

			behavior.addEntry(entry.startBooleanToggle(Component.literal("Respect Pickup Delay"), Config.respectPickupDelay)
				.setDefaultValue(false)
				.setTooltip(Component.literal("When enabled, will not pull items that cannot be picked up yet."))
				.setSaveConsumer(val -> Config.respectPickupDelay = val)
				.build());

			behavior.addEntry(entry.startBooleanToggle(Component.literal("Ignore Invulnerable Players"), Config.ignorePlayersWithGodMode)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Do not attract if the player is in creative or invulnerable mode."))
				.setSaveConsumer(val -> Config.ignorePlayersWithGodMode = val)
				.build());

			behavior.addEntry(entry.startBooleanToggle(Component.literal("Play Sound Effects"), Config.playSoundOnPull)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Play subtle pickup chime on attraction."))
				.setSaveConsumer(val -> Config.playSoundOnPull = val)
				.build());

			behavior.addEntry(entry.startBooleanToggle(Component.literal("Spawn Particles"), Config.spawnParticlesOnPull)
				.setDefaultValue(true)
				.setTooltip(Component.literal("Spawn portal particle trails when pulling entities."))
				.setSaveConsumer(val -> Config.spawnParticlesOnPull = val)
				.build());

			return builder.build();
		};
	}
}
