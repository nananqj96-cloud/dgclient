/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.TextureChangerScreen
 *  com.fakeclient.module.Module
 *  com.fakeclient.module.modules.TextureChanger$TextureChanger$Swap
 *  com.fakeclient.render.TextureSwaps
 *  com.fakeclient.util.ItemCatalog
 *  net.minecraft.class_10192
 *  net.minecraft.class_1304
 *  net.minecraft.class_1747
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_2246
 *  net.minecraft.class_2248
 *  net.minecraft.class_310
 *  net.minecraft.class_3414
 *  net.minecraft.class_3417
 *  net.minecraft.class_6880
 *  net.minecraft.class_9334
 */
package com.fakeclient.module.modules;

import com.fakeclient.Settings;
import com.fakeclient.gui.TextureChangerScreen;
import com.fakeclient.module.Module;
import com.fakeclient.module.modules.TextureChanger;
import com.fakeclient.render.TextureSwaps;
import com.fakeclient.util.ItemCatalog;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_10192;
import net.minecraft.class_1304;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_310;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_6880;
import net.minecraft.class_9334;

public class TextureChanger
extends Module {
    private static final int ELYTRA_PURPLE = 0xFF55FF;
    private static final String CUSTOM_PREFIX = "custom:";
    private final List<TextureChanger$Swap> swaps = new ArrayList<TextureChanger$Swap>();
    private final Map<class_2248, class_2248> blockMap = new IdentityHashMap<class_2248, class_2248>();
    private final Map<class_1792, TextureChanger$Swap> itemMap = new IdentityHashMap<class_1792, TextureChanger$Swap>();
    public boolean renameItems = true;
    public boolean swapSounds = true;
    private boolean wornElytra = false;
    private class_6880<class_3414> pendingRealEquipSound;
    private long pendingRealEquipUntil;

    public TextureChanger() {
        super("Texture Changer", "Render blocks and items as something else.");
        this.setEditor(() -> new TextureChangerScreen(this));
        this.swaps.add(TextureChanger.block("deepslate", class_2246.field_28888, class_2246.field_29220));
        this.swaps.add(TextureChanger.block("cobbled_deepslate", class_2246.field_29031, class_2246.field_10234));
        this.swaps.add(TextureChanger.block("diorite", class_2246.field_10508, class_2246.field_10201));
        this.swaps.add(TextureChanger.block("nether_bricks", class_2246.field_10266, class_2246.field_10205));
        this.swaps.add(TextureChanger.block("pumpkin", class_2246.field_46282, class_2246.field_22109));
        this.swaps.add(TextureChanger.block("purpur", class_2246.field_10286, class_2246.field_23880));
        this.swaps.add(TextureChanger.block("sand", class_2246.field_10102, class_2246.field_10258));
        this.swaps.add(TextureChanger.item("iron_chestplate", class_1802.field_8523, class_1802.field_8833, 0xFF55FF));
        this.loadCustom();
        this.loadState();
        this.rebuild();
    }

    public static boolean isCustom(TextureChanger$Swap swap) {
        return swap.id.startsWith(CUSTOM_PREFIX);
    }

    public boolean addCustom(class_1792 from, class_1792 to) {
        if (from == null || to == null || from == to) {
            return false;
        }
        String id = CUSTOM_PREFIX + ItemCatalog.idOf((class_1792)from) + ">" + ItemCatalog.idOf((class_1792)to);
        for (TextureChanger$Swap s : this.swaps) {
            if (!s.id.equals(id)) continue;
            return false;
        }
        class_2248 fromBlock = TextureChanger.blockOf(from);
        class_2248 toBlock = TextureChanger.blockOf(to);
        if (fromBlock == null || toBlock == null) {
            fromBlock = null;
            toBlock = null;
        }
        this.swaps.add(new TextureChanger$Swap(id, fromBlock, toBlock, from, to, null));
        this.rebuild();
        this.saveCustom();
        this.saveState();
        if (this.isEnabled() && fromBlock != null) {
            TextureSwaps.reloadChunks();
        }
        return true;
    }

    public void removeCustom(TextureChanger$Swap swap) {
        if (!TextureChanger.isCustom(swap) || !this.swaps.remove(swap)) {
            return;
        }
        this.rebuild();
        this.saveCustom();
        Settings.get().textureSwaps.remove(swap.id);
        this.saveState();
        if (this.isEnabled() && swap.fromBlock != null) {
            TextureSwaps.reloadChunks();
        }
    }

    private static class_2248 blockOf(class_1792 item) {
        class_2248 class_22482;
        if (item instanceof class_1747) {
            class_1747 b = (class_1747)item;
            class_22482 = b.method_7711();
        } else {
            class_22482 = null;
        }
        return class_22482;
    }

    private void loadCustom() {
        for (String entry : Settings.get().customSwaps) {
            int split = entry.indexOf(62);
            if (split <= 0) continue;
            class_1792 from = ItemCatalog.byId((String)entry.substring(0, split));
            class_1792 to = ItemCatalog.byId((String)entry.substring(split + 1));
            if (from == null || to == null || from == to) continue;
            class_2248 fb = TextureChanger.blockOf(from);
            class_2248 tb = TextureChanger.blockOf(to);
            if (fb == null || tb == null) {
                fb = null;
                tb = null;
            }
            this.swaps.add(new TextureChanger$Swap(CUSTOM_PREFIX + entry, fb, tb, from, to, null));
        }
    }

    private void saveCustom() {
        Settings s = Settings.get();
        s.customSwaps.clear();
        for (TextureChanger$Swap swap : this.swaps) {
            if (!TextureChanger.isCustom(swap)) continue;
            s.customSwaps.add(swap.id.substring(CUSTOM_PREFIX.length()));
        }
        s.save();
    }

    private static TextureChanger$Swap block(String id, class_2248 from, class_2248 to) {
        return new TextureChanger$Swap(id, from, to, from.method_8389(), to.method_8389(), null);
    }

    private static TextureChanger$Swap item(String id, class_1792 from, class_1792 to, Integer nameColor) {
        return new TextureChanger$Swap(id, null, null, from, to, nameColor);
    }

    public List<TextureChanger$Swap> getSwaps() {
        return this.swaps;
    }

    public class_2248 blockFor(class_2248 from) {
        return this.blockMap.isEmpty() ? null : this.blockMap.get(from);
    }

    public class_1799 stackFor(class_1792 from) {
        TextureChanger$Swap s = this.swapFor(from);
        return s == null ? null : s.toStack;
    }

    public TextureChanger$Swap swapFor(class_1792 from) {
        return this.itemMap.isEmpty() ? null : this.itemMap.get(from);
    }

    public void setSwapEnabled(TextureChanger$Swap swap, boolean value) {
        if (swap.enabled == value) {
            return;
        }
        swap.enabled = value;
        this.rebuild();
        this.saveState();
        if (this.isEnabled() && swap.fromBlock != null) {
            TextureSwaps.reloadChunks();
        }
    }

    public void setRenameItems(boolean value) {
        if (this.renameItems == value) {
            return;
        }
        this.renameItems = value;
        this.saveState();
    }

    public void setSwapSounds(boolean value) {
        if (this.swapSounds == value) {
            return;
        }
        this.swapSounds = value;
        this.saveState();
    }

    public void enableAll() {
        for (TextureChanger$Swap s : this.swaps) {
            s.enabled = true;
        }
        this.rebuild();
        this.saveState();
        if (this.isEnabled()) {
            TextureSwaps.reloadChunks();
        }
    }

    public int activeCount() {
        int n = 0;
        for (TextureChanger$Swap s : this.swaps) {
            if (!s.enabled) continue;
            ++n;
        }
        return n;
    }

    private void rebuild() {
        this.blockMap.clear();
        this.itemMap.clear();
        for (TextureChanger$Swap s : this.swaps) {
            if (!s.enabled) continue;
            if (s.fromBlock != null) {
                this.blockMap.put(s.fromBlock, s.toBlock);
            }
            this.itemMap.put(s.fromItem, s);
        }
    }

    private void loadState() {
        Boolean sounds;
        Settings settings = Settings.get();
        for (TextureChanger$Swap s : this.swaps) {
            Boolean on = (Boolean)settings.textureSwaps.get(s.id);
            if (on == null) continue;
            s.enabled = on;
        }
        Boolean rename = (Boolean)settings.textureSwaps.get("_rename");
        if (rename != null) {
            this.renameItems = rename;
        }
        if ((sounds = (Boolean)settings.textureSwaps.get("_sounds")) != null) {
            this.swapSounds = sounds;
        }
    }

    private void saveState() {
        Settings settings = Settings.get();
        for (TextureChanger$Swap s : this.swaps) {
            settings.textureSwaps.put(s.id, s.enabled);
        }
        settings.textureSwaps.put("_rename", this.renameItems);
        settings.textureSwaps.put("_sounds", this.swapSounds);
        settings.save();
    }

    protected void onEnable() {
        TextureSwaps.reloadChunks();
    }

    protected void onDisable() {
        TextureSwaps.reloadChunks();
        this.wornElytra = false;
    }

    public void tick() {
        boolean isElytra;
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null) {
            this.wornElytra = false;
            return;
        }
        class_1799 chest = mc.field_1724.method_6118(class_1304.field_6174);
        class_1799 swapped = this.stackFor(chest.method_7909());
        boolean bl = isElytra = swapped != null && swapped.method_31574(class_1802.field_8833);
        if (isElytra && !this.wornElytra) {
            mc.field_1724.method_5783((class_3414)class_3417.field_14966.comp_349(), 1.0f, 1.0f);
            class_10192 equippable = (class_10192)chest.method_58694(class_9334.field_54196);
            if (equippable != null) {
                this.pendingRealEquipSound = equippable.comp_3175();
                this.pendingRealEquipUntil = System.currentTimeMillis() + 800L;
            }
        }
        this.wornElytra = isElytra;
    }

    public boolean shouldSuppressEquipSound(class_6880<class_3414> incoming) {
        if (this.pendingRealEquipSound == null || incoming == null) {
            return false;
        }
        if (System.currentTimeMillis() > this.pendingRealEquipUntil) {
            this.pendingRealEquipSound = null;
            return false;
        }
        if (!this.pendingRealEquipSound.equals(incoming)) {
            return false;
        }
        this.pendingRealEquipSound = null;
        return true;
    }
}
