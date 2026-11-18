/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2960
 *  net.minecraft.class_8710
 *  net.minecraft.class_8710$class_9154
 *  net.minecraft.class_9129
 *  net.minecraft.class_9139
 */
package de.donutlarp;

import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9129;
import net.minecraft.class_9139;

public record ActivityPayload(byte[] data) implements class_8710
{
    public static final class_8710.class_9154<ActivityPayload> ID = new class_8710.class_9154(class_2960.method_60655((String)"donutlarp", (String)"activity"));
    public static final class_9139<class_9129, ActivityPayload> CODEC = class_9139.method_56438((value, buf) -> buf.method_52983(value.data), buf -> {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.method_52979(bytes);
        return new ActivityPayload(bytes);
    });

    public class_8710.class_9154<? extends class_8710> method_56479() {
        return ID;
    }
}

