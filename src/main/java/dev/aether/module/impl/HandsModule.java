package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class HandsModule extends Module {
    private final BooleanSetting link = this.addSetting(new BooleanSetting("Link Hands", "Link Hands", false));
    private final ModeSetting editing = this.addSetting(new ModeSetting("Editing", "Editing", "Main Hand", "Main Hand", "Offhand"));
    private final SliderSetting glide = this.addSetting(new SliderSetting("Glide", "Glide", 0.4, 0.0, 1.0, 0.05F));
    private final SliderSetting offsetX = this.addSetting(new SliderSetting("Offset X", "Offset X", 0.0, -1.0, 1.0, 0.05F));
    private final SliderSetting offsetY = this.addSetting(new SliderSetting("Offset Y", "Offset Y", 0.0, -1.0, 1.0, 0.05F));
    private final SliderSetting offsetZ = this.addSetting(new SliderSetting("Offset Z", "Offset Z", 0.0, -1.0, 1.0, 0.05F));

    public HandsModule() {
        super("hand-pose", "Moves, rotates and resizes your first-person hands.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        // Hand pose rendering stub - core hand positioning functionality
        // preserved as module registration; detailed rendering disabled
    }

    public boolean getLink() {
        return this.link.get();
    }

    public String getEditing() {
        return this.editing.get();
    }

    public float getGlide() {
        return this.glide.getFloat();
    }
}
