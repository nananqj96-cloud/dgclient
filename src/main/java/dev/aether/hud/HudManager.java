package dev.aether.hud;

import com.google.gson.JsonObject;
import dev.aether.hud.components.ArmorHud;
import dev.aether.hud.components.ArrayListHud;
import dev.aether.hud.components.InfoHud;
import dev.aether.hud.components.KeystrokesHud;
import dev.aether.hud.components.PotionsHud;
import dev.aether.hud.components.RadarHud;
import dev.aether.hud.components.RegionMapHud;
import dev.aether.hud.components.SpotifyHud;
import dev.aether.hud.components.StaffListHud;
import dev.aether.hud.components.WatermarkHud;
import dev.aether.module.ModuleManager;
import dev.aether.module.Modules;
import dev.aether.notification.NotificationManager;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.spotify.SpotifyService;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.CpsTracker;
import dev.aether.util.TpsTracker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class HudManager {
   private final List<HudComponent> components = new ArrayList<>();
   private final ThemeManager themes;

   public HudManager(ModuleManager var1, ThemeManager var2, SpotifyService var3, NotificationManager var4) {
      this.themes = var2;
      Modules.HudModule var5 = var1.hud;
      this.components.add(new WatermarkHud(var2, () -> var5.isEnabled() && var5.watermark.get()));
      this.components.add(new ArrayListHud(var1, var5, var2, () -> var5.isEnabled() && var5.arrayList.get()));
      this.components
         .add(
            new InfoHud(
               "fps",
               var2,
               "FPS",
               () -> Integer.toString(Minecraft.getInstance().getFps()),
               0.006F,
               0.985F,
               () -> var5.isEnabled() && var5.fps.get()
            )
         );
      this.components
         .add(
            new InfoHud(
               "ping",
               var2,
               "Ping",
               HudManager::pingString,
               0.055F,
               0.985F,
               () -> var5.isEnabled() && var5.ping.get()
            )
         );
      this.components
         .add(
            new InfoHud(
               "coords",
               var2,
               "XYZ",
               HudManager::coordsString,
               0.115F,
               0.985F,
               () -> var5.isEnabled() && var5.coordinates.get()
            )
         );
      this.components
         .add(
            new InfoHud(
               "direction",
               var2,
               "Facing",
               HudManager::directionString,
               0.24F,
               0.985F,
               () -> var5.isEnabled() && var5.direction.get()
            )
         );
      this.components
         .add(
            new InfoHud(
               "tps",
               var2,
               "TPS",
               () -> String.format("%.1f", TpsTracker.get()),
               0.33F,
               0.985F,
               () -> var5.isEnabled() && var5.tps.get()
            )
         );
      this.components
         .add(
            new InfoHud(
               "cps",
               var2,
               "CPS",
               () -> CpsTracker.get(0) + " | " + CpsTracker.get(1),
               0.4F,
               0.985F,
               () -> var5.isEnabled() && var5.cps.get()
            )
         );
      this.components.add(new ArmorHud(var2, () -> var5.isEnabled() && var5.armor.get()));
      this.components.add(new PotionsHud(var2, () -> var5.isEnabled() && var5.potions.get()));
      this.components.add(new KeystrokesHud(var2, () -> var5.isEnabled() && var5.keystrokes.get()));
      this.components.add(new RadarHud(var5, var1.susChunkFinder, var2, () -> var5.isEnabled() && var5.radar.get()));
      this.components.add(new RegionMapHud(var1.regionMap, var2));
      this.components.add(new StaffListHud(var1.staffList, var2));
      this.components.add(new SpotifyHud(var1.spotify, var3, var2));
      this.components.add(var4);
   }

   private static String coordsString() {
      LocalPlayer var0 = Minecraft.getInstance().player;
      if (var0 == null) {
         return "0, 0, 0";
      } else {
         BlockPos var1 = var0.blockPosition();
         return var1.getX() + ", " + var1.getY() + ", " + var1.getZ();
      }
   }

   private static String pingString() {
      Minecraft var0 = Minecraft.getInstance();
      if (var0.player != null && var0.getConnection() != null) {
         PlayerInfo var1 = var0.getConnection().getPlayerInfo(var0.player.getUUID());
         return var1 == null ? "0ms" : var1.getLatency() + "ms";
      } else {
         return "0ms";
      }
   }

   private static String directionString() {
      LocalPlayer var0 = Minecraft.getInstance().player;
      if (var0 == null) {
         return "N";
      } else {
         Direction var1 = var0.getDirection();

         return switch (var1) {
            case NORTH -> "N  -Z";
            case SOUTH -> "S  +Z";
            case WEST -> "W  -X";
            case EAST -> "E  +X";
            default -> var1.getName().toUpperCase();
         };
      }
   }

   public List<HudComponent> getComponents() {
      return this.components;
   }

   public List<HudManager.Placement> layout(NVGRenderer var1, float var2, float var3, boolean var4) {
      ArrayList var5 = new ArrayList();

      for (HudComponent var7 : this.components) {
         if (var4 || var7.visible()) {
            float var8 = var7.getScale();
            float var9 = var7.measureWidth(var1) * var8;
            float var10 = var7.measureHeight(var1) * var8;
            float var11 = var7.getFx() * (var2 - var9);
            float var12 = var7.getFy() * (var3 - var10);
            var5.add(new HudManager.Placement(var7, var11, var12, var9, var10));
         }
      }

      return var5;
   }

   public void render(NVGRenderer var1, float var2, float var3) {
      for (HudManager.Placement var5 : this.layout(var1, var2, var3, false)) {
         this.renderPlacement(var1, var5);
      }
   }

   public void renderPlacement(NVGRenderer var1, HudManager.Placement var2) {
      float var3 = var2.component().getScale();
      Theme var4 = this.themes.current();
      var1.save();
      var1.translate((float)Math.round(var2.x()), (float)Math.round(var2.y()));
      var1.scale(var3);
      HudChrome.underlay(var1, var4, 0.0F, 0.0F, var2.w() / var3, var2.h() / var3);
      var2.component().render(var1, 0.0F, 0.0F, var2.w() / var3, var2.h() / var3);
      HudChrome.overlay(var1, var4, 0.0F, 0.0F, var2.w() / var3, var2.h() / var3);
      var1.restore();
   }

   public JsonObject toJson() {
      JsonObject var1 = new JsonObject();

      for (HudComponent var3 : this.components) {
         JsonObject var4 = new JsonObject();
         var4.addProperty("fx", var3.getFx());
         var4.addProperty("fy", var3.getFy());
         var4.addProperty("scale", var3.getScale());
         var1.add(var3.getId(), var4);
      }

      return var1;
   }

   public void fromJson(JsonObject var1) {
      for (HudComponent var3 : this.components) {
         JsonObject var4 = var1.getAsJsonObject(var3.getId());
         if (var4 != null && var4.has("fx") && var4.has("fy")) {
            var3.setPosition(var4.get("fx").getAsFloat(), var4.get("fy").getAsFloat());
            if (var4.has("scale")) {
               var3.setScale(var4.get("scale").getAsFloat());
            }
         }
      }
   }

   public static record Placement(HudComponent component, float x, float y, float w, float h) {
      public boolean contains(float var1, float var2) {
         return var1 >= this.x && var1 <= this.x + this.w && var2 >= this.y && var2 <= this.y + this.h;
      }
   }
}
