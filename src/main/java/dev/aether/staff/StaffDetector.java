package dev.aether.staff;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.FontDescription.Resource;

public final class StaffDetector {
   public static final int CONFIRMED_STAR = 9733;
   public static final int CONFIRMED_COLOR = 64648;
   public static final String MODE_STAR_RANK = "Star + Rank";
   public static final String MODE_STAR = "Star Only";
   public static final String MODE_RANK = "Rank Only";
   public static final String MODE_NAMES = "Names Only";
   public static final String DEFAULT_SYMBOLS = "★☆✦✧✪✩✫✬✭✮✯⭐✰❂⚝✴✵✶✷✸✹⍟";
   public static final List<String> DEFAULT_RANK_KEYWORDS = List.of(
      "coowner",
      "owner",
      "manager",
      "administrator",
      "admin",
      "developer",
      "dev",
      "srmod",
      "seniormod",
      "moderator",
      "mod",
      "srhelper",
      "seniorhelper",
      "helper",
      "trialmod",
      "trial",
      "builder",
      "support",
      "staff"
   );
   public static final String DEFAULT_RANK_KEYWORDS_STRING = String.join(", ", DEFAULT_RANK_KEYWORDS);
   private static final Map<String, String> RANK_LABELS = Map.ofEntries(
      Map.entry("coowner", "Co-Owner"),
      Map.entry("owner", "Owner"),
      Map.entry("manager", "Manager"),
      Map.entry("administrator", "Admin"),
      Map.entry("admin", "Admin"),
      Map.entry("developer", "Dev"),
      Map.entry("dev", "Dev"),
      Map.entry("srmod", "Sr.Mod"),
      Map.entry("seniormod", "Sr.Mod"),
      Map.entry("moderator", "Mod"),
      Map.entry("mod", "Mod"),
      Map.entry("srhelper", "Sr.Helper"),
      Map.entry("seniorhelper", "Sr.Helper"),
      Map.entry("helper", "Helper"),
      Map.entry("trialmod", "Trial"),
      Map.entry("trial", "Trial"),
      Map.entry("builder", "Builder"),
      Map.entry("support", "Support"),
      Map.entry("staff", "Staff")
   );

   public static boolean matchesConfirmedBadge(int var0, Integer var1, String var2) {
      return var0 == 9733 && var1 != null && (var1 & 16777215) == 64648 && "minecraft:default".equals(var2);
   }

   public static StaffEntry classifyConfirmedBadge(String var0, Component var1, Component var2, Component var3, int var4) {
      if (var0 != null && !var0.isEmpty()) {
         boolean var5 = var1 != null ? hasConfirmedBadge(var1) : hasConfirmedBadge(var2) || hasConfirmedBadge(var3);
         return var5 ? new StaffEntry(var0, "", -16712568, false, var4, 1) : null;
      } else {
         return null;
      }
   }

   private static boolean hasConfirmedBadge(Component var0) {
      return var0 == null ? false : var0.visit((var0x, var1) -> {
         if (var0x.getFont() instanceof Resource var2) {
            Integer var4 = var0x.getColor() == null ? null : var0x.getColor().getValue();
            return var1.codePoints().anyMatch(var2xx -> matchesConfirmedBadge(var2xx, var4, var2.id().toString())) ? Optional.of(true) : Optional.empty();
         } else {
            return Optional.empty();
         }
      }, Style.EMPTY).orElse(false);
   }

   private StaffDetector() {
   }

   public static StaffEntry classify(
      String var0, Component var1, Component var2, Component var3, String var4, boolean var5, int var6, StaffDetector.DetectConfig var7
   ) {
      if (var0 != null && !var0.isEmpty() && (!var5 || var7.showVanished())) {
         boolean var8 = var7.names().contains(var0.toLowerCase(Locale.ROOT));
         StaffDetector.Marker var9 = scanStaffMarker(var1, var7);
         if (var9 == null) {
            var9 = scanStaffMarker(var2, var7);
         }

         if (var9 == null) {
            var9 = scanStaffMarker(var3, var7);
         }

         String var10 = rankText(var2, var0) + " " + rankText(var3, var0) + " " + rankText(var1, var0) + " " + stripName(var4 == null ? "" : var4, var0);
         StaffDetector.Rank var11 = deriveRank(var10, var7.rankKeywords());
         String var12 = var9 == null ? "" : var9.label();
         String var14 = var7.mode();

         boolean var13 = switch (var14) {
            case "Star Only" -> var9 != null;
            case "Rank Only" -> var11 != null;
            case "Names Only" -> false;
            default -> var9 != null || var11 != null;
         };
         return !var8 && !var13
            ? null
            : new StaffEntry(
               var0,
               var11 == null ? var12 : var11.label(),
               var9 == null ? 0 : var9.color(),
               var5,
               var6,
               var11 == null ? (var9 == null ? 0 : 1) : var11.priority()
            );
      } else {
         return null;
      }
   }

   static Integer scanMarker(Component var0, StaffDetector.DetectConfig var1) {
      StaffDetector.Marker var2 = scanStaffMarker(var0, var1);
      return var2 == null ? null : var2.color();
   }

   private static StaffDetector.Marker scanStaffMarker(Component var0, StaffDetector.DetectConfig var1) {
      return var0 == null ? null : (StaffDetector.Marker)var0.visit((var1xx, var2) -> {
         int var4 = 0;

         while (var4 < var2.length()) {
            int var3 = var2.codePointAt(var4);
            TextColor var5 = var1xx.getColor();
            Integer var6 = var5 == null ? null : var5.getValue();
            String var7 = markerLabel(var3, var6, var1);
            if (var7 == null && var1.fontIcons() && isPrivateUse(var3)) {
               StaffDetector.Rank var8 = deriveRank(StaffFontRanks.describe(var1xx, new String(Character.toChars(var3))), var1.rankKeywords());
               if (var8 != null) {
                  var7 = var8.label();
               }
            }

            if (var7 != null) {
               return Optional.of(new StaffDetector.Marker(var6 == null ? 0 : 0xFF000000 | var6, var7));
            }

            var4 += Character.charCount(var3);
         }

         return Optional.empty();
      }, Style.EMPTY).orElse(null);
   }

   private static String rankText(Component var0, String var1) {
      if (var0 == null) {
         return "";
      } else {
         StringBuilder var2 = new StringBuilder(stripName(var0.getString(), var1));
         var0.visit((var2xx, var3) -> {
            var2.append(' ').append(stripName(var3, var1));
            var2.append(StaffFontRanks.describe(var2xx, var3));
            return Optional.empty();
         }, Style.EMPTY);
         return var2.toString();
      }
   }

   private static boolean isPrivateUse(int var0) {
      return Character.getType(var0) == 18;
   }

   public static String markerLabel(int var0, Integer var1, StaffDetector.DetectConfig var2) {
      if (!isGreenStaffColor(var1)) {
         return null;
      } else {
         String var3 = mappedRank(var0, var2);
         if (var2.symbols().indexOf(var0) >= 0 || var2.fontIcons() && isPrivateUse(var0) && var3 != null) {
            return var3 == null ? var2.markerRank() : var3;
         } else {
            return null;
         }
      }
   }

   public static boolean isGreenStaffColor(Integer var0) {
      if (var0 == null) {
         return false;
      } else {
         int var1 = var0 >> 16 & 0xFF;
         int var2 = var0 >> 8 & 0xFF;
         int var3 = var0 & 0xFF;
         return var2 >= 60 && var2 - var1 >= 35 && var2 - var3 >= 35;
      }
   }

   private static StaffDetector.Rank deriveRank(String var0, List<String> var1) {
      String var2 = StaffRankMatcher.match(var0, var1);
      if (var2 == null) {
         return null;
      } else {
         int var3 = -1;

         for (int var4 = 0; var4 < DEFAULT_RANK_KEYWORDS.size(); var4++) {
            if (StaffRankMatcher.canonical(DEFAULT_RANK_KEYWORDS.get(var4)).equals(var2)) {
               var3 = var4;
               break;
            }
         }

         return new StaffDetector.Rank(labelFor(var2), var3 < 0 ? 2 : DEFAULT_RANK_KEYWORDS.size() - var3 + 2);
      }
   }

   private static String labelFor(String var0) {
      String var1 = RANK_LABELS.get(var0);
      if (var1 != null) {
         return var1;
      } else {
         return var0.isEmpty() ? "Staff" : Character.toUpperCase(var0.charAt(0)) + var0.substring(1);
      }
   }

   private static String plain(Component var0) {
      return var0 == null ? "" : var0.getString();
   }

   private static String stripName(String var0, String var1) {
      return var1 != null && !var1.isEmpty() ? StaffRankMatcher.removePlayerName(var0, var1) : var0;
   }

   public static String assignedRank(String var0, StaffDetector.DetectConfig var1) {
      for (int var5 : var0.codePoints().toArray()) {
         String var6 = mappedRank(var5, var1);
         if (var6 != null) {
            return var6;
         }
      }

      return var1.markerRank();
   }

   private static String mappedRank(int var0, StaffDetector.DetectConfig var1) {
      for (String var5 : var1.mappings().split("[,;\\r\\n]+")) {
         String[] var6 = var5.trim().split("=", 2);
         if (var6.length == 2) {
            String var7 = var6[0].trim();

            int var8;
            try {
               var8 = var7.toUpperCase(Locale.ROOT).startsWith("U+")
                  ? Integer.parseInt(var7.substring(2), 16)
                  : (var7.codePointCount(0, var7.length()) == 1 ? var7.codePointAt(0) : -1);
            } catch (RuntimeException var10) {
               continue;
            }

            if (var8 == var0) {
               String var9 = StaffRankMatcher.canonical(var6[1]);
               if (RANK_LABELS.containsKey(var9)) {
                  return labelFor(var9);
               }
            }
         }
      }

      return null;
   }

   public static record DetectConfig(
      String mode, Set<String> names, List<String> rankKeywords, String symbols, boolean fontIcons, boolean showVanished, String markerRank, String mappings
   ) {
      public DetectConfig(String var1, Set<String> var2, List<String> var3, String var4, boolean var5, boolean var6) {
         this(var1, var2, var3, var4, var5, var6, "Staff", "");
      }
   }

   private static record Marker(int color, String label) {
   }

   private static record Rank(String label, int priority) {
   }
}
