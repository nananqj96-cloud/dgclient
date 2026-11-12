package com.dg.client.module.modules.misc;

import com.dg.client.DGClient;
import com.dg.client.core.NotificationManager;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.MouseButtonEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render2DUtil;
import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.EnumSetting;
import com.dg.client.setting.IntSetting;
import com.dg.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;

/**
 * Inventory Helpers:
 * - Middle-Click Pearl / Middle-Click XP Bottle
 * - Neon Green/White 9x3 Shulker Box Tooltip Preview
 * - Low Armor Durability Toast Warning
 * - Hotbar Slot Lock (blocks accidental Q item drops)
 */
public final class InventoryHelpersModule extends Module {
    public enum MiddleClickMode {
        ENDER_PEARL,
        XP_BOTTLE,
        OFF
    }

    public final EnumSetting<MiddleClickMode> middleClickMode = addSetting(new EnumSetting<>(
            "Middle-Click Action", "Uses hotbar Ender Pearl or XP Bottle on middle click", MiddleClickMode.ENDER_PEARL));
    public final BooleanSetting shulkerPreview = addSetting(new BooleanSetting(
            "Shulker Preview", "Renders a sleek green/white 9x3 grid preview when hovering Shulker Boxes", true));
    public final BooleanSetting armorDurabilityWarn = addSetting(new BooleanSetting(
            "Armor Durability Alert", "Fires a warning toast when equipped armor falls below threshold %", true));
    public final IntSetting warnDurabilityPct = addSetting(new IntSetting(
            "Warn Below %", "Armor durability percentage threshold", 15, 5, 40, 5)
            .visibleWhen(armorDurabilityWarn::getValue));
    public final BooleanSetting lockActiveHotbarSlot = addSetting(new BooleanSetting(
            "Lock Hotbar Drops", "Prevents accidental Q-key item drops on locked slots (Slot 1..9)", false));

    private int armorWarnCooldown = 0;

    public InventoryHelpersModule() {
        super("InvHelpers", "Middle-click Pearl/XP, green/white Shulker preview, armor alert & drop lock", ModuleCategory.MISC);
        setEnabled(true, true);
    }

    public boolean shouldBlockDrop() {
        return isEnabled() && lockActiveHotbarSlot.getValue();
    }

    @EventHandler
    public void onMouseButton(MouseButtonEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            return;
        }
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) {
            return;
        }
        if (middleClickMode.getValue() == MiddleClickMode.OFF) {
            return;
        }

        Item target = middleClickMode.getValue() == MiddleClickMode.ENDER_PEARL
                ? Items.ENDER_PEARL
                : Items.EXPERIENCE_BOTTLE;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(target)) {
                int prev = mc.player.getInventory().getSelectedSlot();
                mc.player.getInventory().setSelectedSlot(i);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.getInventory().setSelectedSlot(prev);
                event.cancel();
                return;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null || !armorDurabilityWarn.getValue()) {
            return;
        }
        if (armorWarnCooldown > 0) {
            armorWarnCooldown--;
            return;
        }

        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = mc.player.getEquippedStack(slot);
            if (st.isEmpty() || st.getMaxDamage() <= 0) continue;
            int pct = Math.round(((st.getMaxDamage() - st.getDamage()) * 100.0f) / st.getMaxDamage());
            if (pct <= warnDurabilityPct.getValue()) {
                DGClient.getInstance().getNotificationManager().push(
                        NotificationManager.Type.WARNING,
                        "Armor Durability Low (" + pct + "%)",
                        st.getName().getString() + " is about to break!"
                );
                armorWarnCooldown = 240;
                break;
            }
        }
    }

    public void renderShulkerPreviewIfPresent(DrawContext ctx, ItemStack hovered, int mouseX, int mouseY) {
        if (!isEnabled() || !shulkerPreview.getValue() || hovered == null || hovered.isEmpty()) {
            return;
        }
        ContainerComponent container = hovered.get(DataComponentTypes.CONTAINER);
        if (container == null) {
            return;
        }
        DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
        container.copyTo(items);

        float boxW = 9 * 18.0f + 12.0f;
        float boxH = 3 * 18.0f + 22.0f;
        int sw = mc.getWindow().getScaledWidth();
        float x = Math.min(sw - boxW - 6.0f, mouseX + 12.0f);
        float y = Math.max(6.0f, mouseY - boxH - 8.0f);

        Render2DUtil.setContext(ctx);
        ctx.getMatrices().pushMatrix();

        Render2DUtil.drawGaussianGlow(ctx.getMatrices(), x, y, boxW, boxH, 5.0f, 7.0f, ColorUtil.NEON_GREEN, 0.75f);
        Render2DUtil.drawRoundedRect(ctx.getMatrices(), x, y, boxW, boxH, 5.0f, ColorUtil.PANEL_DARK);
        Render2DUtil.drawRoundedOutline(ctx.getMatrices(), x, y, boxW, boxH, 5.0f, 1.2f, ColorUtil.NEON_GREEN);
        DGFontRenderer.drawScaledText(ctx, hovered.getName().getString(), x + 6.0f, y + 4.5f, ColorUtil.NEON_GREEN, true, 0.8f, false);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = row * 9 + col;
                int sx = Math.round(x + 6.0f + col * 18.0f);
                int sy = Math.round(y + 16.0f + row * 18.0f);
                Render2DUtil.drawQuad(ctx.getMatrices(), sx, sy, 16.0f, 16.0f, ColorUtil.PANEL_ELEVATED);
                ItemStack slotStack = items.get(idx);
                if (!slotStack.isEmpty()) {
                    ctx.drawItem(slotStack, sx, sy);
                    ctx.drawStackOverlay(mc.textRenderer, slotStack, sx, sy);
                }
            }
        }
        ctx.getMatrices().popMatrix();
    }
}
