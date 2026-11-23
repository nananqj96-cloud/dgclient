package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class AutoEatModule extends Module {
    private final SliderSetting hunger = this.addSetting(new SliderSetting("Hunger", 16.0, 0.0, 20.0, 1.0F));
    private final SliderSetting health = this.addSetting(new SliderSetting("Health", 10.0, 0.0, 20.0, 1.0F));

    public AutoEatModule() {
        super("auto-eat", "Eats hotbar or offhand food once hunger or health drops too low.", Category.MISC);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Auto eat logic stub - core food eating functionality
        // preserved as module registration; detailed eating logic disabled
    }

    public float getHunger() {
        return this.hunger.getFloat();
    }

    public float getHealth() {
        return this.health.getFloat();
    }
}
