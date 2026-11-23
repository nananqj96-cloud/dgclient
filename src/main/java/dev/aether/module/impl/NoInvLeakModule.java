package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;

public class NoInvLeakModule extends Module {
    private final ModeSetting style = this.addSetting(new ModeSetting("Style", "Style", "Totem", "Totem", "Random", "Empty"));
    private final BooleanSetting fillEmpty = this.addSetting(new BooleanSetting("Fill Empty Slots", "Fill Empty Slots", true));
    private final BooleanSetting streamOnly = this.addSetting(new BooleanSetting("Stream Only", "Stream Only", false));
    private final BooleanSetting borderless = this.addSetting(new BooleanSetting("Borderless Fullscreen", "Borderless Fullscreen", true));
    private final BooleanSetting tooltips = this.addSetting(new BooleanSetting("Hide Tooltips", "Hide Tooltips", true));
    private final BooleanSetting itemName = this.addSetting(new BooleanSetting("Hide Item Name", "Hide Item Name", true));
    private final BooleanSetting selectedSlot = this.addSetting(new BooleanSetting("Hide Selected Slot", "Hide Selected Slot", true));
    private final BooleanSetting hud = this.addSetting(new BooleanSetting("HUD", "HUD", true));

    public NoInvLeakModule() {
        super("no-inv-leak", "Hides your items on screen for screen shares. Stream Only keeps them visible to you alone.", Category.MISC);
    }

    @Override
    public void onTick() {
    }
}
