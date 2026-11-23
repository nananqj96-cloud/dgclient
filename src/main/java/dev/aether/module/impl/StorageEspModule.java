package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import java.util.HashMap;
import java.util.Map;

public class StorageEspModule extends Module {
    private final BooleanSetting sTracers = this.addSetting(new BooleanSetting("Tracers", "Tracers", false));
    private final ColorSetting sChestColor = this.addSetting(new ColorSetting("Chest Color", "Chest Color", -11513776));
    private final Map<Long, Integer> layer = new HashMap<>();

    public StorageEspModule() {
        super("storage-esp", "Highlights storage blocks and spawners with per-type colours.", Category.EXPLOIT);
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
        this.layer.clear();
    }

    @Override
    public void onTick() {
    }

    public void found(long pos, int color) {
        this.layer.put(pos, color);
    }

    public void removed(long pos) {
        this.layer.remove(pos);
    }

    public void reset() {
        this.layer.clear();
    }

    public int getChestColor() {
        return this.sChestColor.get();
    }
}
