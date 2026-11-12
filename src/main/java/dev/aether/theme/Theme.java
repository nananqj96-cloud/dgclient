package dev.aether.theme;

import dev.aether.util.Colors;

public class Theme {
   private final String name;
   private int accent;
   private final boolean custom;

   public Theme(String name, int accent, boolean custom) {
      this.name = name;
      this.accent = accent;
      this.custom = custom;
   }

   public String getName() {
      return this.name;
   }

   public int accent() {
      return this.accent;
   }

   public void setAccent(int accent) {
      this.accent = accent;
   }

   public boolean isCustom() {
      return this.custom;
   }

   public int accentBright() {
      return Colors.lighten(this.accent(), 0.26F);
   }

   public int accentHover() {
      return Colors.withAlpha(this.accent(), 0.34F);
   }

   public int background() {
      return 0xF20B0F1A;
   }

   public int backgroundTo() {
      return 0xF20F1626;
   }

   public int headerTop() {
      return 0xF6131B2E;
   }

   public int headerBottom() {
      return 0xF60F1728;
   }

   public int surface() {
      return 0xE6141D30;
   }

   public int surfaceElevated() {
      return 0xEC19243B;
   }

   public int borderSubtle() {
      return 0xFF22304D;
   }

   public int moduleActiveFill() {
      return Colors.lerp(0xEC162036, this.accent(), 0.15F);
   }

   public int textPrimary() {
      return 0xFFF1F5F9;
   }

   public int textMuted() {
      return 0xFF94A3B8;
   }

   public int textDisabled() {
      return 0xFF64748B;
   }

   public int statusEnabled() {
      return this.accentBright();
   }

   public int statusDisabled() {
      return 0xFF2B3954;
   }
}
