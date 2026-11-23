package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class WeatherNotifierModule extends Module {
    private static final int STABLE_TICKS = 20;
    private final BooleanSetting sound = this.addSetting(new BooleanSetting("Sound", true));
    private ClientLevel level;
    private boolean raining;
    private int changedTicks;

    public WeatherNotifierModule() {
        super("weather-notifier", "Shows an advancement-style toast when it starts raining.", Category.MISC);
    }

    @Override
    protected void onEnable() {
        this.level = null;
        this.raining = false;
        this.changedTicks = 0;
    }

    @Override
    protected void onDisable() {
        this.level = null;
    }

    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel current = mc.level;
        if (current != this.level) {
            this.level = current;
            this.raining = false;
            this.changedTicks = 0;
        }

        if (current != null && !(mc.screen instanceof LevelLoadingScreen)) {
            if (current.isRaining() == this.raining) {
                this.changedTicks = 0;
            } else if (++this.changedTicks >= 20) {
                this.raining = !this.raining;
                this.changedTicks = 0;
                if (this.raining) {
                    mc.getToastManager().addToast(new WeatherNotifierModule.WeatherToast(this.sound.get()));
                }
            }
        } else {
            this.changedTicks = 0;
        }
    }

//         public static final class WeatherToast extends ChallengeToast {
        private static final Component TITLE = Component.literal("Weather Changed!");
        private static final Component MESSAGE = Component.literal("It started raining.");

        WeatherToast(boolean sound) {
            super(new ItemStack(Items.TRIDENT), TITLE, MESSAGE, sound);
        }
    }
}
