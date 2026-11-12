package dev.aether.staff;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription.Resource;
import net.minecraft.resources.Identifier;

public final class StaffFontRanks {
   private static final Map<StaffFontRanks.Glyph, String> cache = new HashMap<>();
   private static long expires;

   private StaffFontRanks() {
   }

   public static String describe(Style var0, String var1) {
      if (System.nanoTime() > expires) {
         cache.clear();
         expires = System.nanoTime() + 10000000000L;
      }

      if (var0.getFont() instanceof Resource var2) {
         String var11 = var2.id().toString();
         StringBuilder var4 = new StringBuilder();

         for (int var8 : var1.codePoints().toArray()) {
            if (var8 >= 57344) {
               StaffFontRanks.Glyph var9 = new StaffFontRanks.Glyph(var11, var8);
               String var10 = cache.computeIfAbsent(var9, var0x -> resolve(var0x.font(), var0x.codepoint(), new HashSet<>(), 0));
               if (!var10.isEmpty()) {
                  var4.append(' ').append(var10);
               }
            }
         }

         return var4.toString();
      } else {
         return "";
      }
   }

   private static String resolve(String var0, int var1, Set<String> var2, int var3) {
      if (var3 <= 8 && var2.add(var0)) {
         try {
            String[] var4 = var0.split(":", 2);
            String var5 = var4.length == 2 ? var4[0] : "minecraft";
            String var6 = var4.length == 2 ? var4[1] : var4[0];
            Identifier var7 = Identifier.fromNamespaceAndPath(var5, "font/" + var6 + ".json");
            HashSet var8 = new HashSet();
            boolean var9 = false;

            for (net.minecraft.server.packs.resources.Resource var11 : Minecraft.getInstance().getResourceManager().getResourceStack(var7)) {
               try (InputStream var12 = var11.open()) {
                  byte[] var13 = var12.readNBytes(524289);
                  if (var13.length <= 524288) {
                     JsonObject var14 = JsonParser.parseString(new String(var13, StandardCharsets.UTF_8)).getAsJsonObject();
                     if (var14.has("providers")) {
                        for (JsonElement var16 : var14.getAsJsonArray("providers")) {
                           JsonObject var17 = var16.getAsJsonObject();
                           String var18 = var17.has("type") ? var17.get("type").getAsString() : "";
                           if (var18.endsWith("reference") && var17.has("id")) {
                              String var30 = resolve(var17.get("id").getAsString(), var1, new HashSet<>(var2), var3 + 1);
                              if (!var30.isEmpty()) {
                                 var8.add(var30);
                              }
                           } else if (var18.endsWith("bitmap") && var17.has("chars") && var17.has("file")) {
                              boolean var19 = false;
                              int var20 = 0;

                              for (JsonElement var22 : var17.getAsJsonArray("chars")) {
                                 for (int var26 : var22.getAsString().codePoints().toArray()) {
                                    if (var26 != 0 && var26 != 32) {
                                       var20++;
                                    }

                                    if (var26 == var1) {
                                       var19 = true;
                                    }
                                 }
                              }

                              if (var19) {
                                 String var31 = rankImage(var17.get("file").getAsString(), var20);
                                 if (var31 == null) {
                                    var9 = true;
                                 } else {
                                    var8.add(var31);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            return !var9 && var8.size() == 1 ? (String)var8.iterator().next() : "";
         } catch (LinkageError | Exception var291) {
            return "";
         }
      } else {
         return "";
      }
   }

   public static String rankImage(String var0, int var1) {
      if (var1 != 1) {
         return null;
      } else {
         String var2 = var0.substring(Math.max(var0.lastIndexOf(47), var0.lastIndexOf(58)) + 1);
         int var3 = var2.lastIndexOf(46);
         if (var3 > 0) {
            var2 = var2.substring(0, var3);
         }

         return StaffRankMatcher.match(var2, StaffDetector.DEFAULT_RANK_KEYWORDS);
      }
   }

   private static record Glyph(String font, int codepoint) {
   }
}
