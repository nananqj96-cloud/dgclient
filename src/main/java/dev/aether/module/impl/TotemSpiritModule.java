package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class TotemSpiritModule extends Module {
    private final SliderSetting kaTime = this.addSetting(new SliderSetting("Ka Time", "Ka Time", 2.4, 0.6, 6.0, 0.1F));
    private final SliderSetting ascent = this.addSetting(new SliderSetting("Ascent", "Ascent", 1.6, 0.0, 4.0, 0.1F));
    private final ModeSetting look = this.addSetting(new ModeSetting("Look", "Look", "Glass", "Glass", "Wire"));
    private final SliderSetting opacity = this.addSetting(new SliderSetting("Opacity", "Opacity", 0.65, 0.15, 1.0, 0.05F));
    private final BooleanSetting kaPose = this.addSetting(new BooleanSetting("Ka Pose", "Ka Pose", true));
    private final BooleanSetting crest = this.addSetting(new BooleanSetting("Ankh Crest", "Ankh Crest", true));
    private final BooleanSetting seal = this.addSetting(new BooleanSetting("Ground Seal", "Ground Seal", true));
    private final BooleanSetting own = this.addSetting(new BooleanSetting("Own Pops", "Own Pops", false));

    public TotemSpiritModule() {
        super("totem-spirit", "A glowing ghost rises when a player pops a totem.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Totem spirit rendering stub - core detection functionality
        // preserved as module registration; detailed rendering disabled
    }

    public float getKaTime() {
        return this.kaTime.getFloat();
    }

    public float getAscent() {
        return this.ascent.getFloat();
    }

    public String getLook() {
        return this.look.get();
    }

    public float getOpacity() {
        return this.opacity.getFloat();
    }
}
