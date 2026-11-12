package dev.aether.theme;

import dev.aether.util.Colors;

public class Theme {
   private final String name;
   private int accent;
   private int accentBright;
   private final boolean custom;
   private final boolean velvetBloom;

   public Theme(String name, int accent) {
      this(name, accent, false, false);
   }

   public Theme(String name, int accent, boolean custom) {
      this(name, accent, custom, false);
   }

   public Theme(String name, int accent, boolean custom, boolean velvetBloom) {
      this.name = name;
      this.custom = custom;
      this.velvetBloom = velvetBloom;
      this.setAccent(accent);
   }

   public void setAccent(int accent) {
      this.accent = accent | 0xFF000000;
      this.accentBright = Colors.lighten(this.accent, this.velvetBloom ? 0.30F : 0.24F);
   }

   public String getName() {
      return this.name;
   }

   public boolean isCustom() {
      return this.custom;
   }

   public boolean isBloom() {
      return this.velvetBloom;
   }

   public float bloomStrength() {
      return this.velvetBloom ? 1.0F : 0.45F;
   }

   public int accent() {
      return this.accent;
   }

   public int accentBright() {
      return this.accentBright;
   }

   public int background() {
      return this.velvetBloom ? 0xF2130B19 : 0xF20B0F19;
   }

   public int backgroundTo() {
      return this.velvetBloom ? 0xF21B1024 : 0xF2101626;
   }

   public int surface() {
      return this.velvetBloom ? 0xFF1D1227 : 0xFF131929;
   }

   public int surfaceElevated() {
      return this.velvetBloom ? 0xFF281936 : 0xFF1A2236;
   }

   public int surfacePressed() {
      return this.velvetBloom ? 0xFF140C1C : 0xFF0E1320;
   }

   public int headerTop() {
      return Colors.lerp(this.velvetBloom ? 0xFA1E1229 : 0xFA141B2D, this.accent(), this.velvetBloom ? 0.18F : 0.12F);
   }

   public int headerBottom() {
      return this.velvetBloom ? 0xFA150D1E : 0xFA0F1524;
   }

   public int moduleActiveFill() {
      return Colors.lerp(this.surfaceElevated(), this.accent(), this.velvetBloom ? 0.24F : 0.18F);
   }

   public int textPrimary() {
      return this.velvetBloom ? 0xFFFFF5FA : 0xFFF8FAFC;
   }

   public int textMuted() {
      return this.velvetBloom ? 0xFFD8B4CC : 0xFF94A3B8;
   }

   public int textDisabled() {
      return this.velvetBloom ? 0xFF8F6B86 : 0xFF64748B;
   }

   public int borderSubtle() {
      return this.velvetBloom ? 0xFF3A2348 : 0xFF232D42;
   }
}
