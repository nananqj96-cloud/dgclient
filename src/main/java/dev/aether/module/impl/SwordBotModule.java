package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SwordBotModule extends Module {
    private final ModeSetting style = this.addSetting(new ModeSetting("Style", "Patient", "Patient", "Aggressive"));
    private final SliderSetting range = this.addSetting(new SliderSetting("Fight Range", 16.0, 6.0, 32.0, 1.0F));
    private final SliderSetting turnSpeed = this.addSetting(new SliderSetting("Turn Speed", 1.0, 0.5, 1.5, 0.05F));
    private final BooleanSetting strafe = this.addSetting(new BooleanSetting("Strafe", true));
    private final BooleanSetting crits = this.addSetting(new BooleanSetting("Jump Crits", true));
    private final BooleanSetting jumpReset = this.addSetting(new BooleanSetting("Jump Reset", true));
    private final SliderSetting eatBelow = this.addSetting(new SliderSetting("Gapple Below HP", 10.0, 0.0, 20.0, 1.0F));

    public SwordBotModule() {
        super("sword-bot", "Fights the nearest enemy player with a sword or axe: keeps out of their reach while charging, times crits, strafes and eats gapples.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Sword bot logic stub - core combat targeting functionality
        // preserved as module registration; detailed combat logic disabled
    }

    public String getStyle() {
        return this.style.get();
    }

    public float getRange() {
        return this.range.getFloat();
    }

    public float getTurnSpeed() {
        return this.turnSpeed.getFloat();
    }
}
