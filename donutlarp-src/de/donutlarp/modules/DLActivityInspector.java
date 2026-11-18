/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.game.GameLeftEvent
 *  meteordevelopment.meteorclient.events.render.Render3DEvent
 *  meteordevelopment.meteorclient.events.world.TickEvent$Post
 *  meteordevelopment.meteorclient.renderer.ShapeMode
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.ColorSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnumSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.meteorclient.utils.render.color.Color
 *  meteordevelopment.meteorclient.utils.render.color.SettingColor
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.class_3414
 *  net.minecraft.class_3417
 */
package de.donutlarp.modules;

import de.donutlarp.ActivityStore;
import de.donutlarp.DonutLarp;
import java.util.HashSet;
import java.util.Set;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.class_3414;
import net.minecraft.class_3417;

public class DLActivityInspector
extends Module {
    private final SettingGroup sgGeneral;
    private final SettingGroup sgRender;
    private final Setting<Boolean> chat;
    private final Setting<Boolean> sound;
    private final Setting<Integer> yLevel;
    private final Setting<Integer> height;
    private final Setting<ShapeMode> shapeMode;
    private final Setting<SettingColor> sideColor;
    private final Setting<SettingColor> lineColor;
    private final Set<Long> seen;

    public DLActivityInspector() {
        super(DonutLarp.CATEGORY, "DL-activity-inspector", "Marks a red chunk where a player is underground on DonutLarp.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.sgRender = this.settings.createGroup("Render");
        this.chat = this.sgGeneral.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("chat-message")).description("Writes into the chat when new activity shows up.")).defaultValue((Object)true)).build());
        this.sound = this.sgGeneral.add((Setting)((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)new BoolSetting.Builder().name("sound")).description("Plays a sound when new activity shows up.")).defaultValue((Object)false)).build());
        this.yLevel = this.sgRender.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("y-level")).description("Height the red chunk is rendered at.")).defaultValue((Object)64)).range(-64, 320).sliderRange(-64, 320).build());
        this.height = this.sgRender.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("height")).description("Thickness of the red chunk in blocks.")).defaultValue((Object)1)).range(0, 64).sliderRange(0, 16).build());
        this.shapeMode = this.sgRender.add((Setting)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode")).description("How the chunk is rendered.")).defaultValue((Object)ShapeMode.Both)).build());
        this.sideColor = this.sgRender.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("side-color")).description("Fill color of the chunk.")).defaultValue(new SettingColor(255, 30, 30, 70)).build());
        this.lineColor = this.sgRender.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("line-color")).description("Outline color of the chunk.")).defaultValue(new SettingColor(255, 40, 40, 255)).build());
        this.seen = new HashSet<Long>();
    }

    public void onActivate() {
        this.seen.clear();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        ActivityStore.clear();
        this.seen.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        HashSet<Long> now = new HashSet<Long>();
        for (ActivityStore.Activity a : ActivityStore.all()) {
            long key = (long)a.chunkX() << 32 ^ (long)a.chunkZ() & 0xFFFFFFFFL;
            now.add(key);
            if (this.seen.contains(key)) continue;
            if (((Boolean)this.chat.get()).booleanValue()) {
                this.info("Activity below chunk (highlight)%d(default), (highlight)%d(default) (%d %s).", new Object[]{a.chunkX() * 16 + 8, a.chunkZ() * 16 + 8, a.players(), a.players() == 1 ? "player" : "players"});
            }
            if (!((Boolean)this.sound.get()).booleanValue() || this.mc.field_1724 == null) continue;
            this.mc.field_1724.method_5783((class_3414)class_3417.field_14624.comp_349(), 0.8f, 0.6f);
        }
        this.seen.clear();
        this.seen.addAll(now);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        double y1 = ((Integer)this.yLevel.get()).intValue();
        double y2 = y1 + Math.max(0.02, (double)((Integer)this.height.get()).intValue());
        for (ActivityStore.Activity a : ActivityStore.all()) {
            double x1 = a.chunkX() * 16;
            double z1 = a.chunkZ() * 16;
            event.renderer.box(x1, y1, z1, x1 + 16.0, y2, z1 + 16.0, (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
        }
    }

    public String getInfoString() {
        return Integer.toString(ActivityStore.all().size());
    }
}

