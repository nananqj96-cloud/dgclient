package com.dg.client.module.modules.misc;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.IntSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Auto-Eat / Auto-Gap: switches to best hotbar food or Golden Apple when hunger or HP
 * drops below threshold, pauses safely while mining or in menus, and restores slot after.
 */
public final class AutoEatModule extends Module {
    public final IntSetting hungerThreshold = addSetting(new IntSetting(
            "Hunger Threshold", "Eats when hunger level <= this value (max 20)", 15, 4, 19, 1));
    public final BooleanSetting autoGapOnLowHp = addSetting(new BooleanSetting(
            "Auto Golden Apple", "Eats Golden Apple when health drops below Health Threshold", true));
    public final DoubleSetting healthThreshold = addSetting(new DoubleSetting(
            "Health Threshold", "HP level triggering Golden Apple consumption", 12.0, 4.0, 20.0, 0.5)
            .visibleWhen(autoGapOnLowHp::getValue));
    public final BooleanSetting pauseWhileMining = addSetting(new BooleanSetting(
            "Pause While Mining", "Does not interrupt active block breaking", true));

    private boolean eating = false;
    private int returnSlot = -1;

    public AutoEatModule() {
        super("AutoEat", "Automatic hunger food & low-HP Golden Apple consumption", ModuleCategory.MISC);
    }

    @Override
    protected void onDisable() {
        stopEating();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.options == null) {
            return;
        }
        if (mc.currentScreen != null || (pauseWhileMining.getValue() && mc.interactionManager != null && mc.interactionManager.isBreakingBlock())) {
            if (eating) stopEating();
            return;
        }

        boolean needGap = autoGapOnLowHp.getValue() && mc.player.getHealth() <= healthThreshold.getValue().floatValue();
        boolean needFood = mc.player.getHungerManager().getFoodLevel() <= hungerThreshold.getValue();

        if (!needGap && !needFood) {
            if (eating) stopEating();
            return;
        }

        // Check offhand first
        ItemStack offhand = mc.player.getOffHandStack();
        if (needGap && (offhand.isOf(Items.ENCHANTED_GOLDEN_APPLE) || offhand.isOf(Items.GOLDEN_APPLE))) {
            startPressingUse(-1);
            return;
        }
        if (needFood && isSafeFood(offhand)) {
            startPressingUse(-1);
            return;
        }

        int bestHotbar = findBestHotbarFood(needGap);
        if (bestHotbar == -1) {
            if (eating) stopEating();
            return;
        }

        if (!eating) {
            returnSlot = mc.player.getInventory().getSelectedSlot();
        }
        mc.player.getInventory().setSelectedSlot(bestHotbar);
        startPressingUse(returnSlot);
    }

    private void startPressingUse(int savedSlot) {
        eating = true;
        returnSlot = savedSlot;
        mc.options.useKey.setPressed(true);
    }

    private void stopEating() {
        if (mc.options != null) {
            mc.options.useKey.setPressed(false);
        }
        if (returnSlot >= 0 && returnSlot < 9 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(returnSlot);
        }
        eating = false;
        returnSlot = -1;
    }

    private int findBestHotbarFood(boolean preferGapple) {
        if (preferGapple) {
            for (int i = 0; i < 9; i++) {
                ItemStack st = mc.player.getInventory().getStack(i);
                if (st.isOf(Items.ENCHANTED_GOLDEN_APPLE) || st.isOf(Items.GOLDEN_APPLE)) {
                    return i;
                }
            }
        }
        int bestSlot = -1;
        float bestValue = -1.0f;
        for (int i = 0; i < 9; i++) {
            ItemStack st = mc.player.getInventory().getStack(i);
            if (!isSafeFood(st)) continue;
            FoodComponent fc = st.get(DataComponentTypes.FOOD);
            if (fc == null) continue;
            float val = fc.nutrition() + fc.saturation() * 2.0f;
            if (val > bestValue) {
                bestValue = val;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static boolean isSafeFood(ItemStack st) {
        if (st == null || st.isEmpty() || !st.contains(DataComponentTypes.FOOD)) {
            return false;
        }
        return !st.isOf(Items.CHORUS_FRUIT)
                && !st.isOf(Items.ROTTEN_FLESH)
                && !st.isOf(Items.SPIDER_EYE)
                && !st.isOf(Items.PUFFERFISH)
                && !st.isOf(Items.POISONOUS_POTATO);
    }
}
