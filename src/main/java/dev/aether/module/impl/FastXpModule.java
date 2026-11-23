package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FastXpModule extends Module {
    private final SliderSetting delay = this.addSetting(new SliderSetting("Delay", 0.0, 0.0, 3.0, 1.0F));

    public FastXpModule() {
        super("fastxp", "Throws experience bottles faster while use is held.", Category.COMBAT);
    }

//     @EventHandler(priority = EventPriority.LOWEST)
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && mc.screen == null && mc.options.keyUse.isDown()) {
            if (!player.isUsingItem() && holdsXp(player)) {
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

    private static boolean holdsXp(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        return main.is(Items.EXPERIENCE_BOTTLE) || player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE) && !(main.getItem() instanceof BlockItem);
    }
}
