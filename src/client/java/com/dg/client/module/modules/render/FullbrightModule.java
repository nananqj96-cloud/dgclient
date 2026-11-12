package com.dg.client.module.modules.render;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.EnumSetting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Eliminates darkness underground and at night using either lightmap gamma boost
 * or client-side Night Vision status effect.
 */
public final class FullbrightModule extends Module {
    public enum Mode {
        GAMMA,
        NIGHT_VISION
    }

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>(
            "Mode", "Brightness override technique", Mode.GAMMA));
    public final DoubleSetting strength = addSetting(new DoubleSetting(
            "Strength", "Minimum light brightness floor in Gamma mode", 1.0, 0.3, 1.0, 0.05)
            .visibleWhen(() -> mode.getValue() == Mode.GAMMA));

    private boolean appliedEffect = false;

    public FullbrightModule() {
        super("Fullbright", "Illuminates dark caves and night terrain via Gamma or Night Vision", ModuleCategory.RENDER);
    }

    @Override
    public String getDisplayInfo() {
        return mode.getValue() == Mode.GAMMA ? "Gamma" : "NV";
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null) {
            return;
        }
        if (mode.getValue() == Mode.NIGHT_VISION) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 420, 0, false, false, false));
            appliedEffect = true;
        } else if (appliedEffect) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            appliedEffect = false;
        }
    }

    @Override
    protected void onDisable() {
        if (appliedEffect && mc.player != null) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        appliedEffect = false;
    }
}
