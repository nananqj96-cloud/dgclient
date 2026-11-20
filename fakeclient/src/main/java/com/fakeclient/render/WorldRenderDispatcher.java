/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.module.Module
 *  com.fakeclient.render.WorldSpaceRenderer
 *  net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
 *  net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents$AfterEntities
 *  net.minecraft.class_11659
 *  net.minecraft.class_310
 */
package com.fakeclient.render;

import com.fakeclient.FakeClient;
import com.fakeclient.module.Module;
import com.fakeclient.render.WorldSpaceRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.class_11659;
import net.minecraft.class_310;

public final class WorldRenderDispatcher
implements WorldRenderEvents.AfterEntities {
    public void afterEntities(WorldRenderContext context) {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1687 == null || context.matrices() == null) {
            return;
        }
        class_11659 queue = context.commandQueue();
        float tickDelta = mc.method_61966().method_60637(true);
        for (Module m : FakeClient.MODULES.getModules()) {
            if (!m.isEnabled() || !(m instanceof WorldSpaceRenderer)) continue;
            WorldSpaceRenderer r = (WorldSpaceRenderer)m;
            r.renderWorld(context, queue, tickDelta);
        }
    }
}
