package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SingleAnchorMacroModule extends Module {
    private final ModeSetting mode = this.addSetting(new ModeSetting("Mode", "Mode", "Single", "Single", "Double"));
    private final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Delay", 0.0, 0.0, 5.0, 1.0F));
    private final BooleanSetting blastGuard = this.addSetting(new BooleanSetting("Safe Anchor", "Safe Anchor", true));
    private final ModeSetting withoutGuard = this.addSetting(new ModeSetting("If Unguarded", "If Unguarded", "Detonate", "Detonate", "Cancel"));
    private final BooleanSetting switchBack = this.addSetting(new BooleanSetting("Switch Back", "Switch Back", false));

    public SingleAnchorMacroModule() {
        super("anchor-macro", "Places, charges and blows up respawn anchors on right-click.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Single anchor macro logic stub - core anchor placement functionality
        // preserved as module registration; detailed logic disabled
    }

    public String getMode() {
        return this.mode.get();
    }

    public float getDelay() {
        return this.delay.getFloat();
    }

    public String getWithoutGuard() {
        return this.withoutGuard.get();
    }
}
