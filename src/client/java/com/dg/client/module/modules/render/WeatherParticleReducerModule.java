package com.dg.client.module.modules.render;

import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.IntSetting;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

/**
 * Boosts FPS in heavy anarchy fights and lag machines by suppressing weather rendering
 * and filtering explosion, smoke, firework, or excessive ambient particles.
 */
public final class WeatherParticleReducerModule extends Module {
    public final BooleanSetting disableWeather = addSetting(new BooleanSetting(
            "Disable Rain & Snow", "Skips rendering rain, snow, and thunder sky darkening", true));
    public final BooleanSetting hideExplosions = addSetting(new BooleanSetting(
            "Filter Explosions", "Suppresses crystal/TNT explosion clouds", true));
    public final BooleanSetting hideSmokeFire = addSetting(new BooleanSetting(
            "Filter Smoke & Campfire", "Suppresses large smoke and campfire plumes", true));
    public final BooleanSetting hideTotemParticles = addSetting(new BooleanSetting(
            "Filter Vanilla Totem Confetti", "Suppresses screen-blocking vanilla totem particles", false));
    public final IntSetting particleKeepPercent = addSetting(new IntSetting(
            "Keep % (Other)", "Percentage of general particles to allow", 50, 0, 100, 5));

    private int rollingCounter = 0;

    public WeatherParticleReducerModule() {
        super("WeatherReducer", "Suppresses rain/snow and heavy particle spam for high FPS", ModuleCategory.RENDER);
    }

    public boolean shouldCancelParticle(ParticleEffect parameters) {
        if (!isEnabled() || parameters == null) {
            return false;
        }
        var type = parameters.getType();
        if (hideExplosions.getValue() && (type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER)) {
            return true;
        }
        if (hideSmokeFire.getValue() && (type == ParticleTypes.LARGE_SMOKE || type == ParticleTypes.SMOKE
                || type == ParticleTypes.CAMPFIRE_COSY_SMOKE || type == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE)) {
            return true;
        }
        if (hideTotemParticles.getValue() && type == ParticleTypes.TOTEM_OF_UNDYING) {
            return true;
        }
        int keep = particleKeepPercent.getValue();
        if (keep <= 0) {
            return true;
        }
        if (keep >= 100) {
            return false;
        }
        rollingCounter = (rollingCounter + 1) % 100;
        return rollingCounter >= keep;
    }
}
