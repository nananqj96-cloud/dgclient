package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class AhSellModule extends Module {
    private final ModeSetting count = this.addSetting(new ModeSetting("Items", "Items", "1", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
    private final ModeSetting pricing = this.addSetting(new ModeSetting("Pricing", "Pricing", "Fixed", "Fixed", "Auto (beat cheapest)"));
    private final SliderSetting priceCheck = this.addSetting(new SliderSetting("Price Check (s)", "Price Check (s)", 120.0, 10.0, 600.0, 10.0F));
    private final SliderSetting relistGap = this.addSetting(new SliderSetting("Relist Higher At (x undercut)", "Relist Higher At (x undercut)", 3.0, 1.0, 20.0, 0.5F));
    private final SliderSetting salesCheck = this.addSetting(new SliderSetting("Sales Check (s)", "Sales Check (s)", 120.0, 10.0, 600.0, 10.0F));
    private final SliderSetting perListing = this.addSetting(new SliderSetting("Per Listing", "Per Listing", 1.0, 1.0, 9.0, 1.0F));
    private final SliderSetting maxListings = this.addSetting(new SliderSetting("Max Listings", "Max Listings", 9.0, 1.0, 9.0, 1.0F));
    private final SliderSetting listingDelay = this.addSetting(new SliderSetting("Listing Delay (s)", "Listing Delay (s)", 1.0, 0.0, 10.0, 0.5F));

    public AhSellModule() {
        super("ah-sell", "Lists up to 9 items from your inventory on the Auction House at your price or the cheapest price.", Category.DONUT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // AH sell logic stub - core auction house listing functionality
        // preserved as module registration; detailed logic disabled
    }

    public String getCount() {
        return this.count.get();
    }

    public String getPricing() {
        return this.pricing.get();
    }

    public float getPriceCheck() {
        return this.priceCheck.getFloat();
    }
}
