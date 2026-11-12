# Aether Client 2.1.0 — PinkBloom Edition (`1.21.11` Fabric)

A modern, privacy-first, high-FPS Fabric utility client for Minecraft **1.21.11** featuring the **PinkBloom** velvet-rose optical glow aesthetic, a zero-lag **Dual-Mode NanoVG ClickGUI**, and **145 ported CorzClient modules** integrated into a synced **`Corz Port`** secondary UI.

## Direct Download

- **Latest Compiled Mod JAR (`1.21.11` Fabric)**:  
  [`aetherclient-2.1.0.jar`](https://github.com/nananqj96-cloud/dgclient/releases/download/v2.1.0-pinkbloom/aetherclient-2.1.0.jar)

Drop `aetherclient-2.1.0.jar` into `.minecraft/mods/` alongside **Fabric Loader `>= 0.19.0`** and **Fabric API** for Minecraft `1.21.11`. Both Aether Client and the ported Corz engine are bundled inside a single self-contained Jar-in-Jar.

## Highlights

- **PinkBloom Default Theme**: Deep velvet-plum dark glass (`#140B18 -> #0C0710`) paired with sakura-rose (`#FF66C4`) and petal-blush (`#FFA6DF`) optical NanoVG bloom halos — rendered without extra FBO blur passes for zero frame-time penalty.
- **Synced `"Corz Port"` Secondary UI**:
  - Accessible from the **`Corz Port`** subtab in the left sidebar (`Dashboard` mode) or floating top glass dock (`Columns` mode) — keeping the primary category list clean and uncluttered.
  - Launches a second NanoVG window styled dynamically in Aether's active theme (`PinkBloom` or any custom theme) that **opens and closes in 100% sync** with the main ClickGUI (`Right Shift` / `ESC`).
  - Exposes all **145 ported CorzClient modules** across `Bots`, `DonutSMP`, `Combat`, `Movement`, `Network`, `Misc`, `Visuals`, and `Socials` with live search, category pills, interactive sliders/ranges/modes/toggles/text/keybinds, plus a `Studio` button for specialized native editors (HUD Layout, Macros, Waypoints, Radio, Cape, Block ESP).
- **100% Offline & Security-Audited**:
  - Audited both `vulxts-buildable-source.zip` and `CorzClient-SourcedByDexter.zip` across every source file, Mixin, and library entry: **0 RATs / 0 stealers / 0 webhooks**.
  - Sanitized Corz's Windows `attrib +h +s` / PowerShell self-delete routine (`CorzclientAttribUtils`), disabled background Discord IPC/remote license traffic, and removed 3rd-party watermarks.

## Building & Testing

Requires **Java 21**:

```bash
./gradlew :corz-api:jar :corzport:remapJar
./gradlew build
xvfb-run -a -s "-screen 0 1600x900x24" ./gradlew runClientGameTest
```

Compiled mod JARs are written to `build/libs/aetherclient-2.1.0.jar`.
