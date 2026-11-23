package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;

public class NoRenderModule extends Module {
    private static NoRenderModule instance;
    private final BooleanSetting sFireOverlay = this.addSetting(new BooleanSetting("Fire Overlay", true));
    private final BooleanSetting sTotemAnimation = this.addSetting(new BooleanSetting("Totem Animation", true));
    private final BooleanSetting sPumpkinPowderSnow = this.addSetting(new BooleanSetting("Pumpkin & Powder Snow", true));
    private final BooleanSetting sNauseaPortal = this.addSetting(new BooleanSetting("Nausea & Portal", true));
    private final BooleanSetting sBlindnessDarkness = this.addSetting(new BooleanSetting("Blindness & Darkness", true));
    private final BooleanSetting sSpawnerMob = this.addSetting(new BooleanSetting("Spawner Mob", true));
    private final BooleanSetting sFallingBlocks = this.addSetting(new BooleanSetting("Falling Blocks", false));
    private final BooleanSetting sWeather = this.addSetting(new BooleanSetting("Weather", true));
    private final BooleanSetting sLavaFog = this.addSetting(new BooleanSetting("Lava Fog", true));
    private final BooleanSetting sEntityFire = this.addSetting(new BooleanSetting("Entity Fire", false));

    public NoRenderModule() {
        super("no-render", "Hide selected overlays, screen effects and entity renders.", Category.RENDER);
        instance = this;
    }

    public static boolean hideFireOverlay() {
        NoRenderModule module = instance;
        return module != null && module.isEnabled() && module.sFireOverlay.get();
    }

    public static boolean hideTotemAnimation() {
        NoRenderModule module = instance;
        return module != null && module.isEnabled() && module.sTotemAnimation.get();
    }

    public static boolean hidePumpkinPowderSnow() {
        NoRenderModule module = instance;
        return module != null && module.isEnabled() && module.sPumpkinPowderSnow.get();
    }

    public static boolean hideNauseaPortal() {
        NoRenderModule module = instance;
        return module != null && module.isEnabled() && module.sNauseaPortal.get();
    }

    public static boolean hideBlindnessDarkness() {
        NoRenderModule module = instance;
        return module != null && module.isEnabled() && module.sBlindnessDarkness.get();
    }

    public static boolean hideSpawnerMob() {
        NoRenderModule module = instance;
        return module != null && module.isEnabled() && module.sSpawnerMob.get();
    }

    public static boolean hideFallingBlocks() {
        NoRenderModule module = instance;
        return module != null && module.isEnabled() && module.sFallingBlocks.get();
    }

    public static boolean hideWeather() {
        NoRenderModule module = instance;
        return module != null && module.isEnabled() && module.sWeather.get();
    }

    public static boolean hideLavaFog() {
        NoRenderModule module = instance;
        return module != null && module.isEnabled() && module.sLavaFog.get();
    }

    public static boolean hideEntityFire() {
        NoRenderModule module = instance;
        return module != null && module.isEnabled() && module.sEntityFire.get();
    }
}
