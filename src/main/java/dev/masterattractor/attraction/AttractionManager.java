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

	// State tracker for smooth, continuous flight curves per item
	public static final class FlightState {
		public int ticks = 0;
		public int clearance = 4;
		public double lateralFactor = 0.0;
		public Vec3 smoothedVelocity = Vec3.ZERO;
		public boolean initialized = false;
	}

	private static final Map<UUID, FlightState> flightStates = new ConcurrentHashMap<>();

	public static boolean isItemBeingAttracted(UUID uuid) {
		return flightStates.containsKey(uuid);
	}

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
		if (tickCounter % 100 == 0) {
			flightStates.entrySet().removeIf(entry -> entry.getValue().ticks > 600);
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
		// Target either waist/pocket height (0.75m above player feet) or eye position
		Vec3 targetPos = Config.targetPocket ? player.position().add(0, 0.75, 0) : player.getEyePosition();
		Vec3 entityPos = entity.position();

		Vec3 toTarget = targetPos.subtract(entityPos);
		double distanceSq = toTarget.lengthSqr();
		if (distanceSq < 0.01) return false;

		double distance = Math.sqrt(distanceSq);

		// Handle ItemEntity with true no-clip through blocks, continuous parabolic curves, and zero stutter
		if (entity instanceof ItemEntity item) {
			// Mark noPhysics and noGravity so vanilla physics and block collisions are 100% disabled
			item.noPhysics = true;
			item.setNoGravity(true);

			ItemStack stack = item.getItem().copy();
			int countBefore = stack.getCount();

			// Direct pocket absorption: occurs if instantVacuum is on OR when the item reaches the pocket (distance < 0.38m)
			if (Config.instantVacuum || distance < 0.38) {
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
						flightStates.remove(item.getUUID());
					} else {
						item.setItem(stack);
					}
					return true;
				}
			}

			// Get or initialize persistent flight state
			FlightState state = flightStates.computeIfAbsent(item.getUUID(), id -> {
				FlightState s = new FlightState();
				long h = id.getMostSignificantBits();
				s.lateralFactor = (((h & 0xFFFF) / 32767.5) - 1.0) * 0.25;

				// Lock in initial ceiling clearance so moving under individual blocks does not jitter the arc
				if (Config.enableCeilingClamping) {
					BlockPos bPos = item.blockPosition();
					int c = 0;
					for (int dy = 1; dy <= 4; dy++) {
						if (!level.getBlockState(bPos.above(dy)).isAir()) break;
						c++;
					}
					s.clearance = c;
				} else {
					s.clearance = 4;
				}
				s.initialized = true;
				return s;
			});

			state.ticks++;

			Vec3 dir = toTarget.normalize();
			Vec3 lateralDir = new Vec3(-dir.z, 0, dir.x);
			if (lateralDir.lengthSqr() > 1e-4) {
				lateralDir = lateralDir.normalize();
			}

			// Base flight speed with smooth arrival braking (critically damped)
			double baseSpeed = Math.clamp(Config.flightSpeed * 0.52, 0.20, 1.40);
			if (distance < 0.90) {
				baseSpeed *= Math.clamp(distance / 0.90, 0.30, 1.0);
			}

			Vec3 desiredVel;

			switch (Config.animationMode) {
				case ARC_HOP -> {
					// Pure continuous parabolic arc: f(u) = 4u(1-u), derivative v_y = 4(1 - 2u)
					// Starts at 0, peaks smoothly in the middle, and descends cleanly into the pocket
					double totalArcTicks = 18.0;
					double u = Math.min(1.0, state.ticks / totalArcTicks);
					double maxHopScale = (state.clearance == 0) ? 0.02 : Math.min(0.12, 0.03 + state.clearance * 0.02) * Config.jumpStrength;
					double vHop = (u < 1.0) ? (4.0 * (1.0 - 2.0 * u) * maxHopScale) : 0.0;

					// Lateral sway: smooth half-sine wave, zero at start and zero at arrival
					double vLat = Math.sin(u * Math.PI) * state.lateralFactor * Math.min(1.0, distance / 2.0);

					desiredVel = dir.scale(baseSpeed).add(lateralDir.scale(vLat)).add(0, vHop, 0);
				}
				case SPIRAL -> {
					// Smooth orbiting vortex spiraling inwards into the pocket
					double angle = (state.ticks * 0.28) + (state.lateralFactor * 4.0);
					double orbitRadius = Math.clamp(distance * 0.32, 0.10, 1.80);
					Vec3 spiralOffset = new Vec3(Math.cos(angle) * orbitRadius, 0, Math.sin(angle) * orbitRadius);
					Vec3 spiralTarget = targetPos.add(spiralOffset);
					desiredVel = spiralTarget.subtract(item.position()).normalize().scale(baseSpeed);
				}
				case MAGNETIC -> {
					// Tractor beam with smooth exponential magnetic pull
					double accel = 1.0 + (0.8 / Math.max(0.4, distance));
					desiredVel = dir.scale(baseSpeed * accel);
				}
				case FLOAT -> {
					// Smooth floating wave
					double bob = Math.sin(state.ticks * 0.25) * 0.06;
					desiredVel = dir.scale(baseSpeed * 0.85).add(0, bob, 0);
				}
				default -> desiredVel = dir.scale(baseSpeed);
			}

			// Apply exponential velocity smoothing to eliminate all micro-stutters
			if (state.smoothedVelocity == null || state.smoothedVelocity.lengthSqr() < 1e-4) {
				state.smoothedVelocity = desiredVel;
			} else {
				state.smoothedVelocity = state.smoothedVelocity.scale(0.20).add(desiredVel.scale(0.80));
			}

			// Directly assign smoothed velocity and notify network tracking
			item.setDeltaMovement(state.smoothedVelocity);
			item.hurtMarked = true;
			item.needsSync = true;

		} else if (entity instanceof ExperienceOrb xp) {
			xp.noPhysics = true;
			xp.setNoGravity(true);
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			xp.setDeltaMovement(xp.getDeltaMovement().scale(0.5).add(pullVec.scale(0.35)));
			xp.hurtMarked = true;
			xp.needsSync = true;

		} else if (entity instanceof Projectile proj) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			proj.setDeltaMovement(proj.getDeltaMovement().scale(0.5).add(pullVec.scale(0.2)));
			proj.hurtMarked = true;
			proj.needsSync = true;

		} else if (entity instanceof Mob mob) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			mob.setDeltaMovement(mob.getDeltaMovement().scale(0.5).add(pullVec.scale(0.15)));
			mob.hurtMarked = true;

		} else if (entity instanceof Boat boat) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			boat.setDeltaMovement(boat.getDeltaMovement().scale(0.5).add(pullVec.scale(0.2)));
			boat.hurtMarked = true;

		} else if (entity instanceof Minecart cart) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			cart.setDeltaMovement(cart.getDeltaMovement().scale(0.5).add(pullVec.scale(0.2)));
			cart.hurtMarked = true;

		} else if (entity instanceof FallingBlockEntity fb) {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			fb.setDeltaMovement(fb.getDeltaMovement().scale(0.5).add(pullVec.scale(0.2)));
			fb.hurtMarked = true;

		} else {
			double pullFactor = Config.strength / Math.max(1.0, distance);
			Vec3 pullVec = toTarget.normalize().scale(pullFactor);
			entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5).add(pullVec.scale(0.15)));
			entity.hurtMarked = true;
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