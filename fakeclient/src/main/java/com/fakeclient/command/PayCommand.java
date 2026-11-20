/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.module.ModuleManager
 *  com.fakeclient.module.modules.FakePay
 *  com.fakeclient.util.NamePool
 *  com.mojang.authlib.GameProfile
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
 *  net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
 *  net.minecraft.class_2172
 *  net.minecraft.class_2596
 *  net.minecraft.class_310
 *  net.minecraft.class_634
 *  net.minecraft.class_640
 *  net.minecraft.class_7472
 */
package com.fakeclient.command;

import com.fakeclient.FakeClient;
import com.fakeclient.module.ModuleManager;
import com.fakeclient.module.modules.FakePay;
import com.fakeclient.util.NamePool;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.class_2172;
import net.minecraft.class_2596;
import net.minecraft.class_310;
import net.minecraft.class_634;
import net.minecraft.class_640;
import net.minecraft.class_7472;

public final class PayCommand {
    private PayCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register((LiteralArgumentBuilder)ClientCommandManager.literal((String)"pay").then(ClientCommandManager.argument((String)"player", (ArgumentType)StringArgumentType.word()).suggests((context, builder) -> class_2172.method_9265(PayCommand.suggestions(), (SuggestionsBuilder)builder)).then(ClientCommandManager.argument((String)"amount", (ArgumentType)StringArgumentType.greedyString()).executes(context -> {
            String player = StringArgumentType.getString((CommandContext)context, (String)"player");
            String amount = StringArgumentType.getString((CommandContext)context, (String)"amount");
            PayCommand.run(player, amount);
            return 1;
        })))));
    }

    private static void run(String player, String amount) {
        FakePay pay;
        ModuleManager mm = FakeClient.modulesOrNull();
        FakePay fakePay = pay = mm == null ? null : mm.fakePay;
        if (pay != null && pay.isEnabled()) {
            String token = amount.trim().split("\\s+")[0];
            pay.showReceipt(player, token);
            return;
        }
        class_634 net = class_310.method_1551().method_1562();
        if (net != null) {
            net.method_52787((class_2596)new class_7472("pay " + player + " " + amount));
        }
    }

    private static List<String> suggestions() {
        ArrayList<String> out = new ArrayList<String>();
        HashSet<String> seen = new HashSet<String>();
        class_634 net = class_310.method_1551().method_1562();
        if (net != null) {
            for (class_640 entry : net.method_2880()) {
                String name;
                GameProfile profile = entry.method_2966();
                if (profile == null || (name = profile.name()) == null || name.isBlank() || !seen.add(name)) continue;
                out.add(name);
            }
        }
        for (String name : NamePool.get()) {
            if (!seen.add(name)) continue;
            out.add(name);
        }
        return out;
    }
}
