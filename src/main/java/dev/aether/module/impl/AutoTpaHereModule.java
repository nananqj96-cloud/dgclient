package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;

public class AutoTpaHereModule extends Module {
    private final ModeSetting command = this.addSetting(new ModeSetting("Command", "Command", "tpahere", "tpahere", "tpa"));
    private final ModeSetting order = this.addSetting(new ModeSetting("Order", "Order", "A to Z", "A to Z", "Z to A", "Random"));
    private final BooleanSetting numbers = this.addSetting(new BooleanSetting("Include 0-9 and _", "Include 0-9 and _", false));
    private final BooleanSetting bedrock = this.addSetting(new BooleanSetting("Bedrock Players Only", "Bedrock Players Only", false));
    private final BooleanSetting repeat = this.addSetting(new BooleanSetting("Repeat Forever", "Repeat Forever", true));
    private final SliderSetting delay = this.addSetting(new SliderSetting("Delay (s)", "Delay (s)", 1.0, 0.25, 30.0, 0.25F));
    private final BooleanSetting skipOffline = this.addSetting(new BooleanSetting("Skip Offline Players", "Skip Offline Players", true));
    private final BooleanSetting joinersFirst = this.addSetting(new BooleanSetting("New Players First", "New Players First", false));
    private final SliderSetting sendMinutes = this.addSetting(new SliderSetting("Send For (min, 0 = always)", "Send For (min, 0 = always)", 0.0, 0.0, 240.0, 1.0F));
    private final SliderSetting restMinutes = this.addSetting(new SliderSetting("Then Rest (min)", "Then Rest (min)", 0.0, 0.0, 240.0, 1.0F));

    public AutoTpaHereModule() {
        super("auto-tpahere", "Sends tpahere requests to online players one by one, pauses when someone arrives and fights them. Servers can detect this.", Category.DONUT);
    }

    @Override
    public void onTick() {
    }
}
