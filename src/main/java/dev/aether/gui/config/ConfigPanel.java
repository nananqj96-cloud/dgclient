package dev.aether.gui.config;

import dev.aether.AetherClient;
import dev.aether.config.ConfigShareService;
import dev.aether.config.ConfigStore;
import dev.aether.render.anim.Animation;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public class ConfigPanel {
   private static final float CARD_W = 620.0F;
   private static final float HEADER_H = 66.0F;
   private static final float ROW_H = 60.0F;
   private static final int DANGER = -45730;
   private static final int DANGER_BRIGHT = -37252;

   private final Animation openAnim = new Animation(150.0F, 0.0F);
   private boolean open;
   private float cardX;
   private float cardY;
   private float cardH;
   private final List<Hit> hits = new ArrayList<>();
   private float closeX;
   private float closeY;
   private float closeSize;
   private int renamingSlot = -1;
   private final StringBuilder renameBuffer = new StringBuilder();
   private Confirm pendingConfirm;
   private float confirmOkX;
   private float confirmOkY;
   private float confirmOkW;
   private float confirmOkH;
   private float confirmCancelX;
   private float confirmCancelY;
   private float confirmCancelW;
   private float confirmCancelH;
   private float confirmCardX;
   private float confirmCardY;
   private float confirmCardW;
   private float confirmCardH;
   private volatile ShareCompletion shareCompletion;
   private boolean sharing;

   public boolean isOpen() {
      return this.open;
   }

   public void open() {
      this.open = true;
      this.openAnim.setTarget(1.0F);
   }

   public void close() {
      this.open = false;
      this.openAnim.setTarget(0.0F);
      this.cancelRename();
      this.pendingConfirm = null;
   }

   public boolean isListening() {
      return this.open && this.renamingSlot >= 0;
   }

   private ConfigStore store() {
      return AetherClient.configStore();
   }

   public void render(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      this.consumeShareCompletion();
      float t = this.openAnim.value();
      if (t > 0.002F || this.open) {
         Theme theme = AetherClient.themes().current();
         vg.rect(0.0F, 0.0F, uiW, uiH, 0.0F, Colors.withAlpha(0xFF060911, 0.80F * t));
         vg.save();
         vg.alpha(t);
         float scale = 0.95F + 0.05F * t;
         vg.translate(uiW * 0.5F, uiH * 0.5F);
         vg.scale(scale);
         vg.translate(-uiW * 0.5F, -uiH * 0.5F);

         ConfigStore store = this.store();
         this.cardH = 442.0F;
         this.cardX = (uiW - CARD_W) * 0.5F;
         this.cardY = (uiH - this.cardH) * 0.5F;

         vg.glow(this.cardX, this.cardY, CARD_W, this.cardH, 12.0F, 18.0F, Colors.withAlpha(theme.accent(), 0.14F));
         vg.rectGradient(this.cardX, this.cardY, CARD_W, this.cardH, 12.0F, theme.background(), theme.backgroundTo(), true);
         vg.rectOutline(this.cardX, this.cardY, CARD_W, this.cardH, 12.0F, 1.2F, Colors.withAlpha(theme.accent(), 0.42F));
         vg.rect(this.cardX + 20.0F, this.cardY, 52.0F, 2.0F, 1.0F, theme.accentBright());

         this.renderHeader(vg, theme, mx, my);
         this.hits.clear();
         float rowY = this.cardY + HEADER_H + 4.0F;
         for (int i = 0; i < 5; i++) {
            this.renderSlot(vg, theme, store.slot(i), this.cardX + 18.0F, rowY, CARD_W - 36.0F, mx, my, store.activeIndex() == i);
            rowY += 67.0F;
         }
         this.renderFooter(vg, theme);
         if (this.pendingConfirm != null) {
            this.renderConfirm(vg, theme, mx, my, uiW, uiH);
         }
         vg.restore();
      }
   }

   private void renderHeader(NVGRenderer vg, Theme theme, float mx, float my) {
      float lx = this.cardX + 22.0F;
      vg.aetherMark(lx + 9.0F, this.cardY + 32.0F, 9.5F, theme.accent(), theme.accentBright());
      vg.text("CONFIG PROFILES", lx + 28.0F, this.cardY + 25.0F, 16.5F, theme.textPrimary());
      vg.text("SAVE LOCALLY OR SHARE VIA OFFLINE CLIPBOARD TOKEN", lx + 28.0F, this.cardY + 44.0F, 10.5F, theme.textMuted());

      this.closeSize = 16.0F;
      this.closeX = this.cardX + CARD_W - 20.0F - this.closeSize;
      this.closeY = this.cardY + 22.0F;
      boolean closeHover = mx >= this.closeX - 5.0F && mx <= this.closeX + this.closeSize + 5.0F
         && my >= this.closeY - 5.0F && my <= this.closeY + this.closeSize + 5.0F;
      if (closeHover) {
         vg.rect(this.closeX - 5.0F, this.closeY - 5.0F, this.closeSize + 10.0F, this.closeSize + 10.0F, 6.0F, Colors.withAlpha(theme.accent(), 0.18F));
      }
      vg.cross(this.closeX, this.closeY, this.closeSize, 1.8F, closeHover ? theme.accentBright() : theme.textMuted());
      vg.line(this.cardX + 18.0F, this.cardY + HEADER_H - 2.0F, this.cardX + CARD_W - 18.0F, this.cardY + HEADER_H - 2.0F, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.9F));
   }

   private void renderSlot(NVGRenderer vg, Theme theme, ConfigStore.Slot slot, float x, float y, float w, float mx, float my, boolean active) {
      boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + ROW_H;
      int fill = active ? theme.moduleActiveFill() : (hovered ? theme.surfaceElevated() : theme.surface());
      vg.rect(x, y, w, ROW_H, 8.0F, fill);
      vg.rectOutline(x, y, w, ROW_H, 8.0F, 1.0F, Colors.withAlpha(theme.accent(), active ? 0.52F : (hovered ? 0.26F : 0.12F)));
      if (active) {
         vg.rect(x + 2.5F, y + 12.0F, 3.0F, 36.0F, 1.5F, theme.accentBright());
      }

      float dotX = x + 18.0F;
      float dotY = y + 30.0F;
      if (active) {
         vg.circleGlow(dotX, dotY, 4.0F, 5.0F, theme.accent());
         vg.circle(dotX, dotY, 4.0F, theme.accentBright());
      } else if (slot.filled()) {
         vg.circle(dotX, dotY, 3.5F, theme.statusEnabled());
      } else {
         vg.circleOutline(dotX, dotY, 3.5F, 1.2F, theme.statusDisabled());
      }

      float textX = x + 34.0F;
      if (this.renamingSlot == slot.index()) {
         this.renderRenameField(vg, theme, textX, y + 12.0F, 196.0F);
      } else {
         vg.textTruncated(slot.name(), textX, y + 22.0F, 14.5F, theme.textPrimary(), 190.0F);
         float nameW = Math.min(vg.textWidth(slot.name(), 14.5F), 190.0F);
         if (active) {
            this.drawTag(vg, theme, textX + nameW + 8.0F, y + 22.0F, "ACTIVE");
         }
         // Rename click target on slot title
         this.hits.add(new Hit(Action.RENAME, slot.index(), textX, y + 10.0F, Math.max(80.0F, nameW + 12.0F), 22.0F, false));
      }

      String sub = this.renamingSlot == slot.index()
         ? "Enter to confirm · Esc to cancel"
         : (slot.filled() ? "Saved · " + relativeTime(slot.savedAt()) + " · Click name to rename" : "Empty slot");
      vg.text(sub, textX, y + 41.0F, 11.0F, theme.textMuted());
      this.layoutButtons(vg, theme, slot, x + w - 14.0F, y, mx, my);
   }

   private void renderRenameField(NVGRenderer vg, Theme theme, float x, float y, float w) {
      float h = 21.0F;
      vg.rect(x, y, w, h, 6.0F, Colors.withAlpha(0xFF060911, 0.72F));
      vg.rectOutline(x, y, w, h, 6.0F, 1.2F, Colors.withAlpha(theme.accentBright(), 0.9F));
      float tx = x + 8.0F;
      float ty = y + h * 0.5F;
      float tw = vg.text(this.renameBuffer.toString(), tx, ty, 12.5F, theme.textPrimary());
      if (System.nanoTime() / 400000000L % 2L == 0L) {
         vg.rect(tx + tw + 1.5F, ty - 6.0F, 1.4F, 12.0F, 0.7F, theme.accentBright());
      }
   }

   private void drawTag(NVGRenderer vg, Theme theme, float x, float y, String label) {
      float tw = vg.textWidth(label, 9.5F);
      vg.rect(x, y - 7.0F, tw + 12.0F, 14.0F, 7.0F, Colors.withAlpha(theme.accent(), 0.22F));
      vg.text(label, x + 6.0F, y, 9.5F, theme.accentBright());
   }

   private void layoutButtons(NVGRenderer vg, Theme theme, ConfigStore.Slot slot, float rightX, float rowY, float mx, float my) {
      record Spec(Action action, String label, boolean primary, boolean danger) {
      }
      List<Spec> specs = new ArrayList<>(5);
      if (slot.filled()) {
         specs.add(new Spec(Action.ACTIVATE, "Load", true, false));
         specs.add(new Spec(Action.SAVE, "Save", false, false));
         specs.add(new Spec(Action.EXPORT, "Export", false, false));
         specs.add(new Spec(Action.IMPORT, "Import", false, false));
         specs.add(new Spec(Action.DELETE, "Delete", false, true));
      } else {
         specs.add(new Spec(Action.SAVE, "Save Current", true, false));
         specs.add(new Spec(Action.IMPORT, "Import Clipboard", false, false));
      }

      float btnH = 26.0F;
      float padX = 10.0F;
      float gap = 6.0F;
      float font = 11.5F;
      float totalW = 0.0F;
      float[] widths = new float[specs.size()];
      for (int i = 0; i < specs.size(); i++) {
         widths[i] = vg.textWidth(specs.get(i).label(), font) + padX * 2.0F;
         totalW += widths[i] + (i > 0 ? gap : 0.0F);
      }

      float bx = rightX - totalW;
      float by = rowY + (ROW_H - btnH) * 0.5F;
      for (int i = 0; i < specs.size(); i++) {
         Spec s = specs.get(i);
         float bw = widths[i];
         boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + btnH;
         this.drawButton(vg, theme, bx, by, bw, btnH, s.label(), font, s.primary(), hov, s.danger());
         this.hits.add(new Hit(s.action(), slot.index(), bx, by, bw, btnH, s.primary()));
         bx += bw + gap;
      }
   }

   private void drawButton(NVGRenderer vg, Theme theme, float x, float y, float w, float h, String label, float font, boolean primary, boolean hovered, boolean danger) {
      int base = danger ? DANGER : theme.accent();
      int bright = danger ? DANGER_BRIGHT : theme.accentBright();
      float tw = vg.textWidth(label, font);
      if (primary) {
         int top = hovered ? Colors.lighten(bright, 0.1F) : bright;
         vg.rectGradient(x, y, w, h, 6.0F, top, base, true);
         vg.text(label, x + (w - tw) * 0.5F, y + h * 0.5F, font, 0xFF08101D);
      } else {
         vg.rect(x, y, w, h, 6.0F, Colors.withAlpha(0xFFFFFFFF, hovered ? 0.11F : 0.05F));
         vg.rectOutline(x, y, w, h, 6.0F, 1.0F, Colors.withAlpha(hovered ? bright : base, hovered ? 0.72F : 0.28F));
         int tc = danger ? Colors.withAlpha(base, 0.9F) : theme.textMuted();
         vg.text(label, x + (w - tw) * 0.5F, y + h * 0.5F, font, hovered ? (danger ? bright : theme.textPrimary()) : tc);
      }
   }

   private void renderFooter(NVGRenderer vg, Theme theme) {
      float fy = this.cardY + this.cardH - 17.0F;
      vg.text("100% OFFLINE LOCAL PROFILES  ·  CLIPBOARD TOKENS NEVER EXPIRE", this.cardX + 20.0F, fy, 10.5F, theme.textDisabled());
   }

   private void renderConfirm(NVGRenderer vg, Theme theme, float mx, float my, float uiW, float uiH) {
      vg.rect(this.cardX, this.cardY, CARD_W, this.cardH, 12.0F, Colors.withAlpha(0xFF060911, 0.65F));
      this.confirmCardW = 390.0F;
      this.confirmCardH = 148.0F;
      this.confirmCardX = (uiW - this.confirmCardW) * 0.5F;
      this.confirmCardY = (uiH - this.confirmCardH) * 0.5F;
      boolean danger = this.pendingConfirm.action() == Action.DELETE;
      vg.glow(this.confirmCardX, this.confirmCardY, this.confirmCardW, this.confirmCardH, 12.0F, 18.0F, Colors.withAlpha(0xFF000000, 0.55F));
      vg.rectGradient(this.confirmCardX, this.confirmCardY, this.confirmCardW, this.confirmCardH, 12.0F, theme.headerTop(), theme.background(), true);
      vg.rectOutline(this.confirmCardX, this.confirmCardY, this.confirmCardW, this.confirmCardH, 12.0F, 1.2F, Colors.withAlpha(danger ? DANGER : theme.accent(), 0.5F));
      vg.text(this.pendingConfirm.title(), this.confirmCardX + 22.0F, this.confirmCardY + 34.0F, 15.5F, theme.textPrimary());
      vg.textTruncated(this.pendingConfirm.body(), this.confirmCardX + 22.0F, this.confirmCardY + 58.0F, 12.0F, theme.textMuted(), this.confirmCardW - 44.0F);

      float btnH = 30.0F;
      float gap = 10.0F;
      float by = this.confirmCardY + this.confirmCardH - btnH - 20.0F;
      this.confirmOkW = 118.0F;
      this.confirmCancelW = 92.0F;
      this.confirmOkH = btnH;
      this.confirmCancelH = btnH;
      this.confirmOkX = this.confirmCardX + this.confirmCardW - 22.0F - this.confirmOkW;
      this.confirmOkY = by;
      this.confirmCancelX = this.confirmOkX - gap - this.confirmCancelW;
      this.confirmCancelY = by;
      boolean okHov = this.hit(mx, my, this.confirmOkX, this.confirmOkY, this.confirmOkW, this.confirmOkH);
      boolean cancelHov = this.hit(mx, my, this.confirmCancelX, this.confirmCancelY, this.confirmCancelW, this.confirmCancelH);
      this.drawButton(vg, theme, this.confirmCancelX, this.confirmCancelY, this.confirmCancelW, this.confirmCancelH, "Cancel", 12.0F, false, cancelHov, false);
      this.drawButton(vg, theme, this.confirmOkX, this.confirmOkY, this.confirmOkW, this.confirmOkH, this.confirmLabel(), 12.0F, true, okHov, danger);
   }

   private String confirmLabel() {
      return switch (this.pendingConfirm.action()) {
         case IMPORT -> "Import";
         case DELETE -> "Delete";
         default -> "Overwrite";
      };
   }

   public boolean mouseClicked(float mx, float my, int button) {
      if (!this.open) {
         return false;
      }
      if (this.renamingSlot >= 0) {
         this.commitRename();
      }
      if (this.pendingConfirm != null) {
         if (this.hit(mx, my, this.confirmOkX, this.confirmOkY, this.confirmOkW, this.confirmOkH)) {
            this.runConfirm();
         } else if (this.hit(mx, my, this.confirmCancelX, this.confirmCancelY, this.confirmCancelW, this.confirmCancelH)
            || !this.hit(mx, my, this.confirmCardX, this.confirmCardY, this.confirmCardW, this.confirmCardH)) {
            this.pendingConfirm = null;
            UiSounds.select();
         }
         return true;
      }
      if (mx >= this.closeX - 5.0F && mx <= this.closeX + this.closeSize + 5.0F
         && my >= this.closeY - 5.0F && my <= this.closeY + this.closeSize + 5.0F) {
         this.close();
         UiSounds.guiClose();
         return true;
      }
      for (Hit h : this.hits) {
         if (h.contains(mx, my)) {
            this.dispatch(h.action, h.slot);
            return true;
         }
      }
      if (mx < this.cardX || mx > this.cardX + CARD_W || my < this.cardY || my > this.cardY + this.cardH) {
         this.close();
         UiSounds.guiClose();
      }
      return true;
   }

   public boolean keyPressed(int keyCode) {
      if (!this.open) {
         return false;
      }
      if (this.renamingSlot >= 0) {
         switch (keyCode) {
            case 256 -> this.cancelRename();
            case 257, 335 -> this.commitRename();
            case 259 -> {
               if (!this.renameBuffer.isEmpty()) {
                  this.renameBuffer.deleteCharAt(this.renameBuffer.length() - 1);
               }
            }
         }
         return true;
      }
      if (this.pendingConfirm != null) {
         switch (keyCode) {
            case 256 -> {
               this.pendingConfirm = null;
               UiSounds.select();
            }
            case 257, 335 -> this.runConfirm();
         }
         return true;
      }
      if (keyCode == 256) {
         this.close();
         UiSounds.guiClose();
      }
      return true;
   }

   public boolean charTyped(int codepoint) {
      if (this.open && this.renamingSlot >= 0) {
         if (this.renameBuffer.length() < 24) {
            char c = (char)codepoint;
            if (c >= ' ' && c < 127) {
               this.renameBuffer.append(c);
            }
         }
         return true;
      }
      return false;
   }

   private void dispatch(Action action, int slot) {
      if (this.sharing && (action == Action.EXPORT || action == Action.IMPORT)) {
         return;
      }
      ConfigStore store = this.store();
      switch (action) {
         case ACTIVATE -> this.doActivate(slot);
         case SAVE -> {
            if (store.slot(slot).filled()) {
               this.pendingConfirm = new Confirm(
                  Action.SAVE,
                  slot,
                  "Overwrite \"" + store.slot(slot).name() + "\"?",
                  "This replaces the config saved in slot " + (slot + 1) + ".",
                  null
               );
               UiSounds.select();
            } else {
               this.doSave(slot);
            }
         }
         case RENAME -> this.beginRename(slot);
         case EXPORT -> this.doExport(slot);
         case IMPORT -> {
            String clip = this.readClipboard();
            if (clip == null || clip.isBlank()) {
               this.toast("Clipboard is empty");
               UiSounds.select();
               return;
            }
            String trimmed = clip.trim();
            if (!trimmed.startsWith(ConfigShareService.PREFIX) && !trimmed.startsWith("{")) {
               this.toast("Copy an AETHER1:... token or JSON config first");
               UiSounds.select();
               return;
            }
            if (store.slot(slot).filled()) {
               this.pendingConfirm = new Confirm(
                  Action.IMPORT,
                  slot,
                  "Replace \"" + store.slot(slot).name() + "\"?",
                  "Imports clipboard config into slot " + (slot + 1) + ".",
                  trimmed
               );
               UiSounds.select();
            } else {
               this.doImport(slot, trimmed);
            }
         }
         case DELETE -> {
            this.pendingConfirm = new Confirm(
               Action.DELETE,
               slot,
               "Delete \"" + store.slot(slot).name() + "\"?",
               "This permanently removes slot " + (slot + 1) + ".",
               null
            );
            UiSounds.select();
         }
      }
   }

   private void runConfirm() {
      Confirm c = this.pendingConfirm;
      this.pendingConfirm = null;
      if (c != null) {
         switch (c.action()) {
            case SAVE -> this.doSave(c.slot());
            case IMPORT -> this.doImport(c.slot(), c.payload());
            case DELETE -> this.doDelete(c.slot());
            default -> {
            }
         }
      }
   }

   private void doDelete(int slot) {
      String name = this.store().slot(slot).name();
      if (this.store().delete(slot)) {
         this.toast("Deleted \"" + name + "\"");
         UiSounds.select();
      } else {
         this.toast("Couldn't delete the config");
      }
   }

   private void doSave(int slot) {
      if (this.store().save(slot)) {
         this.toast("Saved to \"" + this.store().slot(slot).name() + "\"");
         UiSounds.toggle(true);
      } else {
         this.toast("Couldn't save the config");
      }
   }

   private void doActivate(int slot) {
      if (this.store().activate(slot)) {
         this.toast("Activated \"" + this.store().slot(slot).name() + "\"");
         UiSounds.toggle(true);
      } else {
         this.toast("That slot is empty");
      }
   }

   private void doExport(int slot) {
      String json = this.store().export(slot);
      if (json == null) {
         this.toast("That slot is empty");
      } else {
         this.sharing = true;
         ConfigShareService.export(json).thenAccept(res -> this.shareCompletion = new ShareCompletion(true, slot, res));
         this.consumeShareCompletion();
      }
   }

   private void doImport(int slot, String payload) {
      this.sharing = true;
      ConfigShareService.importCode(payload).thenAccept(res -> this.shareCompletion = new ShareCompletion(false, slot, res));
      this.consumeShareCompletion();
   }

   private void consumeShareCompletion() {
      ShareCompletion sc = this.shareCompletion;
      if (sc != null) {
         this.shareCompletion = null;
         this.sharing = false;
         ConfigShareService.Result res = sc.result();
         if (!res.ok()) {
            this.toast(res.message());
            UiSounds.select();
         } else if (sc.exporting()) {
            this.setClipboard(res.message());
            this.toast("Aether config token copied to clipboard");
            UiSounds.select();
         } else {
            ConfigStore.ImportResult ir = this.store().importInto(sc.slot(), res.config());
            this.toast(ir.message());
            UiSounds.toggle(ir.ok());
         }
      }
   }

   private void beginRename(int slot) {
      this.renamingSlot = slot;
      this.renameBuffer.setLength(0);
      this.renameBuffer.append(this.store().slot(slot).name());
      UiSounds.select();
   }

   private void commitRename() {
      if (this.renamingSlot >= 0) {
         this.store().rename(this.renamingSlot, this.renameBuffer.toString());
         this.renamingSlot = -1;
      }
   }

   private void cancelRename() {
      this.renamingSlot = -1;
   }

   private boolean hit(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   private void toast(String msg) {
      if (AetherClient.notifications() != null) {
         AetherClient.notifications().pushInfo(msg);
      }
   }

   private String readClipboard() {
      try {
         return Minecraft.getInstance().keyboardHandler.getClipboard();
      } catch (Exception e) {
         return null;
      }
   }

   private void setClipboard(String s) {
      try {
         Minecraft.getInstance().keyboardHandler.setClipboard(s);
      } catch (Exception ignored) {
      }
   }

   private static String relativeTime(long savedAt) {
      if (savedAt <= 0L) {
         return "just now";
      }
      long diff = System.currentTimeMillis() - savedAt;
      if (diff < 60_000L) {
         return "just now";
      }
      long mins = diff / 60_000L;
      if (mins < 60L) {
         return mins + "m ago";
      }
      long hrs = mins / 60L;
      return hrs < 24L ? hrs + "h ago" : (hrs / 24L) + "d ago";
   }

   public void debugBeginRename(int slot) {
      this.open();
      this.beginRename(slot);
   }

   public void debugConfirmOverwrite(int slot) {
      this.open();
      this.pendingConfirm = new Confirm(
         Action.SAVE,
         slot,
         "Overwrite \"" + this.store().slot(slot).name() + "\"?",
         "This replaces the config saved in slot " + (slot + 1) + ".",
         null
      );
   }

   public void debugConfirmDelete(int slot) {
      this.open();
      this.pendingConfirm = new Confirm(
         Action.DELETE,
         slot,
         "Delete \"" + this.store().slot(slot).name() + "\"?",
         "This permanently removes slot " + (slot + 1) + ".",
         null
      );
   }

   private enum Action {
      ACTIVATE,
      SAVE,
      RENAME,
      EXPORT,
      IMPORT,
      DELETE
   }

   private record Confirm(Action action, int slot, String title, String body, String payload) {
   }

   private static final class Hit {
      final Action action;
      final int slot;
      final float x;
      final float y;
      final float w;
      final float h;
      final boolean primary;

      Hit(Action action, int slot, float x, float y, float w, float h, boolean primary) {
         this.action = action;
         this.slot = slot;
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.primary = primary;
      }

      boolean contains(float mx, float my) {
         return mx >= this.x && mx <= this.x + this.w && my >= this.y && my <= this.y + this.h;
      }
   }

   private record ShareCompletion(boolean exporting, int slot, ConfigShareService.Result result) {
   }
}
