package dev.masterattractor.attraction;

import dev.masterattractor.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class AttractionManager {

	private static long tickCounter = 0;

	// Per-player attraction state using thread-safe ConcurrentHashMap
	private static final Map<UUID, Boolean> playerStates = new ConcurrentHashMap<>();

	// Per-entity flight tick tracking for unique trajectories and jump animation
	private static final Map<UUID, Integer> flightTicks = new ConcurrentHashMap<>();

	public static boolean isPlayerAttracting(ServerPlayer player) {
		return playerStates.computeIfAbsent(player.getUUID(), id -> Config.enabled);
	}

	public static boolean togglePlayer(ServerPlayer player) {
		boolean newState = !playerStates.getOrDefault(player.getUUID(), Config.enabled);
		playerStates.put(player.getUUID(), newState);
		return newState;
	}

	public static void onPlayerDisconnect(ServerPlayer player) {
		playerStates.remove(player.getUUID());
	}

	public static void tick(MinecraftServer server) {
		tickCounter++;
		if (tickCounter % Config.tickInterval != 0) return;

		// Clean up flight ticks occasionally
		if (tickCounter % 200 == 0) {
			flightTicks.entrySet().removeIf(entry -> entry.getValue() > 400);
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!isPlayerAttracting(player)) continue;
			attractEntitiesForPlayer(server, player);
		}
	}

	private static void attractEntitiesForPlayer(MinecraftServer server, ServerPlayer player) {
		ServerLevel level = player.level();
		if (level == null) return;

		AABB searchBox = AABB.ofSize(player.position(), Config.radius * 2.0, Config.radius * 2.0, Config.radius * 2.0);
		Predicate<Entity> filter = entity -> canEntityBeAttracted(entity, player);

		List<Entity> targets = level.getEntities((Entity) null, searchBox, filter);
		boolean anyPulled = false;

		for (Entity entity : targets) {
			if (entity == player) continue;
			if (shouldSkip(entity, player)) continue;

			// Handle onlyPullToNearestPlayer if enabled
			if (Config.onlyPullToNearestPlayer) {
				double myDistSq = player.distanceToSqr(entity);
				boolean someoneCloser = false;
				for (ServerPlayer other : server.getPlayerList().getPlayers()) {
					if (other != player && other.level() == level && isPlayerAttracting(other)) {
						if (other.distanceToSqr(entity) < myDistSq) {
							someoneCloser = true;
							break;
						}
					}
				}
				if (someoneCloser) continue;
			}

			if (pullEntityToward(entity, player, level)) {
				anyPulled = true;
			}
		}

		// Play sound only ONCE per player cycle to prevent ear-destroying sound stacking
		if (anyPulled && Config.playSoundOnPull && tickCounter % 10 == 0) {
			level.playSound(
				null,
				player.blockPosition(),
				SoundEvents.EXPERIENCE_ORB_PICKUP,
				SoundSource.PLAYERS,
				0.25f,
				1.2f
			);
		}
	}

	private static boolean canPlayerHold(Player player, ItemStack stack) {
		Inventory inv = player.getInventory();
		if (inv.getFreeSlot() != -1) {
			return true;
		}
		// Check main inventory slots (0 to 35: hotbar + main inventory)
		for (int i = 0; i < 36; i++) {
			ItemStack existing = inv.getItem(i);
			if (existing.isEmpty()) return true;
			if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
				return true;
			}
		}
		return false;
	}

	private static boolean canEntityBeAttracted(Entity entity, ServerPlayer player) {
		if (entity instanceof ItemEntity item) {
			if (!Config.attractItems) return false;
			if (Config.respectPickupDelay && item.hasPickUpDelay()) return false;

			ItemStack stack = item.getItem();
			if (stack.isEmpty()) return false;

			// Item filter check (Whitelist / Blacklist) using O(1) set lookup
			if (Config.filterMode != Config.FilterMode.NONE) {
				Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
				String idStr = itemId.toString();
				boolean isListed = Config.isItemFiltered(idStr);

				if (Config.filterMode == Config.FilterMode.BLACKLIST && isListed) {
					return false;
				}
				if (Config.filterMode == Config.FilterMode.WHITELIST && !isListed) {
					return false;
				}
			}

			// Inventory space check (prevents endless swirling/bouncing when inventory is full)
			if (Config.checkInventorySpace && !canPlayerHold(player, stack)) {
				return false;
			}

			return true;
		}
		if (entity instanceof ExperienceOrb) {
			return Config.attractXpOrbs;
		}
		if (entity instanceof Projectile proj) {
			if (!Config.attractProjectiles) return false;
			// Never attract player's own shot projectiles (arrows, ender pearls, tridents)
			if (proj.getOwner() == player) return false;
			// Never attract arrows stuck in ground
			if (proj instanceof AbstractArrow arrow && (arrow.onGround() || arrow.shakeTime > 0)) return false;
			return true;
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
		if (player.isShiftKeyDown() || player.isCrouching()) return true;
		if (Config.ignorePlayersWithGodMode && (player.isCreative() || player.isInvulnerable() || player.getAbilities().invulnerable)) return true;
		if (player.getRootVehicle() == entity.getRootVehicle()) return true;
		return false;
	}

	private static boolean pullEntityToward(Entity entity, ServerPlayer player, ServerLevel level) {
		Vec3 playerPos = player.getEyePosition();
		Vec3 entityPos = entity.position();

		Vec3 toPlayer = playerPos.subtract(entityPos);
		double distanceSq = toPlayer.lengthSqr();
		if (distanceSq < 0.04) return false;

		double distance = Math.sqrt(distanceSq);

		// Handle ItemEntity with no-clip, unique curved flight path, roof height clamping, and safe vacuuming
		if (entity instanceof ItemEntity item) {
			// Enable no-clip through blocks so items never get stuck in walls, ceilings, or terrain
			item.noPhysics = true;

			ItemStack stack = item.getItem().copy();
			int countBefore = stack.getCount();

			// Vacuum mode or within direct grab range: safely insert into inventory (no dupe bug)
			if (Config.vacuumMode || distanceSq < 0.36) {
				player.getInventory().add(stack);
				int inserted = countBefore - stack.getCount();
				if (inserted > 0) {
					level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f);
					if (stack.isEmpty()) {
						item.discard();
						flightTicks.remove(item.getUUID());
					} else {
						item.setItem(stack);
					}
					return true;
				}
			}

			// Track flight progress for jump arc & path curvature
			int ticks = flightTicks.compute(item.getUUID(), (k, v) -> v == null ? 0 : v + 1);

			// Check vertical clearance above the item to clamp jump height under ceilings
			BlockPos blockPos = item.blockPosition();
			int clearance = 0;
			for (int dy = 1; dy <= 4; dy++) {
				if (!level.getBlockState(blockPos.above(dy)).isAir()) {
					break;
				}
				clearance++;
			}

			// Initial jump hop clamped by ceiling height
			double maxJump = (clearance == 0) ? 0.05 : Math.min(0.40, 0.12 + clearance * 0.07);
			double jumpY = (ticks < 6) ? (1.0 - (ticks / 6.0)) * maxJump : 0.0;

			// Deterministic unique lateral curve for each individual item using its UUID hash
			long hash = item.getUUID().getMostSignificantBits();
			double lateralFactor = (((hash & 0xFFFF) / 32767.5) - 1.0); // range -1.0 to 1.0

			Vec3 dir = toPlayer.normalize();
			Vec3 lateralDir = new Vec3(-dir.z, 0, dir.x);
			if (lateralDir.lengthSqr() > 1e-4) {
				lateralDir = lateralDir.normalize();
			}

			// Lateral curve converges smoothly as the item approaches player
			double convergence = Math.clamp(distance / 4.0, 0.0, 1.0);
			Vec3 lateralOffset = lateralDir.scale(lateralFactor * 0.30 * convergence);

			// Straight-to-inventory flight speed
			double speed = Math.clamp(Config.strength * 0.45, 0.30, 1.25);
			Vec3 targetVelocity = dir.scale(speed).add(lateralOffset).add(0, jumpY, 0);

			item.setDeltaMovement(targetVelocity);

		} else if (entity instanceof ExperienceOrb xp) {
			xp.noPhysics = true;
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toPlayer.normalize().scale(pullFactor);
			xp.setDeltaMovement(xp.getDeltaMovement().add(pullVec.scale(0.35)));

		} else if (entity instanceof Projectile proj) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toPlayer.normalize().scale(pullFactor);
			proj.setDeltaMovement(proj.getDeltaMovement().add(pullVec.scale(0.2)));

		} else if (entity instanceof Mob mob) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toPlayer.normalize().scale(pullFactor);
			mob.setDeltaMovement(mob.getDeltaMovement().add(pullVec.scale(0.15)));

		} else if (entity instanceof Boat boat) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toPlayer.normalize().scale(pullFactor);
			boat.setDeltaMovement(boat.getDeltaMovement().add(pullVec.scale(0.2)));

		} else if (entity instanceof Minecart cart) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toPlayer.normalize().scale(pullFactor);
			cart.setDeltaMovement(cart.getDeltaMovement().add(pullVec.scale(0.2)));

		} else if (entity instanceof FallingBlockEntity fb) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toPlayer.normalize().scale(pullFactor);
			fb.setDeltaMovement(fb.getDeltaMovement().add(pullVec.scale(0.2)));

		} else {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toPlayer.normalize().scale(pullFactor);
			entity.setDeltaMovement(entity.getDeltaMovement().add(pullVec.scale(0.15)));
		}

		// Throttled particle emission (avoids packet flood)
		if (Config.spawnParticlesOnPull && tickCounter % 4 == 0) {
			level.sendParticles(
				ParticleTypes.PORTAL,
				entity.getX(), entity.getY() + 0.2, entity.getZ(),
				1,
				0.05, 0.05, 0.05,
				0.02
			);
		}

		return true;
	}
}