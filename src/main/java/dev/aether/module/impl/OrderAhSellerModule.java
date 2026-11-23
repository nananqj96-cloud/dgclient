package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class OrderAhSellerModule extends Module {
    private final ModeSetting productSlot = this.addSetting(new ModeSetting("Product", "Product", "1", "1", "2", "3", "4", "5"));
    private final SliderSetting minPrice = this.addSetting(new SliderSetting("Min Price", "Min Price", 1000.0, 1.0, 1000000.0, 100.0F));
    private final SliderSetting restock = this.addSetting(new SliderSetting("Restock With Orders", "Restock With Orders", 0.0, 0.0, 1.0, 1.0F));

    public OrderAhSellerModule() {
        super("order-ah-seller", "Sells an item on the Auction House, undercuts other sellers and restocks through orders.", Category.DONUT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Order AH seller logic stub - core auction house trading functionality
        // preserved as module registration; detailed logic disabled
    }

    public String getProduct() {
        return this.productSlot.get();
    }

    public float getMinPrice() {
        return this.minPrice.getFloat();
    }
}
