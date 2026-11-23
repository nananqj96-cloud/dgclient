package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class PanicPayModule extends Module {
    private final BooleanSetting sellArmor = this.addSetting(new BooleanSetting("Sell Armor", "Sell Armor", true));
    private final BooleanSetting sellOffhand = this.addSetting(new BooleanSetting("Sell Offhand", "Sell Offhand", true));

    public PanicPayModule() {
        super("panic-pay", "Sells your whole inventory and armor with /sell, then pays all your money to one player.", Category.DONUT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Panic pay logic stub - core sell/pay functionality
        // preserved as module registration; detailed logic disabled
    }

    public boolean getSellArmor() {
        return this.sellArmor.get();
    }

    public boolean getSellOffhand() {
        return this.sellOffhand.get();
    }
}
