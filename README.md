# MasterAttractor 🧲

A modern, highly customizable, and performance-friendly radius magnet mod for **Minecraft 1.21.11 Fabric**.

MasterAttractor effortlessly draws dropped items, experience orbs, projectiles, and optionally entities (mobs, boats, minecarts, falling blocks) directly toward the player. It is built natively for Fabric with full Cloth Config GUI integration and comprehensive in-game controls.

---

## ✨ Features

- **🚀 Versatile Attraction**:
  - **Dropped Items** (configurable to respect or bypass initial pickup delay)
  - **Experience Orbs** (instant absorption with subtle audio-visual feedback)
  - **Projectiles** (arrows, tridents, ender pearls, and throwable items)
  - **Living Mobs** (optional toggle to attract friendly or hostile creatures)
  - **Vehicles & Blocks** (optional attraction for Boats, Minecarts, and Falling Blocks like sand/gravel)
- **🎯 Smart Filtering & Inventory Protection**:
  - **Item Blacklist / Whitelist**: Filter out unwanted junk (dirt, cobblestone, rotten flesh) or pull only high-value materials.
  - **Inventory Overflow Protection**: Stops pulling items when your inventory is completely full, eliminating endless item orbiting and bouncing.
  - **Vacuum Mode**: Option to instantly insert items directly into player inventory upon attraction.
- **⚙️ Deeply Customizable**:
  - Live radius adjustment ($1$ to $128$ blocks).
  - Configurable pull strength / velocity curves.
  - Tick interval throttling ($1$ tick = smoothest, higher = ultra-lightweight performance).
  - Sneak suppression: simply hold `Shift` to temporarily pause the magnet while sorting inventories or building.
  - Creative / invulnerable mode bypass toggle.
- **🎨 Visual & Audio Feedback**:
  - Optional portal particle trail showing incoming attracted entities.
  - Smoothed, throttled audio chime on attraction (prevents sound flooding).
- **🖥️ In-Game Configuration GUI**:
  - Seamless integration with **Mod Menu** and **Cloth Config**.
  - Configure every single setting visually without leaving your game or touching JSON files.
- **⌨️ Instant Keybind & Commands**:
  - Press **`V`** (configurable in Controls) for instant on/off toggling with action-bar confirmation.
  - In-game commands with live auto-completion for operators and players.
- **🌐 Full Localization**:
  - Ready for multi-language translations (`en_us.json` baseline).

---

## 🎮 Controls & Usage

### Keybind
| Key | Action | Description |
|---|---|---|
| **`V`** *(default)* | Toggle Magnet | Instantly activates or deactivates MasterAttractor with an on-screen HUD notice. |

> Rebindable anytime in **Options ➔ Controls ➔ Key Binds ➔ MasterAttractor**.

### In-Game Commands
| Command | Permission | Description |
|---|---|---|
| `/attractor toggle` | All Players | Toggles personal attraction on/off. |
| `/attractor reload` | Operators (Level 2) | Reloads the configuration file from disk live without restarting. |
| `/attractor radius <value>` | Operators (Level 2) | Sets the magnet search radius (e.g., `/attractor radius 24`). |
| `/attractor strength <value>` | Operators (Level 2) | Sets the attraction acceleration force (e.g., `/attractor strength 2.0`). |

---

## 🔧 Configuration

MasterAttractor can be configured in two ways:

### 1. In-Game GUI (Mod Menu)
1. Open the pause menu or title screen and select **Mods**.
2. Select **MasterAttractor** from the list.
3. Click the **Configure** button to open the Cloth Config interface.
4. Settings are divided into three clean tabs:
   - **General**: Master toggle, Radius, Strength, Tick Interval.
   - **Targets**: Items, XP, Projectiles, Mobs, Boats, Minecarts, Falling Blocks.
   - **Behavior & Effects**: Pickup delay respect, God mode bypass, Audio & Particles.

### 2. Configuration File
The configuration file is stored in your instance folder at:
```
config/masterattractor.json
```

```json
{
  "enabled": true,
  "radius": 10.0,
  "strength": 1.25,
  "tickInterval": 1,
  "attractItems": true,
  "attractXpOrbs": true,
  "attractProjectiles": true,
  "attractMobs": false,
  "attractPlayers": false,
  "attractBoats": false,
  "attractMinecarts": false,
  "attractFallingBlocks": false,
  "ignorePlayersWithGodMode": true,
  "onlyPullToNearestPlayer": false,
  "respectPickupDelay": false,
  "playSoundOnPull": true,
  "spawnParticlesOnPull": true,
  "showRadiusOverlay": true,
  "overlayColor": 65280,
  "overlayOpacity": 0.3
}
```

---

## 📦 Requirements

- **Minecraft**: `1.21.11`
- **Fabric Loader**: `0.20.1` or higher
- **Fabric API**: `0.141.6+1.21.11` or higher
- **Java**: `21` or `25`
- **Recommended Client Mods**:
  - [Mod Menu](https://modrinth.com/mod/modmenu)
  - [Cloth Config](https://modrinth.com/mod/cloth-config)

---

## 🛠️ Building from Source

To compile the mod JAR yourself:

1. Clone or download the repository:
   ```bash
   git clone https://github.com/your-username/masterattractor.git
   cd masterattractor
   ```
2. Build with Gradle:
   ```bash
   ./gradlew build
   ```
3. The compiled and remapped JAR will be generated in `build/libs/`:
   ```
   build/libs/masterattractor-1.0.0+1.21.11.jar
   ```

---

## 📄 License

This mod is available under the **MIT License**. Feel free to use it in modpacks or customize it to your needs!
