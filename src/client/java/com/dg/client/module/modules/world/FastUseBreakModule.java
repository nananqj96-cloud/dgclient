package com.dg.client.module.modules.world;

import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.IntSetting;

/**
 * Fast Place / Fast Use & Fast Break with configurable tick delays.
 */
public final class FastUseBreakModule extends Module {
    public final BooleanSetting fastPlace = addSetting(new BooleanSetting(
            "Fast Place / Use", "Reduces right-click item/block use cooldown", true));
    public final IntSetting placeDelayTicks = addSetting(new IntSetting(
            "Use Delay (ticks)", "Ticks between right-click uses (vanilla = 4, 0 = instant)", 0, 0, 4, 1)
            .visibleWhen(fastPlace::getValue));
    public final BooleanSetting xpAndCrystalsOnly = addSetting(new BooleanSetting(
            "XP & Blocks Only", "Applies Fast Use only to blocks, XP bottles, and end crystals", false)
            .visibleWhen(fastPlace::getValue));

    public final BooleanSetting fastBreak = addSetting(new BooleanSetting(
            "Fast Break", "Reduces post-break mining cooldown between blocks", true));
    public final IntSetting breakDelayTicks = addSetting(new IntSetting(
            "Break Delay (ticks)", "Ticks between block breaks (vanilla = 5, 0 = no delay)", 0, 0, 5, 1)
            .visibleWhen(fastBreak::getValue));

    public FastUseBreakModule() {
        super("FastPlace", "Configurable Fast Place / XP Use and Fast Break delays", ModuleCategory.WORLD);
    }

    @Override
    public String getDisplayInfo() {
        return placeDelayTicks.getValue() + "t/" + breakDelayTicks.getValue() + "t";
    }
}
