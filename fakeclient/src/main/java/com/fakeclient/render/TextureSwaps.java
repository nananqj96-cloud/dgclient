/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.module.ModuleManager
 *  com.fakeclient.module.modules.FakeSpawner
 *  com.fakeclient.module.modules.TextureChanger
 *  com.fakeclient.module.modules.TextureChanger$Swap
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_2246
 *  net.minecraft.class_2248
 *  net.minecraft.class_2498
 *  net.minecraft.class_2561
 *  net.minecraft.class_2680
 *  net.minecraft.class_310
 *  net.minecraft.class_9304
 *  net.minecraft.class_9334
 */
package com.fakeclient.render;

import com.fakeclient.module.modules.TextureChanger$Swap;

import com.fakeclient.FakeClient;
import com.fakeclient.module.ModuleManager;
import com.fakeclient.module.modules.FakeSpawner;
import com.fakeclient.module.modules.TextureChanger;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2498;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_9304;
import net.minecraft.class_9334;

public final class TextureSwaps {
    private static final Set<class_1799> EXEMPT = Collections.newSetFromMap(new IdentityHashMap());

    private TextureSwaps() {
    }

    public static void exempt(class_1799 stack) {
        if (stack != null) {
            EXEMPT.add(stack);
        }
    }

    public static boolean isExempt(class_1799 stack) {
        return !EXEMPT.isEmpty() && EXEMPT.contains(stack);
    }

    public static class_2248 blockFor(class_2680 state) {
        ModuleManager mm = FakeClient.modulesOrNull();
        if (mm == null) {
            return null;
        }
        FakeSpawner spawner = mm.fakeSpawner;
        if (spawner != null && spawner.isEnabled() && state.method_27852(class_2246.field_10049)) {
            return class_2246.field_10260;
        }
        TextureChanger tc = mm.textureChanger;
        if (tc != null && tc.isEnabled()) {
            return tc.blockFor(state.method_26204());
        }
        return null;
    }

    public static class_1799 itemFor(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return null;
        }
        ModuleManager mm = FakeClient.modulesOrNull();
        if (mm == null) {
            return null;
        }
        FakeSpawner spawner = mm.fakeSpawner;
        if (spawner != null && spawner.isEnabled() && stack.method_31574(class_1802.field_8095)) {
            return TextureSwaps.isExempt(stack) ? null : spawner.spawnerStack();
        }
        TextureChanger tc = mm.textureChanger;
        if (tc == null || !tc.isEnabled()) {
            return null;
        }
        class_1799 to = tc.stackFor(stack.method_7909());
        if (to == null || TextureSwaps.isExempt(stack)) {
            return null;
        }
        return TextureSwaps.withEnchantments(to, stack);
    }

    private static class_1799 withEnchantments(class_1799 replacement, class_1799 source) {
        class_9304 ench = (class_9304)source.method_58694(class_9334.field_49633);
        if (ench == null || ench.method_57543()) {
            return replacement;
        }
        class_1799 copy = replacement.method_7972();
        copy.method_57379(class_9334.field_49633, ench);
        return copy;
    }

    public static class_2498 soundFor(class_2680 state) {
        TextureChanger tc;
        ModuleManager mm = FakeClient.modulesOrNull();
        TextureChanger textureChanger = tc = mm == null ? null : mm.textureChanger;
        if (tc == null || !tc.isEnabled() || !tc.swapSounds) {
            return null;
        }
        class_2248 to = tc.blockFor(state.method_26204());
        return to == null ? null : to.method_9564().method_26231();
    }

    public static class_2561 nameFor(class_1799 stack) {
        TextureChanger tc;
        ModuleManager mm = FakeClient.modulesOrNull();
        TextureChanger textureChanger = tc = mm == null ? null : mm.textureChanger;
        if (tc == null || !tc.isEnabled() || !tc.renameItems) {
            return null;
        }
        if (stack == null || stack.method_7960() || TextureSwaps.isExempt(stack)) {
            return null;
        }
        TextureChanger$Swap swap = tc.swapFor(stack.method_7909());
        return swap == null ? null : swap.renamed();
    }

    public static void reloadChunks() {
        try {
            class_310 mc = class_310.method_1551();
            if (mc != null && mc.field_1769 != null) {
                mc.field_1769.method_3279();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}
