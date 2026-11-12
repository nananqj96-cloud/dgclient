package dev.aether.gui.panel;

import dev.aether.gui.ClickGuiState;
import dev.aether.render.anim.Animation;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;

public abstract class Panel {
   public static final float WIDTH = 214.0F;
   public static final float HEADER_H = 36.0F;
   public static final float RADIUS = 9.0F;
   protected static final float CONTENT_PAD = 6.0F;
   protected final ThemeManager themes;
   protected final ClickGuiState.PanelState ps;
   private final Animation open;
   private final Animation scroll = new Animation(160.0F, 0.0F);
   private float maxScroll;
   private boolean headerHovered;

   protected Panel(ThemeManager themes, ClickGuiState.PanelState ps) {
      this.themes = themes;
      this.ps = ps;
      this.open = new Animation(180.0F, ps.collapsed ? 0.0F : 1.0F);
   }

   protected Theme theme() {
      return this.themes.current();
   }

   public float x() {
      return this.ps.x;
   }

   public float y() {
      return this.ps.y;
   }

   public void moveTo(float x, float y) {
      this.ps.x = x;
      this.ps.y = y;
   }

   public void toggleCollapsed() {
      this.ps.collapsed = !this.ps.collapsed;
      this.open.setTarget(this.ps.collapsed ? 0.0F : 1.0F);
   }

   protected abstract String title();

   protected int icon() {
      return -1;
   }

   protected abstract float contentHeight(NVGRenderer vg);

   protected abstract void renderContent(NVGRenderer vg, float startY, float mx, float my, float viewTop, float viewBottom);

   protected float maxViewHeight(float uiH) {
      return Math.max(60.0F, uiH - this.ps.y - HEADER_H - 20.0F);
   }

   protected float viewHeight(NVGRenderer vg, float uiH) {
      return Math.min(this.contentHeight(vg), this.maxViewHeight(uiH)) * this.open.value();
   }

   public float totalHeight(NVGRenderer vg, float uiH) {
      return HEADER_H + this.viewHeight(vg, uiH);
   }

   protected float edgeFade(float top, float bottom, float viewTop, float viewBottom) {
      float f1 = Math.clamp((bottom - viewTop) / 14.0F, 0.0F, 1.0F);
      float f2 = Math.clamp((viewBottom - top) / 14.0F, 0.0F, 1.0F);
      return Math.min(f1, f2);
   }

   public void render(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      this.ps.x = Math.clamp(this.ps.x, -170.0F, uiW - 44.0F);
      this.ps.y = Math.clamp(this.ps.y, Math.min(62.0F, uiH - HEADER_H), uiH - HEADER_H);
      Theme theme = this.theme();
      float vh = this.viewHeight(vg, uiH);
      boolean expanded = vh > 0.5F;
      boolean hovered = this.headerHit(mx, my);
      if (hovered && !this.headerHovered) {
         UiSounds.hover();
      }
      this.headerHovered = hovered;

      float totalH = HEADER_H + vh;
      // Soft drop shadow (single box-gradient draw call)
      vg.glow(this.ps.x, this.ps.y + 2.0F, WIDTH, totalH, RADIUS, 9.0F, Colors.withAlpha(0xFF000000, 0.40F));

      // Panel container
      vg.rectGradient(this.ps.x, this.ps.y, WIDTH, totalH, RADIUS, theme.background(), theme.backgroundTo(), true);
      float botR = expanded ? 0.0F : RADIUS;
      vg.rectVaryingGradient(this.ps.x, this.ps.y, WIDTH, HEADER_H, RADIUS, RADIUS, botR, botR, theme.headerTop(), theme.headerBottom());
      vg.rectOutline(
         this.ps.x,
         this.ps.y,
         WIDTH,
         totalH,
         RADIUS,
         1.0F,
         hovered ? Colors.withAlpha(theme.accentBright(), 0.55F) : Colors.withAlpha(theme.borderSubtle(), 0.9F)
      );

      // Top accent line
      vg.rect(this.ps.x + 14.0F, this.ps.y, 32.0F, 2.0F, 1.0F, hovered ? theme.accentBright() : theme.accent());
      if (expanded) {
         vg.line(this.ps.x + 1.0F, this.ps.y + HEADER_H, this.ps.x + WIDTH - 1.0F, this.ps.y + HEADER_H, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.85F));
      }

      float titleX = this.ps.x + 12.0F;
      int iconId = this.icon();
      if (iconId > 0) {
         vg.rect(this.ps.x + 10.0F, this.ps.y + 8.0F, 20.0F, 20.0F, 5.0F, Colors.withAlpha(theme.accent(), 0.14F));
         vg.image(iconId, this.ps.x + 13.0F, this.ps.y + 11.0F, 14.0F, 14.0F, theme.accentBright());
         titleX = this.ps.x + 37.0F;
      }
      vg.text(this.title(), titleX, this.ps.y + HEADER_H * 0.5F, 13.0F, theme.textPrimary());

      float chevX = this.ps.x + WIDTH - 16.0F;
      float chevY = this.ps.y + HEADER_H * 0.5F;
      vg.save();
      vg.translate(chevX, chevY);
      vg.rotate((float)((double)this.open.value() * Math.PI * 0.5));
      vg.chevron(0.0F, 0.0F, 4.0F, 1.7F, hovered ? theme.accentBright() : theme.textMuted(), false);
      vg.restore();

      if (expanded) {
         float viewTop = this.ps.y + HEADER_H;
         float viewBottom = viewTop + vh;
         this.maxScroll = Math.max(0.0F, this.contentHeight(vg) - this.maxViewHeight(uiH));
         this.scroll.setTarget(Math.clamp(this.scroll.getTarget(), 0.0F, this.maxScroll));
         vg.save();
         vg.scissor(this.ps.x, viewTop, WIDTH, vh);
         this.renderContent(vg, viewTop + CONTENT_PAD - this.scroll.value(), mx, my, viewTop, viewBottom);
         vg.restore();
      }
   }

   public boolean headerHit(float mx, float my) {
      return mx >= this.ps.x && mx <= this.ps.x + WIDTH && my >= this.ps.y && my <= this.ps.y + HEADER_H;
   }

   public boolean bodyHit(NVGRenderer vg, float mx, float my, float uiH) {
      float vh = this.viewHeight(vg, uiH);
      return mx >= this.ps.x && mx <= this.ps.x + WIDTH && my >= this.ps.y + HEADER_H && my <= this.ps.y + HEADER_H + vh;
   }

   public void onScroll(double delta) {
      if (this.maxScroll > 0.0F) {
         this.scroll.setTarget(Math.clamp(this.scroll.getTarget() - (float)delta * 40.0F, 0.0F, this.maxScroll));
      }
   }

   public boolean mouseClicked(float mx, float my, int button) {
      return false;
   }

   public void mouseDragged(float mx, float my) {
   }

   public void mouseReleased() {
   }

   public boolean keyPressed(int keyCode) {
      return false;
   }

   public boolean charTyped(int codepoint) {
      return false;
   }

   public boolean isListening() {
      return false;
   }
}
