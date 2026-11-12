package com.dg.client.module.modules.misc;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.IntSetting;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Equips highest-scoring armor automatically while avoiding Curse of Binding,
 * preserving chestplate Elytra when desired, and swapping low-durability pieces.
 */
public final class AutoArmorModule extends Module {
    public final BooleanSetting preserveElytra = addSetting(new BooleanSetting(
            "Preserve Elytra", "Does not replace an equipped Elytra with a chestplate", true));
    public final BooleanSetting avoidBindingCurse = addSetting(new BooleanSetting(
            "Avoid Binding Curse", "Never equips items with Curse of Binding", true));
    public final IntSetting minDurabilityPct = addSetting(new IntSetting(
            "Min Durability %", "Treats armor below this durability % as degraded for replacement", 6, 1, 35, 1));
    public final IntSetting equipDelayTicks = addSetting(new IntSetting(
            "Equip Delay (ticks)", "Ticks between armor swaps", 3, 1, 15, 1));

    private int cooldown = 0;

    public AutoArmorModule() {
        super("AutoArmor", "Equips best armor with curse & low-durability protection", ModuleCategory.MISC);
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen instanceof HandledScreen<?>) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int[] handlerArmorSlots = {5, 6, 7, 8};

        for (int sIdx = 0; sIdx < slots.length; sIdx++) {
            EquipmentSlot eqSlot = slots[sIdx];
            ItemStack equipped = mc.player.getEquippedStack(eqSlot);
            if (eqSlot == EquipmentSlot.CHEST && preserveElytra.getValue() && equipped.isOf(Items.ELYTRA)) {
                continue;
            }
            if (hasBindingCurse(equipped)) {
                continue;
            }

            double currentScore = scoreArmor(equipped, eqSlot);
            int bestInvSlot = -1;
            double bestScore = currentScore;

            for (int i = 0; i < 36; i++) {
                ItemStack cand = mc.player.getInventory().getStack(i);
                double sc = scoreArmor(cand, eqSlot);
                if (sc > bestScore + 0.25) {
                    bestScore = sc;
                    bestInvSlot = i;
                }
            }

            if (bestInvSlot != -1) {
                int fromSlot = bestInvSlot < 9 ? bestInvSlot + 36 : bestInvSlot;
                int targetSlot = handlerArmorSlots[sIdx];
                boolean emptyArmor = equipped.isEmpty();
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                if (!emptyArmor) {
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
                }
                cooldown = equipDelayTicks.getValue();
                return;
            }
        }
    }

    private double scoreArmor(ItemStack stack, EquipmentSlot targetSlot) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armor)) {
            return -1.0;
        }
        if (armor.getType().getEquipmentSlot() != targetSlot) {
            return -1.0;
        }
        if (avoidBindingCurse.getValue() && hasBindingCurse(stack)) {
            return -1.0;
        }

        if (stack.getMaxDamage() > 0) {
            double pct = ((stack.getMaxDamage() - stack.getDamage()) * 100.0) / stack.getMaxDamage();
            if (pct < minDurabilityPct.getValue()) {
                return 0.5;
            }
        }

        double score = armor.getProtection() * 4.0 + armor.getToughness() * 2.5;
        ItemEnchantmentsComponent ench = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        for (RegistryEntry<Enchantment> entry : ench.getEnchantments()) {
            int lvl = ench.getLevel(entry);
            String id = entry.getKey().map(k -> k.getValue().getPath()).orElse("");
            if (id.equals("protection")) score += lvl * 2.8;
            else if (id.contains("protection")) score += lvl * 1.4;
            else if (id.equals("mending")) score += 2.0;
            else if (id.equals("unbreaking")) score += lvl * 0.8;
        }
        return score;
    }

    private static boolean hasBindingCurse(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ItemEnchantmentsComponent ench = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        for (RegistryEntry<Enchantment> entry : ench.getEnchantments()) {
            String id = entry.getKey().map(k -> k.getValue().getPath()).orElse("");
            if (id.contains("binding_curse")) {
                return true;
            }
        }
        return false;
    }
}
