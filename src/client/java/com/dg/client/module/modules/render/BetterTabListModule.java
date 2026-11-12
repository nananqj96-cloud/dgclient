package com.dg.client.module.modules.render;

import com.dg.client.DGClient;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.Locale;

/**
 * Enhances the multiplayer Tab List with numeric latency in green/white, live health
 * for loaded players, and neon highlighting on friends.
 */
public final class BetterTabListModule extends Module {
    public final BooleanSetting numericPing = addSetting(new BooleanSetting(
            "Numeric Ping", "Replaces 5-bar icon with exact numeric ping in ms", true));
    public final BooleanSetting highlightFriends = addSetting(new BooleanSetting(
            "Highlight Friends", "Marks friends in bright mint green with [F] badge", true));
    public final BooleanSetting showHealth = addSetting(new BooleanSetting(
            "Show Loaded HP", "Appends live health for players within render distance", true));

    public BetterTabListModule() {
        super("BetterTabList", "Numeric ping, loaded player health, and friend highlighting in Tab list", ModuleCategory.RENDER);
    }

    public Text decorateName(PlayerListEntry entry, Text original) {
        String rawName = entry.getProfile().getName();
        boolean isFriend = highlightFriends.getValue() && DGClient.getInstance().getFriendManager().isFriend(rawName);
        MutableText out = Text.empty();
        if (isFriend) {
            out.append(Text.literal("[F] ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x39FF14)).withBold(true)));
            out.append(Text.literal(rawName).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00FF88))));
        } else {
            out.append(original.copy());
        }

        if (showHealth.getValue()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                PlayerEntity pe = client.world.getPlayerByUuid(entry.getProfile().getId());
                if (pe != null) {
                    float hp = pe.getHealth() + pe.getAbsorptionAmount();
                    int col = hp >= 14.0f ? 0x39FF14 : 0xFFFFFF;
                    out.append(Text.literal(String.format(Locale.ROOT, " %.0fHP", hp))
                            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(col))));
                }
            }
        }
        return out;
    }
}
