package dev.aether.spotify;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.StdCallLibrary.StdCallCallback;
import java.util.HashMap;

public final class WindowsSpotify {
   private final WindowsSpotify.User32 api = (WindowsSpotify.User32)Native.load("user32", WindowsSpotify.User32.class);
   private volatile Pointer spotifyWindow;

   private boolean isSpotify(Pointer var1) {
      IntByReference var2 = new IntByReference();
      this.api.GetWindowThreadProcessId(var1, var2);
      return var2.getValue() == 0
         ? false
         : ProcessHandle.of(Integer.toUnsignedLong(var2.getValue()))
            .flatMap(var0 -> var0.info().command())
            .map(SpotifyWindowTitle::isSpotifyExecutable)
            .orElse(false);
   }

   public SpotifyState read() {
      SpotifyState[] var1 = new SpotifyState[]{SpotifyState.INACTIVE};
      Pointer[] var2 = new Pointer[]{null};
      HashMap<Integer, Boolean> var3 = new HashMap<>();
      this.api
         .EnumWindows(
            (var4, var5) -> {
               IntByReference var6 = new IntByReference();
               this.api.GetWindowThreadProcessId(var4, var6);
               if (var6.getValue() == 0) {
                  return true;
               } else {
                  char[] var7 = new char[2048];
                  int var8 = this.api.GetWindowTextW(var4, var7, var7.length);
                  if (var8 <= 0) {
                     return true;
                  } else {
                     boolean var9 = var3.computeIfAbsent(
                        var6.getValue(),
                        var0 -> ProcessHandle.of(Integer.toUnsignedLong(var0))
                              .flatMap(var0x -> var0x.info().command())
                              .map(SpotifyWindowTitle::isSpotifyExecutable)
                              .orElse(false)
                     );
                     if (!var9) {
                        return true;
                     } else {
                        SpotifyState var10 = SpotifyWindowTitle.parse(new String(var7, 0, var8), System.nanoTime());
                        if (var2[0] == null || var10.active()) {
                           var2[0] = var4;
                        }

                        if (var10.active()) {
                           var1[0] = var10;
                        }

                        return !var10.active();
                     }
                  }
               }
            },
            null
         );
      this.spotifyWindow = var2[0];
      return var1[0];
   }

   public void command(int var1) {
      Pointer var2 = this.spotifyWindow;
      if (var2 != null && this.isSpotify(var2)) {
         this.api.PostMessageW(var2, 793, var2, Pointer.createConstant((long)var1 << 16));
      }
   }

   public interface User32 extends StdCallLibrary {
      boolean EnumWindows(WindowsSpotify.User32.WindowCallback windowCallback, Pointer pointer);

      int GetWindowThreadProcessId(Pointer pointer, IntByReference intByReference);

      int GetWindowTextW(Pointer pointer, char[] cs, int i);

      boolean PostMessageW(Pointer pointer, int i, Pointer pointer2, Pointer pointer3);

      public interface WindowCallback extends StdCallCallback {
         boolean invoke(Pointer pointer, Pointer pointer2);
      }
   }
}
