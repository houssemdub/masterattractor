package dev.masterattractor.attraction;

import dev.masterattractor.config.Config;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class AttractionManager {

	private static long tickCounter = 0;

	// Per-player attraction state
	private static final Map<ServerPlayer, Boolean> playerStates = new HashMap<>();

	public static boolean isPlayerAttracting(ServerPlayer player) {
		return playerStates.computeIfAbsent(player, p -> Config.enabled);
	}

	public static boolean togglePlayer(ServerPlayer player) {
		boolean newState = !playerStates.getOrDefault(player, Config.enabled);
		playerStates.put(player, newState);
		return newState;
	}

	public static void tick(MinecraftServer server) {
		tickCounter++;
		if (tickCounter % Config.tickInterval != 0) return;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!isPlayerAttracting(player)) continue;
			attractEntitiesForPlayer(player);
		}
	}

	private static void attractEntitiesForPlayer(ServerPlayer player) {
		ServerLevel level = player.level();
		if (level == null) return;

		AABB searchBox = AABB.ofSize(player.position(), Config.radius * 2.0, Config.radius * 2.0, Config.radius * 2.0);
		Predicate<Entity> filter = entity -> canEntityBeAttracted(entity, player);

		List<Entity> targets = level.getEntities((Entity) null, searchBox, filter);

		for (Entity entity : targets) {
			if (entity == player) continue;
			if (shouldSkip(entity, player)) continue;
			pullEntityToward(entity, player, level);
		}
	}

	private static boolean canEntityBeAttracted(Entity entity, ServerPlayer player) {
		if (entity instanceof ItemEntity item) {
			if (!Config.attractItems) return false;
			if (Config.respectPickupDelay && item.hasPickUpDelay()) return false;
			return true;
		}
		if (entity instanceof ExperienceOrb) {
			return Config.attractXpOrbs;
		}
		if (entity instanceof AbstractArrow || entity instanceof Projectile) {
			return Config.attractProjectiles;
		}
		if (entity instanceof Mob) {
			return Config.attractMobs;
		}
		if (entity instanceof Player) {
			return Config.attractPlayers;
		}
		if (entity instanceof Boat) {
			return Config.attractBoats;
		}
		if (entity instanceof Minecart) {
			return Config.attractMinecarts;
		}
		if (entity instanceof FallingBlockEntity) {
			return Config.attractFallingBlocks;
		}
		return false;
	}

	private static boolean shouldSkip(Entity entity, ServerPlayer player) {
		if (player.isShiftKeyDown()) return true;
		if (Config.ignorePlayersWithGodMode && player.isInvulnerable()) return true;
		return false;
	}

	private static void pullEntityToward(Entity entity, ServerPlayer player, ServerLevel level) {
		Vec3 playerPos = player.getEyePosition();
		Vec3 entityPos = entity.position();

		Vec3 toPlayer = playerPos.subtract(entityPos);
		double distanceSq = toPlayer.lengthSqr();
		if (distanceSq < 0.04) return;

		double distance = Math.sqrt(distanceSq);
		double pullFactor = Config.strength / Math.max(1.0, distance);

		Vec3 pullVec = toPlayer.normalize().scale(pullFactor);

		if (entity instanceof ItemEntity item) {
			item.setDeltaMovement(item.getDeltaMovement().add(pullVec.scale(0.35)));
		} else if (entity instanceof ExperienceOrb xp) {
			xp.setDeltaMovement(xp.getDeltaMovement().add(pullVec.scale(0.35)));
		} else if (entity instanceof Projectile proj) {
			proj.setDeltaMovement(proj.getDeltaMovement().add(pullVec.scale(0.2)));
		} else if (entity instanceof Mob mob) {
			mob.setDeltaMovement(mob.getDeltaMovement().add(pullVec.scale(0.15)));
		} else if (entity instanceof Boat boat) {
			boat.setDeltaMovement(boat.getDeltaMovement().add(pullVec.scale(0.2)));
		} else if (entity instanceof Minecart cart) {
			cart.setDeltaMovement(cart.getDeltaMovement().add(pullVec.scale(0.2)));
		} else if (entity instanceof FallingBlockEntity fb) {
			fb.setDeltaMovement(fb.getDeltaMovement().add(pullVec.scale(0.2)));
		} else {
			entity.setDeltaMovement(entity.getDeltaMovement().add(pullVec.scale(0.15)));
		}

		if (Config.playSoundOnPull && tickCounter % 10 == 0) {
			level.playSound(
				null,
				entity.blockPosition(),
				SoundEvents.EXPERIENCE_ORB_PICKUP,
				SoundSource.PLAYERS,
				0.25f,
				1.2f
			);
		}

		if (Config.spawnParticlesOnPull && tickCounter % 2 == 0) {
			level.sendParticles(
				ParticleTypes.PORTAL,
				entity.getX(), entity.getY() + 0.3, entity.getZ(),
				2,
				0.1, 0.1, 0.1,
				0.05
			);
		}
	}
}
