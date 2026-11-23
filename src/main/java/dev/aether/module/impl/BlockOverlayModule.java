package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class BlockOverlayModule extends Module {
    private final ModeSetting style = this.addSetting(new ModeSetting("Style", "Style", "Both", "Both", "Outline", "Fill"));
    private final ModeSetting colorMode = this.addSetting(new ModeSetting("Color Mode", "Color Mode", "Theme", "Theme", "Gradient", "Solid", "Rainbow"));
    private final ColorSetting color = this.addSetting(new ColorSetting("Color", "Color", -871878));
    private final ColorSetting secondColor = this.addSetting(new ColorSetting("Second Color", "Second Color", -6480));
    private final BooleanSetting flow = this.addSetting(new BooleanSetting("Flow", "Flow", true));
    private final SliderSetting fillOpacity = this.addSetting(new SliderSetting("Fill Opacity", "Fill Opacity", 20.0, 2.0, 60.0, 1.0F));
    private final SliderSetting width = this.addSetting(new SliderSetting("Width", "Width", 2.0, 1.0, 5.0, 0.5F));
    private final BooleanSetting glow = this.addSetting(new BooleanSetting("Glow", "Glow", true));
    private final BooleanSetting smooth = this.addSetting(new BooleanSetting("Smooth", "Smooth", true));
    private final SliderSetting smoothSpeed = this.addSetting(new SliderSetting("Smooth Speed", "Smooth Speed", 16.0, 4.0, 40.0, 1.0F));
    private final BooleanSetting miningProgress = this.addSetting(new BooleanSetting("Mining Progress", "Mining Progress", true));
    private final BooleanSetting faceOnly = this.addSetting(new BooleanSetting("Face Only", "Face Only", false));
    private final BooleanSetting seeThrough = this.addSetting(new BooleanSetting("See Through", "See Through", false));

    public BlockOverlayModule() {
        super("block-overlay", "A smooth, animated outline and fill on the block you look at.", Category.VISUALS);
        this.color.visibleWhen(() -> !this.colorMode.is("Theme"));
        this.secondColor.visibleWhen(() -> this.colorMode.is("Gradient"));
        this.flow.visibleWhen(() -> !this.colorMode.is("Solid"));
        this.fillOpacity.visibleWhen(() -> !this.style.is("Outline"));
        this.width.visibleWhen(() -> !this.style.is("Fill"));
        this.glow.visibleWhen(() -> !this.style.is("Fill"));
        this.smoothSpeed.visibleWhen(this.smooth::get);
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Block overlay logic stub - core block outline rendering functionality
        // preserved as module registration; detailed rendering disabled
    }

    public String getStyle() {
        return this.style.get();
    }

    public String getColorMode() {
        return this.colorMode.get();
    }

    public int getColor() {
        return this.color.argb();
    }

    public float getFillOpacity() {
        return this.fillOpacity.getFloat();
    }

    public float getWidth() {
        return this.width.getFloat();
    }
}
