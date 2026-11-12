# DGClient (`v1.0.2`)

**DGClient** is a standalone, client-only anarchy utility mod for **Minecraft 1.21.11 (Fabric)** built from scratch in **Java 21**. It features a custom neon-green & white ClickGUI (`Right Shift`, rebindable via Minecraft Controls, ClickGUI, or `.bind`), bundled FreeType TTF crisp typography, a drag-and-snap HUD editor, **49 standalone modules**, **ESP & Storage Tracers**, an asynchronous multi-indicator **Suspicious Chunk & Base Detector (`BaseFinder`)** with natural-structure suppression & spatial cluster deduplication, and a diagnostic-driven **Y-0 Underground Bypass (`Y0Diagnostics` + `Y0Bypass`)**.

- **Zero external client dependencies** (completely original standalone codebase; no Meteor dependency).
- **Strict Neon Green & White visual identity**: `#39FF14` / `#00FF88` neon emerald accents, `#FFFFFF` crisp white highlights, and `#0A0F0C`–`#121A15` translucent dark charcoal glass panels.
- **Crash-isolated module & event bus**: any runtime exception inside a module or HUD element automatically isolates and disables only the faulting module with a user toast notification instead of crashing Minecraft.

---

## Supported Versions & Requirements

| Component | Required Version |
| :--- | :--- |
| **Minecraft** | **`1.21.11`** |
| **Mod Loader** | **Fabric Loader** `0.19.5` or newer (`>=0.18.0`) |
| **Fabric API** | `0.141.6+1.21.11` (required in `mods/`) |
| **Java Runtime** | **Java 21** (`Temurin 21` recommended) |

---

## Installation (Prism / Modrinth / Official Launcher)

1. Install **Minecraft 1.21.11** with **Fabric Loader `>= 0.19.5`** on **Java 21**.
2. Download **[Fabric API `0.141.6+1.21.11`](https://modrinth.com/mod/fabric-api)** and place it in your instance's `mods/` folder.
3. Download **`dgclient-1.0.2.jar`** from the **[GitHub Releases](https://github.com/nananqj96-cloud/dgclient/releases/tag/v1.0.2)** page and place it in `mods/`.
4. Launch Minecraft and press **`Right Shift`** in-game (configurable in Minecraft *Options -> Controls -> Key Binds -> DGClient* or via `.bind ClickGUI <key>` / `.gui`) to open the ClickGUI.

---

## Building Locally from Source

```bash
git clone https://github.com/nananqj96-cloud/dgclient.git
cd dgclient
./gradlew build --no-daemon
```

The remapped production mod JAR is written to:
```text
build/libs/dgclient-1.0.2.jar
```

---

## Keybinds & Chat Commands

### Default Controls
- **`Right Shift`** — Open / Close **DGClient ClickGUI**, **HUD Drag Editor**, **Profiles**, & **Detections Panel**
- **`Left-Click` Module** — Toggle module ON / OFF
- **`Right-Click` Module** — Expand / Collapse per-module settings (`Slider`, `Toggle`, `Mode`, `Color`, `Keybind`, `Text`)
- **`ESC` during Keybind Bind** — Clear module keybind (`NONE`)

### Chat Commands (Prefix `.`)
All commands intercept client-side and **never** transmit to public server chat:

| Command | Description |
| :--- | :--- |
| `.help` | Lists all registered DGClient commands and syntax |
| `.toggle <module>` (`.t`) | Toggles a module on or off |
| `.gui` (`.clickgui`) | Opens the DGClient ClickGUI directly from chat |
| `.bind <module> <key\|none>` | Binds a module to a keyboard key (syncs with Minecraft Controls) |
| `.friend <add\|remove\|list> [name]` | Manages green-highlighted friends across ESP, Tracers, AutoLog & Chat |
| `.waypoint <add\|del\|list\|clear> ...` | Manages per-server & per-dimension waypoints (with 8:1 Nether sync) |
| `.profile <list\|save\|load\|create\|delete\|reset>` | Switches, saves, or resets JSON config profiles in `config/dgclient/profiles/` |
| `.basefinder <list\|export\|import\|clear>` (`.bf`) | Lists or exports/imports flagged BaseFinder chunks (`.json` & `.csv`) |
| `.y0 <test [chunks]\|save\|reset\|clear>` | Runs `Y0Diagnostics` N-chunk sampling, saves reports, or clears `UndergroundStore` |
| `.say <message>` | Sends raw chat bypassing command prefix and Coordinate-Leak Guard |
| `.debug [on\|off]` | Toggles verbose diagnostic logging |

---

## Complete Module Reference (48 Modules)

### 1. Render & Visuals (`18 Modules` + `5 ESP/Tracer Modules`)
- **HandItemGlow** — Emissive neon-green/white aura, silhouette outline, tint, & pulse speed for held items (`First-Person` & `Third-Person`).
- **Fullbright** — Smooth fade-in/fade-out brightness via `Gamma`, `Lightmap`, or `NightVision`.
- **CustomCrosshair** — Dynamic green/white crosshair (`Cross`, `Circle`, `Dot+Ring`, `Plus`) with movement & attack expansion.
- **BlockHighlight** — Glowing neon-green/white targeted block fill + outline (`Outline`, `Fill`, `Both`).
- **HitGlow** — Timed fade-out neon-green/white hit flash & glowing outline on struck entities.
- **TotemPopEffects** — Green/white shockwave ring + burst particles on player totem pops.
- **DamageIndicators** — Floating drift-up HP delta numbers (`-6.5` / `+4.0`) on entity health changes.
- **ItemGlow** — Highlights dropped ground items with distance-scaled item-count labels.
- **Zoom** — Smooth scroll-adjustable cinematic zoom with mouse-sensitivity scaling.
- **Viewmodel** — Independent mainhand/offhand `X/Y/Z` translation, scale, rotation, & swing duration.
- **SkyFog** — Custom charcoal/emerald sky tint, fog density pushback, & Nether/lava/water fog clearer.
- **TimeChanger** — Client-side time-of-day lock (`Dawn`, `Noon`, `Dusk`, `Midnight`, or custom tick).
- **Bloom** — Global screen-space neon bloom intensity tuning for UI, HUD, and world overlays.
- **Nametags** — Clean dark-glass player/entity nametags with distance, ping, HP, & armor/held item row.
- **Trails** — Fading neon-green/white movement ribbon behind players and in-flight pearls/arrows/tridents.
- **ChunkBorders** — Passable glowing green/white chunk & sub-chunk grid lines.
- **WeatherParticleReducer** — Hides rain/snow/thunder and suppresses explosion/totem/firework/portal lag particles.
- **BetterTabList** — Expanded player tab list with exact numeric `ms` ping badges & highlighted friends.
- **PlayerESP** — `3D Box`, `Shader Glow Outline` (real GLSL `entity_outline` post-chain), `2D Box`, & `Corner Box` modes with friend awareness.
- **EntityESP** — Independent toggles for Hostiles, Passives, Vehicles, Dropped Items, Projectiles, & End Crystals.
- **StorageESP** — Event-driven loaded-chunk container highlighter for Chests (with Double-Chest merging), Trapped Chests, Ender Chests, Shulkers, Barrels, Hoppers, Furnaces/Smokers/Blast Furnaces, Dispensers/Droppers, Brewing Stands, 1.21.11 Shelves/Chiseled Bookshelves, & Decorated Pots (integrates with `Y0Bypass` underground leaks).
- **StorageTracers** — Independently configurable, bobbing-safe 360° 3D storage tracer lines rendered in a single batched GPU draw call with distance limits, per-container toggles, double-chest deduplication, and instant chunk/block unload updates.
- **SpawnerESP** — Glowing spawner box + mob type label + `16m` activation sphere indicator.
- **Tracers** — Smooth camera-bob-compensated batched 3D lines from screen crosshair/bottom/top to entities.

### 2. Detect (`BaseFinder`, `Y0Diagnostics`, `Y0Bypass`)
- **BaseFinder (`Suspicious Chunk & Base Detector`)** — Multi-indicator asynchronous chunk analyzer (`0–100%` score, `LOW`/`MEDIUM`/`HIGH` confidence tiers) requiring corroborated player activity while suppressing vanilla structure false positives (`1.21` Trial Chambers, Mineshafts, Dungeons, Villages, Ancient Cities, Ruined Portals, End Cities, Geodes) and ignoring isolated single blocks:
  - *Storage & Workstation Clusters* (shulkers, ender chests, support-verified container clusters, hoppers, crafting/smithing/enchanting/anvil/bed base stations, written signs)
  - *Automation & Farms* (observers, pistons, comparators, bubble-column elevators, player-modified spawner grinders, vertical kelp/bamboo columns)
  - *Concealment & Excavation* (axis-rotated `X`/`Z` deepslate patches, sealed `1x1` dive shafts, straight tunnels)
  - *Player Entities & Unnatural Builds* (non-elytra item frames, armor stands, traded/underground villagers, tamed pets, concrete/cherry/bamboo/sky builds, active portals & ice highways, `Y0Bypass` leaks)
  - *Spatial Cluster Deduplication & Clean Visuals* — Merges adjacent flagged chunks of the same base into deduplicated clusters (`Cluster Merge Radius`), rate-limits alerts to once per cluster, and renders clutter-free `Flat Marker`, `Corner Brackets`, or `Compact Slice` chunk boundaries with distinct Low vs. High confidence styling.
- **Y0Diagnostics** — Inspects incoming `ChunkData`, `LightUpdate`, `BlockUpdate`, `ChunkDeltaUpdate`, `BlockEntityUpdate`, `EntitySpawn`, `PlaySound`, `Particle`, and `WorldEvent` packets below `Y=0`, renders a live on-screen diagnostic verdict panel, runs N-chunk test sampling (`.y0 test 16`), writes timestamped logs to `config/dgclient/diagnostics/`, and auto-configures `Y0Bypass`.
- **Y0Bypass** — Modular underground reconstruction engine combining 5 toggleable strategies into a confidence-scored `UndergroundStore` (`config/dgclient/y0_store/<server>.json`).

### 3. Movement, World, Fun & Misc Utilities (`20 Modules`)
- **Movement (`5`)**: `AutoWalk` (Walk + AutoSprint + Anti-AFK), `ElytraHelper` (Auto-Deploy, Cruise Pitch Lock, Auto-Firework, Durability/Descent Warnings), `NoFall` (`PACKET_GROUND`, `ELYTRA_GLIDE`, `MLG_BUCKET`), `HighwayHelper` (8-way 45° yaw snap, pitch lock, safe-edge guard), `Freecam` (smooth detached camera blocking outgoing position packets).
- **World (`3`)**: `FastPlace` (configurable Fast Place/XP & Fast Break delays), `Waypoints` (3D glowing beacons, 8:1 Nether sync, Auto Death Waypoint), `CoordLogger` (local death/portal logger in `config/dgclient/coord_log.txt` + distance tracker).
- **Fun (`4`)**: `HeadAnimations` (safe client-side Spin, Nod, Dinnerbone Upside-Down, & Fake-Lag Ghost box), `StashCard` (green/white watermarked screenshot tool with coordinate redaction), `Notifier` (Visual Range, Join/Leave, & Totem-Pop counter), `QueueNotifier` (anarchy queue threshold alert).
- **Misc (`8`)**: `AutoTotem`, `AutoArmor`, `AutoEat` (hunger + low-HP Golden Apple), `InvHelpers` (Middle-Click Pearl/XP, green/white 9x3 Shulker Preview tooltip, Armor Durability Alert, Hotbar Drop Lock), `ChatUtils` (Timestamps, Mention Highlight, Anti-Spam `[xN]`, & **Outgoing Coordinate-Leak Opsec Guard**), `AutoReconnect` (countdown button on `DisconnectedScreen`), `AutoLog` (low-HP/totem/stranger disconnect + glowing **Logout Spots**), `SessionStats`.
- **HUD (`2` + `10 Draggable Widgets`)**: `ClickGUI`, `HUD` (`Watermark`, `Module ArrayList`, `Coordinates + Nether 8:1`, `FPS/TPS/Ping`, `Speed km/h & bps`, `Armor Durability`, `Inventory Mini-Grid`, `Totem Counter`, `Radar`, `Keystrokes`).

---

## Y-0 ("Underground Bypass") Architecture & Honest Server Limitations

Multiplayer anarchy servers hide sub-zero (`Y < 0`, `Y = -64..-1`) terrain in several distinct ways. **DGClient never fabricates or guesses blocks that the server never transmitted.**

| What Works Client-Side | Server-Side Hard Limit |
| :--- | :--- |
| **Packet-Leak Reconstruction**: Captures real `BlockUpdateS2CPacket`, `ChunkDeltaUpdateS2CPacket`, `BlockEntityUpdateS2CPacket`, `ChunkData` block-entity NBT entries, and `WorldEventS2CPacket` below `Y=0` with `96–99%` confidence. | **Zeroed / Stripped Sections**: If a server plugin replaces `Y < 0` sections with `100% Air` or `100% Fake Deepslate` *before* sending `ChunkDataS2CPacket` and sends zero block-entity/light/update leaks, the client physically receives zero information about buried static blocks. In those chunks, `Y0Bypass` honestly displays **`Y<0 Status: No data`**. |
| **Anti-Decoy Statistical Filter**: Detects Paper Anti-Xray Engine-Mode 2/3 decoy floods (`>8.5%` ore density, impossible deep coal/emerald Y-levels) and prioritizes air-adjacent cave exposures (`Engine Mode 1`). | **Strict Anti-Cheat Reach Checks**: `Update Triggers` defaults to **Safe Mode ON** (low-rate `ABORT_DESTROY_BLOCK` only on air-exposed blocks within `4.0m` reach) so Grim/NCP never kick the player for packet spam or out-of-reach queries. |
| **Light, Heightmap & Sound/Particle Inference**: Detects leaked non-zero `BlockLight` nibbles below `Y=0`, open shafts in `MOTION_BLOCKING` heightmaps, and underground piston/chest/shulker/minecart sounds. | |

---

## Configuration Directory Layout

All configurations are stored locally in readable JSON/log files under `.minecraft/config/dgclient/`:
- `config/dgclient/profiles/<profile>.json` — Module states, settings, keybinds, & HUD layout
- `config/dgclient/friends.json` & `waypoints.json` — Friends list & per-server waypoints
- `config/dgclient/basefinder/<server>.json` & `.csv` — Flagged suspicious chunk database
- `config/dgclient/y0_store/<server>.json` — Confirmed underground block-leak store
- `config/dgclient/diagnostics/y0_diagnostic_<timestamp>.log` — Saved `Y0Diagnostics` reports

---

## License

Released under the [MIT License](LICENSE).
