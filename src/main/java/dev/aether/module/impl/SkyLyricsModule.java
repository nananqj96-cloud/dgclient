package dev.aether.module.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aether.AetherClient;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.spotify.SpotifyState;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sky Lyrics — reads the current track from the client's own Spotify stack (SpotifyService,
 * backed by SpotifyMediaBridge / SpotifyWindowTitle / WindowsSpotify), fetches and caches the
 * synced lyrics for that track itself, and exposes them as a snapshot the SkyLyricsRenderer
 * draws as large, gently rotated text floating in the sky ahead of the player.
 *
 * <p>Each word is its own element with a slight random rotation (clamped to ±15°), a small
 * random vertical offset and slight size variation. Unsung words are gray; the moment the song
 * reaches a word's timestamp the word fades to white, picks a fresh random rotation and does a
 * short 1.1x ease-out bump.
 */
public class SkyLyricsModule extends Module {
   private static final Pattern LINE_TS = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?\\]");
   private static final Pattern WORD_TS = Pattern.compile("<(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?>");
   private static final int MAX_CACHE = 24;

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

   private final ExecutorService fetcher = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "Aether-SkyLyrics");
      t.setDaemon(true);
      return t;
   });
   private final Map<String, List<Line>> cache = new LinkedHashMap<>(16, 0.75F, true) {
      protected boolean removeEldestEntry(Map.Entry<String, List<Line>> eldest) {
         return this.size() > MAX_CACHE;
      }
   };
   private volatile String pendingKey;
   private volatile String activeKey;

   public SkyLyricsModule() {
      super("Sky Lyrics", "Float the current Spotify track's synced lyrics in the sky, word by word.", Category.VISUALS);
      this.setEnabled(true);
   }

   /** Status line shown while there is nothing (or no lyrics) to render. */
   public String statusText() {
      SpotifyState state = currentState();
      if (state == null || !state.active()) {
         return "Waiting for Spotify";
      }
      String key = cacheKey(state);
      if (this.cache.containsKey(key)) {
         return null;
      }
      return this.pendingKey != null && this.pendingKey.equals(key) ? "Loading lyrics…" : "Waiting for Spotify";
   }

   private SpotifyState currentState() {
      try {
         return AetherClient.spotify() == null ? null : AetherClient.spotify().state();
      } catch (Throwable t) {
         return null;
      }
   }

   private static String cacheKey(SpotifyState state) {
      return (state.artist() + "|" + state.title() + "|" + state.durMs()).toLowerCase(Locale.ROOT);
   }

   /** Current lyric snapshot, or {@code null} when there is nothing to show. */
   public SkyLyricsModule.Snapshot snapshot() {
      SpotifyState state = currentState();
      if (state == null || !state.active() || state.title().isEmpty()) {
         return null;
      }
      String key = cacheKey(state);
      this.activeKey = key;
      List<Line> lines = this.cache.get(key);
      if (lines == null) {
         this.requestFetch(key, state);
         return null;
      }
      if (lines.isEmpty()) {
         return null;
      }
      long pos = state.livePosMs() + (long)this.timeOffset.getInt();
      int currentLine = indexOfLine(lines, pos);
      int currentWord = currentLine >= 0 ? indexOfWord(lines.get(currentLine), pos) : -1;
      return new SkyLyricsModule.Snapshot(state.playing(), pos, lines, currentLine, currentWord);
   }

   private void requestFetch(String key, SpotifyState state) {
      if (this.pendingKey != null && this.pendingKey.equals(key)) {
         return;
      }
      this.pendingKey = key;
      this.fetcher.execute(() -> {
         try {
            List<Line> fetched = fetchLyrics(state);
            if (fetched != null && !fetched.isEmpty()) {
               this.cache.put(key, fetched);
            }
         } catch (Throwable ignored) {
         } finally {
            if (this.pendingKey != null && this.pendingKey.equals(key)) {
               this.pendingKey = null;
            }
         }
      });
   }

   /** Fetches synced lyrics (LRC) for the track from the public LRCLib API. */
   private List<Line> fetchLyrics(SpotifyState state) throws Exception {
      String artist = enc(state.artist());
      String track = enc(state.title());
      String duration = Long.toString(state.durMs() / 1000L);
      JsonObject json = getJson("https://lrclib.net/api/get?artist_name=" + artist + "&track_name=" + track + "&duration=" + duration);
      if (json == null) {
         json = searchFirst(track, artist);
      }
      if (json == null) {
         return List.of();
      }
      String lrc = json.has("syncedLyrics") && !json.get("syncedLyrics").isJsonNull()
         ? json.get("syncedLyrics").getAsString()
         : (json.has("plainLyrics") && !json.get("plainLyrics").isJsonNull() ? json.get("plainLyrics").getAsString() : null);
      if (lrc == null || lrc.isBlank()) {
         return List.of();
      }
      return parseLrc(lrc);
   }

   private JsonObject getJson(String url) throws Exception {
      HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
      conn.setConnectTimeout(10000);
      conn.setReadTimeout(10000);
      conn.setRequestProperty("Accept", "application/json");
      conn.setRequestMethod("GET");
      conn.setInstanceFollowRedirects(true);
      if (conn.getResponseCode() != 200) {
         return null;
      }
      StringBuilder body = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
         String line;
         while ((line = reader.readLine()) != null) {
            body.append(line);
         }
      }
      return JsonParser.parseString(body.toString()).getAsJsonObject();
   }

   private JsonObject searchFirst(String track, String artist) throws Exception {
      HttpURLConnection conn = (HttpURLConnection) new URI(
         "https://lrclib.net/api/search?track_name=" + track + "&artist_name=" + artist
      ).toURL().openConnection();
      conn.setConnectTimeout(10000);
      conn.setReadTimeout(10000);
      conn.setRequestProperty("Accept", "application/json");
      conn.setRequestMethod("GET");
      conn.setInstanceFollowRedirects(true);
      if (conn.getResponseCode() != 200) {
         return null;
      }
      StringBuilder body = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
         String line;
         while ((line = reader.readLine()) != null) {
            body.append(line);
         }
      }
      JsonArray arr = JsonParser.parseString(body.toString()).getAsJsonArray();
      for (int i = 0; i < arr.size(); i++) {
         JsonObject obj = arr.get(i).getAsJsonObject();
         if (obj.has("syncedLyrics") && !obj.get("syncedLyrics").isJsonNull()) {
            return obj;
         }
      }
      return null;
   }

   private static String enc(String s) {
      return URLEncoder.encode(s, StandardCharsets.UTF_8);
   }

   /** Parses LRC text (line timestamps + optional word-level tags) into lyric lines. */
   static List<Line> parseLrc(String lrc) {
      List<Line> out = new ArrayList<>();
      // State for the line currently being built. Scalar state lives in single-element arrays so
      // the flush helper can mutate it (lambdas cannot reassign captured locals).
      long[] lineStart = {-1L};
      StringBuilder text = new StringBuilder();
      List<String> words = new ArrayList<>();
      List<Long> wordStamps = new ArrayList<>();
      StringBuilder currentWord = new StringBuilder();
      boolean[] hasWordTags = {false};

      Runnable flush = () -> {
         String raw = text.toString().trim();
         if (lineStart[0] >= 0 || !raw.isEmpty()) {
            if (currentWord.length() > 0) {
               words.add(currentWord.toString());
            }
            String[] wordArr = words.toArray(new String[0]);
            long[] ws = new long[wordArr.length];
            long[] we = new long[wordArr.length];
            long start = lineStart[0] >= 0 ? lineStart[0] : 0L;
            long end = start + 4000L;
            if (wordArr.length > 0) {
               if (hasWordTags[0] && wordStamps.size() == wordArr.length) {
                  for (int i = 0; i < wordArr.length; i++) {
                     ws[i] = wordStamps.get(i);
                     we[i] = i + 1 < wordArr.length ? wordStamps.get(i + 1) : end;
                  }
               } else {
                  synthTimings(start, end, wordArr, ws, we);
               }
            }
            out.add(new Line(start, end, raw, wordArr, ws, we));
         }
         text.setLength(0);
         words.clear();
         wordStamps.clear();
         currentWord.setLength(0);
         hasWordTags[0] = false;
         lineStart[0] = -1L;
      };

      Matcher lineMatcher = LINE_TS.matcher(lrc);
      Matcher wordMatcher = WORD_TS.matcher(lrc);
      int i = 0;
      while (i < lrc.length()) {
         char c = lrc.charAt(i);
         if (c == '\n' || c == '\r') {
            flush.run();
            i++;
            continue;
         }
         if (c == '[') {
            lineMatcher.find(i);
            if (lineMatcher.start() == i) {
               flush.run();
               lineStart[0] = toMs(lineMatcher);
               i = lineMatcher.end();
               continue;
            }
         }
         if (c == '<') {
            wordMatcher.find(i);
            if (wordMatcher.start() == i) {
               if (currentWord.length() > 0) {
                  words.add(currentWord.toString());
                  currentWord.setLength(0);
               }
               wordStamps.add(toMs(wordMatcher));
               hasWordTags[0] = true;
               i = wordMatcher.end();
               continue;
            }
         }
         if (!hasWordTags[0]) {
            text.append(c);
         } else {
            currentWord.append(c);
         }
         i++;
      }
      if (currentWord.length() > 0) {
         words.add(currentWord.toString());
      }
      flush.run();
      return out;
   }

   private static long toMs(Matcher m) {
      int min = Integer.parseInt(m.group(1));
      int sec = Integer.parseInt(m.group(2));
      int frac = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
      int decimals = m.group(3) == null ? 0 : m.group(3).length();
      return ((long)min * 60L + sec) * 1000L + frac * (long)Math.pow(10, 3 - decimals);
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

   public record Line(long start, long end, String text, String[] words, long[] ws, long[] we) {
   }

   public record Snapshot(boolean playing, long positionMs, List<Line> lines, int currentLine, int currentWord) {
   }
}
