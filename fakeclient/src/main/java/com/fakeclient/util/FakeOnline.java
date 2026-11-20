/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.mixin.PlayerListAccessor
 *  com.mojang.authlib.GameProfile
 *  net.minecraft.class_310
 *  net.minecraft.class_634
 *  net.minecraft.class_640
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.fakeclient.util;

import com.fakeclient.mixin.PlayerListAccessor;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.class_310;
import net.minecraft.class_634;
import net.minecraft.class_640;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FakeOnline {
    private static final Logger LOGGER = LoggerFactory.getLogger((String)"FakeClient");
    private static final List<UUID> ADDED = new ArrayList<UUID>();
    private static boolean warned = false;

    private FakeOnline() {
    }

    public static void add(String name) {
        block6: {
            if (name == null || name.isBlank()) {
                return;
            }
            class_310 mc = class_310.method_1551();
            class_634 net = mc.method_1562();
            if (net == null) {
                return;
            }
            if (net.method_2874(name) != null) {
                return;
            }
            UUID id = FakeOnline.offlineId(name);
            try {
                Map entries = ((PlayerListAccessor)net).fakeclient$entries();
                Set listed = ((PlayerListAccessor)net).fakeclient$listed();
                if (entries.containsKey(id)) {
                    return;
                }
                class_640 entry = new class_640(new GameProfile(id, name), false);
                entries.put(id, entry);
                listed.add(entry);
                ADDED.add(id);
            }
            catch (Exception | LinkageError e) {
                if (warned) break block6;
                warned = true;
                LOGGER.warn("[FakeClient] Couldn't register {} as online: {}", (Object)name, (Object)e.toString());
            }
        }
    }

    public static void forget() {
        if (ADDED.isEmpty()) {
            return;
        }
        class_310 mc = class_310.method_1551();
        class_634 net = mc.method_1562();
        if (net != null) {
            Map entries = ((PlayerListAccessor)net).fakeclient$entries();
            Set listed = ((PlayerListAccessor)net).fakeclient$listed();
            for (UUID id : ADDED) {
                class_640 gone = (class_640)entries.remove(id);
                if (gone == null) continue;
                listed.remove(gone);
            }
        }
        ADDED.clear();
    }

    public static int count() {
        return ADDED.size();
    }

    public static List<class_640> entries() {
        ArrayList<class_640> out = new ArrayList<class_640>();
        if (ADDED.isEmpty()) {
            return out;
        }
        class_634 net = class_310.method_1551().method_1562();
        if (net == null) {
            return out;
        }
        Map entries = ((PlayerListAccessor)net).fakeclient$entries();
        for (UUID id : ADDED) {
            class_640 entry = (class_640)entries.get(id);
            if (entry == null) continue;
            out.add(entry);
        }
        return out;
    }

    public static boolean isFake(UUID id) {
        return id != null && ADDED.contains(id);
    }

    private static UUID offlineId(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }
}
