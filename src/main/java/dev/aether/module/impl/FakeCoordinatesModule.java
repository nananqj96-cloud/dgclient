package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;

/**
 * Spoofs the coordinates shown in chat and the F3 debug screen with random offsets, so other players
 * (and log readers) see fake position data while you keep your real location private.
 */
public class FakeCoordinatesModule extends Module {
   private static final Pattern COORD_PATTERN = Pattern.compile(
      "(?:(?<=\\u00a7[0-9a-fk-orA-FK-OR])|(?<![\\p{L}\\p{N}\\p{Sc}_.\\-]))(-?\\d{1,8}(?:\\.\\d+)?)(?:,\\s+|\\s*/\\s*|\\s+)(-?\\d{1,8}(?:\\.\\d+)?)(?![\\p{L}\\p{N}\\p{Sc}_%]|\\.\\d)"
   );
   private static final SecureRandom RANDOM = new SecureRandom();

   public final BooleanSetting hideBiome = this.addSetting(
      new BooleanSetting("Hide Biome", "Also hides the biome line on the F3 screen", true)
   );

   private int offsetX;
   private int offsetZ;

   public FakeCoordinatesModule() {
      super("Fake Coordinates", "Spoof coordinates shown in chat and F3", Category.MISC);
      this.reroll();
   }

   public void reroll() {
      this.offsetX = RANDOM.nextInt(2_000_000) - 1_000_000;
      this.offsetZ = RANDOM.nextInt(2_000_000) - 1_000_000;
   }

   public int getOffsetX() {
      return this.offsetX;
   }

   public int getOffsetZ() {
      return this.offsetZ;
   }

   public String spoof(String text) {
      if (text == null || text.isEmpty()) {
         return text;
      }
      Matcher m = COORD_PATTERN.matcher(text);
      StringBuilder sb = new StringBuilder();
      while (m.find()) {
         double x = parseDouble(m.group(1));
         double z = parseDouble(m.group(2));
         String fakeX = formatCoord(x + this.offsetX);
         String fakeZ = formatCoord(z + this.offsetZ);
         m.appendReplacement(sb, Matcher.quoteReplacement(fakeX + m.group(3) + fakeZ));
      }
      m.appendTail(sb);
      return sb.toString();
   }

   public net.minecraft.network.chat.Component spoof(net.minecraft.network.chat.Component text) {
      if (text == null) {
         return text;
      }
      String s = text.getString();
      String fake = this.spoof(s);
      return net.minecraft.network.chat.Text.literal(fake);
   }

   private static double parseDouble(String s) {
      try {
         return Double.parseDouble(s);
      } catch (NumberFormatException e) {
         return 0.0;
      }
   }

   private static String formatCoord(double v) {
      long l = Math.round(v);
      return String.valueOf(l);
   }

   @Override
   protected void onEnable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         this.reroll();
      }
   }
}
