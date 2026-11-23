package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class AutoWalkModule extends Module {
    private final ModeSetting direction = this.addSetting(new ModeSetting("Direction", "Forward", "Forward", "Backward", "Left", "Right"));
    private final BooleanSetting disableOnInput = this.addSetting(new BooleanSetting("Disable On Input", false));
    private final BooleanSetting disableOnY = this.addSetting(new BooleanSetting("Disable On Y Change", false));
    private final BooleanSetting waitForChunks = this.addSetting(new BooleanSetting("No Unloaded Chunks", true));
    private boolean wasWalking;

    public AutoWalkModule() {
        super("auto-walk", "Automatically walks forward.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        if (this.waitForChunks.get()) {
            int px = player.getBlockX();
            int pz = player.getBlockZ();
            if (!mc.level.getChunkSource().hasChunk(px >> 4, pz >> 4)) {
                return;
            }
        }
        boolean walking = player.zza != 0.0F || player.xxa != 0.0F;
        if (walking && !this.wasWalking) {
            this.wasWalking = true;
        } else if (!walking && this.wasWalking) {
            this.wasWalking = false;
        }
    }
}
