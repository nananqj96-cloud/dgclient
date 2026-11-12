package dev.aether.staff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class StaffBadgeDiagnostic {
   private static final Gson JSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

   private StaffBadgeDiagnostic() {
   }

   public static boolean validName(String var0) {
      return var0 != null && var0.matches("[A-Za-z0-9_]{1,16}");
   }

   public static JsonObject capture(String var0, Component var1, Component var2, Component var3, Component var4, String var5, StaffDetector.DetectConfig var6) {
      if (!validName(var0)) {
         throw new IllegalArgumentException("Invalid player name");
      } else {
         JsonObject var7 = new JsonObject();
         var7.addProperty("formatVersion", 1);
         var7.addProperty("player", var0);
         var7.addProperty("detectBy", var6.mode());
         var7.addProperty("fontIcons", var6.fontIcons());
         var7.addProperty("allowedSymbols", var6.symbols());
         var7.addProperty("starRank", var6.markerRank());
         var7.addProperty("starMappings", var6.mappings());
         var7.add("rankKeywords", JSON.toJsonTree(var6.rankKeywords()));
         var7.addProperty("teamName", bounded(var5));
         var7.add("display", describe(var1, var6));
         var7.add("prefix", describe(var2, var6));
         var7.add("suffix", describe(var3, var6));
         var7.add("teamDisplay", describe(var4, var6));
         return var7;
      }
   }

   private static JsonElement describe(Component var0, StaffDetector.DetectConfig var1) {
      if (var0 == null) {
         return JsonNull.INSTANCE;
      } else {
         JsonArray var2 = new JsonArray();
         var0.visit((var2xx, var3) -> {
            String var4 = var2xx.getFont().toString();
            Integer var5 = var2xx.getColor() == null ? null : var2xx.getColor().getValue();
            var2.add(describeRun(var3, var4, var5, var1));
            return var2.size() >= 64 ? Optional.of(Boolean.TRUE) : Optional.empty();
         }, Style.EMPTY);
         return var2;
      }
   }

   public static JsonObject describeRun(String var0, String var1, Integer var2, StaffDetector.DetectConfig var3) {
      JsonObject var4 = new JsonObject();
      String var5 = bounded(var0);
      var4.addProperty("text", var5);
      var4.addProperty("font", bounded(var1));
      var4.addProperty("color", var2 == null ? null : String.format(Locale.ROOT, "#%06X", var2 & 16777215));
      var4.addProperty("passesCurrentGreenFilter", StaffDetector.isGreenStaffColor(var2));
      JsonArray var6 = new JsonArray();
      var5.codePoints().forEach(var3xx -> {
         JsonObject var4x = new JsonObject();
         var4x.addProperty("codepoint", String.format(Locale.ROOT, "U+%04X", var3xx));
         var4x.addProperty("currentMarkerLabel", StaffDetector.markerLabel(var3xx, var2, var3));
         var6.add(var4x);
      });
      var4.add("characters", var6);
      return var4;
   }

   private static String bounded(String var0) {
      return var0 == null ? "" : var0.substring(0, Math.min(var0.length(), 1024));
   }

   public static Path write(Path var0, JsonObject var1) throws IOException {
      Files.createDirectories(var0);
      Path var2 = Files.createTempFile(var0, "staff-badge-", ".json");
      Files.writeString(var2, JSON.toJson(var1), StandardCharsets.UTF_8);
      return var2;
   }
}
