package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class HandModelModule extends Module {
    private final ModeSetting items = this.addSetting(new ModeSetting("Items", "Items", "All Items", "All Items", "Weapons & Tools"));
    private final BooleanSetting mainHand = this.addSetting(new BooleanSetting("Main Hand", "Main Hand", true));
    private final BooleanSetting offHand = this.addSetting(new BooleanSetting("Off Hand", "Off Hand", true));

    public HandModelModule() {
        super("3d-hand", "Shows your own arm holding the item in first person: swords, tools, items and blocks.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Hand model logic stub - core 3D hand rendering functionality
        // preserved as module registration; detailed rendering disabled
    }

    public String getItems() {
        return this.items.get();
    }

    public boolean getMainHand() {
        return this.mainHand.get();
    }

    public boolean getOffHand() {
        return this.offHand.get();
    }
}
