/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.render.Render3DEvent
 *  meteordevelopment.meteorclient.renderer.ShapeMode
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.ColorSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnumSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.meteorclient.utils.render.MeteorToast$Builder
 *  meteordevelopment.meteorclient.utils.render.color.Color
 *  meteordevelopment.meteorclient.utils.render.color.SettingColor
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.class_1802
 *  net.minecraft.class_3414
 *  net.minecraft.class_3417
 *  net.minecraft.class_368
 */
package de.donutlarp.modules;

import de.donutlarp.BaseStore;
import de.donutlarp.DonutLarp;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.class_1802;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_368;

public class DLSusFinder
extends Module {
    private final SettingGroup sgGeneral;
    private final SettingGroup sgRender;
    private final Setting<Integer> areaRadius;
    private final Setting<Integer> fuzz;
    private final Setting<Boolean> toast;
    private final Setting<Boolean> chat;
    private final Setting<Boolean> sound;
    private final Setting<Integer> maxDistance;
    private final Setting<Integer> yLevel;
    private final Setting<ShapeMode> shapeMode;
    private final Setting<SettingColor> sideColor;
    private final Setting<SettingColor> lineColor;

    public DLSusFinder() {
        super(DonutLarp.CATEGORY, "DL-sus-finder", "Marks the rough area where a base is on DonutLarp.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.sgRender = this.settings.createGroup("Render");
        this.areaRadius = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("area-radius")).description("Size of the sus area in chunks around its centre (2 = 5x5 chunks).")).defaultValue((Object)2)).range(1, 8).sliderRange(1, 8).build());
        this.fuzz = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("fuzz")).description("How far (chunks) the area centre may be shifted away from the real base.")).defaultValue((Object)2)).range(0, 8).sliderRange(0, 8).build());
        this.toast = this.sgGeneral.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("achievement-toast")).description("Shows an achievement message when a sus area is found.")).defaultValue((Object)true)).build());
        this.chat = this.sgGeneral.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("chat-message")).description("Also writes the sus area into the chat.")).defaultValue((Object)false)).build());
        this.sound = this.sgGeneral.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("sound")).description("Plays a sound when a sus area is found.")).defaultValue((Object)true)).build());
        this.maxDistance = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("max-distance")).description("Only render sus areas within this many chunks.")).defaultValue((Object)40)).min(1).sliderRange(4, 64).build());
        this.yLevel = this.sgRender.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("y-level")).description("Height the sus area is rendered at.")).defaultValue((Object)63)).range(-64, 320).sliderRange(-64, 320).build());
        this.shapeMode = this.sgRender.add((Setting)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode")).description("How the area is rendered.")).defaultValue((Object)ShapeMode.Both)).build());
        this.sideColor = this.sgRender.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("side-color")).description("Fill color of the sus area.")).defaultValue(new SettingColor(255, 60, 60, 35)).build());
        this.lineColor = this.sgRender.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("line-color")).description("Outline color of the sus area.")).defaultValue(new SettingColor(255, 70, 70, 220)).build());
        BaseStore.onNewBase(b -> {
            if (this.isActive()) {
                this.announce((BaseStore.Base)b);
            }
        });
    }

    private int[] center(BaseStore.Base b) {
        int f = (Integer)this.fuzz.get();
        long h = (long)b.id() * -7046029254386353131L;
        int ox = f == 0 ? 0 : (int)Math.floorMod(h >>> 7, 2L * (long)f + 1L) - f;
        int oz = f == 0 ? 0 : (int)Math.floorMod(h >>> 29, 2L * (long)f + 1L) - f;
        return new int[]{b.chunkX() + ox, b.chunkZ() + oz};
    }

    private void announce(BaseStore.Base b) {
        int[] c = this.center(b);
        int bx = c[0] * 16 + 8;
        int bz = c[1] * 16 + 8;
        if (((Boolean)this.toast.get()).booleanValue() && this.mc.method_1566() != null) {
            this.mc.method_1566().method_1999((class_368)new MeteorToast.Builder("Sus Area Detected").icon(class_1802.field_27070).text("around X " + bx + "  Z " + bz).build());
        }
        if (((Boolean)this.chat.get()).booleanValue()) {
            this.info("Sus area around (highlight)%d(default), (highlight)%d(default).", new Object[]{bx, bz});
        }
        if (((Boolean)this.sound.get()).booleanValue() && this.mc.field_1724 != null) {
            this.mc.field_1724.method_5783((class_3414)class_3417.field_14793.comp_349(), 0.6f, 0.8f);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (this.mc.field_1724 == null) {
            return;
        }
        int pcx = this.mc.field_1724.method_31477() >> 4;
        int pcz = this.mc.field_1724.method_31479() >> 4;
        int max = (Integer)this.maxDistance.get();
        int r = (Integer)this.areaRadius.get();
        double y = ((Integer)this.yLevel.get()).intValue();
        for (BaseStore.Base b : BaseStore.all()) {
            int[] c = this.center(b);
            if (Math.abs(c[0] - pcx) > max || Math.abs(c[1] - pcz) > max) continue;
            double x1 = (c[0] - r) * 16;
            double z1 = (c[1] - r) * 16;
            double x2 = (c[0] + r + 1) * 16;
            double z2 = (c[1] + r + 1) * 16;
            event.renderer.box(x1, y, z1, x2, y + 0.05, z2, (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
        }
    }

    public String getInfoString() {
        return Integer.toString(BaseStore.size());
    }
}

