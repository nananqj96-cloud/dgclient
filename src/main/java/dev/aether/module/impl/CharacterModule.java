package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;

public class CharacterModule extends Module {
    private final ModeSetting character = this.addSetting(new ModeSetting("Character", "Character", "Default", "Default", "Pharaoh", "Guardian"));
    private final BooleanSetting friends = this.addSetting(new BooleanSetting("On Friends", "On Friends", false));
    private final ModeSetting detail = this.addSetting(new ModeSetting("Detail", "Detail", "High", "High", "Low", "Medium"));
    private final BooleanSetting heldItems = this.addSetting(new BooleanSetting("Held Items", "Held Items", false));

    public CharacterModule() {
        super("character-3d", "Replaces your player model with a 3D character.", Category.VISUALS);
    }

    @Override
    public void onTick() {
    }
}
