<p align="center">
  <img src="Logo_Black_Bg.png" alt="MasterAttractor Logo" width="500">
</p>

<h1 align="center">MasterAttractor 🧲</h1>

<p align="center">
  <strong>A modern, highly customizable, and performance-friendly radius magnet mod for Minecraft Fabric.</strong>
</p>

<p align="center">
  <a href="https://github.com/houssemdub/masterattractor/releases"><img src="https://img.shields.io/github/v/release/houssemdub/masterattractor?color=blue&label=Release&style=for-the-badge" alt="GitHub Release"></a>
  <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Minecraft-1.21.11-brightgreen?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft Version"></a>
  <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Modloader-Fabric-dbb879?style=for-the-badge" alt="Fabric"></a>
  <a href="https://adoptium.net/"><img src="https://img.shields.io/badge/Java-21%2B-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21+"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/houssemdub/masterattractor?color=yellow&style=for-the-badge" alt="License: MIT"></a>
  <a href="https://github.com/houssemdub/masterattractor/actions"><img src="https://img.shields.io/github/actions/workflow/status/houssemdub/masterattractor/build.yml?branch=main&label=CI%20Build&style=for-the-badge" alt="Build Status"></a>
</p>

<p align="center">
  <a href="#-features">Features</a> •
  <a href="#-controls--usage">Controls</a> •
  <a href="#-smart-filtering">Item Filtering</a> •
  <a href="#-configuration">Configuration</a> •
  <a href="#-download--installation">Download</a> •
  <a href="#-preview">Preview</a>
</p>

---

## ✨ Features

- 🚀 **Versatile Attraction**:
  - **Dropped Items** (configurable to respect or bypass initial pickup delay)
  - **Experience Orbs** (instant absorption with subtle audio-visual feedback)
  - **Projectiles** (arrows, tridents, ender pearls, and throwable items)
  - **Living Mobs** (optional toggle to attract friendly or hostile creatures)
  - **Vehicles & Blocks** (optional attraction for Boats, Minecarts, and Falling Blocks like sand/gravel)
- 🎯 **Smart Filtering & Anti-Swirl Overflow Protection**:
  - **Item Blacklist / Whitelist**: Filter out unwanted junk (dirt, cobblestone, rotten flesh) or pull only high-value materials.
  - **Inventory Overflow Protection**: Stops pulling items when your inventory is completely full, eliminating endless item orbiting and bouncing.
  - **Vacuum Mode**: Option to instantly insert items directly into player inventory upon attraction.
- ⚙️ **Deeply Customizable**:
  - Live radius adjustment ($1$ to $128$ blocks).
  - Configurable pull strength / velocity curves.
  - Tick interval throttling ($1$ tick = smoothest, higher = ultra-lightweight performance).
  - Sneak suppression: simply hold `Shift` to temporarily pause the magnet while sorting inventories or building.
  - Creative / invulnerable mode bypass toggle.
- 🎨 **Visual & Audio Feedback**:
  - Optional portal particle trail showing incoming attracted entities.
  - Smoothed, throttled audio chime on attraction (prevents sound flooding).
- 🖥️ **In-Game Configuration GUI**:
  - Seamless integration with **Mod Menu** and **Cloth Config**.
  - Configure every single setting visually without leaving your game or touching JSON files.
- ⌨️ **Instant Keybind & Commands**:
  - Press **`V`** (configurable in Controls) for instant on/off toggling with action-bar confirmation.
  - In-game commands with live auto-completion for operators and players.
- 🌐 **Full Localization**:
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

## 🎯 Smart Filtering

MasterAttractor includes a built-in item filter engine configurable directly via the Mod Menu GUI or `config/masterattractor.json`:

| Mode | Behavior |
|---|---|
| `NONE` | Pulls all valid dropped items within range (default). |
| `BLACKLIST` | Pulls everything **except** items in the configured filter list (e.g., cobblestone, dirt, rotten flesh). |
| `WHITELIST` | Pulls **only** items explicitly declared in the configured filter list (e.g., diamonds, ancient debris). |

---

## 🔧 Configuration

### 1. In-Game GUI (Mod Menu + Cloth Config)
1. Open the pause menu or title screen and select **Mods**.
2. Select **MasterAttractor** from the list.
3. Click the **Configure** button to open the settings interface.
4. Settings are divided into four dedicated categories:
   - **General**: Master toggle, Radius, Strength, Tick Interval.
   - **Targets**: Items, XP, Projectiles, Mobs, Boats, Minecarts, Falling Blocks.
   - **Item Filtering**: Filter Mode, Item List, Inventory Space Check, Vacuum Mode.
   - **Behavior & Audio/Visual**: Pickup delay respect, God mode bypass, Audio & Particle toggles.

### 2. Configuration File (`config/masterattractor.json`)
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
  "filterMode": "NONE",
  "filteredItems": [
    "minecraft:cobblestone",
    "minecraft:dirt",
    "minecraft:andesite",
    "minecraft:diorite",
    "minecraft:granite",
    "minecraft:rotten_flesh"
  ],
  "checkInventorySpace": true,
  "vacuumMode": false,
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

## 📦 Requirements & Download

### Download
- Download the latest `.jar` build from the [**GitHub Releases**](https://github.com/houssemdub/masterattractor/releases).

### Requirements
- **Minecraft**: `1.21.11`
- **Fabric Loader**: `0.20.1` or higher
- **Fabric API**: `0.141.6+1.21.11` or higher
- **Java**: `21` or higher
- **Recommended Client Mods**:
  - [Mod Menu](https://modrinth.com/mod/modmenu)
  - [Cloth Config](https://modrinth.com/mod/cloth-config)

---

## 🛠️ Building from Source

To compile the mod JAR yourself:

```bash
git clone https://github.com/houssemdub/masterattractor.git
cd masterattractor
./gradlew build
```

The compiled and remapped JAR will be generated in `build/libs/`:
```
build/libs/masterattractor-1.0.0+1.21.11.jar
```

---

## 📄 License

This mod is available under the **MIT License**. Feel free to use it in modpacks or customize it to your needs!

---

## 📽️ Preview

<p align="center">
  <img src="MasterAttractor.gif" alt="MasterAttractor in Action" width="100%">
</p>
