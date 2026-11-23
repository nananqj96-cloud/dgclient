package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class PharaohHeaddressModule extends Module {
    private final ModeSetting style = this.addSetting(new ModeSetting("Style", "Style", "Nemes", "Nemes", "Khepresh", "Sun Disc"));
    private final SliderSetting stripes = this.addSetting(new SliderSetting("Stripes", "Stripes", 5.0, 3.0, 9.0, 1.0F));
    private final BooleanSetting collar = this.addSetting(new BooleanSetting("Broad Collar", "Broad Collar", true));
    private final SliderSetting size = this.addSetting(new SliderSetting("Size", "Size", 1.0, 0.8, 1.3, 0.05F));
    private final ModeSetting color = this.addSetting(new ModeSetting("Color", "Color", "Theme", "Theme", "Manual"));
    private final ColorSetting gold = this.addSetting(new ColorSetting("Gold Color", "Gold Color", -7688204));
    private final ColorSetting inlay = this.addSetting(new ColorSetting("Inlay Color", "Inlay Color", -13738055));
    private final ColorSetting gem = this.addSetting(new ColorSetting("Gem Color", "Gem Color", -10313473));
    private final BooleanSetting glint = this.addSetting(new BooleanSetting("Glint", "Glint", true));
    private final BooleanSetting friends = this.addSetting(new BooleanSetting("On Friends", "On Friends", false));

    public PharaohHeaddressModule() {
        super("pharaoh-headdress", "Pharaoh Headdress", "Puts a pharaoh headdress on your player model.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        // Pharaoh headdress rendering stub - core styling functionality
        // preserved as module registration; detailed rendering disabled
    }

    public String getStyle() {
        return this.style.get();
    }

    public float getStripes() {
        return this.stripes.getFloat();
    }

    public float getSize() {
        return this.size.getFloat();
    }

    public String getColor() {
        return this.color.get();
    }

    public int getGoldColor() {
        return this.gold.argb();
    }

    public int getInlayColor() {
        return this.inlay.argb();
    }

    public int getGemColor() {
        return this.gem.argb();
    }
}
