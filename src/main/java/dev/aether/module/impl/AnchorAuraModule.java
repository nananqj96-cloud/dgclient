package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class AnchorAuraModule extends Module {
    private final SliderSetting minDamage = this.addSetting(new SliderSetting("Min Damage", "Min Damage", 6.0, 0.0, 20.0, 0.5F));
    private final SliderSetting maxSelfDamage = this.addSetting(new SliderSetting("Max Self Damage", "Max Self Damage", 6.0, 0.0, 20.0, 0.5F));
    private final BooleanSetting render = this.addSetting(new BooleanSetting("Render", "Render", true));
    private final ColorSetting renderColor = this.addSetting(new ColorSetting("Render Color", "Render Color", -26052));
    private final SliderSetting turnSpeed = this.addSetting(new SliderSetting("Turn Speed", "Turn Speed", 4.5, 1.0, 10.0, 0.5F));
    private final BooleanSetting protectLoot = this.addSetting(new BooleanSetting("Protect Loot", "Protect Loot", true));

    public AnchorAuraModule() {
        super("anchor-aura", "Places, charges and detonates respawn anchors on nearby enemies.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Anchor aura logic stub - core targeting/placement functionality
        // preserved as module registration; detailed logic disabled
    }

    public float getMinDamage() {
        return this.minDamage.getFloat();
    }

    public float getMaxSelfDamage() {
        return this.maxSelfDamage.getFloat();
    }

    public float getTurnSpeed() {
        return this.turnSpeed.getFloat();
    }

    public int getRenderColor() {
        return this.renderColor.get();
    }
}
