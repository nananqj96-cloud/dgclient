package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class MaceModule extends Module {
    private final BooleanSetting autoSwap = this.addSetting(new BooleanSetting("Auto Swap", true));
    private final ModeSetting pick = this.addSetting(new ModeSetting("Pick", "Damage", "Damage", "Wind Burst"));
    private final BooleanSetting breachSwap = this.addSetting(new BooleanSetting("Breach Swap", false));
    private final BooleanSetting swapBack = this.addSetting(new BooleanSetting("Swap Back", true));
    private final BooleanSetting smashAssist = this.addSetting(new BooleanSetting("Smash Assist", false));
    private final ModeSetting smashTiming = this.addSetting(new ModeSetting("Smash Timing", "Delayed", "Direct", "Delayed"));
    private final SliderSetting minFall = this.addSetting(new SliderSetting("Min Fall", 3.0, 1.5, 30.0, 0.5F));
    private final BooleanSetting mobs = this.addSetting(new BooleanSetting("Mobs", false));

    public MaceModule() {
        super("mace", "Swaps to the strongest mace for smash hits, with an optional smash assist.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Mace logic stub - core mace swap/smash functionality
        // preserved as module registration; detailed combat logic disabled
    }

    public boolean getAutoSwap() {
        return this.autoSwap.get();
    }

    public String getPick() {
        return this.pick.get();
    }

    public boolean getSmashAssist() {
        return this.smashAssist.get();
    }

    public String getSmashTiming() {
        return this.smashTiming.get();
    }

    public float getMinFall() {
        return this.minFall.getFloat();
    }
}
