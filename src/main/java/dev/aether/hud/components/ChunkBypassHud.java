package dev.aether.hud.components;

import dev.aether.hud.HudComponent;
import dev.aether.module.impl.ChunkBypassModule;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Experimental Chunk Bypass HUD — a glass panel combining a live zone radar, an animated scan
 * sweep, a stats bar and a ranked zone list. Theme-synced via the client palette so it blends
 * with the rest of the HUD. Toggle from the HUD module (Chunk Bypass HUD); visible while the
 * ChunkBypass module is enabled.
 */
public class ChunkBypassHud extends HudComponent {
   private static final float WIDTH = 220.0F;
   private static final float HEIGHT = 262.0F;
   private static final float RADAR_CX = 110.0F;
   private static final float RADAR_CY = 104.0F;
   private static final float RADAR_R = 56.0F;

   private final ChunkBypassModule chunkBypass;
   private final ThemeManager themes;

   public ChunkBypassHud(ChunkBypassModule chunkBypass, ThemeManager themes, BooleanSupplier visibility) {
      super("chunkBypassHud", 0.006F, 0.56F, visibility);
      this.chunkBypass = chunkBypass;
      this.themes = themes;
   }

   @Override
   public float measureWidth(NVGRenderer nvg) {
      return WIDTH;
   }

   @Override
   public float measureHeight(NVGRenderer nvg) {
      return HEIGHT;
   }

   @Override
   public void render(NVGRenderer vg, float x, float y, float w, float h) {
      Theme theme = this.themes.current();
      LocalPlayer player = Minecraft.getInstance().player;
      float t = (float)(System.nanoTime() / 1.0E9);

      this.drawHeader(vg, theme, t);
      this.drawRadar(vg, theme, player, t);
      this.drawStats(vg, theme, player);
      this.drawZoneList(vg, theme, player);
   }

   private void drawHeader(NVGRenderer vg, Theme theme, float t) {
      // Logo
      vg.aetherMark(22.0F, 19.0F, 7.0F, theme.accent(), theme.accentBright());

      // Title with gradient + glow
      String title = "CHUNK BYPASS";
      float titleSize = 14.0F;
      float titleX = 36.0F;
      float titleY = 14.0F;
      vg.textGlow(title, titleX, titleY, titleSize, Colors.withAlpha(theme.accent(), 0.35F));
      vg.textGradient(title, titleX, titleY, titleSize, theme.textPrimary(), theme.accentBright());

      // Subtitle
      vg.text("zone radar · live", 36.0F, 27.0F, 9.0F, theme.textMuted());

      // Pulsing EXPERIMENTAL badge
      float pulse = 0.5F + 0.5F * (float)Math.sin(t * Math.PI * 2.0 / 2.4);
      String badge = "EXPERIMENTAL";
      float bw = vg.textWidth(badge, 8.0F);
      float bx = WIDTH - bw - 14.0F;
      float by = 15.0F;
      vg.rect(bx - 5.0F, by - 3.0F, bw + 10.0F, 14.0F, 7.0F, Colors.withAlpha(theme.accent(), 0.10F + 0.10F * pulse));
      vg.rectOutline(bx - 5.0F, by - 3.0F, bw + 10.0F, 14.0F, 7.0F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.30F + 0.25F * pulse));
      vg.text(badge, bx, by + 1.0F, 8.0F, Colors.withAlpha(theme.accentBright(), 0.85F + 0.15F * pulse));
   }

   private void drawRadar(NVGRenderer vg, Theme theme, LocalPlayer player, float t) {
      // Radar disc
      vg.circle(RADAR_CX, RADAR_CY, RADAR_R, Colors.withAlpha(theme.surface(), 0.72F));
      vg.circleOutline(RADAR_CX, RADAR_CY, RADAR_R - 1.0F, 1.5F, Colors.withAlpha(theme.accent(), 0.45F));
      // Inner ring
      vg.circleOutline(RADAR_CX, RADAR_CY, RADAR_R * 0.62F, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.9F));

      // Grid crosshair
      int grid = Colors.withAlpha(theme.borderSubtle(), 0.55F);
      vg.line(RADAR_CX - RADAR_R, RADAR_CY, RADAR_CX + RADAR_R, RADAR_CY, 1.0F, grid);
      vg.line(RADAR_CX, RADAR_CY - RADAR_R, RADAR_CX, RADAR_CY + RADAR_R, 1.0F, grid);

      // Rotating scan sweep — a partial arc of short segments
      float sweepAngle = (float)(t * 1.6F % (Math.PI * 2.0F));
      int segments = 46;
      float sweepSpan = (float)(Math.PI * 0.72);
      for (int i = 0; i < segments; i++) {
         float a = sweepAngle - sweepSpan + (float)i / (float)(segments - 1) * sweepSpan;
         float inner = RADAR_R * 0.30F;
         float a1 = a + sweepSpan / (float)segments;
         float ax0 = RADAR_CX + (float)Math.cos(a) * inner;
         float ay0 = RADAR_CY + (float)Math.sin(a) * inner;
         float ax1 = RADAR_CX + (float)Math.cos(a1) * RADAR_R;
         float ay1 = RADAR_CY + (float)Math.sin(a1) * RADAR_R;
         float fade = 1.0F - (float)i / (float)segments;
         vg.line(ax0, ay0, ax1, ay1, 2.2F, Colors.withAlpha(theme.accentBright(), 0.10F + 0.34F * fade));
      }

      // Compass labels
      float yaw = player != null ? (float)Math.toRadians(player.getYRot() + 90.0F) : 0.0F;
      String[] dirs = {"N", "E", "S", "W"};
      float[][] axes = {{0.0F, -1.0F}, {1.0F, 0.0F}, {0.0F, 1.0F}, {-1.0F, 0.0F}};
      for (int i = 0; i < 4; i++) {
         float sx = screenX(axes[i][0], axes[i][1], yaw);
         float sy = screenY(axes[i][0], axes[i][1], yaw);
         float lx = RADAR_CX + sx * (RADAR_R - 8.0F);
         float ly = RADAR_CY + sy * (RADAR_R - 8.0F);
         boolean primary = i == 0;
         float fs = 9.0F;
         vg.text(dirs[i], lx - vg.textWidth(dirs[i], fs) / 2.0F, ly - fs / 3.0F, fs, primary ? theme.accentBright() : theme.textMuted());
      }

      // Player centre
      vg.circleGlow(RADAR_CX, RADAR_CY, 3.5F, 5.0F, Colors.withAlpha(theme.accentBright(), 0.55F));
      vg.circle(RADAR_CX, RADAR_CY, 3.0F, theme.accentBright());

      // Zone blips
      if (player != null && this.chunkBypass != null) {
         double range = this.chunkBypass.range.get();
         List<ChunkBypassModule.Zone> zones = this.chunkBypass.zones();
         for (ChunkBypassModule.Zone zone : zones) {
            this.drawZoneBlip(vg, theme, yaw, range, zone, player);
         }
      }
   }

   private void drawZoneBlip(NVGRenderer vg, Theme theme, float yaw, double range, ChunkBypassModule.Zone zone, LocalPlayer player) {
      float dx = (float)(zone.centroidX() - player.getX());
      float dz = (float)(zone.centroidZ() - player.getZ());
      float dist = (float)Math.hypot(dx, dz);
      if (dist < 0.5F) {
         return;
      }

      boolean clamped = dist > range;
      float edge = RADAR_R - 6.0F;
      float projected = clamped ? edge : (float)(dist / range) * edge;
      float sx = screenX(dx, dz, yaw) / Math.max(dist, 0.001F) * projected;
      float sy = screenY(dx, dz, yaw) / Math.max(dist, 0.001F) * projected;
      float bx = RADAR_CX + sx;
      float by = RADAR_CY + sy;

      float strength = this.chunkBypass.minHits() <= 0
         ? 1.0F
         : Math.clamp((float)((zone.maxScore() - this.chunkBypass.minHits()) / (this.chunkBypass.minHits() * 2.0)), 0.0F, 1.0F);

      float t = (float)(System.nanoTime() / 1.0E9);
      float pulse = 0.5F + 0.5F * (float)Math.sin(t * Math.PI * 2.0 / 1.6);
      float r = clamped ? 2.6F : 3.0F + 1.6F * strength;
      float rGlow = r + 1.0F + 1.5F * pulse;

      vg.circleGlow(bx, by, rGlow, 4.0F + 2.0F * pulse, Colors.withAlpha(theme.accent(), (0.28F + 0.22F * strength) * (0.85F + 0.15F * pulse)));
      vg.circle(bx, by, r, Colors.withAlpha(theme.accent(), 0.85F + 0.15F * pulse));
      vg.circleOutline(bx, by, r + 1.6F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.6F * (0.85F + 0.15F * pulse)));

      if (!clamped && dist <= range * 0.6) {
         String label = (int)dist + "m";
         float lw = vg.textWidth(label, 8.0F);
         vg.rect(bx - lw / 2.0F - 3.0F, by - 6.0F, lw + 6.0F, 11.0F, 5.0F, Colors.withAlpha(theme.surfaceElevated(), 0.78F));
         vg.text(label, bx - lw / 2.0F, by - 4.5F, 8.0F, Colors.withAlpha(theme.accentBright(), 0.95F));
      }
   }

   private void drawStats(NVGRenderer vg, Theme theme, LocalPlayer player) {
      int zones = this.chunkBypass != null ? this.chunkBypass.zones().size() : 0;
      int hits = 0;
      double nearest = -1.0;
      if (this.chunkBypass != null && player != null) {
         for (ChunkBypassModule.Zone zone : this.chunkBypass.zones()) {
            hits += (int)zone.totalScore();
            double d = Math.hypot(zone.centroidX() - player.getX(), zone.centroidZ() - player.getZ());
            if (nearest < 0.0 || d < nearest) {
               nearest = d;
            }
         }
      }

      float y = 170.0F;
      float pad = 12.0F;
      float colW = (WIDTH - pad * 2.0F - 12.0F) / 3.0F;
      String[] labels = {"ZONES", "HITS", "NEAREST"};
      String[] values = {String.valueOf(zones), String.valueOf(hits), nearest < 0.0 ? "—" : (int)nearest + "m"};

      for (int i = 0; i < 3; i++) {
         float cx = pad + (float)i * (colW + 6.0F);
         if (i > 0) {
            vg.line(cx - 3.0F, y + 18.0F, cx - 3.0F, y + 44.0F, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.7F));
         }
         vg.text(labels[i], cx, y, 8.0F, theme.textMuted());
         float vSize = 15.0F;
         int vColor = i == 0 && zones > 0 ? theme.accentBright() : theme.textPrimary();
         vg.textGlow(values[i], cx, y + 12.0F, vSize, Colors.withAlpha(theme.accent(), 0.30F));
         vg.text(values[i], cx, y + 12.0F, vSize, vColor);
      }
   }

   private void drawZoneList(NVGRenderer vg, Theme theme, LocalPlayer player) {
      float y = 206.0F;
      float rowH = 16.0F;
      vg.line(12.0F, y - 6.0F, WIDTH - 12.0F, y - 6.0F, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.7F));
      vg.text("TOP ZONES", 12.0F, y - 2.0F, 8.0F, theme.textMuted());

      if (this.chunkBypass == null || player == null) {
         return;
      }

      List<ChunkBypassModule.Zone> zones = this.chunkBypass.zones();
      int max = Math.min(3, zones.size());
      if (max == 0) {
         vg.text("scanning for inhabited ground…", 12.0F, y + 26.0F, 9.0F, theme.textDisabled());
         return;
      }

      int minHits = this.chunkBypass.minHits();
      double top = zones.get(0).totalScore();
      if (top <= 0.0) {
         top = 1.0;
      }

      for (int i = 0; i < max; i++) {
         ChunkBypassModule.Zone zone = zones.get(i);
         float ry = y + 8.0F + (float)i * rowH;
         double dist = Math.hypot(zone.centroidX() - player.getX(), zone.centroidZ() - player.getZ());

         // Rank dot
         vg.circleGlow(20.0F, ry + 6.0F, 3.0F, 3.5F, Colors.withAlpha(theme.accent(), 0.45F));
         vg.circle(20.0F, ry + 6.0F, 2.6F, i == 0 ? theme.accentBright() : theme.accent());

         // Distance (bold) + coords (muted)
         String dLabel = (int)dist + "m";
         vg.text(dLabel, 30.0F, ry, 10.0F, theme.textPrimary());
         float cw = vg.textWidth(dLabel, 10.0F);
         vg.text("x " + (int)zone.centroidX() + " z " + (int)zone.centroidZ(), 30.0F + cw + 6.0F, ry, 8.0F, theme.textMuted());

         // Score progress bar
         float barX = 12.0F;
         float barY = ry + 11.0F;
         float barW = WIDTH - 24.0F;
         float barH = 3.0F;
         vg.rect(barX, barY, barW, barH, 1.5F, Colors.withAlpha(theme.surfaceElevated(), 0.7F));
         float fill = (float)(zone.totalScore() / top);
         vg.rect(barX, barY, barW * fill, barH, 1.5F, Colors.withAlpha(theme.accent(), 0.85F));
      }
   }

   private static float screenX(float x, float z, float yaw) {
      return (float)((-x) * Math.sin(yaw) + z * Math.cos(yaw));
   }

   private static float screenY(float x, float z, float yaw) {
      return (float)(-((x * Math.cos(yaw) + z * Math.sin(yaw))));
   }
}
