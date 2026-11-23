package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class TriggerbotModule extends Module {
    private final SliderSetting reaction = this.addSetting(new SliderSetting("Reaction", "Reaction", 3.0, 0.0, 10.0, 1.0F));
    private final ModeSetting crits = this.addSetting(new ModeSetting("Crits", "Crits", "Smart", "Off", "Smart", "Only"));
    private final BooleanSetting holdAttack = this.addSetting(new BooleanSetting("Hold Attack", "Hold Attack", false));
    private final BooleanSetting players = this.addSetting(new BooleanSetting("Players", "Players", true));
    private final BooleanSetting hostiles = this.addSetting(new BooleanSetting("Hostiles", "Hostiles", false));
    private final BooleanSetting invisible = this.addSetting(new BooleanSetting("Invisible", "Invisible", false));
    private final BooleanSetting weaponsOnly = this.addSetting(new BooleanSetting("Weapons Only", "Weapons Only", true));

    public TriggerbotModule() {
        super("triggerbot", "Hits the entity under the crosshair once the swing is charged, with a reaction delay.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Triggerbot logic stub - core auto-attack functionality
        // preserved as module registration; detailed logic disabled
    }

    public int getReaction() {
        return this.reaction.getInt();
    }

    public String getCrits() {
        return this.crits.get();
    }

    public boolean getHoldAttack() {
        return this.holdAttack.get();
    }
}
