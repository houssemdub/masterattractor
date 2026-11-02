package dev.masterattractor.attraction;

import dev.masterattractor.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
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

import java.util.ArrayList;
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

		// Clean up stale flight tracking entries periodically
		if (tickCounter % 200 == 0) {
			flightTicks.entrySet().removeIf(entry -> entry.getValue() > 600);
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!isPlayerAttracting(player)) continue;
			attractEntitiesForPlayer(server, player);
		}
	}

	private static void attractEntitiesForPlayer(MinecraftServer server, ServerPlayer player) {
		ServerLevel level = player.level();
		if (level == null) return;

		List<Entity> targets;
		double rSq = Config.radius * Config.radius;

		// Handle huge radius values (up to 2048) efficiently:
		// When radius > 128, scanning all loaded entities in memory avoids searching tens of thousands of chunk sections
		if (Config.radius > 128.0) {
			targets = new ArrayList<>();
			for (Entity e : level.getAllEntities()) {
				if (e != player && e.distanceToSqr(player) <= rSq && canEntityBeAttracted(e, player)) {
					targets.add(e);
				}
			}
		} else {
			AABB searchBox = AABB.ofSize(player.position(), Config.radius * 2.0, Config.radius * 2.0, Config.radius * 2.0);
			Predicate<Entity> filter = entity -> canEntityBeAttracted(entity, player);
			targets = level.getEntities((Entity) null, searchBox, filter);
		}

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
				(float) Config.soundVolume,
				(float) Config.soundPitch
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
		// Target either waist/pocket height (0.75m above feet) or eye position
		Vec3 targetPos = Config.targetPocket ? player.position().add(0, 0.75, 0) : player.getEyePosition();
		Vec3 entityPos = entity.position();

		Vec3 toTarget = targetPos.subtract(entityPos);
		double distanceSq = toTarget.lengthSqr();
		if (distanceSq < 0.02) return false;

		double distance = Math.sqrt(distanceSq);

		// Handle ItemEntity with butter-smooth flight physics, animations, and safe pocket absorption
		if (entity instanceof ItemEntity item) {
			// Enable no-clip through blocks so items never get caught on terrain, walls, or fences
			item.noPhysics = true;

			ItemStack stack = item.getItem().copy();
			int countBefore = stack.getCount();

			// Direct pocket absorption: happens either if instantVacuum is explicitly enabled
			// OR when the item physically arrives at the player's pocket (distance < 0.45m)
			if (Config.instantVacuum || distanceSq < 0.20) {
				player.getInventory().add(stack);
				int inserted = countBefore - stack.getCount();
				if (inserted > 0) {
					if (Config.playArrivalChime) {
						level.playSound(
							null,
							player.blockPosition(),
							SoundEvents.ITEM_PICKUP,
							SoundSource.PLAYERS,
							(float) Config.soundVolume,
							(float) Config.soundPitch
						);
					}
					if (Config.arrivalParticles) {
						level.sendParticles(ParticleTypes.CRIT, targetPos.x, targetPos.y, targetPos.z, 3, 0.1, 0.1, 0.1, 0.05);
					}
					if (stack.isEmpty()) {
						item.discard();
						flightTicks.remove(item.getUUID());
					} else {
						item.setItem(stack);
					}
					return true;
				}
			}

			// Track flight progress for animation curves
			int ticks = flightTicks.compute(item.getUUID(), (k, v) -> v == null ? 0 : v + 1);

			// Check ceiling clearance if clamping is enabled
			int clearance = 4;
			if (Config.enableCeilingClamping) {
				BlockPos blockPos = item.blockPosition();
				clearance = 0;
				for (int dy = 1; dy <= 4; dy++) {
					if (!level.getBlockState(blockPos.above(dy)).isAir()) {
						break;
					}
					clearance++;
				}
			}

			Vec3 dir = toTarget.normalize();
			Vec3 lateralDir = new Vec3(-dir.z, 0, dir.x);
			if (lateralDir.lengthSqr() > 1e-4) {
				lateralDir = lateralDir.normalize();
			}

			long hash = item.getUUID().getMostSignificantBits();
			double baseSpeed = Math.clamp(Config.flightSpeed * 0.55, 0.15, 1.8);
			Vec3 desiredVel;

			switch (Config.animationMode) {
				case ARC_HOP -> {
					// Graceful launch hop clamped by roof height, curving smoothly into the pocket
					double maxHop = (clearance == 0) ? 0.04 : Math.min(0.42, 0.10 + clearance * 0.08) * Config.jumpStrength;
					double hopY = (ticks < 8) ? (1.0 - (ticks / 8.0)) * maxHop : 0.0;
					double latFactor = (((hash & 0xFFFF) / 32767.5) - 1.0) * 0.25;
					double convergence = Math.clamp(distance / 3.0, 0.0, 1.0);
					Vec3 lateral = lateralDir.scale(latFactor * convergence);
					desiredVel = dir.scale(baseSpeed).add(lateral).add(0, hopY, 0);
				}
				case SPIRAL -> {
					// Swirls around the player in a decreasing vortex funneling into the pocket
					double angle = (ticks * 0.35) + ((hash & 0xFF) * 0.08);
					double orbitRadius = Math.clamp(distance * 0.35, 0.15, 2.0);
					Vec3 spiralOffset = new Vec3(Math.cos(angle) * orbitRadius, 0, Math.sin(angle) * orbitRadius);
					Vec3 spiralTarget = targetPos.add(spiralOffset);
					desiredVel = spiralTarget.subtract(item.position()).normalize().scale(baseSpeed);
				}
				case MAGNETIC -> {
					// High-tech tractor beam with exponential magnetic ease-in
					double accel = 1.0 + (1.2 / Math.max(0.6, distance));
					desiredVel = dir.scale(baseSpeed * accel);
				}
				case FLOAT -> {
					// Gentle wave drifting motion
					double bob = Math.sin((ticks * 0.3) + (hash & 0xF)) * 0.08;
					desiredVel = dir.scale(baseSpeed * 0.85).add(0, bob, 0);
				}
				default -> desiredVel = dir.scale(baseSpeed);
			}

			// Smooth velocity blending: 40% current inertia + 60% desired trajectory
			// This completely eliminates teleporting/snapping artifacts and creates a gorgeous, organic flight path!
			Vec3 currentVel = item.getDeltaMovement();
			Vec3 smoothedVel = currentVel.scale(0.40).add(desiredVel.scale(0.60));

			// Clamp extreme acceleration
			if (smoothedVel.lengthSqr() > 2.25) {
				smoothedVel = smoothedVel.normalize().scale(1.5);
			}
			item.setDeltaMovement(smoothedVel);

		} else if (entity instanceof ExperienceOrb xp) {
			xp.noPhysics = true;
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			xp.setDeltaMovement(xp.getDeltaMovement().scale(0.5).add(pullVec.scale(0.35)));

		} else if (entity instanceof Projectile proj) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			proj.setDeltaMovement(proj.getDeltaMovement().scale(0.5).add(pullVec.scale(0.2)));

		} else if (entity instanceof Mob mob) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			mob.setDeltaMovement(mob.getDeltaMovement().scale(0.5).add(pullVec.scale(0.15)));

		} else if (entity instanceof Boat boat) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			boat.setDeltaMovement(boat.getDeltaMovement().scale(0.5).add(pullVec.scale(0.2)));

		} else if (entity instanceof Minecart cart) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			cart.setDeltaMovement(cart.getDeltaMovement().scale(0.5).add(pullVec.scale(0.2)));

		} else if (entity instanceof FallingBlockEntity fb) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			fb.setDeltaMovement(fb.getDeltaMovement().scale(0.5).add(pullVec.scale(0.2)));

		} else {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5).add(pullVec.scale(0.15)));
		}

		// Emit customizable particle trails according to trail density
		if (Config.particleTrail != Config.ParticleTrail.NONE && Config.trailDensity != Config.TrailDensity.OFF) {
			SimpleParticleType particle = getParticleType(Config.particleTrail);
			if (particle != null) {
				boolean shouldEmit = switch (Config.trailDensity) {
					case LOW -> tickCounter % 4 == 0;
					case MEDIUM -> tickCounter % 2 == 0;
					case HIGH, ULTRA -> true;
					case OFF -> false;
				};
				if (shouldEmit) {
					int count = (Config.trailDensity == Config.TrailDensity.ULTRA) ? 2 : 1;
					level.sendParticles(
						particle,
						entity.getX(), entity.getY() + 0.15, entity.getZ(),
						count,
						0.02, 0.02, 0.02,
						0.01
					);
				}
			}
		}

		return true;
	}

	private static SimpleParticleType getParticleType(Config.ParticleTrail trail) {
		return switch (trail) {
			case PORTAL -> ParticleTypes.PORTAL;
			case ENCHANTED_HIT -> ParticleTypes.ENCHANTED_HIT;
			case FLAME -> ParticleTypes.FLAME;
			case SOUL_FIRE_FLAME -> ParticleTypes.SOUL_FIRE_FLAME;
			case ELECTRIC_SPARK -> ParticleTypes.ELECTRIC_SPARK;
			case GLOW -> ParticleTypes.GLOW;
			case END_ROD -> ParticleTypes.END_ROD;
			case HEART -> ParticleTypes.HEART;
			case NAUTILUS -> ParticleTypes.NAUTILUS;
			case TOTEM_OF_UNDYING -> ParticleTypes.TOTEM_OF_UNDYING;
			case CHERRY_LEAVES -> ParticleTypes.CHERRY_LEAVES;
			case BUBBLE -> ParticleTypes.BUBBLE;
			case NONE -> null;
		};
	}
}