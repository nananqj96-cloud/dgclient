package dev.aether.spotify;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SpotifyService {
   private volatile SpotifyState current = SpotifyState.INACTIVE;
   private volatile boolean enabled;
   private volatile String status = "Enable SpotifyHUD";
   private ScheduledExecutorService executor;
   private WindowsSpotify desktop;
   private volatile SpotifyMediaBridge media;
   private long generation;

   public synchronized void start() {
      if (this.executor == null || this.executor.isShutdown()) {
         this.executor = Executors.newSingleThreadScheduledExecutor(var0 -> {
            Thread var1 = new Thread(var0, "Aether-Spotify");
            var1.setDaemon(true);
            return var1;
         });
         this.executor.scheduleWithFixedDelay(this::poll, 0L, 1L, TimeUnit.SECONDS);
      }
   }

   public synchronized void setEnabled(boolean var1) {
      if (this.enabled != var1) {
         this.enabled = var1;
         if (!var1 && this.media != null) {
            this.media.close();
         }

         this.generation++;
         this.current = SpotifyState.INACTIVE;
         this.status = var1 ? "Looking for Spotify desktop" : "Enable SpotifyHUD";
      }
   }

   private void poll() {
      long var1;
      synchronized (this) {
         if (!this.enabled) {
            return;
         }

         var1 = this.generation;
      }

      SpotifyState var3 = SpotifyState.INACTIVE;

      String var4;
      try {
         if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            var4 = "Windows Spotify desktop required";
         } else {
            if (this.desktop == null) {
               this.desktop = new WindowsSpotify();
            }

            if (this.media == null) {
               this.media = new SpotifyMediaBridge();
            }

            var3 = this.media.read();
            SpotifyState var5 = this.desktop.read();
            if (!var3.active()) {
               var3 = var5;
            }

            var4 = var3.active() ? "Spotify Desktop" : "Open Spotify and play a song";
         }
      } catch (LinkageError | Exception var91) {
         var4 = "Spotify desktop unavailable";

         try {
            if (this.desktop != null) {
               var3 = this.desktop.read();
               if (var3.active()) {
                  var4 = "Spotify Desktop";
               }
            }
         } catch (LinkageError | RuntimeException var8) {
         }
      }

      synchronized (this) {
         if (this.enabled && var1 == this.generation) {
            this.current = var3;
            this.status = var4;
         } else if (this.media != null) {
            this.media.close();
         }
      }
   }

   public SpotifyState state() {
      return this.enabled ? this.current : SpotifyState.INACTIVE;
   }

   public String status() {
      return this.status;
   }

   public Path artPath() {
      return this.media == null ? Path.of(System.getProperty("java.io.tmpdir"), "aether-no-album-art") : this.media.artPath();
   }

   public synchronized void stop() {
      this.setEnabled(false);
      if (this.executor != null) {
         this.executor.shutdownNow();
      }

      this.executor = null;
   }

   private synchronized void command(int var1) {
      if (this.enabled && this.executor != null && !this.executor.isShutdown()) {
         long var2 = this.generation;
         this.executor.execute(() -> {
            synchronized (this) {
               if (this.enabled && var2 == this.generation && this.desktop != null) {
                  try {
                     this.desktop.command(var1);
                  } catch (LinkageError | RuntimeException var7) {
                  }
               }
            }
         });
      }
   }

   public void next() {
      this.command(11);
   }

   public void previous() {
      this.command(12);
   }

   public void togglePlay() {
      this.command(14);
   }

   public void seekTo(long var1) {
   }

   public void setVolume(int var1) {
   }
}
