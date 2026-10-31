package dev.masterattractor.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.masterattractor.config.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public final class MasterAttractorModMenu implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("config.masterattractor.title"));

			builder.setSavingRunnable(Config::save);

			ConfigEntryBuilder entry = builder.entryBuilder();

			// 1. General Category
			ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.masterattractor.category.general"));

			general.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.enabled"), Config.enabled)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.masterattractor.enabled.tooltip"))
				.setSaveConsumer(val -> Config.enabled = val)
				.build());

			general.addEntry(entry.startDoubleField(Component.translatable("config.masterattractor.radius"), Config.radius)
				.setDefaultValue(10.0)
				.setMin(1.0)
				.setMax(128.0)
				.setTooltip(Component.translatable("config.masterattractor.radius.tooltip"))
				.setSaveConsumer(val -> Config.radius = val)
				.build());

			general.addEntry(entry.startDoubleField(Component.translatable("config.masterattractor.strength"), Config.strength)
				.setDefaultValue(1.25)
				.setMin(0.1)
				.setMax(10.0)
				.setTooltip(Component.translatable("config.masterattractor.strength.tooltip"))
				.setSaveConsumer(val -> Config.strength = val)
				.build());

			general.addEntry(entry.startIntField(Component.translatable("config.masterattractor.tickInterval"), Config.tickInterval)
				.setDefaultValue(1)
				.setMin(1)
				.setMax(20)
				.setTooltip(Component.translatable("config.masterattractor.tickInterval.tooltip"))
				.setSaveConsumer(val -> Config.tickInterval = val)
				.build());

			// 2. Targets Category
			ConfigCategory targets = builder.getOrCreateCategory(Component.translatable("config.masterattractor.category.targets"));

			targets.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.attractItems"), Config.attractItems)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.masterattractor.attractItems.tooltip"))
				.setSaveConsumer(val -> Config.attractItems = val)
				.build());

			targets.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.attractXpOrbs"), Config.attractXpOrbs)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.masterattractor.attractXpOrbs.tooltip"))
				.setSaveConsumer(val -> Config.attractXpOrbs = val)
				.build());

			targets.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.attractProjectiles"), Config.attractProjectiles)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.masterattractor.attractProjectiles.tooltip"))
				.setSaveConsumer(val -> Config.attractProjectiles = val)
				.build());

			targets.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.attractMobs"), Config.attractMobs)
				.setDefaultValue(false)
				.setTooltip(Component.translatable("config.masterattractor.attractMobs.tooltip"))
				.setSaveConsumer(val -> Config.attractMobs = val)
				.build());

			targets.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.attractPlayers"), Config.attractPlayers)
				.setDefaultValue(false)
				.setTooltip(Component.translatable("config.masterattractor.attractPlayers.tooltip"))
				.setSaveConsumer(val -> Config.attractPlayers = val)
				.build());

			targets.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.attractBoats"), Config.attractBoats)
				.setDefaultValue(false)
				.setTooltip(Component.translatable("config.masterattractor.attractBoats.tooltip"))
				.setSaveConsumer(val -> Config.attractBoats = val)
				.build());

			targets.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.attractMinecarts"), Config.attractMinecarts)
				.setDefaultValue(false)
				.setTooltip(Component.translatable("config.masterattractor.attractMinecarts.tooltip"))
				.setSaveConsumer(val -> Config.attractMinecarts = val)
				.build());

			targets.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.attractFallingBlocks"), Config.attractFallingBlocks)
				.setDefaultValue(false)
				.setTooltip(Component.translatable("config.masterattractor.attractFallingBlocks.tooltip"))
				.setSaveConsumer(val -> Config.attractFallingBlocks = val)
				.build());

			// 3. Item Filtering Category
			ConfigCategory filters = builder.getOrCreateCategory(Component.translatable("config.masterattractor.category.filters"));

			filters.addEntry(entry.startEnumSelector(Component.translatable("config.masterattractor.filterMode"), Config.FilterMode.class, Config.filterMode)
				.setDefaultValue(Config.FilterMode.NONE)
				.setTooltip(Component.translatable("config.masterattractor.filterMode.tooltip"))
				.setSaveConsumer(val -> Config.filterMode = val)
				.build());

			filters.addEntry(entry.startStrList(Component.translatable("config.masterattractor.filteredItems"), Config.filteredItems)
				.setDefaultValue(new ArrayList<>(Config.filteredItems))
				.setTooltip(Component.translatable("config.masterattractor.filteredItems.tooltip"))
				.setSaveConsumer(val -> Config.filteredItems = new ArrayList<>(val))
				.build());

			filters.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.checkInventorySpace"), Config.checkInventorySpace)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.masterattractor.checkInventorySpace.tooltip"))
				.setSaveConsumer(val -> Config.checkInventorySpace = val)
				.build());

			filters.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.vacuumMode"), Config.vacuumMode)
				.setDefaultValue(false)
				.setTooltip(Component.translatable("config.masterattractor.vacuumMode.tooltip"))
				.setSaveConsumer(val -> Config.vacuumMode = val)
				.build());

			// 4. Behavior & FX Category
			ConfigCategory behavior = builder.getOrCreateCategory(Component.translatable("config.masterattractor.category.behavior"));

			behavior.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.respectPickupDelay"), Config.respectPickupDelay)
				.setDefaultValue(false)
				.setTooltip(Component.translatable("config.masterattractor.respectPickupDelay.tooltip"))
				.setSaveConsumer(val -> Config.respectPickupDelay = val)
				.build());

			behavior.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.ignorePlayersWithGodMode"), Config.ignorePlayersWithGodMode)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.masterattractor.ignorePlayersWithGodMode.tooltip"))
				.setSaveConsumer(val -> Config.ignorePlayersWithGodMode = val)
				.build());

			behavior.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.playSoundOnPull"), Config.playSoundOnPull)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.masterattractor.playSoundOnPull.tooltip"))
				.setSaveConsumer(val -> Config.playSoundOnPull = val)
				.build());

			behavior.addEntry(entry.startBooleanToggle(Component.translatable("config.masterattractor.spawnParticlesOnPull"), Config.spawnParticlesOnPull)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.masterattractor.spawnParticlesOnPull.tooltip"))
				.setSaveConsumer(val -> Config.spawnParticlesOnPull = val)
				.build());

			return builder.build();
		};
	}
}
