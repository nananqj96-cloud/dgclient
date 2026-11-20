/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.render.TextureSwaps
 *  com.fakeclient.util.ItemCatalog$ItemCatalog$Entry
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1935
 *  net.minecraft.class_2960
 *  net.minecraft.class_7923
 */
package com.fakeclient.util;

import com.fakeclient.render.TextureSwaps;
import com.fakeclient.util.ItemCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1935;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

public final class ItemCatalog {
    private static List<ItemCatalog$Entry> cached;

    private ItemCatalog() {
    }

    public static List<ItemCatalog$Entry> all() {
        if (cached == null) {
            cached = ItemCatalog.build();
        }
        return cached;
    }

    public static List<ItemCatalog$Entry> search(String query) {
        String q;
        List<ItemCatalog$Entry> source = ItemCatalog.all();
        String string = q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return source;
        }
        ArrayList<ItemCatalog$Entry> out = new ArrayList<ItemCatalog$Entry>();
        for (ItemCatalog$Entry e : source) {
            if (!e.search().contains(q)) continue;
            out.add(e);
        }
        return out;
    }

    public static class_1792 byId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (class_1792 item : class_7923.field_41178) {
            if (!ItemCatalog.idOf(item).equals(id)) continue;
            return item;
        }
        return null;
    }

    public static String idOf(class_1792 item) {
        class_2960 id = class_7923.field_41178.method_10221(item);
        return id == null ? "" : id.toString();
    }

    private static List<ItemCatalog$Entry> build() {
        ArrayList<ItemCatalog$Entry> out = new ArrayList<ItemCatalog$Entry>();
        for (class_1792 item : class_7923.field_41178) {
            if (item == class_1802.field_8162) continue;
            class_1799 stack = new class_1799((class_1935)item);
            TextureSwaps.exempt((class_1799)stack);
            String name = stack.method_7964().getString();
            String id = ItemCatalog.idOf(item);
            out.add(new ItemCatalog$Entry(item, stack, name, id, (name + " " + id).toLowerCase(Locale.ROOT)));
        }
        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return out;
    }
}
