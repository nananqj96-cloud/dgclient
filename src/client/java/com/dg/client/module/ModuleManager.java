package com.dg.client.module;

import com.dg.client.DGClient;
import com.dg.client.core.DGLogger;
import com.dg.client.core.NotificationManager;
import com.dg.client.event.EventHandler;
import com.dg.client.event.EventPriority;
import com.dg.client.event.events.KeyEvent;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Central registry for all DGClient modules, handling keybinds, category queries,
 * and automatic crash-isolation when a module throws in an event hook.
 */
public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> byClass = new LinkedHashMap<>();
    private final Map<String, Module> byName = new LinkedHashMap<>();

    public void init() {
        DGClient.getInstance().getEventBus().subscribe(this);
        DGClient.getInstance().getEventBus().setFaultHandler((owner, throwable) -> {
            if (owner instanceof Module faultingModule && faultingModule.isEnabled()) {
                DGLogger.error("Auto-disabling faulting module '" + faultingModule.getName() + "'", throwable);
                faultingModule.setEnabled(false, false);
                DGClient.getInstance().getNotificationManager().push(
                        NotificationManager.Type.WARNING,
                        faultingModule.getName() + " Crashed",
                        "Auto-disabled safely (" + throwable.getClass().getSimpleName() + ")"
                );
            } else {
                DGLogger.error("Event listener exception in " + owner, throwable);
            }
        });
    }

    public void register(Module module) {
        modules.add(module);
        byClass.put(module.getClass(), module);
        byName.put(normalize(module.getName()), module);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        return (T) byClass.get(type);
    }

    public Module getByName(String name) {
        if (name == null) {
            return null;
        }
        Module exact = byName.get(normalize(name));
        if (exact != null) {
            return exact;
        }
        String clean = normalize(name);
        for (Module m : modules) {
            if (normalize(m.getName()).startsWith(clean)) {
                return m;
            }
        }
        return null;
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getByCategory(ModuleCategory category) {
        List<Module> result = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory() == category) {
                result.add(m);
            }
        }
        return result;
    }

    public List<Module> getEnabledModules() {
        List<Module> result = new ArrayList<>();
        for (Module m : modules) {
            if (m.isEnabled()) {
                result.add(m);
            }
        }
        return result;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onKey(KeyEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null || event.getKey() <= 0) {
            return;
        }
        for (Module module : modules) {
            if (!module.getKeybind().isBound() || module.getKeybind().getValue() != event.getKey()) {
                continue;
            }
            if (module.getKeybind().isHoldMode()) {
                if (event.getAction() == GLFW.GLFW_PRESS) {
                    module.setEnabled(true);
                } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                    module.setEnabled(false);
                }
            } else if (event.getAction() == GLFW.GLFW_PRESS) {
                module.toggle();
            }
        }
    }

    private static String normalize(String input) {
        return input.toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "").replace("_", "");
    }
}
