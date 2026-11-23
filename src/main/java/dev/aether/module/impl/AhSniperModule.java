package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class AhSniperModule extends Module {
    private final ModeSetting enchantMatch = this.addSetting(new ModeSetting("Enchant Match", "Exact", "Exact", "Any", "Ignore"));
    private final SliderSetting minHours = this.addSetting(new SliderSetting("Min Time Left (h)", 0.0, 0.0, 48.0, 0.5F));
    private final SliderSetting refresh = this.addSetting(new SliderSetting("Refresh Every (ms)", 300.0, 100.0, 2000.0, 50.0F));
    private final SliderSetting settle = this.addSetting(new SliderSetting("Wait After Page Change (ms)", 200.0, 50.0, 1000.0, 25.0F));
    private final BooleanSetting requireConfirm = this.addSetting(new BooleanSetting("Only Buy With Confirm", false));
    private final SliderSetting maxBuys = this.addSetting(new SliderSetting("Stop After (buys, 0 = never)", 0.0, 0.0, 100.0, 1.0F));
    private final BooleanSetting autoSell = this.addSetting(new BooleanSetting("Auto Sell", false));

    public AhSniperModule() {
        super("ah-sniper", "Refreshes the AH and instantly buys your item below your price, with optional auto selling.", Category.DONUT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // AH sniper logic stub - core auction house sniping functionality
        // preserved as module registration; detailed logic disabled
    }

    public String getEnchantMatch() {
        return this.enchantMatch.get();
    }

    public float getRefresh() {
        return this.refresh.getFloat();
    }

    public float getMaxBuys() {
        return this.maxBuys.getFloat();
    }

    public boolean getAutoSell() {
        return this.autoSell.get();
    }
}
