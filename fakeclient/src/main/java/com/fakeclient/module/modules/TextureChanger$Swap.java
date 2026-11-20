/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.render.TextureSwaps
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1935
 *  net.minecraft.class_2248
 *  net.minecraft.class_2561
 */
package com.fakeclient.module.modules;

import com.fakeclient.render.TextureSwaps;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_2248;
import net.minecraft.class_2561;

public class TextureChanger$Swap {
    public final String id;
    public final class_2248 fromBlock;
    public final class_2248 toBlock;
    public final class_1792 fromItem;
    public final class_1792 toItem;
    public final class_1799 fromStack;
    public final class_1799 toStack;
    private final Integer nameColor;
    public boolean enabled = true;

    public TextureChanger$Swap(String id, class_2248 fromBlock, class_2248 toBlock, class_1792 fromItem, class_1792 toItem, Integer nameColor) {
        this.id = id;
        this.fromBlock = fromBlock;
        this.toBlock = toBlock;
        this.fromItem = fromItem;
        this.toItem = toItem;
        this.nameColor = nameColor;
        this.fromStack = new class_1799((class_1935)fromItem);
        this.toStack = new class_1799((class_1935)toItem);
        TextureSwaps.exempt((class_1799)this.fromStack);
        TextureSwaps.exempt((class_1799)this.toStack);
    }

    public class_2561 fromName() {
        return this.fromStack.method_7964();
    }

    public class_2561 toName() {
        return this.toStack.method_7964();
    }

    public class_2561 renamed() {
        class_2561 base = this.toStack.method_7964();
        if (this.nameColor == null) {
            return base;
        }
        return class_2561.method_43470((String)base.getString()).method_27694(s -> s.method_36139(this.nameColor.intValue()));
    }
}
