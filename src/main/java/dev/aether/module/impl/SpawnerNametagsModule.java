package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SpawnerNametagsModule extends Module {
    private final SliderSetting sRange = this.addSetting(new SliderSetting("Range", 64.0, 8.0, 256.0, 1.0F));
    private final BooleanSetting sRadius = this.addSetting(new BooleanSetting("Activation Radius", true));

    public SpawnerNametagsModule() {
        super("spawner-nametags", "Shows each spawner's mob and activation range over the block.", Category.RENDER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Spawner nametags rendering logic stub - core scanning functionality
        // preserved as module registration; detailed rendering disabled
    }

    public float getRange() {
        return this.sRange.getFloat();
    }

    public boolean getActivationRadius() {
        return this.sRadius.get();
    }
}
