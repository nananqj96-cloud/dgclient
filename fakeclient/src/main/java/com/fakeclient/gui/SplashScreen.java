/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.Anim
 *  com.fakeclient.gui.Gui
 *  com.fakeclient.license.License
 *  net.minecraft.class_10799
 *  net.minecraft.class_11908
 *  net.minecraft.class_11909
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 */
package com.fakeclient.gui;

import com.fakeclient.FakeClient;
import com.fakeclient.Settings;
import com.fakeclient.gui.Anim;
import com.fakeclient.gui.Gui;
import com.fakeclient.license.License;
import java.util.Locale;
import net.minecraft.class_10799;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_437;

public class SplashScreen
extends class_437 {
    private static final class_2960 LOGO = class_2960.method_60655((String)"fakeclient", (String)"textures/gui/logo.png");
    private static final float LIFETIME = 5400.0f;
    private static final float FADE_OUT_AT = 4700.0f;
    private static final float LOAD_TIME = 4300.0f;
    private static final int BAR_W = 210;
    private static final int BAR_H = 5;
    private static final int LOGO_D = 104;
    private static final int TEXT = -1;
    private static final int SUBTEXT = -3419946;
    private static final int MUTED = -6578010;
    private final class_437 parent;
    private long openedAt;
    private boolean dismissed;

    public SplashScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470((String)"Fake Client"));
        this.parent = parent;
    }

    protected void method_25426() {
        if (this.openedAt == 0L) {
            this.openedAt = System.currentTimeMillis();
        }
    }

    private float elapsed() {
        return System.currentTimeMillis() - this.openedAt;
    }

    private float stage(float delay, float span) {
        return Anim.easeOutExpo((float)SplashScreen.clamp01((this.elapsed() - delay) / span));
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    public void method_25420(class_332 ctx, int mouseX, int mouseY, float delta) {
        float in = SplashScreen.clamp01(this.elapsed() / 260.0f);
        float out = 1.0f - SplashScreen.clamp01((this.elapsed() - 4700.0f) / 700.0f);
        int a = Math.round(232.0f * in * out);
        ctx.method_25296(0, 0, this.field_22789, this.field_22790, a << 24 | 0x80808, a << 24 | 0);
    }

    protected void method_57734(class_332 ctx) {
        if (Settings.get().menuBlur) {
            super.method_57734(ctx);
        }
    }

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        if (this.elapsed() >= 5400.0f) {
            this.finish();
            return;
        }
        this.method_25420(ctx, mouseX, mouseY, delta);
        float fade = 1.0f - SplashScreen.clamp01((this.elapsed() - 4700.0f) / 700.0f);
        int cx = this.field_22789 / 2;
        int cy = this.field_22790 / 2;
        float logoIn = this.stage(0.0f, 520.0f);
        float pop = Anim.easeOutBack((float)SplashScreen.clamp01(this.elapsed() / 620.0f));
        int lift = Math.round((1.0f - logoIn) * 18.0f);
        int d = Math.round(104.0f * (0.86f + 0.14f * pop));
        int lx = cx - d / 2;
        int ly = cy - d / 2 - 26 + lift;
        float breathe = 0.85f + 0.15f * (float)Math.sin((double)this.elapsed() / 900.0);
        Gui.bloom((class_332)ctx, (int)(cx - 150), (int)(ly - 40), (int)300, (int)260, (int)130, (int)60, (float)(logoIn * fade * 0.5f * breathe));
        Gui.bloom((class_332)ctx, (int)lx, (int)ly, (int)d, (int)d, (int)(d / 2), (int)10, (float)(logoIn * fade));
        Gui.pushScale((class_332)ctx, (int)lx, (int)ly, (int)d, (int)d, (float)1.0f);
        ctx.method_25290(class_10799.field_56883, LOGO, lx, ly, 0.0f, 0.0f, d, d, d, d);
        Gui.pop((class_332)ctx);
        float nameIn = this.stage(260.0f, 460.0f);
        int nameY = cy + d / 2 - 14 + Math.round((1.0f - nameIn) * 10.0f);
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)"Fake Client".toUpperCase(Locale.ROOT)), (int)cx, (int)nameY, (int)Gui.alpha((int)-1, (float)(nameIn * fade)));
        float ruleIn = this.stage(420.0f, 520.0f);
        int ruleW = Math.round(130.0f * ruleIn);
        if (ruleW > 2) {
            ctx.method_25294(cx - ruleW / 2, nameY + 14, cx + ruleW / 2, nameY + 15, Gui.alpha((int)0x59FFFFFF, (float)fade));
        }
        float subIn = this.stage(620.0f, 520.0f);
        License lic = License.get();
        String sub = lic.isValid() ? "Licensed   \u00b7   " + lic.remainingText() : "Simple. Clean. Powerful.";
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)sub), (int)cx, (int)(nameY + 24), (int)Gui.alpha((int)(lic.isValid() ? -3419946 : -6578010), (float)(subIn * fade)));
        int barY = nameY + 40;
        this.renderLoadBar(ctx, cx, barY, fade);
        float hintIn = this.stage(1400.0f, 600.0f);
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)("Press " + Gui.keyName((int)FakeClient.menuKeyCode()) + " in-game to open")), (int)cx, (int)(barY + 34), (int)Gui.alpha((int)-6578010, (float)(hintIn * fade * 0.85f)));
        super.method_25394(ctx, mouseX, mouseY, delta);
    }

    private void renderLoadBar(class_332 ctx, int cx, int y, float fade) {
        float in = this.stage(820.0f, 520.0f);
        if (in <= 0.01f) {
            return;
        }
        int bx = cx - 105;
        int r = 2;
        float progress = Anim.easeInOutCubic((float)SplashScreen.clamp01(this.elapsed() / 4300.0f));
        Gui.roundedRect((class_332)ctx, (int)bx, (int)y, (int)210, (int)5, (int)r, (int)Gui.alpha((int)0x1FFFFFFF, (float)(in * fade)));
        Gui.roundedGradient((class_332)ctx, (int)(bx + 1), (int)(y + 1), (int)208, (int)3, (int)r, (int)Gui.alpha((int)-16448251, (float)(in * fade)), (int)Gui.alpha((int)-15987700, (float)(in * fade)));
        int fillW = Math.round(210.0f * progress);
        if (fillW >= 5) {
            Gui.roundedGradient((class_332)ctx, (int)bx, (int)y, (int)fillW, (int)5, (int)r, (int)Gui.alpha((int)-723722, (float)(in * fade)), (int)Gui.alpha((int)-3683374, (float)(in * fade)));
            float sweep = (float)(0.5 + 0.5 * Math.sin((double)this.elapsed() / 480.0));
            int bandW = Math.max(10, fillW / 4);
            int bandX = bx + Math.round((float)(fillW - bandW) * sweep);
            Gui.roundedRect((class_332)ctx, (int)bandX, (int)y, (int)bandW, (int)5, (int)r, (int)Gui.alpha((int)0x40FFFFFF, (float)(in * fade)));
            Gui.bloom((class_332)ctx, (int)(bx + fillW - 6), (int)(y - 1), (int)8, (int)7, (int)(r + 1), (int)5, (float)(in * fade * 0.9f));
        }
        class_2561 pct = Gui.font((String)(Math.round(progress * 100.0f) + "%"));
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)pct, (int)cx, (int)(y + 13), (int)Gui.alpha((int)-6578010, (float)(in * fade * 0.9f)));
    }

    private void finish() {
        if (this.dismissed) {
            return;
        }
        this.dismissed = true;
        this.field_22787.method_1507(this.parent);
    }

    public boolean method_25402(class_11909 click, boolean doubled) {
        this.finish();
        return true;
    }

    public boolean method_25404(class_11908 input) {
        this.finish();
        return true;
    }

    public void method_25419() {
        this.finish();
    }

    public boolean method_25421() {
        return false;
    }
}
