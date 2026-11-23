package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class OrderDropperModule extends Module {
    private final SliderSetting orderSlot = this.addSetting(new SliderSetting("Order Slot (0 = first)", 0.0, 0.0, 44.0, 1.0F));
    private final SliderSetting pages = this.addSetting(new SliderSetting("Pages To Drop", 1.0, 1.0, 100.0, 1.0F));
    private final SliderSetting delay = this.addSetting(new SliderSetting("Drop Delay (s)", 0.5, 0.25, 5.0, 0.25F));

    public OrderDropperModule() {
        super("order-dropper", "Opens one of your orders, collects it and drops the chosen number of pages, then turns off.", Category.DONUT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Order dropper logic stub - core order opening/dropping functionality
        // preserved as module registration; detailed logic disabled
    }

    public float getOrderSlot() {
        return this.orderSlot.getFloat();
    }

    public float getPages() {
        return this.pages.getFloat();
    }

    public float getDelay() {
        return this.delay.getFloat();
    }
}
