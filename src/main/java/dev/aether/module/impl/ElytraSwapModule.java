package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class ElytraSwapModule extends Module {
    private final KeybindSetting key = this.addSetting(new KeybindSetting("Key", "Key", -1));
    private final BooleanSetting swapBackOnLanding = this.addSetting(new BooleanSetting("Swap Back on Landing", "Swap Back on Landing", true));
    private final SliderSetting landingDelay = this.addSetting(new SliderSetting("Landing Delay", "Landing Delay", 4.0, 0.0, 20.0, 1.0F));

    public ElytraSwapModule() {
        super("elytra-swap", "Swaps between your chestplate and elytra with one key.", Category.MOVEMENT);
        this.landingDelay.visibleWhen(this.swapBackOnLanding::get);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Elytra swap logic stub - core elytra/chestplate swap functionality
        // preserved as module registration; detailed swap logic disabled
    }

    public boolean getSwapBackOnLanding() {
        return this.swapBackOnLanding.get();
    }

    public int getLandingDelay() {
        return this.landingDelay.getInt();
    }
}
