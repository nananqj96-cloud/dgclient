package dev.aether.module.impl;

import dev.aether.AetherClient;

import com.example.client.mixin.AccessorCreativeSlotWrapper;
import com.sun.jna.Platform;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector2f;

public class NoInvLeakModule extends Module {
    private static final String STYLE_TOTEM = "Totem";
    private static final String STYLE_RANDOM = "Random";
    private static final String STYLE_EMPTY = "Empty";
    private static final int OFFHAND_SEED = 10;
    private static volatile NoInvLeakModule active;
    private static ItemStack totem;
    private static boolean bareModel;
    private static final Vector2f POINT = new Vector2f();
    private final ModeSetting style = this.addSetting(new ModeSetting("Style", "Totem", "Totem", "Random", "Empty"));
    private final BooleanSetting fillEmpty = this.addSetting(new BooleanSetting("Fill Empty Slots", true));
    private final BooleanSetting streamOnly = this.addSetting(new BooleanSetting("Stream Only", false));
    private final BooleanSetting borderless = this.addSetting(new BooleanSetting("Borderless Fullscreen", true));
    private final BooleanSetting tooltips = this.addSetting(new BooleanSetting("Hide Tooltips", true));
    private final BooleanSetting itemName = this.addSetting(new BooleanSetting("Hide Item Name", true));
    private final BooleanSetting selectedSlot = this.addSetting(new BooleanSetting("Hide Selected Slot", true));
    private final BooleanSetting hud = this.addSetting(new BooleanSetting("Anubis HUD", true));
    private final FakeLoadout loadout = new FakeLoadout();

    public NoInvLeakModule() {
        super("no-inv-leak", "Hides your items on screen for screen shares. Stream Only keeps them visible to you alone.", Category.MISC);
        this.fillEmpty.visibleWhen(() -> !this.style.is("Empty"));
        this.streamOnly.visibleWhen(Platform::isWindows);
            } else {
                StreamOverlay.stop();
            }
        });
        this.borderless.visibleWhen(() -> Platform.isWindows() && this.streamOnly.get());
    }

    public static boolean wantsBorderless() {
//         NoInvLeakModule module = ModuleRegistry.get(NoInvLeakModule.class);
        return module != null && module.streamOnly.get() && module.borderless.get()
//             ? module.isEnabled() || ModuleConfigStore.getInstance().enablesOnLoad(module)
            : false;
    }

    @Override
    protected void onEnable() {
        this.loadout.reseed();
        StreamOverlay.retry();
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
        StreamOverlay.stop();
    }

    public static ItemStack hotbar(ItemStack stack, int x, int y, int seed) {
        NoInvLeakModule module = active;
        if (module != null && stack != null) {
            int slot = seed == 10 ? 40 : (seed >= 1 && seed <= 9 ? seed - 1 : 41);
            if (module.streamOverlaying() && StreamOverlay.recordsHotbar()) {
                StreamOverlay.hotbar(slot, x, y, stack);
                return ItemStack.EMPTY;
            } else {
                return module.shown(stack, slot);
            }
        } else {
            return stack;
        }
    }

    public static ItemStack container(ItemStack stack, Slot slot, int x, int y, boolean hovered) {
        NoInvLeakModule module = active;
        if (module != null && stack != null) {
            int index = inventoryIndex(slot);
            if (module.streamOverlaying()) {
                StreamOverlay.container(index, x, y, stack, hovered);
                return ItemStack.EMPTY;
            } else {
                return module.shown(stack, index);
            }
        } else {
            return stack;
        }
    }

    private static int inventoryIndex(Slot slot) {
        Slot real = slot instanceof AccessorCreativeSlotWrapper wrapper ? wrapper.anubis$target() : slot;
        return real.getContainerSlot();
    }

    public static ItemStack dragged(ItemStack stack) {
        NoInvLeakModule module = active;
        if (module != null && stack != null && !stack.isEmpty()) {
            return module.streamOverlaying() ? ItemStack.EMPTY : module.fake(stack, 41);
        } else {
            return stack;
        }
    }

    public static boolean hidesModelOf(LivingEntity entity) {
        return active != null && entity != null && entity == Minecraft.getInstance().player;
    }

    public static void bareModel(boolean on) {
        bareModel = on;
    }

    public static ItemStack modelItem(LivingEntity entity, ItemStack stack) {
        return bareModel && stack != null && !stack.isEmpty() && entity == Minecraft.getInstance().player ? ItemStack.EMPTY : stack;
    }

    public static int screenX(GuiGraphics guiGraphics, int localX, int localY, int fallback) {
        if (guiGraphics != null && active != null) {
            guiGraphics.pose().transformPosition(localX, localY, POINT);
            return Math.round(POINT.x);
        } else {
            return fallback;
        }
    }

    public static int screenY(GuiGraphics guiGraphics, int localX, int localY, int fallback) {
        if (guiGraphics != null && active != null) {
            guiGraphics.pose().transformPosition(localX, localY, POINT);
            return Math.round(POINT.y);
        } else {
            return fallback;
        }
    }

    public static ItemStack carried(ItemStack stack, int x, int y) {
        NoInvLeakModule module = active;
        if (module == null || stack == null || stack.isEmpty()) {
            return stack;
        } else if (module.streamOverlaying()) {
            StreamOverlay.carried(stack, x, y);
            return ItemStack.EMPTY;
        } else {
            return module.fake(stack, 41);
        }
    }

    public static ItemStack shownInHud(ItemStack stack, EquipmentSlot slot) {
        NoInvLeakModule module = active;
        return module != null && module.hud.get() && stack != null && !stack.isEmpty() ? module.fake(stack, inventorySlot(slot)) : stack;
    }

    public static ItemStack fakeFor(int slot) {
        NoInvLeakModule module = active;
        return module == null ? ItemStack.EMPTY : module.fake(ItemStack.EMPTY, slot);
    }

    public static boolean fillsEmpty() {
        NoInvLeakModule module = active;
        return module != null && module.fillEmpty.get() && !module.style.is("Empty");
    }

    public static boolean overlaying() {
        NoInvLeakModule module = active;
        return module != null && module.streamOnly.get() && StreamOverlay.usable();
    }

    private boolean streamOverlaying() {
        return this.streamOnly.get() && StreamOverlay.usable();
    }

    public static boolean ownSlot(Slot slot) {
        return active != null && slot != null && slot.container instanceof Inventory;
    }

    public static boolean hidesTooltips() {
        NoInvLeakModule module = active;
        return module != null && module.tooltips.get();
    }

    public static boolean hidesItemName() {
        NoInvLeakModule module = active;
        return module != null && module.itemName.get();
    }

    public static boolean hidesSelectedSlot() {
        NoInvLeakModule module = active;
        return module != null && module.selectedSlot.get();
    }

    public static boolean overlayWanted() {
        NoInvLeakModule module = active;
        return module != null && module.streamOnly.get();
    }

    public static void streamNotice(String headline, String detail) {
        NoInvLeakModule module = active;
        if (module != null) {
//             AetherClient.chat(module, ModuleChat.body().warn(headline).text(detail));
        }
    }

    public static boolean opaqueCells() {
        NoInvLeakModule module = active;
        return module != null && !module.style.is("Empty");
    }

    private ItemStack shown(ItemStack stack, int slot) {
        return stack.isEmpty() && !this.fillEmpty.get() ? stack : this.fake(stack, slot);
    }

    private ItemStack fake(ItemStack stack, int slot) {
        if (this.style.is("Empty")) {
            return ItemStack.EMPTY;
        }

        if (this.style.is("Random")) {
            return this.loadout.get(slot);
        }

        ItemStack stand = totem;
        if (stand == null) {
            totem = stand = new ItemStack(Items.TOTEM_OF_UNDYING);
        }

        return stand;
    }

    private static int inventorySlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 39;
            case CHEST -> 38;
            case LEGS -> 37;
            case FEET -> 36;
            case OFFHAND -> 40;
            case MAINHAND -> {
                LocalPlayer player = Minecraft.getInstance().player;
                yield player != null ? player.getInventory().getSelectedSlot() : 41;
            }
            default -> 41;
        };
    }
}
