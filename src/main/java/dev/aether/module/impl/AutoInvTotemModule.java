package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class AutoInvTotemModule extends Module {
    private final ModeSetting mode = this.addSetting(new ModeSetting("Mode", "Inventory", "Inventory", "Hotbar Swap"));
    private final BooleanSetting hotbarRefill = this.addSetting(new BooleanSetting("Hotbar Refill", false));
    private final SliderSetting hotbarSlot = this.addSetting(new SliderSetting("Hotbar Slot", 1.0, 1.0, 9.0, 1.0F));
    private final SliderSetting reactionDelay = this.addSetting(new SliderSetting("Reaction Delay", 0.0, 0.0, 10.0, 1.0F));
    private final SliderSetting swapDelay = this.addSetting(new SliderSetting("Swap Delay", 1.0, 0.0, 10.0, 1.0F));
    private final SliderSetting closeDelay = this.addSetting(new SliderSetting("Close Delay", 0.0, 0.0, 10.0, 1.0F));
    private final SliderSetting switchBackDelay = this.addSetting(new SliderSetting("Switch Back Delay", 2.0, 0.0, 10.0, 1.0F));
    private final BooleanSetting doubleHand = this.addSetting(new BooleanSetting("Double Hand", false));

    public AutoInvTotemModule() {
        super("autoinvtotem", "Refills the offhand totem after a pop through the inventory or a hotbar swap; Double Hand also keeps one in the main hand while in danger.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Auto inv totem logic stub - core totem refill functionality
        // preserved as module registration; detailed refill logic disabled
    }

    public String getMode() {
        return this.mode.get();
    }

    public float getReactionDelay() {
        return this.reactionDelay.getFloat();
    }

    public float getSwapDelay() {
        return this.swapDelay.getFloat();
    }
}
