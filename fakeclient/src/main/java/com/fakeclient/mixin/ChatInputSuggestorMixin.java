/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ParseResults
 *  net.minecraft.class_124
 *  net.minecraft.class_2172
 *  net.minecraft.class_2561
 *  net.minecraft.class_4717
 *  net.minecraft.class_5250
 *  net.minecraft.class_5481
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.fakeclient.mixin;

import com.mojang.brigadier.ParseResults;
import java.util.Locale;
import net.minecraft.class_124;
import net.minecraft.class_2172;
import net.minecraft.class_2561;
import net.minecraft.class_4717;
import net.minecraft.class_5250;
import net.minecraft.class_5481;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_4717.class})
public class ChatInputSuggestorMixin {
    private static final String COMMAND = "/pay";

    @Inject(method={"method_23925"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private static void fakeclient$payHighlight(ParseResults<class_2172> parse, String original, int firstCharacterIndex, CallbackInfoReturnable<class_5481> cir) {
        int nameEnd;
        int nameStart;
        String lower = original.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("/pay ") && !lower.equals(COMMAND)) {
            return;
        }
        for (nameStart = COMMAND.length(); nameStart < original.length() && original.charAt(nameStart) == ' '; ++nameStart) {
        }
        for (nameEnd = nameStart; nameEnd < original.length() && original.charAt(nameEnd) != ' '; ++nameEnd) {
        }
        class_5250 out = class_2561.method_43473();
        ChatInputSuggestorMixin.append(out, original, firstCharacterIndex, 0, nameStart, false);
        ChatInputSuggestorMixin.append(out, original, firstCharacterIndex, nameStart, nameEnd, true);
        ChatInputSuggestorMixin.append(out, original, firstCharacterIndex, nameEnd, original.length(), false);
        cir.setReturnValue(out.method_30937());
    }

    private static void append(class_5250 out, String s, int visibleFrom, int from, int to, boolean blue) {
        int b;
        int a = Math.max(from, visibleFrom);
        if (a >= (b = Math.min(to, s.length()))) {
            return;
        }
        String part = s.substring(a, b);
        out.method_10852((class_2561)(blue ? class_2561.method_43470((String)part).method_27692(class_124.field_1075) : class_2561.method_43470((String)part)));
    }
}
