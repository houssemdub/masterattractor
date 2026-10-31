# MasterAttractor — Architecture & Developer Documentation

This document covers the architectural layout, entity vector calculation formulas, networking protocols, and extension guidelines for **MasterAttractor**.

---

## 🏛️ Project Architecture

```
masterattractor/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src/
│   ├── client/
│   │   └── java/dev/masterattractor/client/
│   │       ├── MasterAttractorClient.java      # Client entrypoint, keybind listener, HUD overlay
│   │       └── MasterAttractorModMenu.java     # ModMenu & Cloth Config ScreenFactory
│   └── main/
│       ├── java/dev/masterattractor/
│       │   ├── MasterAttractor.java            # ModInitializer, networking registration, server tick
│       │   ├── attraction/
│       │   │   └── AttractionManager.java      # Core bounding-box search and vector attraction loop
│       │   ├── command/
│       │   │   └── AttractorCommand.java       # Brigadier command registration (/attractor)
│       │   ├── config/
│       │   │   └── Config.java                 # JSON serialisation & runtime config storage
│       │   └── network/
│       │       ├── SyncStatePayload.java       # S2C sync packet payload
│       │       └── TogglePayload.java          # C2S toggle packet payload
│       └── resources/
│           ├── assets/masterattractor/icon.png
│           └── fabric.mod.json                 # Mod metadata & entrypoints
├── build.gradle                                # Fabric Loom configuration & dependencies
├── gradle.properties                           # Toolchain & Minecraft version properties
├── settings.gradle
├── README.md
└── DOC.md
```

---

## ⚡ Core Attraction Mechanics

### 1. Bounding Box & Target Scan
Entities are gathered during server tick events via `ServerTickEvents.END_SERVER_TICK` using Minecraft's spatial level query:

$$\text{searchBox} = \text{AABB.ofSize}(player.position(), 2 \times \text{radius}, 2 \times \text{radius}, 2 \times \text{radius})$$

```java
List<Entity> targets = level.getEntities((Entity) null, searchBox, filter);
```

### 2. Attraction Physics Equation
The vector pulling entities toward the player's eye position is computed as follows:

$$\vec{v}_{\text{toPlayer}} = \vec{p}_{\text{eye}} - \vec{p}_{\text{entity}}$$

$$d = \|\vec{v}_{\text{toPlayer}}\| = \sqrt{\Delta x^2 + \Delta y^2 + \Delta z^2}$$

$$\text{pullFactor} = \frac{\text{Config.strength}}{\max(1.0, d)}$$

$$\vec{F}_{\text{pull}} = \text{normalize}(\vec{v}_{\text{toPlayer}}) \times \text{pullFactor}$$

The resulting vector is scaled according to entity mass and air resistance:
- **Items & XP Orbs**: $\vec{F}_{\text{pull}} \times 0.35$
- **Projectiles, Boats & Minecarts**: $\vec{F}_{\text{pull}} \times 0.20$
- **Living Mobs**: $\vec{F}_{\text{pull}} \times 0.15$

Entities with $d^2 < 0.04$ are ignored to prevent erratic oscillation around the player's eye coordinates.

---

## 📡 Networking Protocol (Fabric Networking v1)

MasterAttractor uses Minecraft 1.21's custom packet payload system:

### 1. `masterattractor:toggle` (C2S)
- **Sender**: Client
- **Trigger**: When player presses the toggle keybind (`V`).
- **Handler**: Flips player attraction state in `AttractionManager`, notifies the player via an action-bar message, and replies with `SyncStatePayload`.

### 2. `masterattractor:sync_state` (S2C)
- **Sender**: Server
- **Payload**: `boolean enabled`
- **Trigger**: Sent upon player join (`ServerPlayConnectionEvents.JOIN`) or after state toggle.
- **Handler**: Updates client-side `isAttractorActive` flag and renders status message via HUD action bar.

---

## 🛠️ Extending MasterAttractor

### Adding a New Entity Target
To add a new target category:
1. Add a boolean flag in `dev.masterattractor.config.Config`:
   ```java
   public static boolean attractCustomEntity = true;
   ```
2. Add the type filter check in `AttractionManager#canEntityBeAttracted`:
   ```java
   if (entity instanceof CustomEntity) return Config.attractCustomEntity;
   ```
3. Add a slider or toggle entry to `MasterAttractorModMenu` in the **Targets** category.

### Building
Compile with official Mojang mappings using:
```bash
./gradlew build
```
The output jar will be in `build/libs/masterattractor-1.0.0+1.21.11.jar`.
