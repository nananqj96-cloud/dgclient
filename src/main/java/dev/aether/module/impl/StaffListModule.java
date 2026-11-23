package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;

public class StaffListModule extends Module {
    private final BooleanSetting regionAlert = this.addSetting(new BooleanSetting("Region Alert", true));

    public StaffListModule() {
        super("staff-list", "Lists online DonutSMP staff.", Category.DONUT);
    }

    @Override
    public void onTick() {
    }
}
