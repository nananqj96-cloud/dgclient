package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;

public class SprintModule extends Module {
    private static volatile SprintModule active;

    public SprintModule() {
        super("sprint", "Keeps you sprinting while you move forward, like vanilla Toggle Sprint.", Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
    }

    public static boolean holding() {
        return active != null;
    }
}
