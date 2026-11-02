package dev.masterattractor.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {

	@Test
	public void testEnums() {
		assertEquals(Config.FilterMode.NONE, Config.FilterMode.valueOf("NONE"));
		assertEquals(Config.FilterMode.BLACKLIST, Config.FilterMode.valueOf("BLACKLIST"));
		assertEquals(Config.FilterMode.WHITELIST, Config.FilterMode.valueOf("WHITELIST"));

		assertEquals(Config.AnimationMode.ARC_HOP, Config.AnimationMode.valueOf("ARC_HOP"));
		assertEquals(Config.AnimationMode.SPIRAL, Config.AnimationMode.valueOf("SPIRAL"));
		assertEquals(Config.AnimationMode.MAGNETIC, Config.AnimationMode.valueOf("MAGNETIC"));
		assertEquals(Config.AnimationMode.FLOAT, Config.AnimationMode.valueOf("FLOAT"));

		assertEquals(Config.ParticleTrail.PORTAL, Config.ParticleTrail.valueOf("PORTAL"));
		assertEquals(Config.ParticleTrail.ENCHANTED_HIT, Config.ParticleTrail.valueOf("ENCHANTED_HIT"));
		assertEquals(Config.ParticleTrail.FLAME, Config.ParticleTrail.valueOf("FLAME"));
		assertEquals(Config.ParticleTrail.ELECTRIC_SPARK, Config.ParticleTrail.valueOf("ELECTRIC_SPARK"));

		assertEquals(Config.TrailDensity.MEDIUM, Config.TrailDensity.valueOf("MEDIUM"));
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
		assertTrue(Config.radius >= 1.0 && Config.radius <= 2048.0);
		assertTrue(Config.strength > 0);
		assertTrue(Config.flightSpeed > 0);
		assertTrue(Config.enableCeilingClamping);
		assertTrue(Config.targetPocket);
		assertTrue(Config.smoothArrival);
	}
}
