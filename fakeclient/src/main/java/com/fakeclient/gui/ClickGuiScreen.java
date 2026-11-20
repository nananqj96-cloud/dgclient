/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.Configs
 *  com.fakeclient.FakeClient
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.Anim
 *  com.fakeclient.gui.ClickGuiScreen$ClickGuiScreen$RowRects
 *  com.fakeclient.gui.ClickGuiScreen$ClickGuiScreen$SRow
 *  com.fakeclient.gui.Gui
 *  com.fakeclient.gui.Icons
 *  com.fakeclient.license.License
 *  com.fakeclient.module.Module
 *  com.fakeclient.util.Sfx
 *  net.minecraft.class_10799
 *  net.minecraft.class_11905
 *  net.minecraft.class_11908
 *  net.minecraft.class_11909
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 *  net.minecraft.class_5348
 *  org.joml.Matrix3x2fStack
 */
package com.fakeclient.gui;

import com.fakeclient.Configs;
import com.fakeclient.FakeClient;
import com.fakeclient.Settings;
import com.fakeclient.gui.Anim;
import com.fakeclient.gui.ClickGuiScreen;
import com.fakeclient.gui.Gui;
import com.fakeclient.gui.Icons;
import com.fakeclient.license.License;
import com.fakeclient.module.Module;
import com.fakeclient.util.Sfx;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_10799;
import net.minecraft.class_11905;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_5348;
import org.joml.Matrix3x2fStack;

public class ClickGuiScreen
extends class_437 {
    private static final int PANEL_W = 704;
    private static final int SIDEBAR_W = 184;
    private static final int TOP_H = 68;
    private static final int FOOT_H = 32;
    private static final int PAD = 20;
    private static final int PANEL_R = 24;
    private static final int CARD_R = 18;
    private static final int ROW_GAP = 8;
    private static final int SW_W = 42;
    private static final int SW_H = 22;
    private static final int PLATE = 36;
    private static final int S1 = 4;
    private static final int S2 = 8;
    private static final int S3 = 12;
    private static final int S4 = 16;
    private static final int EDIT_W = 58;
    private static final int KEY_D = 24;
    private static final int BADGE_W = 30;
    private static final int BADGE_H = 18;
    private static final int ICON_BTN = 38;
    private static final int CAT_H = 42;
    private static final int CAT_GAP = 10;
    private static final int CAT_R = 12;
    private static final int ICON = 16;
    private static final int STATUS_H = 46;
    private static final String ALL = "All";
    private static final int SROW = 40;
    private static final int SGAP = 8;
    private static final int SECTION_H = 26;
    private static final int SLIDER_H = 48;
    private static final int SWATCH_H = 30;
    private static final int CFG_H = 32;
    private static final int CFG_GAP = 5;
    private static final int CFG_BAND = 32;
    private static final int TEXT = -1;
    private static final int SUBTEXT = -3419946;
    private static final int MUTED = -6578010;
    private static final int GHOST = -9143937;
    private static final int DESC = -4933185;
    private static final int SURFACE_0 = -16448251;
    private static final int WINDOW_BG = -16053493;
    private static final int SIDEBAR_BG = -15856114;
    private static final int SURFACE_1 = -16053493;
    private static final int SURFACE_2 = -15527149;
    private static final int SURFACE_3 = -15066598;
    private static final int PRESSED = -14671840;
    private static final int SUNKEN = -16250872;
    private static final int BORDER = 0xFFFFFFF;
    private static final int BORDER_LIT = 0x1AFFFFFF;
    private static final int HAIRLINE = 0xFFFFFFF;
    private static final int DANGER = -2565928;
    private static final int VIEW_MODULES = 0;
    private static final int VIEW_SETTINGS = 1;
    private static final int VIEW_CONFIGS = 2;
    private static final float SCALE_MIN = 0.5f;
    private static final float SCALE_MAX = 1.6f;
    private static final int NAME_MAX_LEN = 24;
    private static final int SEARCH_MAX_LEN = 20;
    private static final int SEARCH_W = 232;
    private static final int SEARCH_W_FOCUS = 264;
    private static final float ROW_STAGGER = 26.0f;
    private static final float ROW_FADE = 210.0f;
    private static final float VIEW_FADE = 200.0f;
    private static final float CAT_STAGGER = 34.0f;
    private static final float CAT_FADE = 240.0f;
    private static final float BAR_HOLD = 900.0f;
    private static final int[] PRESETS = new int[]{0xFFFFFF, 0xE6E6E6, 12765394, 0xA8A8A8, 0x8C8C8C, 0x6E6E6E, 0x555555, 0x3C3C3C};
    private static final String TITLE = "Fake Client".toUpperCase(Locale.ROOT);
    private static final class_2960 LOGO = class_2960.method_60655((String)"fakeclient", (String)"textures/gui/logo.png");
    private int view = 0;
    private int category = 0;
    private float scale = 1.0f;
    private int panelX;
    private int panelY;
    private int panelH;
    private int basePanelY;
    private int contentX;
    private int contentY;
    private int contentColH;
    private int vpX;
    private int vpY;
    private int vpW;
    private int vpH;
    private int bandH;
    private int contentH;
    private int maxScroll;
    private float scrollTarget = 0.0f;
    private float scrollShown = 0.0f;
    private long openAt;
    private Module listening = null;
    private String dragId = null;
    private int dragTrackX;
    private int dragTrackW = 1;
    private Float pendingScale = null;
    private int searchW = 232;
    private long listEpoch;
    private long viewEpoch;
    private int viewDir = 1;
    private String pressed = null;
    private long scrollAt;
    private boolean draggingBar = false;
    private double dragBarOffset = 0.0;
    private boolean draggingPanel = false;
    private double dragPanelX;
    private double dragPanelY;
    private String search = "";
    private boolean searchFocused = false;
    private String configName = "";
    private boolean nameFocused = false;
    private List<String> configCache = new ArrayList<String>();
    private String flash = "";
    private long flashAt = 0L;
    private static final int R_SECTION = 0;
    private static final int R_SWITCH = 1;
    private static final int R_SLIDER = 2;
    private static final int R_BUTTON = 3;
    private static final int R_HUE = 4;
    private static final int R_SWATCH = 5;

    public ClickGuiScreen() {
        super((class_2561)class_2561.method_43470((String)"Fake Client"));
    }

    protected void method_25426() {
        this.listEpoch = this.openAt = System.currentTimeMillis();
        this.viewEpoch = this.openAt;
        this.refreshConfigs();
        this.layout();
    }

    private void restartList() {
        this.listEpoch = System.currentTimeMillis();
        this.scrollTarget = 0.0f;
    }

    private float rowIntro(int i) {
        if (!Settings.get().animations) {
            return 1.0f;
        }
        float t = ((float)(System.currentTimeMillis() - this.listEpoch) - (float)i * 26.0f) / 210.0f;
        return Anim.easeOutExpo((float)ClickGuiScreen.clamp(t, 0.0f, 1.0f));
    }

    private float viewIntro() {
        return Anim.since((long)this.viewEpoch, (float)200.0f);
    }

    private float press(String id) {
        return Anim.bool((String)("press_" + id), (boolean)id.equals(this.pressed), (float)34.0f);
    }

    private boolean hold(String id) {
        this.pressed = id;
        return true;
    }

    private void layout() {
        Settings s = Settings.get();
        this.bandH = this.view == 2 ? 32 : 0;
        int desired = this.pageContentH();
        int chrome = 100 + this.bandH + 14;
        int avail = Math.max(120, this.field_22790 - 40 - chrome);
        this.vpH = Math.min(desired, avail);
        int sidebarNeed = Math.max(0, this.sidebarMinH() - this.bandH - 14);
        this.vpH = Math.max(this.vpH, sidebarNeed);
        this.panelH = chrome + this.vpH;
        this.panelX = (this.field_22789 - 704) / 2 + s.panelOffX;
        this.basePanelY = (this.field_22790 - this.panelH) / 2 + s.panelOffY;
        int freeX = this.field_22789 - 704;
        int freeY = this.field_22790 - this.panelH;
        this.panelX = freeX > 0 ? ClickGuiScreen.clampInt(this.panelX, 0, freeX) : (this.field_22789 - 704) / 2;
        this.panelY = this.basePanelY = freeY > 0 ? ClickGuiScreen.clampInt(this.basePanelY, 0, freeY) : (this.field_22790 - this.panelH) / 2;
        this.contentX = this.panelX + 184;
        this.contentY = this.panelY + 68;
        this.contentColH = this.panelH - 68 - 32;
        this.vpX = this.contentX + 20;
        this.vpW = 480;
        this.vpY = this.contentY + this.bandH + 7;
        this.contentH = desired;
        this.maxScroll = Math.max(0, this.contentH - this.vpH);
        this.scrollTarget = ClickGuiScreen.clamp(this.scrollTarget, 0.0f, this.maxScroll);
        this.scale = this.computeScale();
    }

    private int sidebarMinH() {
        int label = 34;
        int cats = this.sidebarEntries().size() * 52;
        int status = 74;
        return label + cats + status;
    }

    private float computeScale() {
        float want = Settings.get().uiScale;
        float fit = Math.min(((float)this.field_22789 - 8.0f) / 704.0f, ((float)this.field_22790 - 8.0f) / (float)Math.max(1, this.panelH));
        float hi = Math.max(0.5f, Math.min(1.6f, fit));
        return ClickGuiScreen.clamp(want, 0.5f, hi);
    }

    private double ux(double x) {
        return (x - (double)this.field_22789 / 2.0) / (double)this.scale + (double)this.field_22789 / 2.0;
    }

    private double uy(double y) {
        return (y - (double)this.field_22790 / 2.0) / (double)this.scale + (double)this.field_22790 / 2.0;
    }

    private int pageContentH() {
        return switch (this.view) {
            case 1 -> this.settingsTotalH();
            case 2 -> this.configsH();
            default -> this.modulesH();
        };
    }

    private int rowH() {
        Settings s = Settings.get();
        if (s.compactRows) {
            return s.showDescriptions ? 46 : 40;
        }
        return s.showDescriptions ? 64 : 48;
    }

    private int modulesH() {
        int n = this.visibleModules().size();
        if (n == 0) {
            return 60;
        }
        return n * this.rowH() + (n - 1) * 8;
    }

    private int configsH() {
        int rows = this.configCache.size();
        return rows == 0 ? 44 : rows * 32 + Math.max(0, rows - 1) * 5;
    }

    private List<String> sidebarEntries() {
        ArrayList<String> out = new ArrayList<String>();
        out.add(ALL);
        for (Module m : FakeClient.MODULES.getModules()) {
            if (out.contains(m.getCategory())) continue;
            out.add(m.getCategory());
        }
        return out;
    }

    private String selectedCategory() {
        List<String> cats = this.sidebarEntries();
        return cats.get(Math.floorMod(this.category, cats.size()));
    }

    private List<Module> visibleModules() {
        ArrayList<Module> out = new ArrayList<Module>();
        String q = this.search.trim().toLowerCase(Locale.ROOT);
        String cat = this.selectedCategory();
        for (Module m : FakeClient.MODULES.getModules()) {
            if (!q.isEmpty() ? !m.getName().toLowerCase(Locale.ROOT).contains(q) && !m.getDescription().toLowerCase(Locale.ROOT).contains(q) : !ALL.equals(cat) && !m.getCategory().equals(cat)) continue;
            out.add(m);
        }
        return out;
    }

    private int countIn(String cat) {
        if (ALL.equals(cat)) {
            return FakeClient.MODULES.getModules().size();
        }
        int n = 0;
        for (Module m : FakeClient.MODULES.getModules()) {
            if (!m.getCategory().equals(cat)) continue;
            ++n;
        }
        return n;
    }

    private void refreshConfigs() {
        this.configCache = Configs.list();
    }

    private float intro() {
        return Anim.easeOutExpo((float)Anim.since((long)this.openAt, (float)260.0f));
    }

    private float introPop() {
        return Anim.easeOutBack((float)Anim.since((long)this.openAt, (float)340.0f));
    }

    public void method_25420(class_332 ctx, int mouseX, int mouseY, float delta) {
        Gui.backdrop((class_332)ctx, (int)this.field_22789, (int)this.field_22790);
    }

    protected void method_57734(class_332 ctx) {
        if (Settings.get().menuBlur) {
            super.method_57734(ctx);
        }
    }

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        float intro = this.intro();
        Gui.backdrop((class_332)ctx, (int)this.field_22789, (int)this.field_22790, (float)intro);
        this.scale = this.computeScale();
        int dy = Math.round((1.0f - intro) * 12.0f);
        this.panelY = this.basePanelY + dy;
        this.contentY = this.panelY + 68;
        this.vpY = this.contentY + this.bandH + 7;
        int mx = (int)Math.round(this.ux(mouseX));
        int my = (int)Math.round(this.uy(mouseY));
        this.scrollShown = Anim.to((String)"scroll", (float)ClickGuiScreen.clamp(this.scrollTarget, 0.0f, this.maxScroll), (float)18.0f);
        this.searchW = Math.round(Anim.to((String)"searchw", (float)(this.searchFocused ? 264.0f : 232.0f), (float)15.0f));
        float openScale = this.scale * (0.94f + 0.06f * this.introPop());
        Matrix3x2fStack mat = ctx.method_51448();
        mat.pushMatrix();
        mat.translate((float)this.field_22789 / 2.0f, (float)this.field_22790 / 2.0f);
        mat.scale(openScale);
        mat.translate((float)(-this.field_22789) / 2.0f, (float)(-this.field_22790) / 2.0f);
        this.renderChrome(ctx);
        this.renderTopBar(ctx, mx, my);
        this.renderSidebar(ctx, mx, my);
        if (this.view == 2) {
            this.renderConfigBand(ctx, mx, my);
        }
        float vt = this.viewIntro();
        float slide = (1.0f - Anim.easeOutExpo((float)vt)) * 26.0f * (float)this.viewDir;
        ctx.method_44379(this.vpX, this.vpY, this.vpX + this.vpW, this.vpY + this.vpH);
        Gui.pushOffset((class_332)ctx, (float)slide, (float)0.0f);
        int top = this.vpY - Math.round(this.scrollShown);
        switch (this.view) {
            case 1: {
                this.renderSettings(ctx, mx, my, top);
                break;
            }
            case 2: {
                this.renderConfigs(ctx, mx, my, top);
                break;
            }
            default: {
                this.renderModules(ctx, mx, my, top);
            }
        }
        Gui.pop((class_332)ctx);
        ctx.method_44380();
        this.renderScrollbar(ctx, mx, my);
        this.renderFooter(ctx);
        mat.popMatrix();
        super.method_25394(ctx, mouseX, mouseY, delta);
    }

    private void renderChrome(class_332 ctx) {
        Gui.dropShadow((class_332)ctx, (int)this.panelX, (int)this.panelY, (int)704, (int)this.panelH, (int)24, (int)10, (int)130);
        Gui.bloom((class_332)ctx, (int)this.panelX, (int)this.panelY, (int)704, (int)this.panelH, (int)24, (int)7, (float)0.9f);
        float op = (float)Math.max(40, Math.min(100, Settings.get().panelOpacity)) / 100.0f;
        Gui.roundedRect((class_332)ctx, (int)this.panelX, (int)this.panelY, (int)704, (int)this.panelH, (int)24, (int)0x26FFFFFF);
        Gui.roundedRect((class_332)ctx, (int)(this.panelX + 1), (int)(this.panelY + 1), (int)702, (int)(this.panelH - 2), (int)23, (int)Gui.alpha((int)-16053493, (float)op));
        Gui.topHighlight((class_332)ctx, (int)(this.panelX + 1), (int)(this.panelY + 1), (int)702, (int)24, (int)46);
        Gui.roundedGradient((class_332)ctx, (int)(this.panelX + 1), (int)(this.panelY + 1), (int)702, (int)67, (int)23, (int)0xEFFFFFF, (int)0x2FFFFFF);
        ctx.method_25294(this.panelX + 1, this.panelY + 68, this.panelX + 704 - 1, this.panelY + 68 + 1, 0xFFFFFFF);
        int sy = this.panelY + 68 + 1;
        int sh = this.contentColH - 1;
        ctx.method_25294(this.panelX + 1, sy, this.panelX + 184, sy + sh, Gui.alpha((int)-15856114, (float)op));
        ctx.method_25294(this.panelX + 184, sy, this.panelX + 184 + 1, sy + sh, 0xFFFFFFF);
        ctx.method_25294(this.panelX + 1, this.panelY + this.panelH - 32, this.panelX + 704 - 1, this.panelY + this.panelH - 32 + 1, 0xFFFFFFF);
    }

    private void renderTopBar(class_332 ctx, int mouseX, int mouseY) {
        int accent = Gui.accent() & 0xFFFFFF;
        int ty = this.panelY + 29;
        int logoD = 38;
        int lx = this.panelX + 20;
        int ly = this.panelY + (68 - logoD) / 2;
        Gui.bloom((class_332)ctx, (int)lx, (int)ly, (int)logoD, (int)logoD, (int)(logoD / 2), (int)6, (float)0.9f);
        ctx.method_25290(class_10799.field_56883, LOGO, lx, ly, 0.0f, 0.0f, logoD, logoD, logoD, logoD);
        int tx = lx + logoD + 16;
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)TITLE), (int)tx, (int)ty, (int)-1);
        int cx = tx + this.field_22793.method_27525((class_5348)Gui.bold((String)TITLE)) + 16;
        ctx.method_25294(cx, ty - 4, cx + 1, ty + 9 + 4, 0x2EFFFFFF);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)this.breadcrumb()), (int)(cx + 12), (int)ty, (int)-3419946);
        this.renderSearchBox(ctx, mouseX, mouseY, accent);
        for (int i = 0; i < 2; ++i) {
            int[] r = this.iconBtnRect(i);
            boolean active = i == 0 && this.view == 1 || i == 1 && this.view == 2;
            boolean hover = !active && Gui.inside((double)mouseX, (double)mouseY, (int)r[0], (int)r[1], (int)r[2], (int)r[3]);
            float a = Anim.bool((String)("icon_" + i), (boolean)active, (float)16.0f);
            float h = Anim.bool((String)("iconh_" + i), (boolean)hover, (float)16.0f);
            float p = this.press("icon_" + i);
            Gui.pushScale((class_332)ctx, (int)r[0], (int)r[1], (int)r[2], (int)r[3], (float)(1.0f + h * 0.05f - p * 0.09f));
            int rad = 11;
            if (a > 0.4f) {
                Gui.bloom((class_332)ctx, (int)r[0], (int)r[1], (int)r[2], (int)r[3], (int)rad, (int)4, (float)a);
            }
            Gui.roundedRect((class_332)ctx, (int)r[0], (int)r[1], (int)r[2], (int)r[3], (int)rad, (int)Gui.lerp((int)Gui.lerp((int)0xFFFFFFF, (int)0x1AFFFFFF, (float)h), (int)0x4DFFFFFF, (float)a));
            int fill = Gui.lerp((int)-15527149, (int)-15066598, (float)Math.max(a, h * 0.5f));
            Gui.roundedGradient((class_332)ctx, (int)(r[0] + 1), (int)(r[1] + 1), (int)(r[2] - 2), (int)(r[3] - 2), (int)(rad - 1), (int)(Gui.lighten((int)(fill & 0xFFFFFF), (int)6) | 0xFF000000), (int)fill);
            Gui.topHighlight((class_332)ctx, (int)(r[0] + 1), (int)(r[1] + 1), (int)(r[2] - 2), (int)rad, (int)30);
            int col = Gui.lerp((int)Gui.lerp((int)-6578010, (int)-1, (float)h), (int)-1, (float)a);
            int bg = Gui.lerp((int)-15527149, (int)-15066598, (float)a);
            int gx = r[0] + Icons.box((int)r[2]);
            int gy = r[1] + Icons.box((int)r[3]);
            if (i == 0) {
                float turn = Anim.to((String)"gear", (float)(h * 1.2f + a * 6.283f), (float)6.0f);
                Icons.gear((class_332)ctx, (int)gx, (int)gy, (int)col, (int)bg, (float)turn);
            } else {
                Icons.folder((class_332)ctx, (int)gx, (int)gy, (int)col, (int)bg, (float)Math.max(h, a));
            }
            Gui.pop((class_332)ctx);
        }
    }

    private void renderSearchBox(class_332 ctx, int mouseX, int mouseY, int accent) {
        int[] sr = this.searchRect();
        boolean hover = Gui.inside((double)mouseX, (double)mouseY, (int)sr[0], (int)sr[1], (int)sr[2], (int)sr[3]);
        int rad = sr[3] / 2;
        float focus = Anim.bool((String)"searchfocus", (boolean)this.searchFocused, (float)14.0f);
        if (focus > 0.4f) {
            Gui.bloom((class_332)ctx, (int)sr[0], (int)sr[1], (int)sr[2], (int)sr[3], (int)rad, (int)5, (float)focus);
        }
        Gui.roundedRect((class_332)ctx, (int)sr[0], (int)sr[1], (int)sr[2], (int)sr[3], (int)rad, (int)Gui.lerp((int)(hover ? 0x1AFFFFFF : 0xFFFFFFF), (int)0x6BFFFFFF, (float)focus));
        Gui.roundedGradient((class_332)ctx, (int)(sr[0] + 1), (int)(sr[1] + 1), (int)(sr[2] - 2), (int)(sr[3] - 2), (int)(rad - 1), (int)-16250872, (int)-16053493);
        Gui.topHighlight((class_332)ctx, (int)(sr[0] + 1), (int)(sr[1] + 1), (int)(sr[2] - 2), (int)rad, (int)56);
        int icx = sr[0] + 8;
        int icy = sr[1] + Icons.box((int)sr[3]);
        Icons.search((class_332)ctx, (int)icx, (int)icy, (int)(this.searchFocused ? -3419946 : -6578010), (int)-16250872);
        int fieldX = icx + 16 + 8;
        int fieldW = sr[0] + sr[2] - 12 - fieldX;
        int ty = sr[1] + (sr[3] - 9) / 2;
        if (this.search.isEmpty() && !this.searchFocused) {
            Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"Search modules..."), (int)(fieldX + fieldW / 2), (int)ty, (int)-6578010);
            return;
        }
        String shown = this.trim(this.search, fieldW - 6);
        int tw = this.field_22793.method_27525((class_5348)Gui.font((String)shown));
        int tx = fieldX + (fieldW - tw) / 2;
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)shown), (int)tx, (int)ty, (int)-1);
        if (this.searchFocused && System.currentTimeMillis() / 500L % 2L == 0L) {
            ctx.method_25294(tx + tw + 1, ty - 1, tx + tw + 2, ty + 9, 0xFF000000 | accent);
        }
    }

    private String breadcrumb() {
        if (!this.search.trim().isEmpty()) {
            return "Search";
        }
        return switch (this.view) {
            case 1 -> "Settings";
            case 2 -> "Configs";
            default -> this.selectedCategory();
        };
    }

    private int[] iconBtnRect(int i) {
        int y = this.panelY + 15;
        int settingsX = this.panelX + 704 - 20 - 38;
        int configsX = settingsX - 8 - 38;
        return new int[]{i == 0 ? settingsX : configsX, y, 38, 38};
    }

    private int[] searchRect() {
        int y = this.panelY + 15;
        int right = this.iconBtnRect(1)[0] - 16;
        return new int[]{right - this.searchW, y, this.searchW, 38};
    }

    private boolean inDragStrip(double mx, double my) {
        if (!Gui.inside((double)mx, (double)my, (int)this.panelX, (int)this.panelY, (int)704, (int)68)) {
            return false;
        }
        int[] sr = this.searchRect();
        if (Gui.inside((double)mx, (double)my, (int)sr[0], (int)sr[1], (int)sr[2], (int)sr[3])) {
            return false;
        }
        for (int i = 0; i < 2; ++i) {
            int[] r = this.iconBtnRect(i);
            if (!Gui.inside((double)mx, (double)my, (int)r[0], (int)r[1], (int)r[2], (int)r[3])) continue;
            return false;
        }
        return true;
    }

    private void renderSidebar(class_332 ctx, int mouseX, int mouseY) {
        int accent = Gui.accent() & 0xFFFFFF;
        Gui.tracked((class_332)ctx, (class_327)this.field_22793, (String)"CATEGORIES", (int)(this.panelX + 16), (int)(this.contentY + 16), (int)-3419946, (int)2);
        List<String> cats = this.sidebarEntries();
        boolean showSel = this.view == 0 && this.search.trim().isEmpty();
        float animIdx = Anim.spring((String)"catidx", (float)(showSel ? (float)this.category : -1.4f), (float)260.0f, (float)22.0f);
        if (animIdx > -1.0f) {
            int[] base = this.catRect(0);
            int barH = Math.round(19.32f);
            int barY = base[1] + Math.round(animIdx * 52.0f) + (42 - barH) / 2;
            float fade = ClickGuiScreen.clamp(animIdx + 1.0f, 0.0f, 1.0f);
            Gui.roundedRect((class_332)ctx, (int)(base[0] - 4), (int)barY, (int)3, (int)barH, (int)1, (int)Gui.alpha((int)(0xFF000000 | accent), (float)fade));
        }
        for (int i = 0; i < cats.size(); ++i) {
            String cat = cats.get(i);
            int[] r = this.catRect(i);
            boolean selected = showSel && i == this.category;
            boolean hover = !selected && Gui.inside((double)mouseX, (double)mouseY, (int)r[0], (int)r[1], (int)r[2], (int)r[3]);
            float a = Anim.bool((String)("cat_" + cat), (boolean)selected, (float)15.0f);
            float h = Anim.bool((String)("cath_" + cat), (boolean)hover, (float)15.0f);
            float p = this.press("cat_" + cat);
            float entry = Anim.easeOutExpo((float)Anim.since((long)(this.openAt + (long)((float)i * 34.0f)), (float)240.0f));
            Gui.pushOffset((class_332)ctx, (float)((1.0f - entry) * -14.0f), (float)0.0f);
            Gui.pushScale((class_332)ctx, (int)r[0], (int)r[1], (int)r[2], (int)r[3], (float)(1.0f - p * 0.04f));
            float lit = Math.max(a, h * 0.55f);
            if (lit > 0.4f) {
                Gui.bloom((class_332)ctx, (int)r[0], (int)r[1], (int)r[2], (int)r[3], (int)12, (int)4, (float)lit);
            }
            Gui.roundedRect((class_332)ctx, (int)r[0], (int)r[1], (int)r[2], (int)r[3], (int)12, (int)Gui.lerp((int)Gui.lerp((int)0xFFFFFF, (int)0x1AFFFFFF, (float)h), (int)0x59FFFFFF, (float)a));
            if (lit > 0.01f) {
                Gui.roundedGradient((class_332)ctx, (int)(r[0] + 1), (int)(r[1] + 1), (int)(r[2] - 2), (int)(r[3] - 2), (int)11, (int)Gui.alpha((int)-15066598, (float)lit), (int)Gui.alpha((int)-15527149, (float)lit));
                Gui.topHighlight((class_332)ctx, (int)(r[0] + 1), (int)(r[1] + 1), (int)(r[2] - 2), (int)12, (int)Math.round(26.0f * lit));
            }
            int col = Gui.lerp((int)Gui.lerp((int)-6578010, (int)-3419946, (float)h), (int)-1, (float)a);
            int iconX = r[0] + 12;
            Icons.category((class_332)ctx, (String)cat, (int)iconX, (int)(r[1] + Icons.box((int)r[3])), (int)Gui.alpha((int)col, (float)entry), (int)Gui.lerp((int)-16053493, (int)-15066598, (float)lit));
            int badgeX = r[0] + r[2] - 12 - 30;
            int badgeY = r[1] + (r[3] - 18) / 2;
            Gui.roundedRect((class_332)ctx, (int)badgeX, (int)badgeY, (int)30, (int)18, (int)9, (int)Gui.alpha((int)Gui.lerp((int)0xFFFFFFF, (int)0x1AFFFFFF, (float)lit), (float)entry));
            Gui.roundedRect((class_332)ctx, (int)(badgeX + 1), (int)(badgeY + 1), (int)28, (int)16, (int)8, (int)Gui.alpha((int)Gui.lerp((int)-16053493, (int)-15066598, (float)lit), (float)entry));
            Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)String.valueOf(this.countIn(cat))), (int)(badgeX + 15), (int)(badgeY + 4), (int)Gui.alpha((int)(a > 0.5f ? -1 : -6578010), (float)entry));
            int labelX = iconX + 16 + 12;
            int labelMax = badgeX - 8 - labelX;
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)(a > 0.5f ? Gui.bold((String)this.trimBold(cat, labelMax)) : Gui.font((String)this.trim(cat, labelMax))), (int)labelX, (int)(r[1] + (r[3] - 9) / 2), (int)Gui.alpha((int)col, (float)entry));
            Gui.pop((class_332)ctx);
            Gui.pop((class_332)ctx);
        }
        long on = FakeClient.MODULES.getModules().stream().filter(Module::isEnabled).count();
        int total = FakeClient.MODULES.getModules().size();
        int cx = this.panelX + 16;
        int cw = 152;
        int ch = 46;
        int cy = this.panelY + this.panelH - 32 - ch - 16;
        int lastCat = this.catRect(cats.size() - 1)[1] + 42 + 12;
        if (cy < lastCat) {
            return;
        }
        Gui.roundedRect((class_332)ctx, (int)cx, (int)cy, (int)cw, (int)ch, (int)12, (int)0xFFFFFFF);
        Gui.roundedGradient((class_332)ctx, (int)(cx + 1), (int)(cy + 1), (int)(cw - 2), (int)(ch - 2), (int)11, (int)-15527149, (int)-16053493);
        Gui.topHighlight((class_332)ctx, (int)(cx + 1), (int)(cy + 1), (int)(cw - 2), (int)12, (int)22);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)(total + " Installed")), (int)(cx + 12), (int)(cy + 11), (int)-1);
        float pulse = Settings.get().animations ? 0.55f + 0.45f * (float)Math.abs(Math.sin((double)System.currentTimeMillis() / 620.0)) : 1.0f;
        int dotColor = on > 0L ? Gui.alpha((int)-1, (float)pulse) : -9143937;
        Gui.roundedRect((class_332)ctx, (int)(cx + 12), (int)(cy + 27), (int)5, (int)5, (int)2, (int)dotColor);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)(on + " Enabled")), (int)(cx + 22), (int)(cy + 26), (int)(on > 0L ? -3419946 : -6578010));
    }

    private int[] catRect(int i) {
        int x = this.panelX + 16;
        int w = 152;
        int y = this.contentY + 36 + i * 52;
        return new int[]{x, y, w, 42};
    }

    private ClickGuiScreen$RowRects rowRects(Module m, int rx, int ry, int rw, int rh) {
        ClickGuiScreen$RowRects r = new ClickGuiScreen$RowRects();
        int right = rx + rw - 16;
        r.toggle = new int[]{right - 42, ry + (rh - 22) / 2, 42, 22};
        right = r.toggle[0] - 12;
        r.key = new int[]{right - 24, ry + (rh - 24) / 2, 24, 24};
        right = r.key[0] - 8;
        if (m.hasEditor()) {
            r.edit = new int[]{right - 58, ry + (rh - 24) / 2, 58, 24};
        }
        return r;
    }

    private String keyLabel(Module m) {
        if (this.listening == m) {
            return "...";
        }
        return m.getKey() > 0 ? Gui.keyName((int)m.getKey()) : "+";
    }

    private int[] rowRect(int i, int top) {
        int rh = this.rowH();
        return new int[]{this.vpX, top + i * (rh + 8), this.contentW(), rh};
    }

    private void renderModules(class_332 ctx, int mouseX, int mouseY, int top) {
        List<Module> mods = this.visibleModules();
        Settings st = Settings.get();
        int accent = Gui.accent() & 0xFFFFFF;
        int rh = this.rowH();
        if (mods.isEmpty()) {
            boolean searching = !this.search.trim().isEmpty();
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)(searching ? "No modules match that." : "Nothing in this category.")), (int)this.vpX, (int)(this.vpY + 12), (int)-3419946);
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)(searching ? "Clear the search to browse by category." : "Register one in ModuleManager to fill it.")), (int)this.vpX, (int)(this.vpY + 26), (int)-6578010);
            return;
        }
        for (int i = 0; i < mods.size(); ++i) {
            Module m = mods.get(i);
            String id = m.getName();
            int[] rr = this.rowRect(i, top);
            int rx = rr[0];
            int ry = rr[1];
            int rw = rr[2];
            if (ry + rh < this.vpY || ry > this.vpY + this.vpH) continue;
            boolean maint = m.isUnderMaintenance();
            boolean on = m.isEnabled();
            boolean rowHover = !maint && this.inVp(mouseX, mouseY) && Gui.inside((double)mouseX, (double)mouseY, (int)rx, (int)ry, (int)rw, (int)rh);
            float h = Anim.bool((String)("row_" + id), (boolean)rowHover, (float)15.0f);
            float e = Anim.bool((String)("on_" + id), (boolean)on, (float)15.0f);
            float pr = this.press("row_" + id);
            float p = this.rowIntro(i);
            int slide = Math.round((1.0f - p) * 22.0f);
            Gui.pushScale((class_332)ctx, (int)(rx += slide), (int)(ry -= Math.round(h * 2.0f)), (int)rw, (int)rh, (float)(1.0f - pr * 0.012f));
            float lift = Math.max(h * 0.55f, e);
            if (lift > 0.35f) {
                Gui.bloom((class_332)ctx, (int)rx, (int)ry, (int)rw, (int)rh, (int)18, (int)5, (float)(lift * p));
            }
            Gui.roundedRect((class_332)ctx, (int)rx, (int)ry, (int)rw, (int)rh, (int)18, (int)Gui.alpha((int)Gui.lerp((int)Gui.lerp((int)0xFFFFFFF, (int)0x1AFFFFFF, (float)h), (int)0x66FFFFFF, (float)e), (float)p));
            Gui.roundedGradient((class_332)ctx, (int)(rx + 1), (int)(ry + 1), (int)(rw - 2), (int)(rh - 2), (int)17, (int)Gui.alpha((int)Gui.lerp((int)Gui.lerp((int)-15527149, (int)-15066598, (float)h), (int)-15066598, (float)e), (float)p), (int)Gui.alpha((int)Gui.lerp((int)-16053493, (int)-15527149, (float)e), (float)p));
            Gui.topHighlight((class_332)ctx, (int)(rx + 1), (int)(ry + 1), (int)(rw - 2), (int)18, (int)Math.round(Gui.lerp01((float)14.0f, (float)40.0f, (float)lift) * p));
            ClickGuiScreen$RowRects r = this.rowRects(m, rx, ry, rw, rh);
            int textRight = maint ? rx + rw - 110 : (r.edit != null ? r.edit[0] : r.key[0]);
            int plateX = rx + 14;
            int plateY = ry + (rh - 36) / 2;
            Gui.pushScale((class_332)ctx, (int)plateX, (int)plateY, (int)36, (int)36, (float)(1.0f + lift * 0.05f));
            Gui.iconPlate((class_332)ctx, (int)plateX, (int)plateY, (int)36, (float)(maint ? 0.0f : lift * p));
            int iconCol = Gui.alpha((int)(maint ? -9143937 : Gui.lerp((int)Gui.lerp((int)-6578010, (int)-3419946, (float)h), (int)-1, (float)e)), (float)p);
            Icons.module((class_332)ctx, (String)m.getName(), (int)(plateX + 10), (int)(plateY + 10), (int)iconCol, (int)-15527149);
            Gui.pop((class_332)ctx);
            int textX = plateX + 36 + 14;
            int textMaxW = Math.max(40, textRight - textX - 12);
            int nameColor = Gui.alpha((int)(maint ? -6578010 : Gui.lerp((int)Gui.lerp((int)-3419946, (int)-1, (float)h), (int)-1, (float)e)), (float)p);
            if (st.showDescriptions) {
                int nameY = ry + (rh - 27) / 2;
                Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)this.trimBold(m.getName(), textMaxW)), (int)textX, (int)nameY, (int)nameColor);
                Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)this.trim(m.getDescription(), textMaxW)), (int)textX, (int)(nameY + 18), (int)Gui.alpha((int)(maint ? -9143937 : -4933185), (float)p));
            } else {
                Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)this.trimBold(m.getName(), textMaxW)), (int)textX, (int)(ry + (rh - 9) / 2), (int)nameColor);
            }
            if (maint) {
                class_2561 tag = Gui.font((String)"Maintenance");
                int tw = this.field_22793.method_27525((class_5348)tag) + 18;
                int tx = rx + rw - 13 - tw;
                Gui.roundedRect((class_332)ctx, (int)tx, (int)(ry + (rh - 17) / 2), (int)tw, (int)17, (int)8, (int)0x1EFFFFFF);
                Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)tag, (int)(tx + tw / 2), (int)(ry + (rh - 9) / 2), (int)-6578010);
                Gui.pop((class_332)ctx);
                continue;
            }
            if (r.edit != null) {
                boolean eh = this.inVp(mouseX, mouseY) && Gui.inside((double)mouseX, (double)mouseY, (int)r.edit[0], (int)r.edit[1], (int)r.edit[2], (int)r.edit[3]);
                float ea = Anim.bool((String)("edit_" + id), (boolean)eh, (float)16.0f);
                float ep = this.press("edit_" + id);
                int er = r.edit[3] / 2;
                Gui.pushScale((class_332)ctx, (int)r.edit[0], (int)r.edit[1], (int)r.edit[2], (int)r.edit[3], (float)(1.0f + ea * 0.04f - ep * 0.08f));
                if (ea > 0.5f) {
                    Gui.bloom((class_332)ctx, (int)r.edit[0], (int)r.edit[1], (int)r.edit[2], (int)r.edit[3], (int)er, (int)4, (float)(ea * 0.7f));
                }
                Gui.roundedRect((class_332)ctx, (int)r.edit[0], (int)r.edit[1], (int)r.edit[2], (int)r.edit[3], (int)er, (int)Gui.lerp((int)0xFFFFFFF, (int)0x4DFFFFFF, (float)ea));
                Gui.roundedGradient((class_332)ctx, (int)(r.edit[0] + 1), (int)(r.edit[1] + 1), (int)(r.edit[2] - 2), (int)(r.edit[3] - 2), (int)(er - 1), (int)Gui.lerp((int)-15527149, (int)-15066598, (float)ea), (int)Gui.lerp((int)-16053493, (int)-15527149, (float)ea));
                Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)"Edit"), (int)(r.edit[0] + r.edit[2] / 2), (int)(r.edit[1] + (r.edit[3] - 9) / 2), (int)Gui.lerp((int)-3419946, (int)-1, (float)ea));
                Gui.pop((class_332)ctx);
            }
            boolean kh = this.inVp(mouseX, mouseY) && Gui.inside((double)mouseX, (double)mouseY, (int)r.key[0], (int)r.key[1], (int)r.key[2], (int)r.key[3]);
            boolean bound = m.getKey() > 0;
            boolean waiting = this.listening == m;
            float ka = Anim.bool((String)("key_" + id), (kh || waiting ? 1 : 0) != 0, (float)16.0f);
            float kp = this.press("key_" + id);
            int kr = r.key[3] / 2;
            Gui.pushScale((class_332)ctx, (int)r.key[0], (int)r.key[1], (int)r.key[2], (int)r.key[3], (float)(1.0f + ka * 0.06f - kp * 0.1f));
            if (ka > 0.5f) {
                Gui.bloom((class_332)ctx, (int)r.key[0], (int)r.key[1], (int)r.key[2], (int)r.key[3], (int)kr, (int)4, (float)(ka * 0.7f));
            }
            Gui.roundedRect((class_332)ctx, (int)r.key[0], (int)r.key[1], (int)r.key[2], (int)r.key[3], (int)kr, (int)(waiting ? -1929379841 : Gui.lerp((int)0xFFFFFFF, (int)0x4DFFFFFF, (float)ka)));
            Gui.roundedGradient((class_332)ctx, (int)(r.key[0] + 1), (int)(r.key[1] + 1), (int)(r.key[2] - 2), (int)(r.key[3] - 2), (int)(kr - 1), (int)Gui.lerp((int)-15527149, (int)-15066598, (float)ka), (int)Gui.lerp((int)-16053493, (int)-15527149, (float)ka));
            String label = this.keyLabel(m);
            if (this.field_22793.method_27525((class_5348)Gui.font((String)label)) > r.key[2] - 6) {
                label = this.trim(label, r.key[2] - 8);
            }
            Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)label), (int)(r.key[0] + r.key[2] / 2), (int)(r.key[1] + (r.key[3] - 9) / 2), (int)(waiting ? -1 : (bound ? Gui.lerp((int)-3419946, (int)-1, (float)ka) : Gui.lerp((int)-6578010, (int)-1, (float)ka))));
            Gui.pop((class_332)ctx);
            float knob = Anim.spring((String)("knob_" + id), (float)(on ? 1.0f : 0.0f), (float)210.0f, (float)16.0f);
            Gui.toggleSwitch((class_332)ctx, (int)r.toggle[0], (int)r.toggle[1], (int)42, (int)22, (float)knob, (float)h);
            Gui.pop((class_332)ctx);
        }
    }

    private void renderConfigBand(class_332 ctx, int mouseX, int mouseY) {
        int accent = Gui.accent() & 0xFFFFFF;
        int[] nb = this.nameBoxRect();
        int rad = nb[3] / 2;
        Gui.roundedRect((class_332)ctx, (int)nb[0], (int)nb[1], (int)nb[2], (int)nb[3], (int)rad, (int)(this.nameFocused ? 0x5CFFFFFF : 0x1AFFFFFF));
        Gui.roundedRect((class_332)ctx, (int)(nb[0] + 1), (int)(nb[1] + 1), (int)(nb[2] - 2), (int)(nb[3] - 2), (int)(rad - 1), (int)-16250872);
        int tx = nb[0] + 12;
        int tcy = nb[1] + (nb[3] - 9) / 2;
        if (this.configName.isEmpty() && !this.nameFocused) {
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"New config name..."), (int)tx, (int)tcy, (int)-9143937);
        } else {
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)this.configName), (int)tx, (int)tcy, (int)-1);
            if (this.nameFocused && System.currentTimeMillis() / 500L % 2L == 0L) {
                int cx = tx + this.field_22793.method_27525((class_5348)Gui.font((String)this.configName));
                ctx.method_25294(cx + 1, tcy - 1, cx + 2, tcy + 9, 0xFF000000 | accent);
            }
        }
        int[] sb = this.saveBtnRect();
        boolean hover = Gui.inside((double)mouseX, (double)mouseY, (int)sb[0], (int)sb[1], (int)sb[2], (int)sb[3]);
        float a = Anim.bool((String)"cfg_save", (boolean)hover, (float)16.0f);
        Gui.roundedRect((class_332)ctx, (int)sb[0], (int)sb[1], (int)sb[2], (int)sb[3], (int)(sb[3] / 2), (int)Gui.lerp((int)0x30FFFFFF, (int)0x6BFFFFFF, (float)a));
        Gui.roundedRect((class_332)ctx, (int)(sb[0] + 1), (int)(sb[1] + 1), (int)(sb[2] - 2), (int)(sb[3] - 2), (int)(sb[3] / 2 - 1), (int)Gui.lerp((int)-15527149, (int)-15066598, (float)a));
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)"Save"), (int)(sb[0] + sb[2] / 2), (int)(sb[1] + (sb[3] - 9) / 2), (int)Gui.lerp((int)-3419946, (int)-1, (float)a));
    }

    private int[] saveBtnRect() {
        return new int[]{this.vpX + this.vpW - 58, this.contentY + 6, 58, 22};
    }

    private int[] nameBoxRect() {
        return new int[]{this.vpX, this.contentY + 6, this.vpW - 64, 22};
    }

    private void renderConfigs(class_332 ctx, int mouseX, int mouseY, int top) {
        int x = this.vpX;
        int w = this.contentW();
        if (this.configCache.isEmpty()) {
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"No saved configs yet."), (int)x, (int)(this.vpY + 12), (int)-3419946);
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"Name it above and press Save."), (int)x, (int)(this.vpY + 26), (int)-6578010);
            return;
        }
        for (int i = 0; i < this.configCache.size(); ++i) {
            String name = this.configCache.get(i);
            int cy = top + i * 37;
            if (cy + 32 < this.vpY || cy > this.vpY + this.vpH) continue;
            boolean isActive = name.equals(Configs.active());
            boolean rowHover = this.inVp(mouseX, mouseY) && Gui.inside((double)mouseX, (double)mouseY, (int)x, (int)cy, (int)w, (int)32);
            float h = Anim.bool((String)("cfgrow_" + name), (boolean)rowHover, (float)14.0f);
            Gui.roundedRect((class_332)ctx, (int)x, (int)cy, (int)w, (int)32, (int)11, (int)Gui.lerp((int)0x14FFFFFF, (int)0x2CFFFFFF, (float)h));
            Gui.roundedGradient((class_332)ctx, (int)(x + 1), (int)(cy + 1), (int)(w - 2), (int)30, (int)10, (int)-15527149, (int)-15987700);
            int dot = Configs.accentOf((String)name) & 0xFFFFFF;
            if (dot == 0) {
                dot = 9081508;
            }
            Gui.roundedRect((class_332)ctx, (int)(x + 13), (int)(cy + 12), (int)7, (int)7, (int)4, (int)(0xFF000000 | dot));
            String shown = this.trim(name, w - 215);
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)shown), (int)(x + 28), (int)(cy + 11), (int)(isActive ? -1 : -3419946));
            if (isActive) {
                Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"active"), (int)(x + 28 + this.field_22793.method_27525((class_5348)Gui.bold((String)shown)) + 9), (int)(cy + 11), (int)-9143937);
            }
            String[] labels = new String[]{"Delete", "Copy", "Load"};
            for (int slot = 0; slot < 3; ++slot) {
                int[] b = this.cfgBtn(x, cy, w, slot);
                boolean bh = this.inVp(mouseX, mouseY) && Gui.inside((double)mouseX, (double)mouseY, (int)b[0], (int)b[1], (int)b[2], (int)b[3]);
                float ba = Anim.bool((String)("cfgb_" + slot + "_" + name), (boolean)bh, (float)16.0f);
                Gui.roundedRect((class_332)ctx, (int)b[0], (int)b[1], (int)b[2], (int)b[3], (int)(b[3] / 2), (int)Gui.lerp((int)0x1AFFFFFF, (int)0x52FFFFFF, (float)ba));
                Gui.roundedRect((class_332)ctx, (int)(b[0] + 1), (int)(b[1] + 1), (int)(b[2] - 2), (int)(b[3] - 2), (int)(b[3] / 2 - 1), (int)Gui.lerp((int)-15527149, (int)-15066598, (float)ba));
                int col = slot == 0 ? Gui.lerp((int)-6578010, (int)-2565928, (float)ba) : Gui.lerp((int)-3419946, (int)-1, (float)ba);
                Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)(slot == 2 ? Gui.bold((String)labels[slot]) : Gui.font((String)labels[slot])), (int)(b[0] + b[2] / 2), (int)(b[1] + (b[3] - 9) / 2), (int)col);
            }
        }
    }

    private int[] cfgBtn(int x, int cy, int w, int slot) {
        int[] widths = new int[]{58, 50, 52};
        int gx = x + w - 9;
        for (int s = 0; s <= slot; ++s) {
            gx -= widths[s] + (s > 0 ? 5 : 0);
        }
        return new int[]{gx, cy + 6, widths[slot], 19};
    }

    private void doSaveConfig() {
        String name = this.configName.trim();
        if (Configs.sanitize((String)name).isEmpty()) {
            this.setFlash("Enter a name");
            return;
        }
        if (Configs.save((String)name)) {
            this.configName = "";
            this.refreshConfigs();
            this.layout();
            this.setFlash("Saved \"" + Configs.sanitize((String)name) + "\"");
        } else {
            this.setFlash("Couldn't save");
        }
    }

    private void setFlash(String s) {
        this.flash = s;
        this.flashAt = System.currentTimeMillis();
    }

    private List<ClickGuiScreen$SRow> settingsRows() {
        Settings s = Settings.get();
        ArrayList<ClickGuiScreen$SRow> rows = new ArrayList<ClickGuiScreen$SRow>();
        rows.add(this.section("INTERFACE"));
        rows.add(this.sw("animations", "Animations", s.animations));
        float shownScale = "uiscale".equals(this.dragId) && this.pendingScale != null ? this.pendingScale.floatValue() : s.uiScale;
        rows.add(this.sl("uiscale", "UI scale", Math.round(shownScale * 100.0f) + "%", (shownScale - 0.5f) / 1.0f));
        rows.add(this.sl("speed", "Animation speed", s.animSpeed + "%", (float)(s.animSpeed - 50) / 150.0f));
        rows.add(this.sl("opac", "Menu opacity", s.panelOpacity + "%", (float)(s.panelOpacity - 70) / 30.0f));
        rows.add(this.sl("dim", "Background dim", s.bgDim + "%", (float)s.bgDim / 100.0f));
        rows.add(this.sl("glow", "Glow intensity", s.glow + "%", (float)s.glow / 150.0f));
        rows.add(this.section("APPEARANCE"));
        rows.add(this.hue("hue", "Accent colour"));
        rows.add(this.swatches());
        rows.add(this.sw("rainbow", "Rainbow accent", s.rainbow));
        rows.add(this.sw("blur", "Background blur", s.menuBlur));
        rows.add(this.section("LAYOUT"));
        rows.add(this.sw("compact", "Compact mode", s.compactRows));
        rows.add(this.sw("descs", "Show module descriptions", s.showDescriptions));
        rows.add(this.btn("resetpos", "Reset UI position", false));
        rows.add(this.section("MISC"));
        rows.add(this.sw("sound", "Click sound", s.menuSound));
        rows.add(this.sw("watermark", "Watermark", s.watermark));
        rows.add(this.btn("save", "Save settings", false));
        rows.add(this.btn("reset", "Reset settings to defaults", true));
        int y = 2;
        for (ClickGuiScreen$SRow r : rows) {
            r.y = y;
            y += r.h + 8;
        }
        return rows;
    }

    private int settingsTotalH() {
        List<ClickGuiScreen$SRow> rows = this.settingsRows();
        ClickGuiScreen$SRow last = rows.get(rows.size() - 1);
        return last.y + last.h + 4;
    }

    private ClickGuiScreen$SRow section(String label) {
        ClickGuiScreen$SRow r = new ClickGuiScreen$SRow();
        r.type = 0;
        r.label = label;
        r.h = 26;
        return r;
    }

    private ClickGuiScreen$SRow sw(String id, String label, boolean on) {
        ClickGuiScreen$SRow r = new ClickGuiScreen$SRow();
        r.type = 1;
        r.id = id;
        r.label = label;
        r.on = on;
        r.h = 40;
        return r;
    }

    private ClickGuiScreen$SRow sl(String id, String label, String value, float frac) {
        ClickGuiScreen$SRow r = new ClickGuiScreen$SRow();
        r.type = 2;
        r.id = id;
        r.label = label;
        r.value = value;
        r.frac = ClickGuiScreen.clamp(frac, 0.0f, 1.0f);
        r.h = 48;
        return r;
    }

    private ClickGuiScreen$SRow btn(String id, String label, boolean danger) {
        ClickGuiScreen$SRow r = new ClickGuiScreen$SRow();
        r.type = 3;
        r.id = id;
        r.label = label;
        r.danger = danger;
        r.h = 40;
        return r;
    }

    private ClickGuiScreen$SRow hue(String id, String label) {
        ClickGuiScreen$SRow r = new ClickGuiScreen$SRow();
        r.type = 4;
        r.id = id;
        r.label = label;
        r.h = 48;
        return r;
    }

    private ClickGuiScreen$SRow swatches() {
        ClickGuiScreen$SRow r = new ClickGuiScreen$SRow();
        r.type = 5;
        r.id = "swatch";
        r.h = 30;
        return r;
    }

    private void renderSettings(class_332 ctx, int mouseX, int mouseY, int top) {
        int x = this.vpX;
        int w = this.contentW();
        for (ClickGuiScreen$SRow r : this.settingsRows()) {
            int y = top + r.y;
            if (y + r.h < this.vpY || y > this.vpY + this.vpH) continue;
            switch (r.type) {
                case 0: {
                    Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)r.label), (int)(x + 2), (int)(y + 8), (int)-6578010);
                    int lx = x + 6 + this.field_22793.method_27525((class_5348)Gui.bold((String)r.label)) + 10;
                    ctx.method_25294(lx, y + 12, x + w - 2, y + 13, 0xFFFFFFF);
                    break;
                }
                case 1: {
                    this.renderSwitchRow(ctx, mouseX, mouseY, x, y, w, r);
                    break;
                }
                case 2: {
                    this.renderSliderRow(ctx, mouseX, mouseY, x, y, w, r);
                    break;
                }
                case 3: {
                    this.renderButtonRow(ctx, mouseX, mouseY, x, y, w, r);
                    break;
                }
                case 4: {
                    this.renderHueRow(ctx, mouseX, mouseY, x, y, w, r);
                    break;
                }
                case 5: {
                    this.renderSwatchRow(ctx, mouseX, mouseY, x, y, w);
                    break;
                }
            }
        }
    }

    private void renderSwitchRow(class_332 ctx, int mouseX, int mouseY, int x, int y, int w, ClickGuiScreen$SRow r) {
        boolean hover = this.inVp(mouseX, mouseY) && Gui.inside((double)mouseX, (double)mouseY, (int)x, (int)y, (int)w, (int)r.h);
        float h = Anim.bool((String)("srow_" + r.id), (boolean)hover, (float)14.0f);
        this.rowPlate(ctx, x, y, w, r.h, h);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)r.label), (int)(x + 16), (int)(y + (r.h - 9) / 2), (int)Gui.lerp((int)-3419946, (int)-1, (float)h));
        Gui.toggleSwitch((class_332)ctx, (int)(x + w - 16 - 42), (int)(y + (r.h - 22) / 2), (int)42, (int)22, (float)Anim.spring((String)("sw_" + r.id), (float)(r.on ? 1.0f : 0.0f), (float)210.0f, (float)16.0f), (float)h);
    }

    private int[] trackRect(int x, int y, int w, int rowH) {
        return new int[]{x + 16, y + rowH - 22, w - 32, 14};
    }

    private void renderSliderRow(class_332 ctx, int mouseX, int mouseY, int x, int y, int w, ClickGuiScreen$SRow r) {
        int[] t = this.trackRect(x, y, w, r.h);
        boolean hover = this.inVp(mouseX, mouseY) && Gui.inside((double)mouseX, (double)mouseY, (int)x, (int)y, (int)w, (int)r.h) || r.id.equals(this.dragId);
        float h = Anim.bool((String)("srow_" + r.id), (boolean)hover, (float)14.0f);
        this.rowPlate(ctx, x, y, w, r.h, h);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)r.label), (int)(x + 16), (int)(y + 11), (int)Gui.lerp((int)-3419946, (int)-1, (float)h));
        class_2561 v = Gui.font((String)r.value);
        int vw = this.field_22793.method_27525((class_5348)v) + 14;
        int vx = x + w - 16 - vw;
        Gui.roundedRect((class_332)ctx, (int)vx, (int)(y + 7), (int)vw, (int)16, (int)8, (int)Gui.lerp((int)0xFFFFFFF, (int)0x1AFFFFFF, (float)h));
        Gui.roundedRect((class_332)ctx, (int)(vx + 1), (int)(y + 8), (int)(vw - 2), (int)14, (int)7, (int)-16053493);
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)v, (int)(vx + vw / 2), (int)(y + 11), (int)-1);
        Gui.slider((class_332)ctx, (int)t[0], (int)(t[1] + 4), (int)t[2], (int)6, (float)r.frac, (float)Anim.bool((String)("sl_" + r.id), (boolean)hover, (float)16.0f));
    }

    private void renderButtonRow(class_332 ctx, int mouseX, int mouseY, int x, int y, int w, ClickGuiScreen$SRow r) {
        boolean hover = this.inVp(mouseX, mouseY) && Gui.inside((double)mouseX, (double)mouseY, (int)x, (int)y, (int)w, (int)r.h);
        float a = Anim.bool((String)("btn_" + r.id), (boolean)hover, (float)16.0f);
        float p = this.press("btn_" + r.id);
        int rad = r.h / 2;
        Gui.pushScale((class_332)ctx, (int)x, (int)y, (int)w, (int)r.h, (float)(1.0f - p * 0.015f));
        if (a > 0.4f && !r.danger) {
            Gui.bloom((class_332)ctx, (int)x, (int)y, (int)w, (int)r.h, (int)rad, (int)4, (float)(a * 0.7f));
        }
        Gui.roundedRect((class_332)ctx, (int)x, (int)y, (int)w, (int)r.h, (int)rad, (int)Gui.lerp((int)0xFFFFFFF, (int)0x4DFFFFFF, (float)a));
        Gui.roundedGradient((class_332)ctx, (int)(x + 1), (int)(y + 1), (int)(w - 2), (int)(r.h - 2), (int)(rad - 1), (int)Gui.lerp((int)-15527149, (int)-15066598, (float)a), (int)Gui.lerp((int)-16053493, (int)-15527149, (float)a));
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)r.label), (int)(x + w / 2), (int)(y + (r.h - 9) / 2), (int)(r.danger ? Gui.lerp((int)-6578010, (int)-2565928, (float)a) : Gui.lerp((int)-3419946, (int)-1, (float)a)));
        Gui.pop((class_332)ctx);
    }

    private void renderHueRow(class_332 ctx, int mouseX, int mouseY, int x, int y, int w, ClickGuiScreen$SRow r) {
        Settings s = Settings.get();
        int accent = s.accent & 0xFFFFFF;
        int[] t = this.trackRect(x, y, w, r.h);
        boolean hover = this.inVp(mouseX, mouseY) && Gui.inside((double)mouseX, (double)mouseY, (int)x, (int)y, (int)w, (int)r.h) || "hue".equals(this.dragId);
        float ha = Anim.bool((String)"sl_hue", (boolean)hover, (float)16.0f);
        this.rowPlate(ctx, x, y, w, r.h, ha);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)r.label), (int)(x + 16), (int)(y + 11), (int)Gui.lerp((int)-3419946, (int)-1, (float)ha));
        Gui.roundedRect((class_332)ctx, (int)(x + w - 44), (int)(y + 7), (int)28, (int)16, (int)8, (int)Gui.lerp((int)0xFFFFFFF, (int)0x1AFFFFFF, (float)ha));
        Gui.roundedRect((class_332)ctx, (int)(x + w - 43), (int)(y + 8), (int)26, (int)14, (int)7, (int)(0xFF000000 | Gui.accent() & 0xFFFFFF));
        int rx = t[0];
        int rw = t[2];
        int ry = t[1] + 4;
        int rh = 6;
        Gui.roundedRect((class_332)ctx, (int)(rx - 1), (int)(ry - 1), (int)(rw + 2), (int)(rh + 2), (int)(rh / 2 + 1), (int)Gui.lerp((int)0x2EFFFFFF, (int)0x66FFFFFF, (float)ha));
        for (int i = 0; i < rw; ++i) {
            int col = 0xFF000000 | Gui.hsbToRgb((float)((float)i / (float)rw), (float)0.42f, (float)0.95f);
            int inset = i < 2 ? 1 : (i > rw - 3 ? 1 : 0);
            ctx.method_25294(rx + i, ry + inset, rx + i + 1, ry + rh - inset, col);
        }
        float hue = s.rainbow ? (float)(System.currentTimeMillis() % 6000L) / 6000.0f : Gui.hueOf((int)accent);
        int kd = rh + 6 + Math.round(ha * 3.0f);
        int kx = rx + Math.round((float)(rw - kd) * ClickGuiScreen.clamp(hue, 0.0f, 1.0f));
        int ky = ry + rh / 2 - kd / 2;
        Gui.roundedRect((class_332)ctx, (int)kx, (int)(ky + 1), (int)kd, (int)kd, (int)(kd / 2), (int)0x66000000);
        Gui.roundedGradient((class_332)ctx, (int)kx, (int)ky, (int)kd, (int)kd, (int)(kd / 2), (int)-1, (int)-2235412);
    }

    private void renderSwatchRow(class_332 ctx, int mouseX, int mouseY, int x, int y, int w) {
        int current = Settings.get().accent & 0xFFFFFF;
        for (int i = 0; i < PRESETS.length; ++i) {
            int[] p = this.swatchRect(x, y, w, i);
            boolean hover = this.inVp(mouseX, mouseY) && Gui.inside((double)mouseX, (double)mouseY, (int)p[0], (int)p[1], (int)p[2], (int)p[3]);
            boolean sel = PRESETS[i] == current;
            float a = Anim.bool((String)("sws_" + i), (hover || sel ? 1 : 0) != 0, (float)16.0f);
            Gui.roundedRect((class_332)ctx, (int)(p[0] - 1), (int)(p[1] - 1), (int)(p[2] + 2), (int)(p[3] + 2), (int)((p[3] + 2) / 2), (int)Gui.lerp((int)0x22FFFFFF, (int)(sel ? -1 : 0x77FFFFFF), (float)a));
            Gui.roundedRect((class_332)ctx, (int)p[0], (int)p[1], (int)p[2], (int)p[3], (int)(p[3] / 2), (int)(0xFF000000 | PRESETS[i]));
        }
    }

    private int[] swatchRect(int x, int y, int w, int i) {
        int d = 16;
        int gap = 8;
        return new int[]{x + 2 + i * (d + gap), y + 3, d, d};
    }

    private void rowPlate(class_332 ctx, int x, int y, int w, int h, float hover) {
        Gui.roundedRect((class_332)ctx, (int)x, (int)y, (int)w, (int)h, (int)18, (int)Gui.lerp((int)0xFFFFFFF, (int)0x1AFFFFFF, (float)hover));
        Gui.roundedGradient((class_332)ctx, (int)(x + 1), (int)(y + 1), (int)(w - 2), (int)(h - 2), (int)17, (int)Gui.lerp((int)-15527149, (int)-15066598, (float)hover), (int)Gui.lerp((int)-16053493, (int)-15527149, (float)hover));
        Gui.topHighlight((class_332)ctx, (int)(x + 1), (int)(y + 1), (int)(w - 2), (int)18, (int)Math.round(Gui.lerp01((float)14.0f, (float)34.0f, (float)hover)));
    }

    private int contentW() {
        return this.vpW - (this.maxScroll > 0 ? 11 : 0);
    }

    private void renderScrollbar(class_332 ctx, int mouseX, int mouseY) {
        if (this.maxScroll <= 0) {
            return;
        }
        int baseX = this.vpX + this.vpW - 5;
        int thumbH = this.thumbHeight();
        int thumbY = this.vpY + Math.round(this.scrollShown / (float)this.maxScroll * (float)(this.vpH - thumbH));
        boolean hover = Gui.inside((double)mouseX, (double)mouseY, (int)(baseX - 4), (int)thumbY, (int)12, (int)thumbH) || this.draggingBar;
        float h = Anim.bool((String)"bar", (boolean)hover, (float)16.0f);
        float recent = 1.0f - Anim.since((long)(this.scrollAt + 900L), (float)320.0f);
        float vis = Anim.to((String)"barvis", (float)Math.max(recent, hover ? 1.0f : 0.0f), (float)12.0f);
        if (vis <= 0.01f) {
            return;
        }
        int trackW = Math.round(Gui.lerp01((float)3.0f, (float)5.0f, (float)h));
        int trackX = baseX + (5 - trackW);
        Gui.roundedRect((class_332)ctx, (int)trackX, (int)this.vpY, (int)trackW, (int)this.vpH, (int)(trackW / 2), (int)Gui.alpha((int)0x12FFFFFF, (float)vis));
        Gui.roundedRect((class_332)ctx, (int)trackX, (int)thumbY, (int)trackW, (int)thumbH, (int)(trackW / 2), (int)Gui.alpha((int)Gui.lerp((int)0x59FFFFFF, (int)-1275068417, (float)h), (float)vis));
    }

    private int thumbHeight() {
        return Math.max(24, Math.round((float)this.vpH * (float)this.vpH / (float)this.contentH));
    }

    private void renderFooter(class_332 ctx) {
        int x = this.panelX + 20;
        int w = 664;
        int fy = this.panelY + this.panelH - 32 + 9;
        long on = FakeClient.MODULES.getModules().stream().filter(Module::isEnabled).count();
        float pulse = Settings.get().animations ? 0.5f + 0.5f * (float)Math.abs(Math.sin((double)System.currentTimeMillis() / 620.0)) : 1.0f;
        Gui.roundedRect((class_332)ctx, (int)x, (int)(fy + 2), (int)5, (int)5, (int)2, (int)(on > 0L ? Gui.alpha((int)-1, (float)pulse) : -9143937));
        String cfg = Configs.active();
        ArrayList<Object> items = new ArrayList<Object>();
        items.add(on > 0L ? "Active" : "Idle");
        items.add(on + " Enabled");
        items.add(cfg.isEmpty() ? "No Config Loaded" : cfg);
        if (License.get().isValid()) {
            items.add(License.get().remainingText());
        }
        items.add("Version 1.0.0");
        int ix = x + 12;
        for (int i = 0; i < items.size(); ++i) {
            if (i > 0) {
                ctx.method_25294(ix, fy + 1, ix + 1, fy + 8, 0x24FFFFFF);
                ix += 13;
            }
            class_2561 t = i == 0 ? Gui.bold((String)((String)items.get(i))) : Gui.font((String)((String)items.get(i)));
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)t, (int)ix, (int)fy, (int)(i == 0 && on > 0L ? -3419946 : -6578010));
            ix += this.field_22793.method_27525((class_5348)t) + 12;
        }
        boolean showFlash = !this.flash.isEmpty() && System.currentTimeMillis() - this.flashAt < 1600L;
        class_2561 hint = showFlash ? Gui.bold((String)this.flash) : Gui.font((String)("Hold " + Gui.keyName((int)FakeClient.menuKeyCode()) + " to close   \u00b7   drag to move"));
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)hint, (int)(x + w - this.field_22793.method_27525((class_5348)hint)), (int)fy, (int)(showFlash ? -3419946 : -9143937));
    }

    private boolean inVp(double mx, double my) {
        return my >= (double)this.vpY && my < (double)(this.vpY + this.vpH) && mx >= (double)this.vpX && mx < (double)(this.vpX + this.vpW);
    }

    public boolean method_25402(class_11909 click, boolean doubled) {
        int thumbH;
        int thumbY;
        int trackX;
        double mx = this.ux(click.comp_4798());
        double my = this.uy(click.comp_4799());
        int button = click.method_74245();
        if (button == 0 && this.maxScroll > 0 && Gui.inside((double)mx, (double)my, (int)((trackX = this.vpX + this.vpW - 4) - 4), (int)(thumbY = this.vpY + Math.round(this.scrollShown / (float)this.maxScroll * (float)(this.vpH - (thumbH = this.thumbHeight())))), (int)11, (int)thumbH)) {
            this.draggingBar = true;
            this.dragBarOffset = my - (double)thumbY;
            return true;
        }
        if (button == 1 && this.view == 0 && this.inVp(mx, my)) {
            int top = this.vpY - Math.round(this.scrollShown);
            List<Module> mods = this.visibleModules();
            for (int i = 0; i < mods.size(); ++i) {
                int[] r = this.rowRect(i, top);
                if (!Gui.inside((double)mx, (double)my, (int)r[0], (int)r[1], (int)r[2], (int)r[3]) || mods.get(i).isUnderMaintenance()) continue;
                this.listening = this.listening == mods.get(i) ? null : mods.get(i);
                return true;
            }
        }
        if (button != 0) {
            return super.method_25402(click, doubled);
        }
        int[] sr = this.searchRect();
        boolean hitSearch = Gui.inside((double)mx, (double)my, (int)sr[0], (int)sr[1], (int)sr[2], (int)sr[3]);
        if (!hitSearch) {
            this.searchFocused = false;
        }
        int[] nb = this.nameBoxRect();
        if (this.view == 2 && !Gui.inside((double)mx, (double)my, (int)nb[0], (int)nb[1], (int)nb[2], (int)nb[3])) {
            this.nameFocused = false;
        }
        if (hitSearch) {
            this.searchFocused = true;
            return true;
        }
        for (int i = 0; i < 2; ++i) {
            int[] r = this.iconBtnRect(i);
            if (!Gui.inside((double)mx, (double)my, (int)r[0], (int)r[1], (int)r[2], (int)r[3])) continue;
            this.hold("icon_" + i);
            int target = i == 0 ? 1 : 2;
            this.switchView(this.view == target ? 0 : target);
            Sfx.click();
            return true;
        }
        if (this.inDragStrip(mx, my)) {
            this.draggingPanel = true;
            this.dragPanelX = mx - (double)this.panelX;
            this.dragPanelY = my - (double)this.panelY;
            return true;
        }
        List<String> cats = this.sidebarEntries();
        for (int i = 0; i < cats.size(); ++i) {
            int[] r = this.catRect(i);
            if (!Gui.inside((double)mx, (double)my, (int)r[0], (int)r[1], (int)r[2], (int)r[3])) continue;
            this.hold("cat_" + cats.get(i));
            this.viewDir = i >= this.category ? 1 : -1;
            this.viewEpoch = System.currentTimeMillis();
            this.category = i;
            this.search = "";
            if (this.view != 0) {
                this.switchView(0);
            } else {
                this.listening = null;
                this.restartList();
                this.layout();
            }
            Sfx.click();
            return true;
        }
        if (this.view == 2) {
            int[] sb = this.saveBtnRect();
            if (Gui.inside((double)mx, (double)my, (int)sb[0], (int)sb[1], (int)sb[2], (int)sb[3])) {
                this.doSaveConfig();
                return true;
            }
            if (Gui.inside((double)mx, (double)my, (int)nb[0], (int)nb[1], (int)nb[2], (int)nb[3])) {
                this.nameFocused = true;
                return true;
            }
        }
        if (this.inVp(mx, my)) {
            if (this.view == 0 && this.moduleClick(mx, my)) {
                return true;
            }
            if (this.view == 1 && this.settingsClick(mx, my)) {
                return true;
            }
            if (this.view == 2 && this.configsClick(mx, my)) {
                return true;
            }
        }
        if (!Gui.inside((double)mx, (double)my, (int)this.panelX, (int)this.panelY, (int)704, (int)this.panelH)) {
            this.method_25419();
            return true;
        }
        return super.method_25402(click, doubled);
    }

    private void switchView(int target) {
        this.viewDir = target >= this.view ? 1 : -1;
        this.viewEpoch = System.currentTimeMillis();
        this.view = target;
        this.listening = null;
        this.nameFocused = false;
        this.searchFocused = false;
        this.restartList();
        if (this.view == 2) {
            this.refreshConfigs();
        }
        this.layout();
    }

    private boolean moduleClick(double mx, double my) {
        int top = this.vpY - Math.round(this.scrollShown);
        List<Module> mods = this.visibleModules();
        int rh = this.rowH();
        for (int i = 0; i < mods.size(); ++i) {
            Module m = mods.get(i);
            int[] rr = this.rowRect(i, top);
            if (!Gui.inside((double)mx, (double)my, (int)rr[0], (int)rr[1], (int)rr[2], (int)rr[3])) continue;
            if (m.isUnderMaintenance()) {
                this.setFlash(m.getName() + " is under maintenance");
                return true;
            }
            ClickGuiScreen$RowRects r = this.rowRects(m, rr[0], rr[1], rr[2], rh);
            if (Gui.inside((double)mx, (double)my, (int)r.key[0], (int)r.key[1], (int)r.key[2], (int)r.key[3])) {
                this.listening = this.listening == m ? null : m;
                return true;
            }
            if (r.edit != null && Gui.inside((double)mx, (double)my, (int)r.edit[0], (int)r.edit[1], (int)r.edit[2], (int)r.edit[3])) {
                this.hold("edit_" + m.getName());
                class_437 editor = m.createEditor();
                if (editor != null) {
                    this.field_22787.method_1507(editor);
                    return true;
                }
            }
            this.hold("row_" + m.getName());
            m.toggle();
            Sfx.click();
            FakeClient.MODULES.saveState();
            return true;
        }
        return false;
    }

    private boolean configsClick(double mx, double my) {
        int top = this.vpY - Math.round(this.scrollShown);
        int x = this.vpX;
        int w = this.contentW();
        for (int i = 0; i < this.configCache.size(); ++i) {
            String name = this.configCache.get(i);
            int cy = top + i * 37;
            int[] del = this.cfgBtn(x, cy, w, 0);
            int[] copy = this.cfgBtn(x, cy, w, 1);
            int[] load = this.cfgBtn(x, cy, w, 2);
            if (Gui.inside((double)mx, (double)my, (int)load[0], (int)load[1], (int)load[2], (int)load[3])) {
                if (Configs.load((String)name)) {
                    this.setFlash("Loaded \"" + name + "\"");
                }
                return true;
            }
            if (Gui.inside((double)mx, (double)my, (int)copy[0], (int)copy[1], (int)copy[2], (int)copy[3])) {
                if (Configs.duplicate((String)name)) {
                    this.refreshConfigs();
                    this.layout();
                    this.setFlash("Copied \"" + name + "\"");
                }
                return true;
            }
            if (!Gui.inside((double)mx, (double)my, (int)del[0], (int)del[1], (int)del[2], (int)del[3])) continue;
            Configs.delete((String)name);
            this.refreshConfigs();
            this.layout();
            this.setFlash("Deleted \"" + name + "\"");
            return true;
        }
        return false;
    }

    private boolean settingsClick(double mx, double my) {
        Settings s = Settings.get();
        int top = this.vpY - Math.round(this.scrollShown);
        int x = this.vpX;
        int w = this.contentW();
        for (ClickGuiScreen$SRow r : this.settingsRows()) {
            int y = top + r.y;
            switch (r.type) {
                case 1: {
                    if (!Gui.inside((double)mx, (double)my, (int)x, (int)y, (int)w, (int)r.h)) break;
                    this.toggleSetting(s, r.id);
                    s.save();
                    this.layout();
                    return true;
                }
                case 2: 
                case 4: {
                    int[] t = this.trackRect(x, y, w, r.h);
                    if (!Gui.inside((double)mx, (double)my, (int)(t[0] - 4), (int)(t[1] - 4), (int)(t[2] + 8), (int)(t[3] + 8))) break;
                    this.dragId = r.type == 4 ? "hue" : r.id;
                    this.dragTrackX = t[0];
                    this.dragTrackW = t[2];
                    this.updateSlider(mx);
                    return true;
                }
                case 5: {
                    for (int i = 0; i < PRESETS.length; ++i) {
                        int[] p = this.swatchRect(x, y, w, i);
                        if (!Gui.inside((double)mx, (double)my, (int)(p[0] - 2), (int)(p[1] - 2), (int)(p[2] + 4), (int)(p[3] + 4))) continue;
                        s.accent = 0xFF000000 | PRESETS[i];
                        s.rainbow = false;
                        s.save();
                        Sfx.click();
                        return true;
                    }
                    break;
                }
                case 3: {
                    if (!Gui.inside((double)mx, (double)my, (int)x, (int)y, (int)w, (int)r.h)) break;
                    this.hold("btn_" + r.id);
                    switch (r.id) {
                        case "resetpos": {
                            s.panelOffX = 0;
                            s.panelOffY = 0;
                            s.save();
                            this.layout();
                            this.setFlash("Position reset");
                            break;
                        }
                        case "save": {
                            s.save();
                            FakeClient.MODULES.saveState();
                            this.setFlash("Settings saved");
                            break;
                        }
                        case "reset": {
                            s.resetToDefaults();
                            this.layout();
                            this.setFlash("Settings reset");
                            break;
                        }
                    }
                    Sfx.click();
                    return true;
                }
            }
        }
        return false;
    }

    private void toggleSetting(Settings s, String id) {
        switch (id) {
            case "animations": {
                s.animations = !s.animations;
                break;
            }
            case "rainbow": {
                s.rainbow = !s.rainbow;
                break;
            }
            case "blur": {
                s.menuBlur = !s.menuBlur;
                break;
            }
            case "compact": {
                s.compactRows = !s.compactRows;
                break;
            }
            case "descs": {
                s.showDescriptions = !s.showDescriptions;
                break;
            }
            case "watermark": {
                s.watermark = !s.watermark;
                break;
            }
            case "sound": {
                s.menuSound = !s.menuSound;
                Sfx.click();
                break;
            }
        }
    }

    private void updateSlider(double mx) {
        if (this.dragId == null) {
            return;
        }
        Settings s = Settings.get();
        float t = ClickGuiScreen.clamp((float)((mx - (double)this.dragTrackX) / (double)Math.max(1, this.dragTrackW)), 0.0f, 1.0f);
        switch (this.dragId) {
            case "uiscale": {
                this.pendingScale = Float.valueOf(0.5f + (float)Math.round(t * 1.0f * 20.0f) / 20.0f);
                break;
            }
            case "dim": {
                s.bgDim = Math.round(t * 100.0f);
                break;
            }
            case "speed": {
                s.animSpeed = 50 + Math.round(t * 150.0f);
                break;
            }
            case "glow": {
                s.glow = Math.round(t * 150.0f);
                break;
            }
            case "opac": {
                s.panelOpacity = 70 + Math.round(t * 30.0f);
                break;
            }
            case "hue": {
                s.accent = 0xFF000000 | Gui.hsbToRgb((float)t, (float)0.42f, (float)0.95f);
                s.rainbow = false;
                break;
            }
        }
    }

    public boolean method_25403(class_11909 click, double offsetX, double offsetY) {
        if (this.draggingPanel) {
            Settings s = Settings.get();
            double mx = this.ux(click.comp_4798());
            double my = this.uy(click.comp_4799());
            s.panelOffX += (int)Math.round(mx - this.dragPanelX - (double)this.panelX);
            s.panelOffY += (int)Math.round(my - this.dragPanelY - (double)this.panelY);
            this.layout();
            return true;
        }
        if (this.draggingBar) {
            int thumbH = this.thumbHeight();
            float frac = (float)((this.uy(click.comp_4799()) - this.dragBarOffset - (double)this.vpY) / (double)(this.vpH - thumbH));
            this.scrollTarget = ClickGuiScreen.clamp(frac, 0.0f, 1.0f) * (float)this.maxScroll;
            return true;
        }
        if (this.dragId != null) {
            this.updateSlider(this.ux(click.comp_4798()));
            return true;
        }
        return super.method_25403(click, offsetX, offsetY);
    }

    public boolean method_25406(class_11909 click) {
        this.pressed = null;
        if (this.draggingPanel) {
            this.draggingPanel = false;
            Settings.get().save();
            return true;
        }
        if (this.draggingBar) {
            this.draggingBar = false;
            return true;
        }
        if (this.dragId != null) {
            if ("uiscale".equals(this.dragId) && this.pendingScale != null) {
                Settings.get().uiScale = this.pendingScale.floatValue();
                this.pendingScale = null;
            }
            this.dragId = null;
            Settings.get().save();
            this.layout();
            return true;
        }
        return super.method_25406(click);
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.maxScroll > 0 && this.inVp(this.ux(mouseX), this.uy(mouseY))) {
            this.scrollTarget = ClickGuiScreen.clamp(this.scrollTarget - (float)verticalAmount * 28.0f, 0.0f, this.maxScroll);
            this.scrollAt = System.currentTimeMillis();
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean method_25404(class_11908 input) {
        int code = input.comp_4795();
        if (this.listening != null) {
            this.listening.setKey(code == 256 ? -1 : code);
            this.listening = null;
            FakeClient.MODULES.saveState();
            return true;
        }
        if (this.searchFocused) {
            switch (code) {
                case 256: {
                    if (this.search.isEmpty()) {
                        this.searchFocused = false;
                    } else {
                        this.search = "";
                    }
                    this.restartList();
                    this.layout();
                    return true;
                }
                case 257: 
                case 335: {
                    this.searchFocused = false;
                    return true;
                }
                case 259: {
                    if (!this.search.isEmpty()) {
                        this.search = this.search.substring(0, this.search.length() - 1);
                    }
                    this.restartList();
                    this.layout();
                    return true;
                }
            }
            return true;
        }
        if (this.nameFocused) {
            switch (code) {
                case 257: 
                case 335: {
                    this.doSaveConfig();
                    return true;
                }
                case 256: {
                    this.nameFocused = false;
                    return true;
                }
                case 259: {
                    if (!this.configName.isEmpty()) {
                        this.configName = this.configName.substring(0, this.configName.length() - 1);
                    }
                    return true;
                }
            }
            return true;
        }
        int menuKey = FakeClient.menuKeyCode();
        if (menuKey > 0 && code == menuKey) {
            this.method_25419();
            return true;
        }
        return super.method_25404(input);
    }

    public boolean method_25400(class_11905 input) {
        char c = (char)input.comp_4793();
        if (c < ' ') {
            return super.method_25400(input);
        }
        if (this.searchFocused) {
            if (this.search.length() < 20) {
                this.search = this.search + c;
                this.restartList();
                this.layout();
            }
            return true;
        }
        if (this.nameFocused) {
            if (this.configName.length() < 24) {
                this.configName = this.configName + c;
            }
            return true;
        }
        return super.method_25400(input);
    }

    public boolean method_25421() {
        return false;
    }

    private String trim(String s, int maxW) {
        return this.trim(s, maxW, false);
    }

    private String trimBold(String s, int maxW) {
        return this.trim(s, maxW, true);
    }

    private String trim(String s, int maxW, boolean bold) {
        int end;
        if (s == null || s.isEmpty() || maxW <= 0) {
            return "";
        }
        if (this.textWidth(s, bold) <= maxW) {
            return s;
        }
        int ellipsis = this.textWidth("...", bold);
        if (ellipsis >= maxW) {
            return "...";
        }
        for (end = s.length(); end > 0 && this.textWidth(s.substring(0, end), bold) + ellipsis > maxW; --end) {
        }
        return s.substring(0, end).stripTrailing() + "...";
    }

    private int textWidth(String s, boolean bold) {
        return this.field_22793.method_27525((class_5348)(bold ? Gui.bold((String)s) : Gui.font((String)s)));
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int clampInt(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
