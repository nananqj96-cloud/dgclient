package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class FloatingRelicsModule extends Module {
    private final ModeSetting style = this.addSetting(new ModeSetting("Style", "Ankh", "Ankh", "Scarab", "Sand", "Spark"));
    private final SliderSetting trailLength = this.addSetting(new SliderSetting("Trail Length", 1.0, 0.2, 3.0, 0.1F));
    private final SliderSetting count = this.addSetting(new SliderSetting("Count", 6.0, 1.0, 32.0, 1.0F));
    private final SliderSetting size = this.addSetting(new SliderSetting("Size", 0.3, 0.15, 0.75, 0.05F));
    private final SliderSetting spread = this.addSetting(new SliderSetting("Spread", 2.6, 1.2, 6.0, 0.1F));
    private final SliderSetting bounce = this.addSetting(new SliderSetting("Bounce", 0.6, 0.1, 0.95, 0.05F));
    private final SliderSetting drift = this.addSetting(new SliderSetting("Drift", 0.5, 0.0, 2.5, 0.1F));
    private final BooleanSetting blocks = this.addSetting(new BooleanSetting("Block Bounce", true));

    public FloatingRelicsModule() {
        super("floating-relics", "Glowing orbs float around you and leave a trail as they move.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Floating relics rendering stub - core orb/trail functionality
        // preserved as module registration; detailed rendering disabled
    }

    public String getStyle() {
        return this.style.get();
    }

    public float getTrailLength() {
        return this.trailLength.getFloat();
    }

    public float getCount() {
        return this.count.getFloat();
    }

    public float getSize() {
        return this.size.getFloat();
    }
}
