package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public class RadarModule extends Module {
    private final SliderSetting range = this.addSetting(new SliderSetting("Range", "Range", 48.0, 8.0, 256.0, 1.0F));
    private final SliderSetting size = this.addSetting(new SliderSetting("Size", "Size", 150.0, 80.0, 320.0, 1.0F));
    private final BooleanSetting rotate = this.addSetting(new BooleanSetting("Rotate", "Rotate", true));
    private final ColorSetting dotColor = this.addSetting(new ColorSetting("Player Color", "Player Color", -47814));

    public RadarModule() {
        super("radar", "A round radar of the players around you.", Category.CLIENT);
    }

    public static RadarModule get() {
//         return ModuleRegistry.get(RadarModule.class);
    }

    public static boolean shouldShowHud() {
        RadarModule module = get();
        return module != null && module.isEnabled();
    }

    public float range() {
        return this.range.get();
    }

    public float size() {
        return this.size.get();
    }

    public boolean rotate() {
        return this.rotate.get();
    }

    public int dotColor() {
        return this.dotColor.get();
    }

    public boolean shows(Player player) {
        return !(player instanceof LocalPlayer) && !player.isRemoved() && !player.isDeadOrDying() && !player.isSpectator();
    }
}
