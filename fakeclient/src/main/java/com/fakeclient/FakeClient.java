/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.Settings
 *  com.fakeclient.command.PayCommand
 *  com.fakeclient.gui.ClickGuiScreen
 *  com.fakeclient.gui.Gui
 *  com.fakeclient.gui.LicenseScreen
 *  com.fakeclient.gui.SplashScreen
 *  com.fakeclient.license.License
 *  com.fakeclient.module.Module
 *  com.fakeclient.module.ModuleManager
 *  com.fakeclient.render.WorldRenderDispatcher
 *  com.fakeclient.util.FakeOnline
 *  com.fakeclient.util.OnlineHarvest
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
 *  net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
 *  net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
 *  net.minecraft.class_1041
 *  net.minecraft.class_304
 *  net.minecraft.class_304$class_11900
 *  net.minecraft.class_310
 *  net.minecraft.class_3675
 *  net.minecraft.class_3675$class_307
 *  net.minecraft.class_437
 *  net.minecraft.class_442
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.fakeclient;

import com.fakeclient.Settings;
import com.fakeclient.command.PayCommand;
import com.fakeclient.gui.ClickGuiScreen;
import com.fakeclient.gui.Gui;
import com.fakeclient.gui.LicenseScreen;
import com.fakeclient.gui.SplashScreen;
import com.fakeclient.license.License;
import com.fakeclient.module.Module;
import com.fakeclient.module.ModuleManager;
import com.fakeclient.render.WorldRenderDispatcher;
import com.fakeclient.util.FakeOnline;
import com.fakeclient.util.OnlineHarvest;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.class_1041;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_437;
import net.minecraft.class_442;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeClient
implements ClientModInitializer {
    public static final String MOD_ID = "fakeclient";
    public static final String NAME = "Fake Client";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"Fake Client");
    public static final ModuleManager MODULES = new ModuleManager();
    private static class_304 openGuiKey;
    private static final int RECHECK_TICKS = 72000;
    private final Set<Integer> heldBinds = new HashSet<Integer>();
    private int licenseTick;
    private boolean licensePrompted;
    private boolean greeted;

    public static ModuleManager modulesOrNull() {
        return MODULES;
    }

    public static int menuKeyCode() {
        if (openGuiKey == null) {
            return -1;
        }
        return class_3675.method_15981((String)openGuiKey.method_1428()).method_1444();
    }

    public void onInitializeClient() {
        LOGGER.info("[{}] Initializing...", (Object)NAME);
        openGuiKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.fakeclient.opengui", class_3675.class_307.field_1668, 344, class_304.class_11900.field_62556));
        License.get().revalidate();
        PayCommand.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            License license = License.get();
            if (++this.licenseTick >= 72000) {
                this.licenseTick = 0;
                license.revalidate();
            }
            boolean licensed = license.isValid();
            while (openGuiKey.method_1436()) {
                client.method_1507((class_437)(licensed ? new ClickGuiScreen() : new LicenseScreen(client.field_1755)));
            }
            if (!licensed) {
                MODULES.disableAll();
                if (!this.licensePrompted && client.field_1755 instanceof class_442) {
                    this.licensePrompted = true;
                    client.method_1507((class_437)new LicenseScreen(client.field_1755));
                }
            } else {
                this.licensePrompted = false;
                if (!this.greeted && client.field_1755 instanceof class_442) {
                    this.greeted = true;
                    client.method_1507((class_437)new SplashScreen(client.field_1755));
                }
            }
            if (client.field_1687 == null) {
                FakeOnline.forget();
                OnlineHarvest.forget();
            } else {
                OnlineHarvest.tick();
            }
            this.pollModuleHotkeys(client);
            MODULES.tickEnabled();
        });
        WorldRenderEvents.AFTER_ENTITIES.register(new WorldRenderDispatcher());
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            class_310 mc = class_310.method_1551();
            if (mc.field_1690.field_1842 || !Settings.get().watermark) {
                return;
            }
            int accent = Gui.accent() & 0xFFFFFF;
            ctx.method_51439(mc.field_1772, Gui.bold((String)NAME), 4, 4, 0xFF000000 | accent, true);
            int y = 15;
            for (Module m : MODULES.getModules()) {
                if (!m.isEnabled()) continue;
                ctx.method_51439(mc.field_1772, Gui.font((String)m.getName()), 4, y, -3158065, true);
                y += 11;
            }
        });
        LOGGER.info("[{}] Ready. Press RIGHT SHIFT to open the menu.", (Object)NAME);
    }

    private void pollModuleHotkeys(class_310 client) {
        for (Module m : MODULES.getModules()) {
            int k = m.getKey();
            if (k <= 0) continue;
            boolean down = class_3675.method_15987((class_1041)client.method_22683(), (int)k);
            boolean was = this.heldBinds.contains(k);
            if (down) {
                this.heldBinds.add(k);
            } else {
                this.heldBinds.remove(k);
            }
            if (!down || was || client.field_1755 != null) continue;
            m.toggle();
            MODULES.saveState();
        }
    }
}
