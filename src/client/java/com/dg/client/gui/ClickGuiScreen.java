package com.dg.client.gui;

import com.dg.client.DGClient;
import com.dg.client.core.ConfigManager;
import com.dg.client.core.WaypointManager;
import com.dg.client.core.detection.DetectionEntry;
import com.dg.client.hud.HudEditorScreen;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.module.modules.hud.ClickGuiModule;
import com.dg.client.render.Render2DUtil;
import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.ColorSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.EnumSetting;
import com.dg.client.setting.IntSetting;
import com.dg.client.setting.KeybindSetting;
import com.dg.client.setting.Setting;
import com.dg.client.setting.SettingGroup;
import com.dg.client.setting.StringListSetting;
import com.dg.client.setting.StringSetting;
import com.dg.client.util.ChatUtil;
import com.dg.client.util.ColorUtil;
import com.dg.client.util.KeyUtil;
import com.dg.client.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DGClient's neon green & white ClickGUI featuring 7 draggable category panels,
 * real post-process background blur, multi-pass gaussian outer bloom, smooth animations,
 * live module search, full setting controls (including HSVA color pickers & tag lists),
 * a Profile Manager window, and an interactive BaseFinder Detections window.
 */
public final class ClickGuiScreen extends Screen {
    private static final float PANEL_WIDTH = 126.0f;
    private static final float HEADER_HEIGHT = 21.0f;
    private static final float MODULE_HEIGHT = 18.0f;

    private static final class CategoryPanel {
        final ModuleCategory category;
        float x;
        float y;
        boolean expanded = true;
        float expandAnim = 1.0f;
        float scrollOffset = 0.0f;
        float contentHeight = 0.0f;

        CategoryPanel(ModuleCategory category, float x, float y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }
    }

    private final List<CategoryPanel> panels = new ArrayList<>();
    private final Map<Module, Float> hoverMap = new HashMap<>();
    private final Map<ColorSetting, Boolean> openColorPickers = new HashMap<>();

    private CategoryPanel focusedPanel = null;
    private CategoryPanel draggingPanel = null;
    private float dragOffX;
    private float dragOffY;

    // Active interactive controls
    private Setting<?> draggingSlider = null;
    private float sliderTrackX;
    private float sliderTrackW;

    private ColorSetting activeColorDrag = null;
    private int colorDragPart = 0; // 1 = SV box, 2 = Hue bar, 3 = Alpha bar
    private float colorBoxX, colorBoxY, colorBoxW, colorBoxH;

    private KeybindSetting listeningBind = null;
    private StringSetting editingString = null;
    private StringListSetting editingList = null;
    private String listDraft = "";

    // Top bar search
    private boolean searchFocused = false;
    private String searchQuery = "";

    // Floating Profile Manager window
    private boolean showProfilesPanel = false;
    private float profX = 60.0f, profY = 72.0f;
    private boolean draggingProfiles = false;
    private boolean profileInputFocused = false;
    private String newProfileName = "";

    // Floating Detections window
    private boolean showDetectionsPanel = false;
    private float detX = 280.0f, detY = 72.0f;
    private boolean draggingDetections = false;
    private boolean sortDetectionsByDistance = false;
    private float detScroll = 0.0f;

    // Screen open animation & hover tooltip
    private float openProgress = 0.0f;
    private String currentTooltip = null;
    private boolean layoutInitialized = false;
    private long openedAtNano = 0L;

    public ClickGuiScreen() {
        super(Text.literal("DGClient"));
        ModuleCategory[] cats = ModuleCategory.values();
        for (int i = 0; i < cats.length; i++) {
            panels.add(new CategoryPanel(cats[i], 14.0f + i * (PANEL_WIDTH + 8.0f), 44.0f));
        }
        if (!panels.isEmpty()) {
            focusedPanel = panels.get(0);
        }
    }

    public void markJustOpened() {
        this.openedAtNano = System.nanoTime();
    }

    @Override
    protected void init() {
        super.init();
        openProgress = 0.05f;
        openedAtNano = System.nanoTime();
        ConfigManager cm = DGClient.getInstance().getConfigManager();
        if (!layoutInitialized) {
            layoutInitialized = true;
            for (CategoryPanel p : panels) {
                float[] saved = cm.getPanelPosition(p.category.name());
                if (saved != null) {
                    p.x = saved[0];
                    p.y = saved[1];
                    p.expanded = saved[2] > 0.5f;
                    p.expandAnim = p.expanded ? 1.0f : 0.0f;
                }
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Explicit post-process blur + emerald dim handled in render()
    }

    private float getScale() {
        ClickGuiModule mod = DGClient.getInstance().getModuleManager().get(ClickGuiModule.class);
        return mod != null ? mod.guiScale.getValue().floatValue() : 1.0f;
    }

    private float getGlow() {
        ClickGuiModule mod = DGClient.getInstance().getModuleManager().get(ClickGuiModule.class);
        return mod != null ? mod.glowStrength.getValue().floatValue() : 1.0f;
    }

    private float getAnimSpeed() {
        ClickGuiModule mod = DGClient.getInstance().getModuleManager().get(ClickGuiModule.class);
        return mod != null ? mod.animSpeed.getValue().floatValue() : 1.0f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Render2DUtil.setContext(context);
        ClickGuiModule guiMod = DGClient.getInstance().getModuleManager().get(ClickGuiModule.class);
        if (guiMod == null || guiMod.blurBackground.getValue()) {
            try {
                this.applyBlur(context);
            } catch (IllegalStateException ignored) {
                // Guard against GuiRenderState single-blur-per-frame constraint if another mod already blurred
            }
        }

        float animSpeed = 0.18f * getAnimSpeed();
        openProgress = MathUtil.approach(openProgress, 1.0f, animSpeed);
        float openEase = MathUtil.easeOutCubic(openProgress);

        float dimAlpha = guiMod != null ? guiMod.dimOpacity.getValue().floatValue() : 0.55f;
        Matrix3x2fStack ms = context.getMatrices();
        Render2DUtil.drawQuad(ms, 0, 0, this.width, this.height,
                ColorUtil.rgba(8, 13, 10, Math.round(255.0f * dimAlpha * openEase)));

        currentTooltip = null;
        float uiScale = getScale();
        float glow = getGlow();

        // Render top command bar (unscaled screen coordinates for crisp top alignment)
        renderTopBar(context, mouseX, mouseY, glow);

        // Scaled main panel workspace
        float invScale = 1.0f / uiScale;
        float scaledMx = mouseX * invScale;
        float scaledMy = mouseY * invScale;
        float maxPanelBottom = (this.height * invScale) - 18.0f;

        ms.pushMatrix();
        float introScale = uiScale * (0.94f + 0.06f * openEase);
        float centerShiftX = (this.width * (1.0f - (0.94f + 0.06f * openEase))) * 0.5f;
        float centerShiftY = (this.height * (1.0f - (0.94f + 0.06f * openEase))) * 0.5f;
        ms.translate(centerShiftX, centerShiftY);
        ms.scale(introScale, introScale);

        for (CategoryPanel panel : panels) {
            renderCategoryPanel(context, panel, scaledMx, scaledMy, maxPanelBottom, glow, animSpeed, uiScale, centerShiftX, centerShiftY);
        }

        ms.popMatrix();

        if (showProfilesPanel) {
            renderProfilesWindow(context, mouseX, mouseY, glow);
        }
        if (showDetectionsPanel) {
            renderDetectionsWindow(context, mouseX, mouseY, glow);
        }

        if (currentTooltip != null && (guiMod == null || guiMod.tooltips.getValue())) {
            renderTooltip(context, mouseX, mouseY, currentTooltip, glow);
        }

        // Also render live toasts on top of ClickGUI
        DGClient.getInstance().getHudManager().renderToasts(context, this.width, glow);
    }

    private void renderTopBar(DrawContext context, int mouseX, int mouseY, float glow) {
        Matrix3x2fStack ms = context.getMatrices();
        float barW = Math.min(this.width - 28.0f, 640.0f);
        float barX = (this.width - barW) * 0.5f;
        float barY = 8.0f;
        float barH = 24.0f;

        Render2DUtil.drawGaussianGlow(ms, barX, barY, barW, barH, 7.0f, 9.0f, ColorUtil.NEON_GREEN, glow * 0.7f);
        Render2DUtil.drawRoundedRect(ms, barX, barY, barW, barH, 7.0f, ColorUtil.PANEL_DARK);
        Render2DUtil.drawRoundedOutline(ms, barX, barY, barW, barH, 7.0f, 1.1f, ColorUtil.withAlpha(ColorUtil.NEON_GREEN, 185));

        DGFontRenderer.drawAnimatedGradientText(context, "DGClient", barX + 10.0f, barY + 7.2f, true, 1.05f, 1.2f, glow);

        // Search box
        float searchX = barX + 82.0f;
        float searchW = barW - 318.0f;
        float searchY = barY + 3.5f;
        float searchH = 17.0f;
        int searchBorder = searchFocused ? ColorUtil.NEON_GREEN : ColorUtil.BORDER_SUBTLE;
        Render2DUtil.drawRoundedRect(ms, searchX, searchY, searchW, searchH, 5.0f, ColorUtil.PANEL_ELEVATED);
        Render2DUtil.drawRoundedOutline(ms, searchX, searchY, searchW, searchH, 5.0f, 1.0f, searchBorder);
        String cursor = (searchFocused && (System.currentTimeMillis() / 450L) % 2 == 0) ? "|" : "";
        String displaySearch = searchQuery.isEmpty() && !searchFocused
                ? "Search modules... (/)"
                : searchQuery + cursor;
        int searchCol = searchQuery.isEmpty() && !searchFocused ? ColorUtil.MUTED_WHITE : ColorUtil.PURE_WHITE;
        DGFontRenderer.drawScaledText(context, displaySearch, searchX + 7.0f, searchY + 4.5f, searchCol, false, 0.86f, false);

        // Action buttons: [HUD Editor] [Profiles] [Detections]
        float btnY = barY + 3.5f;
        float btnH = 17.0f;
        float btnX = searchX + searchW + 8.0f;
        drawTopButton(context, "HUD Editor", btnX, btnY, 70.0f, btnH, false, mouseX, mouseY);
        drawTopButton(context, "Profiles", btnX + 75.0f, btnY, 62.0f, btnH, showProfilesPanel, mouseX, mouseY);
        int detCount = DGClient.getInstance().getDetectionRegistry().getAll().size();
        String detLabel = detCount > 0 ? "Detections (" + detCount + ")" : "Detections";
        drawTopButton(context, detLabel, btnX + 142.0f, btnY, 82.0f, btnH, showDetectionsPanel, mouseX, mouseY);
    }

    private void drawTopButton(DrawContext ctx, String label, float x, float y, float w, float h, boolean active, int mx, int my) {
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int bg = active ? 0xDD173021 : (hov ? ColorUtil.PANEL_HOVER : ColorUtil.PANEL_ELEVATED);
        int border = active ? ColorUtil.NEON_GREEN : (hov ? ColorUtil.MINT_GREEN : ColorUtil.BORDER_SUBTLE);
        Render2DUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, 4.5f, bg);
        Render2DUtil.drawRoundedOutline(ctx.getMatrices(), x, y, w, h, 4.5f, 1.0f, border);
        DGFontRenderer.drawCenteredText(ctx, label, x + w * 0.5f, y + 4.8f, active ? ColorUtil.NEON_GREEN : ColorUtil.PURE_WHITE, active, 0.82f);
    }

    private void renderCategoryPanel(
            DrawContext ctx,
            CategoryPanel panel,
            float mx,
            float my,
            float maxPanelBottom,
            float glow,
            float animSpeed,
            float uiScale,
            float shiftX,
            float shiftY
    ) {
        Matrix3x2fStack ms = ctx.getMatrices();
        panel.expandAnim = MathUtil.approach(panel.expandAnim, panel.expanded ? 1.0f : 0.0f, animSpeed);
        float expandEase = MathUtil.easeOutCubic(panel.expandAnim);

        List<Module> modules = getFilteredModules(panel.category);
        float rawContentH = measureModulesHeight(modules, animSpeed);
        panel.contentHeight = rawContentH;

        float maxBodyH = Math.max(60.0f, maxPanelBottom - (panel.y + HEADER_HEIGHT));
        float visibleBodyH = Math.min(rawContentH, maxBodyH) * expandEase;
        panel.scrollOffset = MathUtil.clamp(panel.scrollOffset, 0.0f, Math.max(0.0f, rawContentH - maxBodyH));

        float totalH = HEADER_HEIGHT + visibleBodyH;
        boolean isFocused = (panel == focusedPanel);

        // Outer gaussian glow + drop shadow
        Render2DUtil.drawDropShadow(ms, panel.x, panel.y, PANEL_WIDTH, totalH, 6.5f, 8.0f, 150);
        float pulse = isFocused ? (float) (0.75 + 0.25 * Math.sin(System.currentTimeMillis() / 260.0)) : 0.42f;
        Render2DUtil.drawGaussianGlow(ms, panel.x, panel.y, PANEL_WIDTH, totalH, 6.5f, isFocused ? 9.5f : 6.0f,
                ColorUtil.NEON_GREEN, glow * pulse);

        // Panel chassis
        Render2DUtil.drawRoundedRect(ms, panel.x, panel.y, PANEL_WIDTH, totalH, 6.5f, ColorUtil.PANEL_BG);
        int borderCol = isFocused
                ? ColorUtil.lerp(ColorUtil.MINT_GREEN, ColorUtil.NEON_GREEN, pulse)
                : ColorUtil.withAlpha(ColorUtil.NEON_GREEN, 105);
        Render2DUtil.drawRoundedOutline(ms, panel.x, panel.y, PANEL_WIDTH, totalH, 6.5f, isFocused ? 1.4f : 1.0f, borderCol);

        // Header bar
        Render2DUtil.drawRoundedRect(ms, panel.x + 1.0f, panel.y + 1.0f, PANEL_WIDTH - 2.0f, HEADER_HEIGHT - 1.5f, 5.5f, ColorUtil.PANEL_ELEVATED);
        DGFontRenderer.drawGlowText(ctx, panel.category.getDisplayName(), panel.x + 8.0f, panel.y + 6.2f,
                ColorUtil.PURE_WHITE, ColorUtil.NEON_GREEN, true, 0.94f, isFocused ? glow * 0.65f : 0.0f);
        String countGlyph = modules.size() + (panel.expanded ? " v" : " >");
        float cw = DGFontRenderer.getWidth(countGlyph, false, 0.8f);
        DGFontRenderer.drawScaledText(ctx, countGlyph, panel.x + PANEL_WIDTH - cw - 7.0f, panel.y + 6.8f, ColorUtil.MINT_GREEN, false, 0.8f, false);

        if (visibleBodyH <= 1.0f) {
            return;
        }

        // Scissor clip body in screen space
        float scX = shiftX + panel.x * uiScale;
        float scY = shiftY + (panel.y + HEADER_HEIGHT) * uiScale;
        float scW = PANEL_WIDTH * uiScale;
        float scH = visibleBodyH * uiScale;
        Render2DUtil.pushScissor(ctx, scX, scY, scW, scH);

        float cursorY = panel.y + HEADER_HEIGHT + 2.0f - panel.scrollOffset;
        for (Module mod : modules) {
            cursorY = renderModuleEntry(ctx, panel, mod, cursorY, mx, my, panel.y + HEADER_HEIGHT, panel.y + HEADER_HEIGHT + visibleBodyH, glow, animSpeed);
        }

        Render2DUtil.popScissor(ctx);

        // Subtle scrollbar thumb if overflowing
        if (rawContentH > maxBodyH + 2.0f) {
            float ratio = maxBodyH / rawContentH;
            float thumbH = Math.max(14.0f, visibleBodyH * ratio);
            float thumbY = panel.y + HEADER_HEIGHT + (panel.scrollOffset / (rawContentH - maxBodyH)) * (visibleBodyH - thumbH);
            Render2DUtil.drawRoundedRect(ms, panel.x + PANEL_WIDTH - 2.6f, thumbY, 1.6f, thumbH, 0.8f, ColorUtil.NEON_GREEN);
        }
    }

    private float renderModuleEntry(
            DrawContext ctx,
            CategoryPanel panel,
            Module mod,
            float y,
            float mx,
            float my,
            float clipTop,
            float clipBottom,
            float glow,
            float animSpeed
    ) {
        Matrix3x2fStack ms = ctx.getMatrices();
        float x = panel.x + 3.0f;
        float w = PANEL_WIDTH - 6.0f;
        boolean inClip = (my >= clipTop && my <= clipBottom);
        boolean hovered = inClip && mx >= x && mx <= x + w && my >= y && my <= y + MODULE_HEIGHT;
        if (hovered) {
            currentTooltip = mod.getDescription();
        }

        float hover = MathUtil.approach(hoverMap.getOrDefault(mod, 0.0f), hovered ? 1.0f : 0.0f, 0.25f);
        hoverMap.put(mod, hover);
        mod.setToggleAnim(MathUtil.approach(mod.getToggleAnim(), mod.isEnabled() ? 1.0f : 0.0f, animSpeed));
        mod.setExpandAnim(MathUtil.approach(mod.getExpandAnim(), mod.isSettingsOpen() ? 1.0f : 0.0f, animSpeed));

        float tAnim = mod.getToggleAnim();
        int rowBg = ColorUtil.lerp(
                ColorUtil.lerp(0x99101813, ColorUtil.PANEL_HOVER, hover),
                0xDD152B1E,
                tAnim
        );
        Render2DUtil.drawRoundedRect(ms, x, y + 1.0f, w, MODULE_HEIGHT - 2.0f, 4.0f, rowBg);

        if (tAnim > 0.03f) {
            int outlineCol = ColorUtil.withAlpha(ColorUtil.NEON_GREEN, Math.round(185 * tAnim));
            Render2DUtil.drawGaussianGlow(ms, x, y + 1.0f, w, MODULE_HEIGHT - 2.0f, 4.0f, 4.0f, ColorUtil.NEON_GREEN, glow * 0.45f * tAnim);
            Render2DUtil.drawRoundedOutline(ms, x, y + 1.0f, w, MODULE_HEIGHT - 2.0f, 4.0f, 0.9f, outlineCol);
        }

        int textCol = ColorUtil.lerp(ColorUtil.SOFT_WHITE, ColorUtil.PURE_WHITE, Math.max(hover, tAnim));
        DGFontRenderer.drawScaledText(ctx, mod.getName(), x + 6.0f, y + 5.2f, textCol, mod.isEnabled(), 0.86f, false);

        // Animated mini pill switch on right
        float swW = 16.0f;
        float swH = 8.5f;
        float swX = x + w - swW - 5.0f;
        float swY = y + (MODULE_HEIGHT - swH) * 0.5f;
        int trackCol = ColorUtil.lerp(0xFF1C2921, ColorUtil.withAlpha(ColorUtil.NEON_GREEN, 210), tAnim);
        Render2DUtil.drawRoundedRect(ms, swX, swY, swW, swH, swH * 0.5f, trackCol);
        float knobX = swX + 1.3f + (swW - swH) * tAnim;
        Render2DUtil.drawCircle(ms, knobX + (swH - 2.6f) * 0.5f, swY + swH * 0.5f, (swH - 2.6f) * 0.5f, ColorUtil.PURE_WHITE);

        float nextY = y + MODULE_HEIGHT;
        float exp = MathUtil.easeOutCubic(mod.getExpandAnim());
        if (exp > 0.01f) {
            float fullDrawerH = measureDrawerHeight(mod);
            float curDrawerH = fullDrawerH * exp;
            Render2DUtil.drawRoundedRect(ms, x + 1.5f, nextY, w - 3.0f, curDrawerH, 4.0f, 0xCC090E0B);
            if (exp > 0.8f) {
                renderDrawerContents(ctx, mod, x + 4.0f, nextY + 3.0f, w - 8.0f, mx, my, inClip, glow);
            }
            nextY += curDrawerH + 2.0f;
        }
        return nextY;
    }

    private void renderDrawerContents(
            DrawContext ctx,
            Module mod,
            float x,
            float y,
            float w,
            float mx,
            float my,
            boolean inClip,
            float glow
    ) {
        Matrix3x2fStack ms = ctx.getMatrices();
        float cy = y;

        // Keybind row
        KeybindSetting kb = mod.getKeybind();
        boolean listening = (listeningBind == kb);
        String kbText = listening ? "Press key... [ESC=None]"
                : "Bind: " + KeyUtil.getKeyName(kb.getValue()) + (kb.isHoldMode() ? " (Hold)" : "");
        boolean kbHov = inClip && mx >= x && mx <= x + w && my >= cy && my <= cy + 14.0f;
        Render2DUtil.drawRoundedRect(ms, x, cy, w, 13.5f, 3.0f, listening ? 0xDD1A3826 : (kbHov ? ColorUtil.PANEL_HOVER : ColorUtil.PANEL_ELEVATED));
        if (listening) {
            Render2DUtil.drawRoundedOutline(ms, x, cy, w, 13.5f, 3.0f, 1.0f, ColorUtil.NEON_GREEN);
        }
        DGFontRenderer.drawScaledText(ctx, kbText, x + 4.5f, cy + 3.2f, listening ? ColorUtil.NEON_GREEN : ColorUtil.SOFT_WHITE, false, 0.78f, false);
        cy += 15.5f;

        for (SettingGroup group : mod.getSettingGroups()) {
            List<Setting<?>> visSettings = group.getSettings().stream().filter(Setting::isVisible).toList();
            if (visSettings.isEmpty()) {
                continue;
            }
            if (!group.getName().equals("General") || mod.getSettingGroups().size() > 1) {
                Render2DUtil.drawRoundedRect(ms, x, cy, w, 12.0f, 2.5f, 0xAA132018);
                String prefix = group.isExpanded() ? "▾ " : "▸ ";
                DGFontRenderer.drawScaledText(ctx, prefix + group.getName(), x + 3.5f, cy + 2.5f, ColorUtil.MINT_GREEN, true, 0.76f, false);
                cy += 13.5f;
                if (!group.isExpanded()) {
                    continue;
                }
            }

            for (Setting<?> s : visSettings) {
                float sh = getSettingHeight(s);
                if (inClip && mx >= x && mx <= x + w && my >= cy && my <= cy + sh && !s.getDescription().isEmpty()) {
                    currentTooltip = s.getDescription();
                }

                if (s instanceof BooleanSetting bs) {
                    DGFontRenderer.drawScaledText(ctx, bs.getName(), x + 2.0f, cy + 3.0f, ColorUtil.SOFT_WHITE, false, 0.78f, false);
                    float pillW = 14.0f, pillH = 7.5f;
                    float px = x + w - pillW - 2.0f, py = cy + 2.5f;
                    Render2DUtil.drawRoundedRect(ms, px, py, pillW, pillH, 3.75f, bs.getValue() ? ColorUtil.NEON_GREEN : 0xFF1D2A22);
                    float kx = px + (bs.getValue() ? (pillW - pillH + 1.0f) : 1.0f);
                    Render2DUtil.drawCircle(ms, kx + (pillH - 2.0f) * 0.5f, py + pillH * 0.5f, (pillH - 2.0f) * 0.5f, ColorUtil.PURE_WHITE);
                } else if (s instanceof IntSetting is) {
                    renderSlider(ctx, is.getName(), String.valueOf(is.getValue()), is.getNormalized(), x, cy, w, glow);
                } else if (s instanceof DoubleSetting ds) {
                    renderSlider(ctx, ds.getName(), String.format(Locale.ROOT, "%.2f", ds.getValue()), ds.getNormalized(), x, cy, w, glow);
                } else if (s instanceof EnumSetting<?> es) {
                    Render2DUtil.drawRoundedRect(ms, x, cy, w, 14.0f, 3.0f, ColorUtil.PANEL_ELEVATED);
                    DGFontRenderer.drawScaledText(ctx, es.getName(), x + 3.5f, cy + 3.4f, ColorUtil.SOFT_WHITE, false, 0.76f, false);
                    String val = es.getValue().name();
                    float vw = DGFontRenderer.getWidth(val, true, 0.76f);
                    DGFontRenderer.drawScaledText(ctx, val, x + w - vw - 4.0f, cy + 3.4f, ColorUtil.NEON_GREEN, true, 0.76f, false);
                } else if (s instanceof ColorSetting cs) {
                    Render2DUtil.drawRoundedRect(ms, x, cy, w, 14.0f, 3.0f, ColorUtil.PANEL_ELEVATED);
                    DGFontRenderer.drawScaledText(ctx, cs.getName(), x + 3.5f, cy + 3.4f, ColorUtil.SOFT_WHITE, false, 0.76f, false);
                    Render2DUtil.drawRoundedRect(ms, x + w - 18.0f, cy + 2.5f, 14.0f, 9.0f, 2.5f, cs.getColor());
                    Render2DUtil.drawRoundedOutline(ms, x + w - 18.0f, cy + 2.5f, 14.0f, 9.0f, 2.5f, 0.8f, ColorUtil.PURE_WHITE);
                    if (Boolean.TRUE.equals(openColorPickers.get(cs))) {
                        renderColorPickerBody(ctx, cs, x + 2.0f, cy + 16.0f, w - 4.0f);
                    }
                } else if (s instanceof KeybindSetting ks) {
                    boolean active = (listeningBind == ks);
                    Render2DUtil.drawRoundedRect(ms, x, cy, w, 14.0f, 3.0f, active ? 0xDD1A3826 : ColorUtil.PANEL_ELEVATED);
                    String label = ks.getName() + ": " + (active ? "..." : KeyUtil.getKeyName(ks.getValue()));
                    DGFontRenderer.drawScaledText(ctx, label, x + 3.5f, cy + 3.4f, active ? ColorUtil.NEON_GREEN : ColorUtil.SOFT_WHITE, false, 0.76f, false);
                } else if (s instanceof StringSetting ss) {
                    boolean active = (editingString == ss);
                    DGFontRenderer.drawScaledText(ctx, ss.getName(), x + 2.0f, cy + 1.5f, ColorUtil.MUTED_WHITE, false, 0.74f, false);
                    Render2DUtil.drawRoundedRect(ms, x, cy + 10.0f, w, 13.0f, 3.0f, ColorUtil.PANEL_ELEVATED);
                    Render2DUtil.drawRoundedOutline(ms, x, cy + 10.0f, w, 13.0f, 3.0f, 0.9f, active ? ColorUtil.NEON_GREEN : ColorUtil.BORDER_SUBTLE);
                    String cur = active && (System.currentTimeMillis() / 450L) % 2 == 0 ? "|" : "";
                    DGFontRenderer.drawScaledText(ctx, ss.getValue() + cur, x + 3.5f, cy + 13.0f, ColorUtil.PURE_WHITE, false, 0.75f, false);
                } else if (s instanceof StringListSetting sls) {
                    DGFontRenderer.drawScaledText(ctx, sls.getName() + " (" + sls.getItems().size() + ")", x + 2.0f, cy + 1.5f, ColorUtil.MINT_GREEN, false, 0.75f, false);
                    float ly = cy + 11.0f;
                    for (String item : sls.getItems()) {
                        Render2DUtil.drawRoundedRect(ms, x + 2.0f, ly, w - 4.0f, 11.5f, 2.5f, ColorUtil.PANEL_ELEVATED);
                        DGFontRenderer.drawScaledText(ctx, item, x + 5.0f, ly + 2.2f, ColorUtil.SOFT_WHITE, false, 0.72f, false);
                        DGFontRenderer.drawScaledText(ctx, "x", x + w - 9.5f, ly + 2.0f, ColorUtil.NEON_GREEN, true, 0.74f, false);
                        ly += 12.5f;
                    }
                    boolean active = (editingList == sls);
                    Render2DUtil.drawRoundedRect(ms, x + 2.0f, ly, w - 4.0f, 12.5f, 2.5f, 0xCC0D1711);
                    Render2DUtil.drawRoundedOutline(ms, x + 2.0f, ly, w - 4.0f, 12.5f, 2.5f, 0.8f, active ? ColorUtil.NEON_GREEN : ColorUtil.BORDER_SUBTLE);
                    String prompt = active ? (listDraft + "|") : "+ Add entry...";
                    DGFontRenderer.drawScaledText(ctx, prompt, x + 5.0f, ly + 2.8f, active ? ColorUtil.PURE_WHITE : ColorUtil.MUTED_WHITE, false, 0.72f, false);
                }
                cy += sh + 2.0f;
            }
        }
    }

    private void renderSlider(DrawContext ctx, String name, String valText, float norm, float x, float y, float w, float glow) {
        Matrix3x2fStack ms = ctx.getMatrices();
        DGFontRenderer.drawScaledText(ctx, name, x + 2.0f, y + 1.5f, ColorUtil.SOFT_WHITE, false, 0.76f, false);
        float vw = DGFontRenderer.getWidth(valText, true, 0.75f);
        DGFontRenderer.drawScaledText(ctx, valText, x + w - vw - 2.0f, y + 1.5f, ColorUtil.NEON_GREEN, true, 0.75f, false);

        float trackY = y + 12.0f;
        float trackH = 3.6f;
        Render2DUtil.drawRoundedRect(ms, x + 2.0f, trackY, w - 4.0f, trackH, 1.8f, 0xFF19261E);
        float fillW = Math.max(2.0f, (w - 4.0f) * MathUtil.clamp(norm, 0.0f, 1.0f));
        Render2DUtil.drawGaussianGlow(ms, x + 2.0f, trackY, fillW, trackH, 1.8f, 3.5f, ColorUtil.NEON_GREEN, glow * 0.55f);
        Render2DUtil.drawRoundedRect(ms, x + 2.0f, trackY, fillW, trackH, 1.8f, ColorUtil.NEON_GREEN);

        float knobX = x + 2.0f + fillW;
        float knobY = trackY + trackH * 0.5f;
        Render2DUtil.drawGaussianGlow(ms, knobX - 3.2f, knobY - 3.2f, 6.4f, 6.4f, 3.2f, 3.5f, ColorUtil.PURE_WHITE, glow * 0.5f);
        Render2DUtil.drawCircle(ms, knobX, knobY, 3.1f, ColorUtil.PURE_WHITE);
    }

    private void renderColorPickerBody(DrawContext ctx, ColorSetting cs, float x, float y, float w) {
        Matrix3x2fStack ms = ctx.getMatrices();
        float svH = 38.0f;
        int pureHue = ColorUtil.hsvToArgb(cs.getHue(), 1.0f, 1.0f, 255);
        Render2DUtil.drawGradientQuad(ms, x, y, w, svH, ColorUtil.PURE_WHITE, pureHue, pureHue, ColorUtil.PURE_WHITE);
        Render2DUtil.drawGradientQuad(ms, x, y, w, svH, 0x00000000, 0x00000000, 0xFF000000, 0xFF000000);
        Render2DUtil.drawRoundedOutline(ms, x, y, w, svH, 2.0f, 0.8f, ColorUtil.BORDER_SUBTLE);

        float cx = x + cs.getSaturation() * w;
        float cy = y + (1.0f - cs.getBrightness()) * svH;
        Render2DUtil.drawCircleOutline(ms, cx, cy, 2.8f, 1.1f, ColorUtil.PURE_WHITE);

        // Green hue band slider (95..165)
        float hueY = y + svH + 4.0f;
        Render2DUtil.drawGradientQuad(ms, x, hueY, w, 5.0f, ColorUtil.NEON_GREEN, ColorUtil.MINT_GREEN, ColorUtil.MINT_GREEN, ColorUtil.NEON_GREEN);
        float hueNorm = (cs.getHue() - 95.0f) / 70.0f;
        Render2DUtil.drawCircle(ms, x + hueNorm * w, hueY + 2.5f, 2.8f, ColorUtil.PURE_WHITE);

        // Alpha slider
        float alphaY = hueY + 8.0f;
        Render2DUtil.drawGradientQuad(ms, x, alphaY, w, 5.0f, 0x22FFFFFF, ColorUtil.withAlpha(cs.getColor(), 255),
                ColorUtil.withAlpha(cs.getColor(), 255), 0x22FFFFFF);
        Render2DUtil.drawCircle(ms, x + (cs.getAlpha() / 255.0f) * w, alphaY + 2.5f, 2.8f, ColorUtil.PURE_WHITE);

        // Pulse toggle row
        float pulseY = alphaY + 8.0f;
        DGFontRenderer.drawScaledText(ctx, "Neon Pulse: " + (cs.isPulse() ? "ON" : "OFF"), x + 2.0f, pulseY,
                cs.isPulse() ? ColorUtil.NEON_GREEN : ColorUtil.MUTED_WHITE, false, 0.72f, false);
    }

    private void renderProfilesWindow(DrawContext ctx, int mx, int my, float glow) {
        Matrix3x2fStack ms = ctx.getMatrices();
        ConfigManager cm = DGClient.getInstance().getConfigManager();
        List<String> profiles = cm.listProfiles();
        float w = 186.0f;
        float h = 76.0f + profiles.size() * 16.0f;

        Render2DUtil.drawGaussianGlow(ms, profX, profY, w, h, 7.0f, 9.0f, ColorUtil.NEON_GREEN, glow * 0.75f);
        Render2DUtil.drawRoundedRect(ms, profX, profY, w, h, 7.0f, ColorUtil.PANEL_DARK);
        Render2DUtil.drawRoundedOutline(ms, profX, profY, w, h, 7.0f, 1.2f, ColorUtil.NEON_GREEN);

        DGFontRenderer.drawScaledText(ctx, "PROFILES  [" + cm.getActiveProfile() + "]", profX + 8.0f, profY + 6.5f, ColorUtil.NEON_GREEN, true, 0.86f, false);

        // New profile input + [+ Create]
        float inY = profY + 21.0f;
        Render2DUtil.drawRoundedRect(ms, profX + 6.0f, inY, 118.0f, 15.0f, 3.5f, ColorUtil.PANEL_ELEVATED);
        Render2DUtil.drawRoundedOutline(ms, profX + 6.0f, inY, 118.0f, 15.0f, 3.5f, 0.9f, profileInputFocused ? ColorUtil.NEON_GREEN : ColorUtil.BORDER_SUBTLE);
        String pTxt = newProfileName.isEmpty() && !profileInputFocused ? "New profile..." : newProfileName + (profileInputFocused ? "|" : "");
        DGFontRenderer.drawScaledText(ctx, pTxt, profX + 10.0f, inY + 4.0f, newProfileName.isEmpty() ? ColorUtil.MUTED_WHITE : ColorUtil.PURE_WHITE, false, 0.78f, false);
        drawTopButton(ctx, "+ Save", profX + 128.0f, inY, 52.0f, 15.0f, false, mx, my);

        float py = profY + 41.0f;
        for (String p : profiles) {
            boolean active = p.equalsIgnoreCase(cm.getActiveProfile());
            Render2DUtil.drawRoundedRect(ms, profX + 6.0f, py, w - 12.0f, 14.0f, 3.0f, active ? 0xDD173021 : ColorUtil.PANEL_ELEVATED);
            DGFontRenderer.drawScaledText(ctx, p, profX + 10.0f, py + 3.4f, active ? ColorUtil.NEON_GREEN : ColorUtil.PURE_WHITE, active, 0.8f, false);
            DGFontRenderer.drawScaledText(ctx, "Load", profX + w - 52.0f, py + 3.4f, ColorUtil.MINT_GREEN, false, 0.76f, false);
            if (!p.equalsIgnoreCase("default")) {
                DGFontRenderer.drawScaledText(ctx, "x", profX + w - 16.0f, py + 3.2f, ColorUtil.SOFT_WHITE, true, 0.78f, false);
            }
            py += 16.0f;
        }

        drawTopButton(ctx, "Reset Current to Defaults", profX + 6.0f, py + 4.0f, w - 12.0f, 15.0f, false, mx, my);
    }

    private void renderDetectionsWindow(DrawContext ctx, int mx, int my, float glow) {
        Matrix3x2fStack ms = ctx.getMatrices();
        var reg = DGClient.getInstance().getDetectionRegistry();
        List<DetectionEntry> all = new ArrayList<>(reg.getDeduplicatedClusters());
        MinecraftClient mc = MinecraftClient.getInstance();
        if (sortDetectionsByDistance && mc.player != null) {
            all.sort(Comparator.comparingDouble(d -> mc.player.squaredDistanceTo(d.getCenterX(), d.getCenterY(), d.getCenterZ())));
        }

        float w = 278.0f;
        float h = 196.0f;
        Render2DUtil.drawGaussianGlow(ms, detX, detY, w, h, 7.0f, 9.0f, ColorUtil.NEON_GREEN, glow * 0.75f);
        Render2DUtil.drawRoundedRect(ms, detX, detY, w, h, 7.0f, ColorUtil.PANEL_DARK);
        Render2DUtil.drawRoundedOutline(ms, detX, detY, w, h, 7.0f, 1.2f, ColorUtil.NEON_GREEN);

        DGFontRenderer.drawScaledText(ctx, "BASEFINDER CLUSTERS (" + all.size() + ")", detX + 8.0f, detY + 6.5f, ColorUtil.NEON_GREEN, true, 0.86f, false);

        float ctrlY = detY + 20.0f;
        drawTopButton(ctx, sortDetectionsByDistance ? "Sort: Dist" : "Sort: Score", detX + 6.0f, ctrlY, 68.0f, 14.0f, false, mx, my);
        drawTopButton(ctx, "Export JSON/CSV", detX + 78.0f, ctrlY, 88.0f, 14.0f, false, mx, my);
        drawTopButton(ctx, "Import", detX + 170.0f, ctrlY, 48.0f, 14.0f, false, mx, my);
        drawTopButton(ctx, "Clear", detX + 222.0f, ctrlY, 50.0f, 14.0f, false, mx, my);

        float listTop = detY + 38.0f;
        float listH = h - 44.0f;
        Render2DUtil.pushScissor(ctx, detX + 4.0f, listTop, w - 8.0f, listH);

        if (all.isEmpty()) {
            DGFontRenderer.drawCenteredText(ctx, "No suspicious base clusters recorded yet.", detX + w * 0.5f, listTop + 56.0f, ColorUtil.MUTED_WHITE, false, 0.84f);
        } else {
            float maxScroll = Math.max(0.0f, all.size() * 24.0f - listH);
            detScroll = MathUtil.clamp(detScroll, 0.0f, maxScroll);
            float ry = listTop - detScroll;
            for (DetectionEntry d : all) {
                int rowCol = d.getStatus() == DetectionEntry.Status.VERIFIED ? 0xDD173524
                        : (d.getStatus() == DetectionEntry.Status.IGNORED ? 0x88101412 : ColorUtil.PANEL_ELEVATED);
                Render2DUtil.drawRoundedRect(ms, detX + 6.0f, ry, w - 12.0f, 22.0f, 3.5f, rowCol);
                double dist = mc.player != null ? Math.sqrt(mc.player.squaredDistanceTo(d.getCenterX(), mc.player.getY(), d.getCenterZ())) : 0.0;
                String clusterBadge = d.getClusterSize() > 1 ? " [" + d.getClusterSize() + "ch]" : "";
                String head = String.format(Locale.ROOT, "%s %.0f%%%s  X:%d Z:%d (%.0fm)",
                        d.getConfidence().name(), d.getScore(), clusterBadge, d.getCenterX(), d.getCenterZ(), dist);
                int headCol = d.getConfidence().ordinal() >= 2 ? ColorUtil.NEON_GREEN : ColorUtil.PURE_WHITE;
                DGFontRenderer.drawScaledText(ctx, head, detX + 10.0f, ry + 3.0f, headCol, true, 0.76f, false);
                DGFontRenderer.drawScaledText(ctx, d.getFormattedReasons(), detX + 10.0f, ry + 12.2f, ColorUtil.MINT_GREEN, false, 0.72f, false);

                // Row buttons: [WP] [V] [X]
                DGFontRenderer.drawScaledText(ctx, "[WP]", detX + w - 66.0f, ry + 7.0f, ColorUtil.NEON_GREEN, true, 0.76f, false);
                DGFontRenderer.drawScaledText(ctx, "[V]", detX + w - 40.0f, ry + 7.0f, ColorUtil.MINT_GREEN, true, 0.76f, false);
                DGFontRenderer.drawScaledText(ctx, "[X]", detX + w - 21.0f, ry + 7.0f, ColorUtil.SOFT_WHITE, true, 0.76f, false);
                ry += 24.0f;
            }
        }
        Render2DUtil.popScissor(ctx);
    }

    private void renderTooltip(DrawContext ctx, int mx, int my, String text, float glow) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Matrix3x2fStack ms = ctx.getMatrices();
        float tw = DGFontRenderer.getWidth(text, false, 0.82f) + 14.0f;
        float th = 16.0f;
        float tx = Math.min(this.width - tw - 6.0f, mx + 10.0f);
        float ty = Math.max(6.0f, my - 18.0f);
        Render2DUtil.drawGaussianGlow(ms, tx, ty, tw, th, 4.5f, 5.0f, ColorUtil.NEON_GREEN, glow * 0.55f);
        Render2DUtil.drawRoundedRect(ms, tx, ty, tw, th, 4.5f, ColorUtil.PANEL_DARK);
        Render2DUtil.drawRoundedOutline(ms, tx, ty, tw, th, 4.5f, 0.9f, ColorUtil.NEON_GREEN);
        DGFontRenderer.drawScaledText(ctx, text, tx + 7.0f, ty + 4.2f, ColorUtil.PURE_WHITE, false, 0.82f, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        searchFocused = false;
        profileInputFocused = false;
        editingString = null;

        // 1. Check top bar
        float barW = Math.min(this.width - 28.0f, 640.0f);
        float barX = (this.width - barW) * 0.5f;
        float barY = 8.0f;
        float searchX = barX + 82.0f;
        float searchW = barW - 318.0f;
        float btnY = barY + 3.5f;
        if (mouseY >= btnY && mouseY <= btnY + 17.0f) {
            if (mouseX >= searchX && mouseX <= searchX + searchW) {
                if (button == 1) searchQuery = "";
                searchFocused = true;
                return true;
            }
            float btnX = searchX + searchW + 8.0f;
            if (mouseX >= btnX && mouseX <= btnX + 70.0f) {
                if (this.client != null) {
                    this.client.setScreen(new HudEditorScreen());
                }
                return true;
            }
            if (mouseX >= btnX + 75.0f && mouseX <= btnX + 137.0f) {
                showProfilesPanel = !showProfilesPanel;
                return true;
            }
            if (mouseX >= btnX + 142.0f && mouseX <= btnX + 224.0f) {
                showDetectionsPanel = !showDetectionsPanel;
                return true;
            }
        }

        // 2. Floating Detections window clicks
        if (showDetectionsPanel && handleDetectionsClick((float) mouseX, (float) mouseY, button)) {
            return true;
        }

        // 3. Floating Profiles window clicks
        if (showProfilesPanel && handleProfilesClick((float) mouseX, (float) mouseY, button)) {
            return true;
        }

        // 4. Category panels (scaled space, back-to-front)
        float uiScale = getScale();
        float mx = (float) mouseX / uiScale;
        float my = (float) mouseY / uiScale;

        for (int i = panels.size() - 1; i >= 0; i--) {
            CategoryPanel panel = panels.get(i);
            if (mx >= panel.x && mx <= panel.x + PANEL_WIDTH && my >= panel.y && my <= panel.y + HEADER_HEIGHT) {
                focusPanel(panel);
                if (button == 0) {
                    draggingPanel = panel;
                    dragOffX = mx - panel.x;
                    dragOffY = my - panel.y;
                } else if (button == 1) {
                    panel.expanded = !panel.expanded;
                }
                return true;
            }
            if (!panel.expanded) {
                continue;
            }
            float maxPanelBottom = (this.height / uiScale) - 18.0f;
            float maxBodyH = Math.max(60.0f, maxPanelBottom - (panel.y + HEADER_HEIGHT));
            float visH = Math.min(panel.contentHeight, maxBodyH);
            if (mx >= panel.x && mx <= panel.x + PANEL_WIDTH && my >= panel.y + HEADER_HEIGHT && my <= panel.y + HEADER_HEIGHT + visH) {
                focusPanel(panel);
                if (handlePanelBodyClick(panel, mx, my, button)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean handlePanelBodyClick(CategoryPanel panel, float mx, float my, int button) {
        float cy = panel.y + HEADER_HEIGHT + 2.0f - panel.scrollOffset;
        float x = panel.x + 3.0f;
        float w = PANEL_WIDTH - 6.0f;

        for (Module mod : getFilteredModules(panel.category)) {
            if (mx >= x && mx <= x + w && my >= cy && my <= cy + MODULE_HEIGHT) {
                if (button == 0) {
                    mod.toggle();
                } else if (button == 1) {
                    mod.setSettingsOpen(!mod.isSettingsOpen());
                } else if (button == 2) {
                    listeningBind = mod.getKeybind();
                }
                return true;
            }
            cy += MODULE_HEIGHT;
            if (mod.isSettingsOpen()) {
                float sx = x + 4.0f;
                float sw = w - 8.0f;
                float sy = cy + 3.0f;

                // Keybind row
                if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + 13.5f) {
                    if (button == 0) {
                        listeningBind = (listeningBind == mod.getKeybind()) ? null : mod.getKeybind();
                    } else if (button == 1) {
                        mod.getKeybind().setHoldMode(!mod.getKeybind().isHoldMode());
                    }
                    return true;
                }
                sy += 15.5f;

                for (SettingGroup group : mod.getSettingGroups()) {
                    List<Setting<?>> vis = group.getSettings().stream().filter(Setting::isVisible).toList();
                    if (vis.isEmpty()) continue;
                    if (!group.getName().equals("General") || mod.getSettingGroups().size() > 1) {
                        if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + 12.0f) {
                            group.setExpanded(!group.isExpanded());
                            return true;
                        }
                        sy += 13.5f;
                        if (!group.isExpanded()) continue;
                    }

                    for (Setting<?> s : vis) {
                        float sh = getSettingHeight(s);
                        if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh) {
                            handleSettingClick(s, sx, sy, sw, mx, my, button);
                            return true;
                        }
                        sy += sh + 2.0f;
                    }
                }
                cy += measureDrawerHeight(mod) + 2.0f;
            }
        }
        return false;
    }

    private void handleSettingClick(Setting<?> s, float sx, float sy, float sw, float mx, float my, int button) {
        if (s instanceof BooleanSetting bs) {
            bs.toggle();
        } else if (s instanceof IntSetting is) {
            draggingSlider = is;
            sliderTrackX = sx + 2.0f;
            sliderTrackW = sw - 4.0f;
            is.setFromNormalized((mx - sliderTrackX) / sliderTrackW);
        } else if (s instanceof DoubleSetting ds) {
            draggingSlider = ds;
            sliderTrackX = sx + 2.0f;
            sliderTrackW = sw - 4.0f;
            ds.setFromNormalized((mx - sliderTrackX) / sliderTrackW);
        } else if (s instanceof EnumSetting<?> es) {
            if (button == 1) es.cycleBackward();
            else es.cycleForward();
        } else if (s instanceof ColorSetting cs) {
            if (my <= sy + 14.0f) {
                openColorPickers.put(cs, !Boolean.TRUE.equals(openColorPickers.get(cs)));
            } else if (Boolean.TRUE.equals(openColorPickers.get(cs))) {
                float px = sx + 2.0f, py = sy + 16.0f, pw = sw - 4.0f;
                colorBoxX = px; colorBoxY = py; colorBoxW = pw; colorBoxH = 38.0f;
                if (my <= py + 38.0f) {
                    activeColorDrag = cs;
                    colorDragPart = 1;
                    updateColorDrag(mx, my);
                } else if (my <= py + 48.0f) {
                    activeColorDrag = cs;
                    colorDragPart = 2;
                    updateColorDrag(mx, my);
                } else if (my <= py + 56.0f) {
                    activeColorDrag = cs;
                    colorDragPart = 3;
                    updateColorDrag(mx, my);
                } else {
                    cs.setPulse(!cs.isPulse());
                }
            }
        } else if (s instanceof KeybindSetting ks) {
            listeningBind = (listeningBind == ks) ? null : ks;
        } else if (s instanceof StringSetting ss) {
            editingString = ss;
        } else if (s instanceof StringListSetting sls) {
            float ly = sy + 11.0f;
            for (String item : new ArrayList<>(sls.getItems())) {
                if (my >= ly && my <= ly + 11.5f) {
                    if (mx >= sx + sw - 16.0f) {
                        sls.removeItem(item);
                    }
                    return;
                }
                ly += 12.5f;
            }
            if (my >= ly && my <= ly + 12.5f) {
                editingList = sls;
                listDraft = "";
            }
        }
    }

    private boolean handleProfilesClick(float mx, float my, int button) {
        ConfigManager cm = DGClient.getInstance().getConfigManager();
        List<String> profiles = cm.listProfiles();
        float w = 186.0f;
        float h = 76.0f + profiles.size() * 16.0f;
        if (mx < profX || mx > profX + w || my < profY || my > profY + h) {
            return false;
        }
        if (my <= profY + 19.0f) {
            draggingProfiles = true;
            dragOffX = mx - profX;
            dragOffY = my - profY;
            return true;
        }
        float inY = profY + 21.0f;
        if (my >= inY && my <= inY + 15.0f) {
            if (mx <= profX + 124.0f) {
                profileInputFocused = true;
            } else {
                String target = newProfileName.isBlank() ? cm.getActiveProfile() : newProfileName.trim();
                cm.saveProfile(target);
                newProfileName = "";
            }
            return true;
        }
        float py = profY + 41.0f;
        for (String p : profiles) {
            if (my >= py && my <= py + 14.0f) {
                if (!p.equalsIgnoreCase("default") && mx >= profX + w - 22.0f) {
                    cm.deleteProfile(p);
                } else {
                    cm.loadProfile(p);
                }
                return true;
            }
            py += 16.0f;
        }
        if (my >= py + 4.0f && my <= py + 19.0f) {
            cm.resetCurrentProfileToDefaults();
            return true;
        }
        return true;
    }

    private boolean handleDetectionsClick(float mx, float my, int button) {
        float w = 278.0f;
        float h = 196.0f;
        if (mx < detX || mx > detX + w || my < detY || my > detY + h) {
            return false;
        }
        if (my <= detY + 18.0f) {
            draggingDetections = true;
            dragOffX = mx - detX;
            dragOffY = my - detY;
            return true;
        }
        var reg = DGClient.getInstance().getDetectionRegistry();
        float ctrlY = detY + 20.0f;
        if (my >= ctrlY && my <= ctrlY + 14.0f) {
            if (mx >= detX + 6.0f && mx <= detX + 74.0f) {
                sortDetectionsByDistance = !sortDetectionsByDistance;
            } else if (mx >= detX + 78.0f && mx <= detX + 166.0f) {
                String srv = WaypointManager.getCurrentServerId();
                Path j = reg.exportJson(srv);
                reg.exportCsv(srv);
                ChatUtil.accent("Exported BaseFinder:", j != null ? j.toString() : "done");
            } else if (mx >= detX + 170.0f && mx <= detX + 218.0f) {
                reg.loadForServer(WaypointManager.getCurrentServerId());
            } else if (mx >= detX + 222.0f && mx <= detX + 272.0f) {
                reg.clearAll();
            }
            return true;
        }

        List<DetectionEntry> all = new ArrayList<>(reg.getDeduplicatedClusters());
        MinecraftClient mc = MinecraftClient.getInstance();
        if (sortDetectionsByDistance && mc.player != null) {
            all.sort(Comparator.comparingDouble(d -> mc.player.squaredDistanceTo(d.getCenterX(), d.getCenterY(), d.getCenterZ())));
        }
        float listTop = detY + 38.0f;
        float ry = listTop - detScroll;
        for (DetectionEntry d : all) {
            if (my >= Math.max(listTop, ry) && my <= ry + 22.0f) {
                if (mx >= detX + w - 70.0f && mx <= detX + w - 44.0f) {
                    DGClient.getInstance().getWaypointManager().addWaypoint(
                            "Base_" + d.getChunkX() + "_" + d.getChunkZ(),
                            d.getCenterX(), d.getCenterY(), d.getCenterZ(), false);
                    DGClient.getInstance().getConfigManager().saveAll();
                    ChatUtil.accent("Waypoint Set:", "X:" + d.getCenterX() + " Z:" + d.getCenterZ());
                } else if (mx >= detX + w - 43.0f && mx <= detX + w - 25.0f) {
                    d.setStatus(DetectionEntry.Status.VERIFIED);
                    reg.saveCurrentServer();
                } else if (mx >= detX + w - 24.0f) {
                    d.setStatus(DetectionEntry.Status.IGNORED);
                    reg.saveCurrentServer();
                }
                return true;
            }
            ry += 24.0f;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (draggingProfiles) {
            profX = (float) mouseX - dragOffX;
            profY = (float) mouseY - dragOffY;
            return true;
        }
        if (draggingDetections) {
            detX = (float) mouseX - dragOffX;
            detY = (float) mouseY - dragOffY;
            return true;
        }
        float uiScale = getScale();
        float mx = (float) mouseX / uiScale;
        float my = (float) mouseY / uiScale;

        if (draggingPanel != null) {
            draggingPanel.x = MathUtil.clamp(mx - dragOffX, 2.0f, (this.width / uiScale) - PANEL_WIDTH - 2.0f);
            draggingPanel.y = MathUtil.clamp(my - dragOffY, 34.0f / uiScale, (this.height / uiScale) - HEADER_HEIGHT - 4.0f);
            return true;
        }
        if (draggingSlider instanceof IntSetting is) {
            is.setFromNormalized((mx - sliderTrackX) / sliderTrackW);
            return true;
        }
        if (draggingSlider instanceof DoubleSetting ds) {
            ds.setFromNormalized((mx - sliderTrackX) / sliderTrackW);
            return true;
        }
        if (activeColorDrag != null) {
            updateColorDrag(mx, my);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    private void updateColorDrag(float mx, float my) {
        if (activeColorDrag == null) return;
        if (colorDragPart == 1) {
            float sat = MathUtil.clamp((mx - colorBoxX) / colorBoxW, 0.0f, 1.0f);
            float bri = 1.0f - MathUtil.clamp((my - colorBoxY) / colorBoxH, 0.0f, 1.0f);
            activeColorDrag.setHsva(activeColorDrag.getHue(), sat, bri, activeColorDrag.getAlpha());
        } else if (colorDragPart == 2) {
            float hue = 95.0f + MathUtil.clamp((mx - colorBoxX) / colorBoxW, 0.0f, 1.0f) * 70.0f;
            activeColorDrag.setHsva(hue, activeColorDrag.getSaturation(), activeColorDrag.getBrightness(), activeColorDrag.getAlpha());
        } else if (colorDragPart == 3) {
            int alpha = Math.round(MathUtil.clamp((mx - colorBoxX) / colorBoxW, 0.0f, 1.0f) * 255.0f);
            activeColorDrag.setHsva(activeColorDrag.getHue(), activeColorDrag.getSaturation(), activeColorDrag.getBrightness(), alpha);
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingPanel = null;
        draggingSlider = null;
        activeColorDrag = null;
        draggingProfiles = false;
        draggingDetections = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (showDetectionsPanel && mouseX >= detX && mouseX <= detX + 278.0f && mouseY >= detY && mouseY <= detY + 196.0f) {
            detScroll -= (float) verticalAmount * 18.0f;
            return true;
        }
        float uiScale = getScale();
        float mx = (float) mouseX / uiScale;
        float my = (float) mouseY / uiScale;
        for (int i = panels.size() - 1; i >= 0; i--) {
            CategoryPanel p = panels.get(i);
            if (p.expanded && mx >= p.x && mx <= p.x + PANEL_WIDTH && my >= p.y) {
                p.scrollOffset -= (float) verticalAmount * 18.0f;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.getKeycode();
        if (listeningBind != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                listeningBind.unbind();
            } else {
                listeningBind.setValue(keyCode);
            }
            listeningBind = null;
            DGClient.getInstance().syncGuiKeybindFromModule();
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            return true;
        }
        if (profileInputFocused) {
            if (keyCode == GLFW.GLFW_KEY_ENTER && !newProfileName.isBlank()) {
                DGClient.getInstance().getConfigManager().saveProfile(newProfileName.trim());
                newProfileName = "";
                profileInputFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !newProfileName.isEmpty()) {
                newProfileName = newProfileName.substring(0, newProfileName.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                profileInputFocused = false;
                return true;
            }
            return true;
        }
        if (editingString != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingString = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !editingString.getValue().isEmpty()) {
                String cur = editingString.getValue();
                editingString.setValue(cur.substring(0, cur.length() - 1));
                return true;
            }
            return true;
        }
        if (editingList != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                if (!listDraft.isBlank()) {
                    editingList.addItem(listDraft.trim());
                }
                listDraft = "";
                editingList = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingList = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !listDraft.isEmpty()) {
                listDraft = listDraft.substring(0, listDraft.length() - 1);
                return true;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_SLASH) {
            searchFocused = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        ClickGuiModule guiMod = DGClient.getInstance().getModuleManager().get(ClickGuiModule.class);
        int guiKey = guiMod != null ? guiMod.getKeybind().getValue() : GLFW.GLFW_KEY_RIGHT_SHIFT;
        if (keyCode == guiKey || keyCode == DGClient.getInstance().getBoundGuiKeyCode()) {
            // Debounce against same-keypress / same-frame bounce when opening ClickGUI
            if (System.nanoTime() - openedAtNano >= 50_000_000L) {
                close();
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        String str = input.asString();
        if (str.isEmpty() || !input.isValidChar()) {
            return false;
        }
        char chr = str.charAt(0);
        if (chr < 32 || chr == 127) {
            return false;
        }
        if (searchFocused) {
            if (searchQuery.length() < 36) searchQuery += chr;
            return true;
        }
        if (profileInputFocused) {
            if (newProfileName.length() < 24) newProfileName += chr;
            return true;
        }
        if (editingString != null) {
            editingString.setValue(editingString.getValue() + chr);
            return true;
        }
        if (editingList != null) {
            if (listDraft.length() < 48) listDraft += chr;
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        ConfigManager cm = DGClient.getInstance().getConfigManager();
        for (CategoryPanel p : panels) {
            cm.setPanelPosition(p.category.name(), p.x, p.y, p.expanded);
        }
        DGClient.getInstance().syncGuiKeybindFromModule();
        DGClient.getInstance().drainGuiKeyBindings();
        cm.saveAll();
        super.close();
    }

    private void focusPanel(CategoryPanel panel) {
        focusedPanel = panel;
        panels.remove(panel);
        panels.add(panel);
    }

    private List<Module> getFilteredModules(ModuleCategory cat) {
        List<Module> base = DGClient.getInstance().getModuleManager().getByCategory(cat);
        if (searchQuery.isBlank()) {
            return base;
        }
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module m : base) {
            if (m.getName().toLowerCase(Locale.ROOT).contains(q) || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(m);
            }
        }
        return out;
    }

    private float measureModulesHeight(List<Module> modules, float animSpeed) {
        float h = 4.0f;
        for (Module m : modules) {
            h += MODULE_HEIGHT;
            float exp = MathUtil.easeOutCubic(m.getExpandAnim());
            if (exp > 0.01f) {
                h += measureDrawerHeight(m) * exp + 2.0f;
            }
        }
        return h;
    }

    private float measureDrawerHeight(Module mod) {
        float h = 20.0f; // padding + keybind row
        for (SettingGroup group : mod.getSettingGroups()) {
            List<Setting<?>> vis = group.getSettings().stream().filter(Setting::isVisible).toList();
            if (vis.isEmpty()) continue;
            if (!group.getName().equals("General") || mod.getSettingGroups().size() > 1) {
                h += 13.5f;
                if (!group.isExpanded()) continue;
            }
            for (Setting<?> s : vis) {
                h += getSettingHeight(s) + 2.0f;
            }
        }
        return h;
    }

    private float getSettingHeight(Setting<?> s) {
        if (s instanceof IntSetting || s instanceof DoubleSetting) return 17.5f;
        if (s instanceof StringSetting) return 24.0f;
        if (s instanceof ColorSetting cs) {
            return Boolean.TRUE.equals(openColorPickers.get(cs)) ? 82.0f : 14.5f;
        }
        if (s instanceof StringListSetting sls) {
            return 24.5f + sls.getItems().size() * 12.5f;
        }
        return 14.0f;
    }
}
