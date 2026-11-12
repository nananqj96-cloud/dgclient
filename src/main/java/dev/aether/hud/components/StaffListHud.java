package dev.aether.hud.components;

import dev.aether.hud.HudComponent;
import dev.aether.module.impl.StaffListModule;
import dev.aether.render.anim.Animation;
import dev.aether.render.anim.Easing;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.staff.StaffEntry;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StaffListHud extends HudComponent {
   private static final float PAD = 8.0F;
   private static final float HEADER_H = 15.0F;
   private static final float GAP_HEADER = 5.0F;
   private static final float ROW_H = 18.0F;
   private static final float EMPTY_H = 16.0F;
   private static final float OVERFLOW_H = 13.0F;
   private static final float MARK_W = 15.0F;
   private static final float PING_W = 11.0F;
   private static final float FONT_TITLE = 9.0F;
   private static final float FONT_NAME = 12.5F;
   private static final float FONT_RANK = 9.0F;
   private static final float FONT_EMPTY = 11.0F;
   private static final float MIN_CONTENT_W = 92.0F;
   private static final float MAX_NAME_W = 128.0F;
   private static final int GREEN = -11870592;
   private static final int YELLOW = -340971;
   private static final int RED = -495247;
   private final StaffListModule module;
   private final ThemeManager themes;
   private final Map<String, StaffListHud.RowAnim> rows = new LinkedHashMap<>();

   public StaffListHud(StaffListModule var1, ThemeManager var2) {
      super("staffList", 0.008F, 0.27F, var1::isEnabled);
      this.module = var1;
      this.themes = var2;
   }

   private int shownCount(List<StaffEntry> var1) {
      return Math.min(var1.size(), Math.max(1, this.module.maxRows.getInt()));
   }

   private List<StaffListHud.RowAnim> layoutRows(List<StaffEntry> var1) {
      int var3 = this.shownCount(var1);
      HashSet<String> var4 = new HashSet<>();

      for (int var5 = 0; var5 < var3 && var5 < var1.size(); var5++) {
         StaffEntry var6 = var1.get(var5);
         var4.add(var6.name());
         StaffListHud.RowAnim var2 = this.rows.get(var6.name());
         if (var2 == null) {
            var2 = new StaffListHud.RowAnim(var6);
            this.rows.put(var6.name(), var2);
         } else {
            var2.entry = var6;
         }

         var2.anim.setTarget(1.0F);
      }

      ArrayList<StaffListHud.RowAnim> var8 = new ArrayList<>();

      for (int var9 = 0; var9 < var3 && var9 < var1.size(); var9++) {
         var8.add(this.rows.get(var1.get(var9).name()));
      }

      Iterator var10 = this.rows.values().iterator();

      while (var10.hasNext()) {
         StaffListHud.RowAnim var7 = (StaffListHud.RowAnim)var10.next();
         if (!var4.contains(var7.entry.name())) {
            var7.anim.setTarget(0.0F);
            if (var7.anim.value() <= 0.01F) {
               var10.remove();
            } else {
               var8.add(var7);
            }
         }
      }

      return var8;
   }

   private float rowWidth(NVGRenderer var1, StaffEntry var2) {
      float var3 = 15.0F + Math.min(var1.textWidth(var2.name(), 12.5F), 128.0F);
      if (this.module.showRank.get() && !var2.rankLabel().isEmpty()) {
         var3 += 6.0F + var1.textWidth(var2.rankLabel(), 9.0F);
      }

      if (this.module.showPing.get()) {
         var3 += 19.0F;
      }

      return var3;
   }

   @Override
   public float measureWidth(NVGRenderer var1) {
      List<StaffEntry> var2 = this.module.staff();
      float var3 = 92.0F;
      String var4 = Integer.toString(var2.size());
      var3 = Math.max(var3, var1.textWidth("STAFF", 9.0F) + 10.0F + var1.textWidth(var4, 8.5F) + 9.0F);

      for (StaffListHud.RowAnim var6 : this.layoutRows(var2)) {
         var3 = Math.max(var3, this.rowWidth(var1, var6.entry));
      }

      if (var2.isEmpty()) {
         var3 = Math.max(var3, 14.0F + var1.textWidth("No staff online", 11.0F));
      }

      return 16.0F + var3;
   }

   @Override
   public float measureHeight(NVGRenderer var1) {
      List<StaffEntry> var2 = this.module.staff();
      float var3 = 0.0F;

      for (StaffListHud.RowAnim var5 : this.layoutRows(var2)) {
         var3 += 18.0F * var5.anim.value();
      }

      if (var2.isEmpty() && var3 < 0.5F) {
         var3 = 16.0F;
      }

      float var6 = 28.0F + var3 + 8.0F;
      if (var2.size() > this.shownCount(var2)) {
         var6 += 13.0F;
      }

      return var6;
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3, float var4, float var5) {
      Theme var6 = this.themes.current();
      List<StaffEntry> var7 = this.module.staff();
      var1.glow(var2, var3, var4, var5, 4.0F, 7.0F, Colors.withAlpha(-16777216, 0.3F));
      var1.rectGradient(var2, var3, var4, var5, 3.0F, var6.background(), var6.backgroundTo(), true);
      this.drawHeader(var1, var6, var2, var3, var4, var7.size());
      float var8 = var3 + 8.0F + 15.0F + 5.0F;
      if (var7.isEmpty()) {
         float var13 = var8 + 8.0F;
         var1.checkmark(var2 + 8.0F, var13 - 4.0F, 8.0F, 1.7F, -11870592);
         var1.text("No staff online", var2 + 8.0F + 14.0F, var13, 11.0F, var6.textMuted());
      } else {
         for (StaffListHud.RowAnim var10 : this.layoutRows(var7)) {
            float var11 = Math.clamp(var10.anim.value(), 0.0F, 1.0F);
            if (!(var11 <= 0.01F)) {
               this.drawRow(var1, var6, var10.entry, var2, var8, var4, var11);
               var8 += 18.0F * var11;
            }
         }

         int var12 = var7.size() - this.shownCount(var7);
         if (var12 > 0) {
            var1.text("+" + var12 + " more", var2 + 8.0F + 15.0F, var8 + 6.5F, 9.0F, var6.textDisabled());
         }
      }
   }

   private void drawHeader(NVGRenderer var1, Theme var2, float var3, float var4, float var5, int var6) {
      float var7 = var4 + 8.0F + 7.5F;
      var1.text("STAFF", var3 + 8.0F, var7, 9.0F, var2.textPrimary());
      String var8 = Integer.toString(var6);
      float var9 = var1.textWidth(var8, 8.5F);
      float var10 = var9 + 9.0F;
      float var11 = 12.5F;
      float var12 = var3 + var5 - 8.0F - var10;
      float var13 = var7 - var11 / 2.0F;
      boolean var14 = var6 > 0;
      var1.rect(var12, var13, var10, var11, 6.0F, Colors.withAlpha(var2.accent(), var14 ? 0.2F : 0.1F));
      var1.rectOutline(var12, var13, var10, var11, 6.0F, 1.0F, Colors.withAlpha(var2.accent(), var14 ? 0.5F : 0.22F));
      var1.text(var8, var12 + 4.5F, var7, 8.5F, var14 ? var2.accentBright() : var2.textMuted());
   }

   private void drawRow(NVGRenderer var1, Theme var2, StaffEntry var3, float var4, float var5, float var6, float var7) {
      float var8 = var5 + 9.0F;
      int var9 = var3.hasColor() ? var3.color() : var2.accent();
      var1.save();
      var1.alpha(var7);
      var1.translate((1.0F - var7) * -10.0F, 0.0F);
      this.drawStar(var1, var4 + 8.0F + 7.5F - 2.0F, var8, 4.2F, var9, var3.vanished());
      float var10 = var4 + 8.0F + 15.0F;
      int var11 = var3.vanished() ? var2.textMuted() : var2.textPrimary();
      float var12 = var1.textTruncated(var3.name(), var10, var8, 12.5F, var11, 128.0F);
      if (this.module.showRank.get() && !var3.rankLabel().isEmpty()) {
         int var13 = var3.hasColor() ? Colors.lighten(var9, 0.25F) : var2.accent();
         var1.text(var3.rankLabel(), var10 + var12 + 6.0F, var8 + 0.5F, 9.0F, Colors.withAlpha(var13, var3.vanished() ? 0.6F : 0.95F));
      }

      if (this.module.showPing.get()) {
         this.drawPingBars(var1, var4 + var6 - 8.0F - 11.0F, var8, var3.latency(), var2);
      }

      var1.restore();
   }

   private void drawStar(NVGRenderer var1, float var2, float var3, float var4, int var5, boolean var6) {
      if (var6) {
         var1.save();
         var1.translate(var2, var3);
         var1.rotate((float) (Math.PI / 4));
         var1.rectOutline(-var4 * 0.55F, -var4 * 0.55F, var4 * 1.1F, var4 * 1.1F, var4 * 0.25F, 1.2F, Colors.withAlpha(var5, 0.55F));
         var1.restore();
      } else {
         int var7 = Colors.lighten(var5, 0.4F);
         var1.circleGlow(var2, var3, var4 * 0.45F, var4 * 1.1F, Colors.withAlpha(var5, 0.42F));
         var1.circleOutline(var2, var3, var4, 1.2F, Colors.withAlpha(var7, 0.82F));
         var1.save();
         var1.translate(var2, var3);
         var1.rotate((float) (Math.PI / 4));
         var1.rect(-var4 * 0.35F, -var4 * 0.35F, var4 * 0.7F, var4 * 0.7F, var4 * 0.14F, var5);
         var1.restore();
      }
   }

   private void drawPingBars(NVGRenderer var1, float var2, float var3, int var4, Theme var5) {
      int var6 = var4 < 0 ? 0 : (var4 <= 80 ? 4 : (var4 <= 150 ? 3 : (var4 <= 300 ? 2 : (var4 <= 600 ? 1 : 0))));
      int var7 = var6 >= 3 ? -11870592 : (var6 == 2 ? -340971 : -495247);

      for (int var8 = 0; var8 < 4; var8++) {
         float var9 = 2.0F + (float)var8 * 2.0F;
         float var10 = var2 + (float)var8 * 3.0F;
         float var11 = var3 + 4.0F - var9;
         int var12 = var8 < var6 ? var7 : Colors.withAlpha(var5.textDisabled(), 0.45F);
         var1.rect(var10, var11, 2.0F, var9, 0.5F, var12);
      }
   }

   private static final class RowAnim {
      StaffEntry entry;
      final Animation anim = new Animation(220.0F, 0.0F, Easing.EASE_OUT_CUBIC);

      RowAnim(StaffEntry var1) {
         this.entry = var1;
      }
   }
}
