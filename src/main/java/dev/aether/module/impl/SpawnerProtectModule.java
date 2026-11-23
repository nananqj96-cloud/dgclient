package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SpawnerProtectModule extends Module {
    private final SliderSetting targetStackCount = this.addSetting(new SliderSetting("Stack To Deposit", "Stack To Deposit", 35.0, 1.0, 35.0, 1.0F));
    private final SliderSetting scanRange = this.addSetting(new SliderSetting("Trigger Range", "Trigger Range", 64.0, 16.0, 128.0, 0.1F));
    private final SliderSetting rotationSpeed = this.addSetting(new SliderSetting("Rotation Speed", "Rotation Speed", 15.0, 1.0, 30.0, 0.1F));
    private final BooleanSetting detectBlockUpdates = this.addSetting(new BooleanSetting("Detect Block Updates", "Detect Block Updates", true));
    private final SliderSetting breakRange = this.addSetting(new SliderSetting("Break Range", "Break Range", 5.5, 1.0, 8.0, 0.1F));
    private final SliderSetting blockUpdateThreshold = this.addSetting(new SliderSetting("Block Update Threshold", "Block Update Threshold", 5.0, 1.0, 20.0, 1.0F));
    private final SliderSetting blockUpdateWindow = this.addSetting(new SliderSetting("Block Update Window", "Block Update Window", 60.0, 1.0, 600.0, 1.0F));

    public SpawnerProtectModule() {
        super("spawner-protect", "Protects spawners on block updates; player arrivals only send webhook notifications.", Category.DONUT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Spawner protect logic stub - core spawner protection functionality
        // preserved as module registration; detailed logic disabled
    }

    public float getTargetStackCount() {
        return this.targetStackCount.getFloat();
    }

    public float getScanRange() {
        return this.scanRange.getFloat();
    }

    public boolean getDetectBlockUpdates() {
        return this.detectBlockUpdates.get();
    }
}
