package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;

public class HotkeysModule extends Module {
    public HotkeysModule() {
        super("hotkeys", "Lists the modules you have bound to a key, and their keys.", Category.CLIENT);
    }

    public static HotkeysModule get() {
//         return ModuleRegistry.get(HotkeysModule.class);
    }

    public static boolean shouldShowHud() {
        HotkeysModule module = get();
        return module != null && module.isEnabled();
    }
}
