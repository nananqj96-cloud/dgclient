package com.dg.client.module.modules.render;

import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.EnumSetting;
import com.dg.client.setting.IntSetting;

/**
 * Client-side time-of-day override without altering server TPS estimation.
 */
public final class TimeChangerModule extends Module {
    public enum TimePreset {
        NOON(6000),
        SUNSET(12600),
        MIDNIGHT(18000),
        SUNRISE(23200),
        CUSTOM(-1);

    final int ticks;
        TimePreset(int ticks) {
            this.ticks = ticks;
        }
    }

    public final EnumSetting<TimePreset> preset = addSetting(new EnumSetting<>(
            "Preset", "Time of day preset", TimePreset.MIDNIGHT));
    public final IntSetting customTicks = addSetting(new IntSetting(
            "Custom Time", "Client world time in ticks (0..24000)", 6000, 0, 24000, 200)
            .visibleWhen(() -> preset.getValue() == TimePreset.CUSTOM));
    public final BooleanSetting smoothCycle = addSetting(new BooleanSetting(
            "Slow Ambient Drift", "Gently advances client sun/moon angle", false));

    public TimeChangerModule() {
        super("TimeChanger", "Overrides client-side time of day (Noon, Sunset, Midnight, Custom)", ModuleCategory.RENDER);
    }

    @Override
    public String getDisplayInfo() {
        return preset.getValue().name();
    }

    public long getClientTime() {
        long base = preset.getValue() == TimePreset.CUSTOM ? customTicks.getValue() : preset.getValue().ticks;
        if (smoothCycle.getValue()) {
            base = (base + (System.currentTimeMillis() / 50L)) % 24000L;
        }
        return base;
    }
}
