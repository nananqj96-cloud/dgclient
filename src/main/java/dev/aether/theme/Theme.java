package dev.aether.theme;

import dev.aether.util.Colors;

public class Theme {
   private final String name;
   private int accent;
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

   public String getName() {
      return this.name;
   }

   public int accent() {
      return this.accent;
   }

   public void setAccent(int accent) {
      this.accent = accent | 0xFF000000;
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

   public int accentBright() {
      return Colors.lighten(this.accent(), this.velvetBloom ? 0.30F : 0.26F);
   }

   public int accentHover() {
      return Colors.withAlpha(this.accent(), this.velvetBloom ? 0.40F : 0.34F);
   }

   public int background() {
      return this.velvetBloom ? 0xF2140B18 : 0xF20B0F1A;
   }

   public int backgroundTo() {
      return this.velvetBloom ? 0xF21C1024 : 0xF20F1626;
   }

   public int headerTop() {
      return Colors.lerp(this.velvetBloom ? 0xFA1F1229 : 0xF6131B2E, this.accent(), this.velvetBloom ? 0.16F : 0.08F);
   }

   public int headerBottom() {
      return this.velvetBloom ? 0xFA150D1E : 0xF60F1728;
   }

   public int surface() {
      return this.velvetBloom ? 0xEE1D1227 : 0xE6141D30;
   }

   public int surfaceElevated() {
      return this.velvetBloom ? 0xF2281936 : 0xEC19243B;
   }

   public int surfacePressed() {
      return this.velvetBloom ? 0xFF140C1C : 0xFF0E1320;
   }

   public int borderSubtle() {
      return this.velvetBloom ? 0xFF3A2348 : 0xFF22304D;
   }

   public int moduleActiveFill() {
      return Colors.lerp(this.velvetBloom ? 0xF0241530 : 0xEC162036, this.accent(), this.velvetBloom ? 0.22F : 0.15F);
   }

   public int textPrimary() {
      return this.velvetBloom ? 0xFFFFF5FA : 0xFFF1F5F9;
   }

   public int textMuted() {
      return this.velvetBloom ? 0xFFD8B4CC : 0xFF94A3B8;
   }

   public int textDisabled() {
      return this.velvetBloom ? 0xFF8F6B86 : 0xFF64748B;
   }

   public int statusEnabled() {
      return this.accentBright();
   }

   public int statusDisabled() {
      return this.velvetBloom ? 0xFF362044 : 0xFF2B3954;
   }
}
