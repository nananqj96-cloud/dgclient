package dev.aether.theme;

import dev.aether.util.Colors;

/**
 * Full Tenacity-style GUI theme — glass panels, dark palette, Inter font.
 * Captures Tenacity's visual design: liquid-glass surfaces with refraction,
 * gradient borders, dark background, and the signature accent palette.
 */
public class TenacityTheme extends Theme {
   // Tenacity palette indices: {tint, rim, accent} per theme variant
   private static final int[][] PALETTE = {
      // Default — cool blue-violet
      {0xFF4851B0, 0xFF670E50, 0xFF38BDF8},
      // Christmas — red/green
      {0xFF2E7D32, 0xFFC62828, 0xFF66BB6A},
      // Halloween — orange/purple
      {0xFF7B1FA2, 0xFFE65100, 0xFFAB47BC}
   };

   private final int variant;

   public TenacityTheme(int accent, int variant) {
      super("Tenacity", accent, false);
      this.variant = variant;
   }

   @Override
   public int background() {
      return 0xF20A0E18;
   }

   @Override
   public int backgroundTo() {
      return 0xF2080C14;
   }

   @Override
   public int headerTop() {
      return 0xF6101A2C;
   }

   @Override
   public int headerBottom() {
      return 0xF60C1624;
   }

   @Override
   public int surface() {
      return 0xE60E1328;
   }

   @Override
   public int surfaceElevated() {
      return 0xEC121933;
   }

   @Override
   public int borderSubtle() {
      return 0xFF2A3550;
   }

   /** Glass tint — the signature Tenacity liquid-glass color. */
   public int glassTint() {
      int v = PALETTE[variant][0];
      return (int)(132.6F) << 24 | (v & 0xFFFFFF);
   }

   /** Rim/gradient border color. */
   public int rimColor() {
      return PALETTE[variant][1];
   }

   /** Accent color for this variant. */
   public int variantAccent() {
      return PALETTE[variant][2];
   }

   @Override
   public int accent() {
      return variantAccent();
   }

   @Override
   public int moduleActiveFill() {
      return Colors.lerp(0xEC121933, this.accent(), 0.18F);
   }

   @Override
   public int textPrimary() {
      return 0xFFF1F5F9;
   }

   @Override
   public int textMuted() {
      return 0xFF8B9BB0;
   }

   @Override
   public int textDisabled() {
      return 0xFF5A6B82;
   }

   @Override
   public int statusEnabled() {
      return this.accentBright();
   }

   @Override
   public int statusDisabled() {
      return 0xFF1E2A3D;
   }

   /** Tenacity uses Inter font. */
   public String fontMode() {
      return "Tenacity";
   }

   /** Whether glass/blur panels should be used (Tenacity signature). */
   public boolean glassEnabled() {
      return true;
   }

   /** Roundness multiplier — Tenacity uses 0.6 default. */
   public float roundness() {
      return 0.6F;
   }
}
