package dev.masterattractor.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {

	@Test
	public void testFilterModeEnum() {
		assertEquals(Config.FilterMode.NONE, Config.FilterMode.valueOf("NONE"));
		assertEquals(Config.FilterMode.BLACKLIST, Config.FilterMode.valueOf("BLACKLIST"));
		assertEquals(Config.FilterMode.WHITELIST, Config.FilterMode.valueOf("WHITELIST"));
	}

	@Test
	public void testItemFilteringSetLookup() {
		Config.updateFilteredItems(List.of("minecraft:diamond", "minecraft:iron_ingot"));
		assertTrue(Config.isItemFiltered("minecraft:diamond"));
		assertTrue(Config.isItemFiltered("minecraft:iron_ingot"));
		assertFalse(Config.isItemFiltered("minecraft:dirt"));
	}

	@Test
	public void testDefaults() {
		assertTrue(Config.enabled);
		assertTrue(Config.radius >= 1.0 && Config.radius <= 48.0);
		assertTrue(Config.strength > 0);
	}
}