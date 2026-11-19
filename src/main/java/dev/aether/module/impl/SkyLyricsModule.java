package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Sky Lyrics — renders the lyrics the Corz Spotify HUD already fetches as large, gently rotated
 * text floating in the sky ahead of the player. It reuses the exact data the Corz Spotify HUD
 * uses (its cached lyrics document + current track position) through a small reflection bridge,
 * so no Spotify/API/lyrics-fetching code is duplicated here.
 *
 * <p>Each word is its own element with a slight random rotation (clamped to ±15°), a small
 * random vertical offset and slight size variation, so the lines look hand-placed rather than
 * machine-straight. Unsung words are gray; the moment the song reaches a word's timestamp the
 * word fades to white, picks a fresh random rotation and does a short 1.1x ease-out bump.
 */
public class SkyLyricsModule extends Module {
   public final SliderSetting textScale = this.addSetting(
      new SliderSetting("Text Scale", "Size of the sky lyrics", 1.0, 0.4, 2.5, 0.1)
   );
   public final SliderSetting distance = this.addSetting(
      new SliderSetting("Distance", "How far ahead of the player the lyrics float", 60.0, 20.0, 200.0, 5.0, " b")
   );
   public final SliderSetting height = this.addSetting(
      new SliderSetting("Height", "Height of the lyrics above the player", 40.0, 10.0, 120.0, 5.0, " b")
   );
   public final SliderSetting maxRotation = this.addSetting(
      new SliderSetting("Max Rotation", "Maximum per-word tilt (clamped to 15°)", 12.0, 0.0, 15.0, 1.0, "°")
   );
   public final ColorSetting grayColor = this.addSetting(
      new ColorSetting("Gray Color", "Color of unsung words", -5263198)
   );
   public final ColorSetting whiteColor = this.addSetting(
      new ColorSetting("White Color", "Color of sung words", -1)
   );
   public final BooleanSetting glow = this.addSetting(
      new BooleanSetting("Glow", "Soft glow behind the lyrics", true)
   );
   public final SliderSetting visibleLines = this.addSetting(
      new SliderSetting("Visible Lines", "How many lines are shown around the current one", 3.0, 1.0, 5.0, 1.0)
   );
   public final SliderSetting timeOffset = this.addSetting(
      new SliderSetting("Time Offset", "Shift the lyrics timing", 0.0, -2000.0, 2000.0, 50.0, " ms")
   );

   private static final String CORE_CLASS = "com.corz.client.categories.visuals.SpotifyHUD";
   private static final String LYRICS_CLASS = CORE_CLASS + "$Lyrics";
   private static final long LOOKAHEAD_MS = 120L;

   private Class<?> corzClass;
   private Field trackField;
   private Class<?> lyricsClass;
   private Method getDocMethod;
   private boolean bridgeReady;

   public SkyLyricsModule() {
      super("Sky Lyrics", "Float the Corz Spotify HUD's synced lyrics in the sky, word by word.", Category.VISUALS);
   }

   private void initBridge() {
      if (this.bridgeReady) {
         return;
      }
      this.bridgeReady = true;
      try {
         this.corzClass = Class.forName(CORE_CLASS);
         this.trackField = this.corzClass.getDeclaredField("track");
         this.trackField.setAccessible(true);
         this.lyricsClass = Class.forName(LYRICS_CLASS);
         this.getDocMethod = this.lyricsClass.getDeclaredMethod("getDoc", String.class);
         this.getDocMethod.setAccessible(true);
      } catch (Throwable t) {
         // Corz port not present — Sky Lyrics simply has no data and renders nothing.
         this.corzClass = null;
      }
   }

   /** Current lyric snapshot, or {@code null} when there is nothing to show. */
   public SkyLyricsModule.Snapshot snapshot() {
      this.initBridge();
      if (this.corzClass == null) {
         return null;
      }
      try {
         Object track = this.trackField.get(null);
         if (track == null) {
            return null;
         }
         String lyricKey = (String) call(track, "lyricKey");
         if (lyricKey == null || lyricKey.isEmpty()) {
            return null;
         }
         Boolean playingObj = (Boolean) call(track, "playing");
         boolean playing = playingObj != null && playingObj;
         Long baseElapsed = (Long) call(track, "baseElapsedMs");
         Long stateMs = (Long) call(track, "stateMs");
         Long durationSec = (Long) call(track, "durationSec");
         if (baseElapsed == null) {
            return null;
         }
         long durationMs = (durationSec == null ? 0L : durationSec) * 1000L;
         long livePos = baseElapsed;
         if (playing && stateMs != null) {
            livePos = baseElapsed + Math.max(0L, System.currentTimeMillis() - stateMs);
         }
         if (durationMs > 0L) {
            livePos = Math.min(livePos, durationMs);
         }
         livePos = Math.max(0L, livePos);

         Object doc = this.getDocMethod.invoke(null, lyricKey);
         if (doc == null) {
            return null;
         }
         Object linesObj = call(doc, "lines");
         if (!(linesObj instanceof Object[] rawLines) || rawLines.length == 0) {
            return null;
         }
         List<Line> lines = new ArrayList<>(rawLines.length);
         for (Object raw : rawLines) {
            Long start = (Long) call(raw, "start");
            Long end = (Long) call(raw, "end");
            String text = (String) call(raw, "text");
            Object wordsObj = call(raw, "words");
            Object wsObj = call(raw, "ws");
            Object weObj = call(raw, "we");
            String[] words = wordsObj instanceof String[] w ? w : new String[0];
            long[] ws = wsObj instanceof long[] a ? a : new long[0];
            long[] we = weObj instanceof long[] a ? a : new long[0];
            if (words.length != ws.length || words.length != we.length) {
               ws = new long[words.length];
               we = new long[words.length];
            }
            if (words.length > 0 && ws.length == 0) {
               synthTimings(start == null ? 0L : start, end == null ? 0L : end, words, ws, we);
            }
            lines.add(new Line(start == null ? 0L : start, end == null ? 0L : end, text == null ? "" : text, words, ws, we));
         }

         long pos = livePos + (long)this.timeOffset.getInt();
         int currentLine = indexOfLine(lines, pos);
         int currentWord = currentLine >= 0 ? indexOfWord(lines.get(currentLine), pos) : -1;
         return new SkyLyricsModule.Snapshot(playing, pos, lines, currentLine, currentWord);
      } catch (Throwable t) {
         return null;
      }
   }

   /** Splits a line's duration across its words proportionally to word length. */
   private static void synthTimings(long start, long end, String[] words, long[] ws, long[] we) {
      long dur = Math.max(0L, end - start);
      int total = 0;
      for (String w : words) {
         total += Math.max(1, w.length());
      }
      long cum = start;
      for (int i = 0; i < words.length; i++) {
         long span = dur * Math.max(1, words[i].length()) / Math.max(1, total);
         ws[i] = cum;
         we[i] = cum + span;
         cum += span;
      }
   }

   private static int indexOfLine(List<Line> lines, long pos) {
      int best = -1;
      for (int i = 0; i < lines.size(); i++) {
         Line l = lines.get(i);
         if (pos >= l.start && pos < l.end) {
            return i;
         }
         if (pos >= l.end) {
            best = i;
         }
      }
      return best;
   }

   private static int indexOfWord(Line line, long pos) {
      int last = -1;
      for (int i = 0; i < line.words.length; i++) {
         if (pos >= line.ws[i] && pos < line.we[i]) {
            return i;
         }
         if (pos >= line.we[i]) {
            last = i;
         }
      }
      return last;
   }

   private static Object call(Object target, String name) {
      try {
         Method m = target.getClass().getMethod(name);
         m.setAccessible(true);
         return m.invoke(target);
      } catch (Throwable t) {
         return null;
      }
   }

   public record Line(long start, long end, String text, String[] words, long[] ws, long[] we) {
   }

   public record Snapshot(boolean playing, long positionMs, List<Line> lines, int currentLine, int currentWord) {
   }
}
