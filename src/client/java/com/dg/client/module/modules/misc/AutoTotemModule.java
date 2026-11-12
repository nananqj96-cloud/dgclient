package com.dg.client.module.modules.misc;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.EnumSetting;
import com.dg.client.setting.IntSetting;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Smart Offhand / Auto-Totem with health, Elytra flight, and fall-distance danger awareness.
 */
public final class AutoTotemModule extends Module {
    public enum SafeOffhandItem {
        TOTEM_ALWAYS,
        GOLDEN_APPLE,
        END_CRYSTAL
    }

    public final EnumSetting<SafeOffhandItem> safeItem = addSetting(new EnumSetting<>(
            "Safe Offhand", "Item held when above danger health threshold", SafeOffhandItem.TOTEM_ALWAYS));
    public final DoubleSetting totemHealth = addSetting(new DoubleSetting(
            "Totem Health", "Forces Totem into offhand when HP+absorption <= this value", 14.0, 4.0, 36.0, 0.5));
    public final BooleanSetting elytraSafety = addSetting(new BooleanSetting(
            "Force on Elytra/Fall", "Always forces Totem while flying with Elytra or falling > 5m", true));
    public final IntSetting swapDelayTicks = addSetting(new IntSetting(
            "Swap Delay (ticks)", "Minimum ticks between inventory offhand swaps", 1, 0, 10, 1));

    private int delayLeft = 0;

    public AutoTotemModule() {
        super("AutoTotem", "Smart offhand totem management with health, Elytra & fall awareness", ModuleCategory.MISC);
    }

    @Override
    public String getDisplayInfo() {
        if (mc.player == null) return "0";
        return String.valueOf(countItem(Items.TOTEM_OF_UNDYING));
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }
        if (mc.currentScreen instanceof HandledScreen<?> && !(mc.currentScreen instanceof InventoryScreen)) {
            return;
        }
        if (delayLeft > 0) {
            delayLeft--;
            return;
        }

        float totalHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        boolean danger = totalHp <= totemHealth.getValue().floatValue()
                || (elytraSafety.getValue() && (mc.player.isFallFlying() || mc.player.fallDistance > 5.0f));

        Item desired = Items.TOTEM_OF_UNDYING;
        if (!danger) {
            if (safeItem.getValue() == SafeOffhandItem.GOLDEN_APPLE && countItem(Items.ENCHANTED_GOLDEN_APPLE) + countItem(Items.GOLDEN_APPLE) > 0) {
                desired = countItem(Items.ENCHANTED_GOLDEN_APPLE) > 0 ? Items.ENCHANTED_GOLDEN_APPLE : Items.GOLDEN_APPLE;
            } else if (safeItem.getValue() == SafeOffhandItem.END_CRYSTAL && countItem(Items.END_CRYSTAL) > 0) {
                desired = Items.END_CRYSTAL;
            }
        }

        if (mc.player.getOffHandStack().isOf(desired)) {
            return;
        }
        // Fallback to totem if desired item isn't found
        int slot = findInventorySlot(desired);
        if (slot == -1 && desired != Items.TOTEM_OF_UNDYING) {
            slot = findInventorySlot(Items.TOTEM_OF_UNDYING);
        }
        if (slot == -1) {
            return;
        }

        int screenSlot = slot < 9 ? slot + 36 : slot;
        boolean offhandEmpty = mc.player.getOffHandStack().isEmpty();
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, screenSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
        if (!offhandEmpty) {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, screenSlot, 0, SlotActionType.PICKUP, mc.player);
        }
        delayLeft = swapDelayTicks.getValue();
    }

    public int countItem(Item item) {
        if (mc.player == null) return 0;
        int sum = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack st = mc.player.getInventory().getStack(i);
            if (st.isOf(item)) sum += st.getCount();
        }
        return sum;
    }

    private int findInventorySlot(Item item) {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }
}
