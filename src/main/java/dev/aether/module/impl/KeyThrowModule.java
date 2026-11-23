package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class KeyThrowModule extends Module {
    private final KeybindSetting pearlKey = this.addSetting(new KeybindSetting("Pearl Key", -1));
    private final KeybindSetting windChargeKey = this.addSetting(new KeybindSetting("Wind Charge Key", -1));
    private int originalSlot = -1;

    public KeyThrowModule() {
        super("key-throw", "Throws a pearl or a wind charge with a key and swaps back.", Category.COMBAT);
    }

    @Override
    public boolean onKeyPress(int keyCode) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.getInventory() == null) {
            return false;
        }
        Inventory inventory = player.getInventory();

        if (this.pearlKey.matches(keyCode)) {
            int slot = findSlot(inventory, Items.ENDER_PEARL);
            if (slot >= 0) {
                this.swapAndThrow(player, slot, InteractionHand.MAIN_HAND);
                return true;
            }
        }
        if (this.windChargeKey.matches(keyCode)) {
            int slot = findSlot(inventory, Items.WIND_CHARGE);
            if (slot >= 0) {
                this.swapAndThrow(player, slot, InteractionHand.MAIN_HAND);
                return true;
            }
        }
        return false;
    }

    private int findSlot(Inventory inventory, net.minecraft.world.item.Item item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private void swapAndThrow(LocalPlayer player, int slot, InteractionHand hand) {
        int selected = player.getInventory().getSelected();
        player.getInventory().swapItem(slot, selected);
        player.swing(hand);
        this.originalSlot = selected;
    }

    @Override
    public void onTick() {
        if (this.originalSlot >= 0) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player != null && player.getInventory() != null) {
                player.getInventory().setSelected(this.originalSlot);
                this.originalSlot = -1;
            }
        }
    }
}
