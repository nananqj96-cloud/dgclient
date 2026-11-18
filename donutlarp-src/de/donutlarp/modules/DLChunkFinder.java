/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.game.GameLeftEvent
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
 *  net.minecraft.class_3417
 *  net.minecraft.class_368
 */
package de.donutlarp.modules;

import de.donutlarp.BaseStore;
import de.donutlarp.DonutLarp;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
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
import net.minecraft.class_3417;
import net.minecraft.class_368;

public class DLChunkFinder
extends Module {
    private final SettingGroup sgGeneral;
    private final SettingGroup sgRender;
    private final Setting<Boolean> toast;
    private final Setting<Boolean> chat;
    private final Setting<Boolean> sound;
    private final Setting<Integer> maxDistance;
    private final Setting<Integer> yLevel;
    private final Setting<Integer> height;
    private final Setting<ShapeMode> shapeMode;
    private final Setting<SettingColor> sideColor;
    private final Setting<SettingColor> lineColor;

    public DLChunkFinder() {
        super(DonutLarp.CATEGORY, "DL-chunk-finder", "Marks a golden chunk above every base on DonutLarp.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.sgRender = this.settings.createGroup("Render");
        this.toast = this.sgGeneral.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("achievement-toast")).description("Shows an achievement message when a base chunk is found.")).defaultValue((Object)true)).build());
        this.chat = this.sgGeneral.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("chat-message")).description("Also writes the found chunk into the chat.")).defaultValue((Object)true)).build());
        this.sound = this.sgGeneral.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("sound")).description("Plays the challenge sound when a base chunk is found.")).defaultValue((Object)true)).build());
        this.maxDistance = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("max-distance")).description("Only render base chunks within this many chunks.")).defaultValue((Object)32)).min(1).sliderRange(4, 64).build());
        this.yLevel = this.sgRender.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("y-level")).description("Height the golden chunk is rendered at.")).defaultValue((Object)64)).range(-64, 320).sliderRange(-64, 320).build());
        this.height = this.sgRender.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("height")).description("Thickness of the golden chunk in blocks.")).defaultValue((Object)1)).range(0, 64).sliderRange(0, 16).build());
        this.shapeMode = this.sgRender.add((Setting)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode")).description("How the chunk is rendered.")).defaultValue((Object)ShapeMode.Both)).build());
        this.sideColor = this.sgRender.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("side-color")).description("Fill color of the chunk.")).defaultValue(new SettingColor(255, 196, 0, 70)).build());
        this.lineColor = this.sgRender.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("line-color")).description("Outline color of the chunk.")).defaultValue(new SettingColor(255, 215, 0, 255)).build());
        BaseStore.onNewBase(b -> {
            if (this.isActive()) {
                this.announce((BaseStore.Base)b);
            }
        });
    }

    private void announce(BaseStore.Base b) {
        int bx = b.chunkX() * 16 + 8;
        int bz = b.chunkZ() * 16 + 8;
        if (((Boolean)this.toast.get()).booleanValue() && this.mc.method_1566() != null) {
            this.mc.method_1566().method_1999((class_368)new MeteorToast.Builder("Base Chunk Found!").icon(class_1802.field_8494).text("X " + bx + "  Z " + bz).build());
        }
        if (((Boolean)this.chat.get()).booleanValue()) {
            this.info("Golden chunk found at (highlight)%d(default), (highlight)%d(default).", new Object[]{bx, bz});
        }
        if (((Boolean)this.sound.get()).booleanValue() && this.mc.field_1724 != null) {
            this.mc.field_1724.method_5783(class_3417.field_15195, 0.6f, 1.2f);
        }
    }

    public void onActivate() {
        if (BaseStore.size() == 0) {
            this.info("Waiting for base data from the server...", new Object[0]);
        } else {
            this.info("%d base chunks known.", new Object[]{BaseStore.size()});
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        BaseStore.clear();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (this.mc.field_1724 == null) {
            return;
        }
        int pcx = this.mc.field_1724.method_31477() >> 4;
        int pcz = this.mc.field_1724.method_31479() >> 4;
        int max = (Integer)this.maxDistance.get();
        double y1 = ((Integer)this.yLevel.get()).intValue();
        double y2 = y1 + Math.max(0.02, (double)((Integer)this.height.get()).intValue());
        for (BaseStore.Base b : BaseStore.all()) {
            int cx = b.chunkX();
            int cz = b.chunkZ();
            if (Math.abs(cx - pcx) > max || Math.abs(cz - pcz) > max) continue;
            double x1 = cx * 16;
            double z1 = cz * 16;
            event.renderer.box(x1, y1, z1, x1 + 16.0, y2, z1 + 16.0, (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
        }
    }

    public String getInfoString() {
        return Integer.toString(BaseStore.size());
    }
}

