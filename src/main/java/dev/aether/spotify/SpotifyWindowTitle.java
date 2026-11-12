package dev.aether.spotify;

import java.util.Locale;

public final class SpotifyWindowTitle {
   private SpotifyWindowTitle() {
   }

   public static boolean isSpotifyExecutable(String var0) {
      if (var0 == null) {
         return false;
      } else {
         String var1 = var0.replace('\\', '/');
         return var1.substring(var1.lastIndexOf(47) + 1).equalsIgnoreCase("Spotify.exe");
      }
   }

   public static SpotifyState parse(String var0, long var1) {
      if (var0 != null && !var0.isBlank()) {
         String var3 = var0.trim();
         String var4 = var3.toLowerCase(Locale.ROOT);
         if (!var4.equals("spotify") && !var4.equals("spotify premium") && !var4.equals("spotify free")) {
            var3 = var3.replaceFirst("(?i)\\s+[-–—]\\s+Spotify(?: Premium| Free)?$", "");
            String[] var5 = var3.split("\\s+[-–—]\\s+", 2);
            return var5.length == 2 && !var5[0].isBlank() && !var5[1].isBlank()
               ? new SpotifyState(true, var5[1].trim(), var5[0].trim(), 0L, 0L, true, false, 0, -1, var1)
               : SpotifyState.INACTIVE;
         } else {
            return SpotifyState.INACTIVE;
         }
      } else {
         return SpotifyState.INACTIVE;
      }
   }
}
