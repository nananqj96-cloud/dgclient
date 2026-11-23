package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class RelicParticlesModule extends Module {
    private final BooleanSetting pops = this.addSetting(new BooleanSetting("Totem Pops", "Totem Pops", true));
    private final ModeSetting popGlyph = this.addSetting(new ModeSetting("Pop Glyph", "Pop Glyph", "Mixed", "Mixed", "Ankh", "Scarab", "Sand", "Spark"));
    private final SliderSetting popBurst = this.addSetting(new SliderSetting("Pop Burst", "Pop Burst", 26.0, 4.0, 80.0, 1.0F));
    private final BooleanSetting ownPops = this.addSetting(new BooleanSetting("Own Pops", "Own Pops", true));
    private final BooleanSetting projectiles = this.addSetting(new BooleanSetting("Projectiles", "Projectiles", true));
    private final BooleanSetting pearls = this.addSetting(new BooleanSetting("Pearls", "Pearls", true));
    private final BooleanSetting arrows = this.addSetting(new BooleanSetting("Arrows", "Arrows", true));
    private final BooleanSetting tridents = this.addSetting(new BooleanSetting("Tridents", "Tridents", true));
    private final ColorSetting firstColor = this.addSetting(new ColorSetting("First Color", "First Color", -14992226));
    private final ColorSetting secondColor = this.addSetting(new ColorSetting("Second Color", "Second Color", -14992226));

    public RelicParticlesModule() {
        super("particles", "Egyptian particles for totem pops, projectiles and footsteps.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Relic particles rendering stub - core particle functionality
        // preserved as module registration; detailed rendering disabled
    }

    public String getPopGlyph() {
        return this.popGlyph.get();
    }

    public float getPopBurst() {
        return this.popBurst.getFloat();
    }

    public int getFirstColor() {
        return this.firstColor.argb();
    }

    public int getSecondColor() {
        return this.secondColor.argb();
    }
}
