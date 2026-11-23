package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class AutoTrapModule extends Module {
    private final SliderSetting range = this.addSetting(new SliderSetting("Range", 4.0, 1.0, 6.0, 0.5F));
    private final BooleanSetting rotate = this.addSetting(new BooleanSetting("Rotate", true));
    private final SliderSetting perTick = this.addSetting(new SliderSetting("Blocks Per Tick", 4.0, 1.0, 8.0, 1.0F));
    private final BooleanSetting top = this.addSetting(new BooleanSetting("Top Block", true));
    private final BooleanSetting show = this.addSetting(new BooleanSetting("Show Target", true));

    public AutoTrapModule() {
        super("auto-trap", "Boxes the nearest enemy in with obsidian: sides at feet and head height, then the top.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Auto trap logic stub - core trapping functionality
        // preserved as module registration; detailed trapping logic disabled
    }

    public float getRange() {
        return this.range.getFloat();
    }

    public float getPerTick() {
        return this.perTick.getFloat();
    }
}
