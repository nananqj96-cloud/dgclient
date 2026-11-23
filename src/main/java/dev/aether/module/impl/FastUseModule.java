package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class FastUseModule extends Module {
    private final SliderSetting delay = this.addSetting(new SliderSetting("Delay", 0.0, 0.0, 3.0, 1.0F));
    private final BooleanSetting blocks = this.addSetting(new BooleanSetting("Blocks", true));
    private final BooleanSetting items = this.addSetting(new BooleanSetting("Items", true));

    public FastUseModule() {
        super("fast-use", "Places blocks and uses items faster while use is held.", Category.MISC);
    }

//     @EventHandler(priority = EventPriority.LOWEST)
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && mc.screen == null && mc.options.keyUse.isDown()) {
            if (!player.isUsingItem() && this.covers(player)) {
                AccessorCombatMinecraft access = (AccessorCombatMinecraft)mc;
                int wait = access.anubis$rightClickDelay();
                if (!ServerRotation.pending() && !MeleeAttacks.actedThisTick(player)) {
                    if (wait > this.delay.getInt() + 1) {
                        access.anubis$setRightClickDelay(this.delay.getInt() + 1);
                    }
                } else {
                    access.anubis$setRightClickDelay(Math.max(wait, 2));
                    KeyMapping use = mc.options.keyUse;

                    while (use.consumeClick()) {
                    }
                }
            }
        }
    }

    private boolean covers(LocalPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            held = player.getOffhandItem();
        }

        if (held.isEmpty()) {
            return false;
        } else {
            return held.getItem() instanceof BlockItem ? this.blocks.get() : this.items.get();
        }
    }
}
