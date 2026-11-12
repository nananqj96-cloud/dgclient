# Aether Client (`1.21.11` Fabric)

A modern, privacy-first, high-FPS Fabric utility client for Minecraft **1.21.11** featuring a zero-lag NanoVG overlay engine and a **Dual-Mode ClickGUI**.

## Highlights

- **100% Offline & Audited**: Zero outbound telemetry, zero remote config servers, and verified official LWJGL 3.3.3 NanoVG natives. Config sharing uses self-contained offline `AETHER1:...` GZIP+Base64URL clipboard tokens.
- **Dual-Mode Interface** (`Right Shift`):
  - **Unified Sidebar Dashboard** *(default)*: Centered obsidian-cyan glass window with category navigation rail, active module count badges, global search, `Favs` filter, 2-column masonry module cards, integrated **Themes & Audio** studio, and **Config Profiles** manager.
  - **Draggable Category Columns**: One-click `Columns` / `Dashboard` toggle in the top bar switches live between the unified dashboard and classic draggable category columns with a floating glass command dock.
- **Zero-Lag Rendering Pipeline**:
  - Zero per-frame `glGetIntegerv` driver stalls during HUD & GUI rendering.
  - Persistent off-heap `NVGColor` / `NVGPaint` buffers, `nvgFontFaceId` integer font lookups, and cached glyph width measurements.
  - Background blur defaults to **OFF** for maximum frame rates on all GPUs (optional toggle in `Client -> ClickGUI`).

## Building & Testing

Requires **Java 21**:

```bash
./gradlew build
./gradlew runClientGameTest
```

Compiled mod JARs are written to `build/libs/`.
