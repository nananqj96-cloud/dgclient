package dev.aether.module.impl;

import dev.aether.AetherClient;

import com.sun.jna.Platform;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpotifyHudModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
    private static final long POLL_INTERVAL_MS = 500L;
    private static final float TINT_STEP_IN = 0.016666668F;
    private static final float TINT_STEP_OUT = 0.033333335F;
    private static final long WINDOW_POLL_MS = 1000L;
    private static final long REPOLL_AFTER_KEY_MS = 250L;
    private static final long KEY_COOLDOWN_MS = 200L;
    private static final int ART_MAX_SIDE = 256;
    private static final int STABLE_POLLS = 2;
    private static final int SESSION_HICCUPS = 2;
    private static final long ERROR_LOG_INTERVAL_MS = 60000L;
    private final BooleanSetting showArtist = this.addSetting(new BooleanSetting("Show Artist", true));
    private final BooleanSetting showAlbumArt = this.addSetting(new BooleanSetting("Show Album Art", true));
    private final BooleanSetting showProgress = this.addSetting(new BooleanSetting("Show Progress", true));
    private final BooleanSetting scrollTitles = this.addSetting(new BooleanSetting("Scroll Long Titles", true));
    private final BooleanSetting showLyrics = this.addSetting(new BooleanSetting("Show Lyrics", true));
    private final ModeSetting lyricLines = this.addSetting(new ModeSetting("Lyric Lines", "2", "1", "2", "3"));
    private final ModeSetting lyricMotion = this.addSetting(new ModeSetting("Lyric Motion", "Wave", "Wave", "Glide", "Fade"));
    private final ModeSetting lyricHighlight = this.addSetting(new ModeSetting("Lyric Highlight", "Karaoke", "Karaoke", "Line", "Off"));
    private final BooleanSetting lyricsOnline = this.addSetting(new BooleanSetting("Lyrics Online", true));
    private final BooleanSetting skyLyrics = this.addSetting(new BooleanSetting("Sky Lyrics", true));
    private final BooleanSetting skyTitle = this.addSetting(new BooleanSetting("Sky Song Title", false));
    private final BooleanSetting skyEmojis = this.addSetting(new BooleanSetting("Sky Emojis", true));
    private final ModeSetting skyEmojiStyle = this.addSetting(
        "sky-emoji-style", new ModeSetting("Sky Emoji Style", "Behind", "Beside", "Behind", "Replace Word")
    );
    private final BooleanSetting skyFollow = this.addSetting(new BooleanSetting("Sky Follows View", true));
    private final SliderSetting skyHeight = this.addSetting(new SliderSetting("Sky Height", 10.0, 10.0, 70.0, 1.0F));
    private final SliderSetting skySize = this.addSetting(new SliderSetting("Sky Text Size", 1.25, 0.3, 3.0, 0.05F));
    private final SliderSetting skyTitleSize = this.addSetting(new SliderSetting("Sky Title Size", 1.0, 0.3, 3.0, 0.05F));
    private final SliderSetting skyEmojiSize = this.addSetting(new SliderSetting("Sky Emoji Size", 1.35, 0.3, 3.0, 0.05F));
    private final SliderSetting skyOpacity = this.addSetting(new SliderSetting("Sky Opacity", 0.9, 0.1, 1.0, 0.05F));
    private final ColorSetting skyTextColor = this.addSetting(new ColorSetting("Sky Text Color", -1));
    private final ColorSetting skyTitleColor = this.addSetting(new ColorSetting("Sky Title Color", -1447442));
    private final BooleanSetting skyGlowAtmosphere = this.addSetting(new BooleanSetting("Sky Glow From Atmosphere", true));
    private final ColorSetting skyGlowColor = this.addSetting(new ColorSetting("Sky Glow Color", -7713281));
    private final SliderSetting skyGlow = this.addSetting(new SliderSetting("Sky Glow", 0.8, 0.0, 1.0, 0.05F));
    private final BooleanSetting easterEggs = this.addSetting(new BooleanSetting("Easter Eggs", true));
    private final ModeSetting pinkWater = this.addSetting(new ModeSetting("Pink Water", PinkWater.Design.DEFAULT.label(), PinkWater.modes()));
    private final SliderSetting cardWidth = this.addSetting(new SliderSetting("Card Width", 320.0, 200.0, 560.0, 1.0F));
    private final ColorSetting titleColor = this.addSetting(new ColorSetting("Title Color", -1447442));
    private final ColorSetting artistColor = this.addSetting(new ColorSetting("Artist Color", -6052949));
    private final ColorSetting lyricsColor = this.addSetting(new ColorSetting("Lyrics Color", -657929));
    private final ColorSetting upcomingLyricsColor = this.addSetting(new ColorSetting("Upcoming Lyrics Color", -6052949));
    private final ColorSetting highlightColor = this.addSetting(new ColorSetting("Highlight Color", -14756000));
    private final BooleanSetting showWhenIdle = this.addSetting(new BooleanSetting("Show When Idle", false));
    private final BooleanSetting mediaKeys = this.addSetting(new BooleanSetting("Media Keys", false));
    private final KeybindSetting playPauseKey = this.addSetting(new KeySetting("Play / Pause", -1));
    private final KeybindSetting nextKey = this.addSetting(new KeySetting("Next Track", -1));
    private final KeybindSetting previousKey = this.addSetting(new KeySetting("Previous Track", -1));
    private volatile NowPlaying nowPlaying = NowPlaying.WAITING;
    private volatile SpotifyHudModule.TrackLyrics lyrics;
    private volatile SpotifyHudModule.Poller poller;
    private final Object publishLock = new Object();
    private final KeySetting[] keys = new KeySetting[]{this.playPauseKey, this.nextKey, this.previousKey};
    private final SpotifyWindow.MediaKey[] actions = new SpotifyWindow.MediaKey[]{
        SpotifyWindow.MediaKey.PLAY_PAUSE, SpotifyWindow.MediaKey.NEXT, SpotifyWindow.MediaKey.PREVIOUS
    };
    private final boolean[] wasDown = new boolean[this.keys.length];
    private long lastKeyAt;

    public SpotifyHudModule() {
        super("spotify-hud", "Shows what Spotify is playing, with synced lyrics and optional media keys.", Category.CLIENT);
        this.usesNotifications();
        this.artistColor.visibleWhen(this.showArtist::get);
        this.lyricsColor.visibleWhen(() -> this.showLyrics.get() && !this.lyricHighlight.is("Line"));
        this.upcomingLyricsColor.visibleWhen(this.showLyrics::get);
        this.highlightColor.visibleWhen(() -> this.showLyrics.get() && !this.lyricHighlight.is("Off"));
        this.lyricLines.visibleWhen(this.showLyrics::get);
        this.lyricMotion.visibleWhen(this.showLyrics::get);
        this.lyricHighlight.visibleWhen(this.showLyrics::get);
        this.lyricsOnline.visibleWhen(this.showLyrics::get);
        this.skyLyrics.visibleWhen(this.showLyrics::get);
        this.skyTitle.visibleWhen(this::skyLyrics);
        this.skyEmojis.visibleWhen(this::skyLyrics);
        this.skyFollow.visibleWhen(this::skyLyrics);
        this.skyHeight.visibleWhen(this::skyLyrics);
        this.skySize.visibleWhen(this::skyLyrics);
        this.skyTitleSize.visibleWhen(() -> this.skyLyrics() && this.skyTitle.get());
        this.skyEmojiStyle.visibleWhen(() -> this.skyLyrics() && this.skyEmojis.get());
        this.skyEmojiSize.visibleWhen(() -> this.skyLyrics() && this.skyEmojis.get());
        this.skyOpacity.visibleWhen(this::skyLyrics);
        this.skyTextColor.visibleWhen(this::skyLyrics);
        this.skyTitleColor.visibleWhen(() -> this.skyLyrics() && this.skyTitle.get());
        this.skyGlowAtmosphere.visibleWhen(this::skyLyrics);
        this.skyGlowColor.visibleWhen(this::skyLyrics);
        this.skyGlow.visibleWhen(this::skyLyrics);
        this.lyricsOnline.onChange(() -> {
            SpotifyHudModule.Poller running = this.poller;
            if (running != null) {
                running.lyricsService.setOnline(this.lyricsOnline.get());
            }
        });
        this.pinkWater.visibleWhen(this.easterEggs::get);
        this.playPauseKey.visibleWhen(this.mediaKeys::get);
        this.nextKey.visibleWhen(this.mediaKeys::get);
        this.previousKey.visibleWhen(this.mediaKeys::get);
    }

    @Override
    protected void onEnable() {
        Arrays.fill(this.wasDown, true);
        if (!PlayerSource.supported()) {
            synchronized (this.publishLock) {
                this.nowPlaying = NowPlaying.UNSUPPORTED;
                this.lyrics = null;
            }

//             AetherClient.chat(this, ModuleChat.body().danger("Not supported").text(", only the Spotify app on Windows, macOS and Linux works"));
        } else {
            SpotifyHudModule.Poller started = new SpotifyHudModule.Poller();
            synchronized (this.publishLock) {
                this.poller = started;
                this.nowPlaying = NowPlaying.WAITING;
                this.lyrics = null;
            }

            started.start();
        }
    }

    @Override
    protected void onDisable() {
//         ThemeTint.pink(0.0F);
        PinkWater.reset(Minecraft.getInstance());
        SpotifyHudModule.Poller stopped;
        synchronized (this.publishLock) {
            stopped = this.poller;
            this.poller = null;
            this.nowPlaying = PlayerSource.supported() ? NowPlaying.WAITING : NowPlaying.UNSUPPORTED;
            this.lyrics = null;
        }

        if (stopped != null) {
            stopped.stop();
        }
    }

    private void publish(SpotifyHudModule.Poller from, NowPlaying state, SpotifyHudModule.TrackLyrics trackLyrics) {
        synchronized (this.publishLock) {
            if (from == this.poller) {
                this.nowPlaying = state;
                this.lyrics = trackLyrics;
            }
        }
    }

    public static SpotifyHudModule get() {
//         return ModuleRegistry.get(SpotifyHudModule.class);
    }

    public static SpotifyHudModule active() {
        SpotifyHudModule module = get();
        return module != null && module.isEnabled() ? module : null;
    }

    public static void hudButton(SpotifyWindow.MediaKey key) {
        SpotifyHudModule module = active();
        if (module != null) {
            module.press(key);
        }
    }

    public static boolean shouldShowHud() {
        SpotifyHudModule module = active();
        if (module == null) {
            return false;
        }

        NowPlaying now = module.nowPlaying;
        return now.status() != NowPlaying.Status.UNSUPPORTED && now.status() != NowPlaying.Status.WAITING ? now.active() || module.showWhenIdle.get() : false;
    }

    public NowPlaying nowPlaying() {
        return this.nowPlaying;
    }

    public Lyrics lyricsFor(NowPlaying now) {
        SpotifyHudModule.TrackLyrics current = this.lyrics;
        SpotifyHudModule.Poller running = this.poller;
        if (current != null && running != null && this.wantsLyrics(now)) {
            return current.title().equals(now.title()) && current.artist().equals(now.artist()) ? running.lyricsService.lookup(current.query()) : null;
        } else {
            return null;
        }
    }

    public boolean wantsLyrics(NowPlaying now) {
        return this.showLyrics.get() && now != null && now.lyricsEligible();
    }

    public boolean showArtist() {
        return this.showArtist.get();
    }

    public boolean showAlbumArt() {
        return this.showAlbumArt.get();
    }

    public boolean showProgress() {
        return this.showProgress.get();
    }

    public boolean scrollTitles() {
        return this.scrollTitles.get();
    }

    public boolean showLyrics() {
        return this.showLyrics.get();
    }

    public boolean skyLyrics() {
        return this.showLyrics.get() && this.skyLyrics.get();
    }

    public boolean skyTitle() {
        return this.skyTitle.get();
    }

    public boolean skyEmojis() {
        return this.skyEmojis.get();
    }

    public SpotifyHudModule.SkyEmojiStyle skyEmojiStyle() {
        return switch (this.skyEmojiStyle.get()) {
            case "Behind" -> SpotifyHudModule.SkyEmojiStyle.BEHIND;
            case "Replace Word" -> SpotifyHudModule.SkyEmojiStyle.REPLACE_WORD;
            default -> SpotifyHudModule.SkyEmojiStyle.BESIDE;
        };
    }

    public boolean skyFollow() {
        return this.skyFollow.get();
    }

    public float skyHeight() {
        return this.skyHeight.get();
    }

    public float skySize() {
        return this.skySize.get();
    }

    public float skyTitleSize() {
        return this.skyTitleSize.get();
    }

    public float skyEmojiSize() {
        return this.skyEmojiSize.get();
    }

    public float skyOpacity() {
        return this.skyOpacity.get();
    }

    public int skyTextColor() {
        return this.skyTextColor.argb();
    }

    public int skyTitleColor() {
        return this.skyTitleColor.argb();
    }

    public boolean skyGlowAtmosphere() {
        return this.skyGlowAtmosphere.get();
    }

    public int skyGlowColor() {
        return this.skyGlowColor.argb();
    }

    public boolean peepPlaying() {
        NowPlaying now = this.nowPlaying;
        return this.easterEggs.get()
            && now != null
            && now.playing()
            && now.artist() != null
            && now.artist().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "").contains("lilpeep");
    }

    public PinkWater.Design pinkWaterDesign() {
        return this.easterEggs.get() ? PinkWater.Design.byLabel(this.pinkWater.get()) : null;
    }

    public float skyGlow() {
        return this.skyGlow.get();
    }

    public int lyricLines() {
        try {
            return Math.max(1, Math.min(3, Integer.parseInt(this.lyricLines.get())));
        } catch (NumberFormatException impossible) {
            return 2;
        }
    }

    public SpotifyHudModule.LyricMotion lyricMotion() {
        return switch (this.lyricMotion.get()) {
            case "Glide" -> SpotifyHudModule.LyricMotion.GLIDE;
            case "Fade" -> SpotifyHudModule.LyricMotion.FADE;
            default -> SpotifyHudModule.LyricMotion.WAVE;
        };
    }

    public SpotifyHudModule.LyricHighlight lyricHighlight() {
        return switch (this.lyricHighlight.get()) {
            case "Line" -> SpotifyHudModule.LyricHighlight.LINE;
            case "Off" -> SpotifyHudModule.LyricHighlight.OFF;
            default -> SpotifyHudModule.LyricHighlight.KARAOKE;
        };
    }

    public boolean lyricsOnline() {
        return this.lyricsOnline.get();
    }

    public float cardWidth() {
        return this.cardWidth.get();
    }

    public int titleColor() {
        return this.titleColor.argb();
    }

    public int artistColor() {
        return this.artistColor.argb();
    }

    public int lyricsColor() {
        return this.lyricsColor.argb();
    }

    public int upcomingLyricsColor() {
        return this.upcomingLyricsColor.argb();
    }

    public int highlightColor() {
        return this.highlightColor.argb();
    }

    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
//         float tint = ThemeTint.pink();
        boolean peep = this.peepPlaying();
//         ThemeTint.pink(peep ? tint + 0.016666668F : tint - 0.033333335F);
        PinkWater.tick(mc, peep, this.pinkWaterDesign());
        boolean accept = this.mediaKeys.get() && mc.screen == null && mc.isWindowActive() && mc.player != null;

        for (int i = 0; i < this.keys.length; i++) {
            boolean down = isDown(mc, this.keys[i].value());
            if (down && !this.wasDown[i] && accept) {
                this.press(this.actions[i]);
            }

            this.wasDown[i] = down;
        }
    }

    private void press(SpotifyWindow.MediaKey key) {
        long now = System.currentTimeMillis();
        SpotifyHudModule.Poller running = this.poller;
        if (running != null && now - this.lastKeyAt >= 200L) {
            this.lastKeyAt = now;
            if (running.press(key)) {
                if (key == SpotifyWindow.MediaKey.PLAY_PAUSE) {
                    synchronized (this.publishLock) {
                        if (this.poller == running) {
                            this.nowPlaying = this.nowPlaying.toggled();
                        }
                    }
                }
            }
        }
    }

    private static boolean isDown(Minecraft mc, int key) {
//         if (!ModuleKeybinds.isBindable(key)) {
            return false;
        } else {
//             return key >= 1000 ? GLFW.glfwGetMouseButton(mc.getWindow().handle(), key - 1000) == 1 : AnubisKeyPolling.isKeyDown(mc.getWindow(), key);
        }
    }

        public enum LyricHighlight {
        KARAOKE,
        LINE,
        OFF;
    }

        public enum LyricMotion {
        WAVE,
        GLIDE,
        FADE;
    }

        private final class Poller {
        final ScheduledThreadPoolExecutor executor;
        final LyricsService lyricsService = new LyricsService();
        private PlayerSource media;
        private boolean mediaFailed;
        private SpotifyWindow window;
        private boolean windowFailed;
        private long nextWindowReadAt;
        private AlbumArt art;
        private int artVersion;
        private boolean artDecoded;
        private LyricsQuery lyricsCandidate;
        private int lyricsCandidatePolls;
        private LyricsQuery lyricsQuery;
        private SpotifyHudModule.TrackLyrics published;
        private int sessionFailures;
        private String lastError;
        private long lastErrorLoggedAt;

        Poller() {
            this.lyricsService.setOnline(SpotifyHudModule.this.lyricsOnline.get());
            this.executor = new ScheduledThreadPoolExecutor(1, runnable -> {
                Thread thread = new Thread(runnable, "Anubis-Spotify");
                thread.setDaemon(true);
                return thread;
            });
            this.executor.setRemoveOnCancelPolicy(true);
            this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        }

        void start() {
            this.executor.scheduleWithFixedDelay(this::poll, 0L, 500L, TimeUnit.MILLISECONDS);
        }

        void stop() {
            try {
                this.executor.execute(this::closeMedia);
            } catch (RejectedExecutionException ignored) {
            }

            this.executor.shutdown();
            this.lyricsService.close();
        }

        private void closeMedia() {
            if (this.media != null) {
                this.media.close();
            }
        }

        boolean press(SpotifyWindow.MediaKey key) {
            try {
                this.executor.execute(() -> this.control(key));
                this.executor.schedule(this::poll, 250L, TimeUnit.MILLISECONDS);
                return true;
            } catch (RejectedExecutionException stopped) {
                return false;
            }
        }

        private void poll() {
            if (SpotifyHudModule.this.poller == this) {
                try {
                    NowPlaying next = this.readSession();
                    if (next == null) {
                        if (this.sessionFailures > 0 && this.sessionFailures <= 2 && SpotifyHudModule.this.nowPlaying.source() == NowPlaying.Source.SESSION) {
                            return;
                        }

                        long now = System.currentTimeMillis();
                        if (SpotifyHudModule.this.nowPlaying.source() != NowPlaying.Source.SESSION && now < this.nextWindowReadAt) {
                            return;
                        }

                        this.nextWindowReadAt = now + 1000L;
                        next = this.readWindow();
                    }

                    if (next != null) {
                        SpotifyHudModule.this.publish(this, next, this.lyricsFor(next));
                    }
                } catch (RuntimeException | LinkageError failure) {
                    SpotifyHudModule.LOGGER.debug("[Anubis] Spotify HUD poll failed", failure);
                }
            }
        }

        private NowPlaying readSession() {
            if (this.mediaFailed) {
                return null;
            }

            if (this.media == null) {
                try {
                    this.media = PlayerSource.spotify();
                } catch (LinkageError failure) {
                    SpotifyHudModule.LOGGER.warn("[Anubis] Spotify HUD: media session unavailable ({})", failure.toString());
                    this.mediaFailed = true;
                    return null;
                }
            }

            if (this.media.isUnsupported()) {
                return null;
            } else {
                MediaSession.Snapshot snapshot = this.media.poll();
                if (snapshot != null) {
                    this.sessionFailures = 0;
                    return NowPlaying.fromSession(snapshot, this.artFor(snapshot), SpotifyHudModule.this.nowPlaying);
                } else {
                    String error = this.media.lastError();
                    this.logError(error);
                    this.sessionFailures = error != null && !this.media.isUnsupported() ? this.sessionFailures + 1 : 0;
                    return null;
                }
            }
        }

        private AlbumArt artFor(MediaSession.Snapshot snapshot) {
            if (!SpotifyHudModule.this.showAlbumArt.get()) {
                this.art = null;
                this.artDecoded = false;
                return null;
            }

            if (!this.artDecoded || snapshot.thumbnailVersion() != this.artVersion) {
                this.artDecoded = true;
                this.artVersion = snapshot.thumbnailVersion();
                this.art = null;
                if (snapshot.hasThumbnail()) {
                    try {
                        this.art = AlbumArt.decode(snapshot.thumbnail(), 256);
                    } catch (LinkageError | RuntimeException failure) {
                        SpotifyHudModule.LOGGER.debug("[Anubis] Spotify HUD: album art could not be decoded", failure);
                    }
                }
            }

            return this.art;
        }

        private NowPlaying readWindow() {
            if (!Platform.isWindows()) {
                if (this.media != null && !this.mediaFailed && !this.media.isUnsupported()) {
                    return NowPlaying.NOT_RUNNING;
                }

                this.executor.shutdown();
                return NowPlaying.UNSUPPORTED;
            } else {
                if (!this.windowFailed && this.window == null) {
                    try {
                        this.window = new SpotifyWindow();
                    } catch (LinkageError failure) {
                        this.windowFailed(failure);
                    }
                }

                if (this.window != null) {
                    try {
                        return NowPlaying.fromWindowTitle(this.window.readTitle(), SpotifyHudModule.this.nowPlaying);
                    } catch (LinkageError failure) {
                        this.windowFailed(failure);
                    }
                }

                if (this.media != null && !this.mediaFailed && !this.media.isUnsupported()) {
                    return NowPlaying.NOT_RUNNING;
                }

                this.executor.shutdown();
                return NowPlaying.UNSUPPORTED;
            }
        }

        private void windowFailed(LinkageError failure) {
            SpotifyHudModule.LOGGER.warn("[Anubis] Spotify HUD: Win32 access unavailable ({})", failure.toString());
            this.window = null;
            this.windowFailed = true;
        }

        private SpotifyHudModule.TrackLyrics lyricsFor(NowPlaying now) {
            if (!SpotifyHudModule.this.wantsLyrics(now)) {
                this.lyricsCandidate = null;
                this.lyricsCandidatePolls = 0;
                this.lyricsQuery = null;
                this.published = null;
                return null;
            }

            LyricsQuery query = new LyricsQuery(now.title(), now.artist(), now.album(), 0).withDurationMillis(now.durationMs());
            if (query.equals(this.lyricsCandidate)) {
                this.lyricsCandidatePolls++;
            } else {
                this.lyricsCandidate = query;
                this.lyricsCandidatePolls = 1;
            }

            if (this.lyricsCandidatePolls >= 2) {
                this.lyricsQuery = this.lyricsCandidate;
            }

            if (this.lyricsQuery != null && this.lyricsQuery.title().equals(now.title()) && this.lyricsQuery.artist().equals(now.artist())) {
                if (this.published == null
                    || !this.published.query().equals(this.lyricsQuery)
                    || !this.published.title().equals(now.title())
                    || !this.published.artist().equals(now.artist())) {
                    this.published = new SpotifyHudModule.TrackLyrics(now.title(), now.artist(), this.lyricsQuery);
                }

                return this.published;
            } else {
                return this.published;
            }
        }

        private void control(SpotifyWindow.MediaKey key) {
            if (SpotifyHudModule.this.poller == this) {
                if (this.media != null && SpotifyHudModule.this.nowPlaying.source() == NowPlaying.Source.SESSION) {
                    boolean accepted = switch (key) {
                        case PLAY_PAUSE -> this.media.togglePlayPause();
                        case NEXT -> this.media.skipNext();
                        case PREVIOUS -> this.media.skipPrevious();
                    };
                    if (accepted || this.media.lastError() == null) {
                        return;
                    }

                    SpotifyHudModule.LOGGER.debug("[Anubis] Spotify HUD: media session control failed ({}); sending the media key", this.media.lastError());
                }

                if (Platform.isWindows()) {
                    try {
                        SpotifyWindow.tap(key);
                    } catch (LinkageError | RuntimeException failure) {
                        SpotifyHudModule.LOGGER.debug("[Anubis] Spotify HUD media key failed", failure);
                    }
                }
            }
        }

        private void logError(String error) {
            if (error != null) {
                long now = System.currentTimeMillis();
                if (!Objects.equals(error, this.lastError) || now - this.lastErrorLoggedAt >= 60000L) {
                    this.lastError = error;
                    this.lastErrorLoggedAt = now;
                    SpotifyHudModule.LOGGER.debug("[Anubis] Spotify HUD media session: {}", error);
                }
            }
        }
    }

        public enum SkyEmojiStyle {
        BESIDE,
        BEHIND,
        REPLACE_WORD;
    }

        private record TrackLyrics(String title, String artist, LyricsQuery query) {
    }
}
