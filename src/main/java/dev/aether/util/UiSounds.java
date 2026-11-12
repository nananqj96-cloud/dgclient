package dev.aether.util;

import dev.aether.theme.SoundSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

public final class UiSounds {
   private static SoundSettings settings = new SoundSettings();
   private static long lastHoverNanos;
   private static long lastSliderNanos;

   private UiSounds() {
   }

   public static void init(SoundSettings var0) {
      settings = var0;
   }

   private static void play(SoundEvent var0, float var1, float var2) {
      float var3 = settings.volume();
      if (!(var3 <= 0.01F)) {
         Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(var0, var1, var2 * var3));
      }
   }

   public static void guiOpen() {
      if (settings.guiSounds.get()) {
         play(UiSoundEvents.GUI_OPEN, 1.0F, 0.85F);
      }
   }

   public static void guiClose() {
      if (settings.guiSounds.get()) {
         play(UiSoundEvents.GUI_CLOSE, 1.0F, 0.75F);
      }
   }

   public static void hover() {
      if (settings.hoverSounds.get()) {
         long var0 = System.nanoTime();
         if (var0 - lastHoverNanos >= 45000000L) {
            lastHoverNanos = var0;
            play(UiSoundEvents.HOVER, 0.95F + (float)(var0 % 7L) * 0.015F, 0.5F);
         }
      }
   }

   public static void toggle(boolean var0) {
      if (settings.clickSounds.get()) {
         play(UiSoundEvents.SELECT, var0 ? 1.22F : 0.78F, 0.62F);
      }
   }

   public static void checkbox(boolean var0) {
      if (settings.clickSounds.get()) {
         play(UiSoundEvents.SELECT, var0 ? 1.22F : 0.78F, 0.62F);
      }
   }

   public static void select() {
      if (settings.clickSounds.get()) {
         play(UiSoundEvents.SELECT, 1.0F, 0.65F);
      }
   }

   public static void sliderTick(float var0) {
      if (settings.clickSounds.get()) {
         long var1 = System.nanoTime();
         if (var1 - lastSliderNanos >= 60000000L) {
            lastSliderNanos = var1;
            play(UiSoundEvents.SLIDER, 0.85F + var0 * 0.55F, 0.5F);
         }
      }
   }

   public static void keybindListen() {
      if (settings.clickSounds.get()) {
         play(UiSoundEvents.KEYBIND, 0.8F, 0.6F);
      }
   }

   public static void keybindSet() {
      if (settings.clickSounds.get()) {
         play(UiSoundEvents.KEYBIND, 1.1F, 0.6F);
      }
   }

   public static void notification(boolean var0) {
      if (settings.notificationSounds.get()) {
         if (var0) {
            play(UiSoundEvents.NOTIFY_ON, 1.17F, 0.56F);
            play(UiSoundEvents.SELECT, 1.46F, 0.18F);
         } else {
            play(UiSoundEvents.NOTIFY_OFF, 0.92F, 0.52F);
         }
      }
   }

   public static void panelCollapse() {
      if (settings.clickSounds.get()) {
         play(UiSoundEvents.SELECT, 0.8F, 0.5F);
      }
   }

   public static void playStartup() {
   }
}
