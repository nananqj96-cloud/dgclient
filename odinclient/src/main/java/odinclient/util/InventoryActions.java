package odinclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.InputConstants;
import net.minecraft.client.util.Window;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import org.lwjgl.glfw.GLFW;

public final class InventoryActions {
    private InventoryActions() {}

    public static boolean isReady(Minecraft client) {
        return client != null
                && client.player != null
                && client.world != null
                && client.interactionManager != null;
    }

    public static boolean isInputPressed(Minecraft client, int code) {
        if (client != null && code >= 0) {
            Window window = client.getWindow();
            if (window == null) {
                return false;
            } else if (code >= 1000 && code <= 1007) {
                int button = code - 1000;
                return GLFW.glfwGetMouseButton(window.getHandle(), button) == 1;
            } else if (code >= 0 && code <= 7) {
                return GLFW.glfwGetMouseButton(window.getHandle(), code) == 1;
            } else {
                return InputUtil.isKeyPressed(window, code);
            }
        } else {
            return false;
        }
    }

    static int findHotbarSlot(Minecraft client, Item item) {
        if (client != null && client.player != null) {
            for (int i = 0; i < 9; i++) {
                if (client.player.getInventory().getStack(i).isOf(item)) {
                    return i;
                }
            }

            return -1;
        } else {
            return -1;
        }
    }

    public static int findInventorySlot(Minecraft client, Item item) {
        if (client != null && client.player != null) {
            for (int i = 0; i < 36; i++) {
                if (client.player.getInventory().getStack(i).isOf(item)) {
                    return i;
                }
            }

            return -1;
        } else {
            return -1;
        }
    }

    public static boolean selectItem(Minecraft client, Item item) {
        int slot = findHotbarSlot(client, item);
        if (slot < 0) {
            return false;
        } else {
            client.player.getInventory().setSelectedSlot(slot);
            return true;
        }
    }

    static int toScreenSlot(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }

    public static boolean swapToOffhand(Minecraft client, int slot) {
        if (isReady(client) && slot >= 0) {
            client.interactionManager.clickSlot(
                    client.player.playerScreenHandler.syncId,
                    toScreenSlot(slot),
                    40,
                    SlotActionType.SWAP,
                    client.player);
            return true;
        } else {
            return false;
        }
    }

    public static boolean swapToHotbar(Minecraft client, int slot, int hotbarSlot) {
        if (isReady(client) && slot >= 0 && hotbarSlot >= 0 && hotbarSlot <= 8) {
            client.interactionManager.clickSlot(
                    client.player.playerScreenHandler.syncId,
                    toScreenSlot(slot),
                    hotbarSlot,
                    SlotActionType.SWAP,
                    client.player);
            return true;
        } else {
            return false;
        }
    }

    public static void placeBlock(Minecraft client, BlockHitResult hitResult) {
        if (isReady(client) && hitResult != null) {
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
            client.player.swingHand(Hand.MAIN_HAND);
        }
    }

    public static boolean canPlaceAt(Minecraft client, BlockPos pos) {
        return isReady(client)
                && client.world.getBlockState(pos).isReplaceable()
                && client.world.getCrammedEntities(client.player, new Box(pos)).isEmpty();
    }
}
