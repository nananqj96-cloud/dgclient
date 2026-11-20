/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.module.Module
 *  com.fakeclient.render.TextureSwaps
 *  com.fakeclient.render.WorldSpaceRenderer
 *  net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
 *  net.minecraft.class_10017
 *  net.minecraft.class_11659
 *  net.minecraft.class_12075
 *  net.minecraft.class_1297
 *  net.minecraft.class_1299
 *  net.minecraft.class_1613
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1935
 *  net.minecraft.class_1937
 *  net.minecraft.class_2246
 *  net.minecraft.class_2338
 *  net.minecraft.class_2338$class_2339
 *  net.minecraft.class_2394
 *  net.minecraft.class_2398
 *  net.minecraft.class_243
 *  net.minecraft.class_310
 *  net.minecraft.class_4587
 *  net.minecraft.class_638
 *  net.minecraft.class_702
 *  net.minecraft.class_839
 *  net.minecraft.class_898
 */
package com.fakeclient.module.modules;

import com.fakeclient.module.Module;
import com.fakeclient.render.TextureSwaps;
import com.fakeclient.render.WorldSpaceRenderer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.class_10017;
import net.minecraft.class_11659;
import net.minecraft.class_12075;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1613;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1935;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2394;
import net.minecraft.class_2398;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_638;
import net.minecraft.class_702;
import net.minecraft.class_839;
import net.minecraft.class_898;

public class FakeSpawner
extends Module
implements WorldSpaceRenderer {
    private static final int SCAN_RADIUS = 12;
    private static final int RESCAN_INTERVAL = 10;
    private static final int IDLE_INTERVAL = 60;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float BASE_SCALE = 0.53125f;
    private static final float SPIN_PER_TICK = 52.0f;
    private final List<class_2338> cages = new ArrayList<class_2338>();
    private final class_1799 spawnerStack = new class_1799((class_1935)class_1802.field_8849);
    private class_1613 display;
    private int rescanCooldown;
    private float spin;
    private class_2338 lastScan;

    public FakeSpawner() {
        super("Fake Spawner", "Yellow glass looks like a spawner.");
        TextureSwaps.exempt((class_1799)this.spawnerStack);
    }

    public class_1799 spawnerStack() {
        return this.spawnerStack;
    }

    protected void onEnable() {
        TextureSwaps.reloadChunks();
    }

    protected void onDisable() {
        this.cages.clear();
        this.lastScan = null;
        TextureSwaps.reloadChunks();
    }

    public void tick() {
        boolean moved;
        class_310 mc = class_310.method_1551();
        class_638 world = mc.field_1687;
        if (world == null || mc.field_1724 == null) {
            return;
        }
        class_2338 at = mc.field_1724.method_24515();
        boolean bl = moved = this.lastScan == null || !this.lastScan.equals((Object)at);
        if (--this.rescanCooldown <= 0 || moved && this.rescanCooldown < 8) {
            this.rescanCooldown = moved ? 10 : 60;
            this.lastScan = at.method_10062();
            this.rescan(world, at);
        }
        class_702 particles = mc.field_1713;
        for (class_2338 pos : this.cages) {
            double x = (double)pos.method_10263() + world.field_9229.method_43058();
            double y = (double)pos.method_10264() + world.field_9229.method_43058();
            double z = (double)pos.method_10260() + world.field_9229.method_43058();
            particles.method_3056((class_2394)class_2398.field_11251, x, y, z, 0.0, 0.0, 0.0);
            particles.method_3056((class_2394)class_2398.field_11240, x, y, z, 0.0, 0.0, 0.0);
        }
        this.spin = (this.spin + 52.0f) % 360.0f;
    }

    private void rescan(class_638 world, class_2338 center) {
        this.cages.clear();
        class_2338.class_2339 p = new class_2338.class_2339();
        for (int x = -12; x <= 12; ++x) {
            for (int y = -12; y <= 12; ++y) {
                for (int z = -12; z <= 12; ++z) {
                    p.method_10103(center.method_10263() + x, center.method_10264() + y, center.method_10260() + z);
                    if (!world.method_8320((class_2338)p).method_27852(class_2246.field_10049)) continue;
                    this.cages.add(p.method_10062());
                }
            }
        }
    }

    public void renderWorld(WorldRenderContext context, class_11659 queue, float tickDelta) {
        if (this.cages.isEmpty()) {
            return;
        }
        class_310 mc = class_310.method_1551();
        class_638 world = mc.field_1687;
        if (world == null) {
            return;
        }
        if (this.display == null || this.display.method_73183() != world) {
            this.display = new class_1613(class_1299.field_6137, (class_1937)world);
        }
        class_898 dispatcher = mc.method_1561();
        class_10017 state = dispatcher.method_72977((class_1297)this.display, tickDelta);
        state.field_61820 = 0xF000F0;
        float scale = 0.53125f;
        float maxDim = Math.max(this.display.method_17681(), this.display.method_17682());
        if (maxDim > 1.0f) {
            scale /= maxDim;
        }
        class_4587 matrices = context.matrices();
        class_12075 camera = context.worldState().field_63082;
        class_243 cam = camera.field_63078;
        for (class_2338 pos : this.cages) {
            matrices.method_22903();
            matrices.method_22904((double)pos.method_10263() - cam.field_1352, (double)pos.method_10264() - cam.field_1351, (double)pos.method_10260() - cam.field_1350);
            class_839.method_55253((class_4587)matrices, (class_11659)queue, (class_10017)state, (class_898)dispatcher, (float)this.spin, (float)scale, (class_12075)camera);
            matrices.method_22909();
        }
    }
}
