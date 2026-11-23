package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

public class HoverTotemModule extends Module {
    private final BooleanSetting hotbarRefill = this.addSetting(new BooleanSetting("Hotbar Refill", "Hotbar Refill", false));
    private final SliderSetting hotbarSlot = this.addSetting(new SliderSetting("Hotbar Slot", "Hotbar Slot", 1.0, 1.0, 9.0, 1.0F));

    public HoverTotemModule() {
        super("hovertotem", "Swaps a hovered totem from any open container into the offhand or selected hotbar slot.", Category.COMBAT);
        this.hotbarSlot.visibleWhen(this.hotbarRefill::get);
    }

//     @EventHandler
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && mc.gameMode != null && !player.hasInfiniteMaterials()) {
            if (mc.screen instanceof AbstractContainerScreen<?> screen && AutoInvTotemModule.clickSafe(player) && !AutoInvTotemModule.busy()) {
                AbstractContainerMenu menu = screen.getMenu();
                Slot hovered = ((AccessorAbstractContainerScreen)screen).anubis$getHoveredSlot();
                if (hovered != null && menu.getCarried().isEmpty() && hovered.getItem().is(Items.TOTEM_OF_UNDYING)) {
                    if (hovered.container != player.getInventory() || hovered.getContainerSlot() != 40) {
                        int swapButton;
                        if (!player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                            swapButton = 40;
                        } else {
                            if (!this.hotbarRefill.get()) {
                                return;
                            }

                            swapButton = this.hotbarSlot.getInt() - 1;
                            if (player.getInventory().getItem(swapButton).is(Items.TOTEM_OF_UNDYING)) {
                                return;
                            }
                        }

                        mc.gameMode.handleInventoryMouseClick(menu.containerId, hovered.index, swapButton, ClickType.SWAP, player);
                    }
                }
            }
        }
    }
}
