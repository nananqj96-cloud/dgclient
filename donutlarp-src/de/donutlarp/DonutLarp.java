/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  meteordevelopment.meteorclient.addons.MeteorAddon
 *  meteordevelopment.meteorclient.systems.modules.Category
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.meteorclient.systems.modules.Modules
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
 *  net.minecraft.class_1802
 *  org.slf4j.Logger
 */
package de.donutlarp;

import com.mojang.logging.LogUtils;
import de.donutlarp.ActivityPayload;
import de.donutlarp.ActivityStore;
import de.donutlarp.BaseStore;
import de.donutlarp.BasesPayload;
import de.donutlarp.modules.DLActivityInspector;
import de.donutlarp.modules.DLChunkFinder;
import de.donutlarp.modules.DLSusFinder;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.class_1802;
import org.slf4j.Logger;

public class DonutLarp
extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("DonutLarp", class_1802.field_8494.method_7854());

    public void onInitialize() {
        LOG.info("Initializing DonutLarp");
        Modules.get().add((Module)new DLChunkFinder());
        Modules.get().add((Module)new DLSusFinder());
        Modules.get().add((Module)new DLActivityInspector());
        PayloadTypeRegistry.playS2C().register(BasesPayload.ID, BasesPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(BasesPayload.ID, (payload, context) -> context.client().execute(() -> BaseStore.handle(payload.data())));
        PayloadTypeRegistry.playS2C().register(ActivityPayload.ID, ActivityPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ActivityPayload.ID, (payload, context) -> context.client().execute(() -> ActivityStore.handle(payload.data())));
    }

    public void onRegisterCategories() {
        Modules.registerCategory((Category)CATEGORY);
    }

    public String getPackage() {
        return "de.donutlarp";
    }
}

