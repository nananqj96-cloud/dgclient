package com.dg.client.module.modules.render;

import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.SettingGroup;

/**
 * Customizes first-person mainhand & offhand item position, scale, rotation,
 * swing speed, and optional offhand hiding.
 */
public final class ViewmodelModule extends Module {
    private final SettingGroup mainGroup = createGroup("Main Hand");
    private final SettingGroup offGroup = createGroup("Off Hand");

    public final DoubleSetting mainX = mainGroup.add(new DoubleSetting("Main X", "Horizontal offset", 0.0, -1.5, 1.5, 0.05));
    public final DoubleSetting mainY = mainGroup.add(new DoubleSetting("Main Y", "Vertical offset", 0.0, -1.5, 1.5, 0.05));
    public final DoubleSetting mainZ = mainGroup.add(new DoubleSetting("Main Z", "Depth offset", 0.0, -1.5, 1.5, 0.05));
    public final DoubleSetting mainScale = mainGroup.add(new DoubleSetting("Main Scale", "Item scale factor", 1.0, 0.25, 2.0, 0.05));
    public final DoubleSetting rotPitch = mainGroup.add(new DoubleSetting("Pitch", "X-axis tilt degrees", 0.0, -180.0, 180.0, 2.0));
    public final DoubleSetting rotYaw = mainGroup.add(new DoubleSetting("Yaw", "Y-axis spin degrees", 0.0, -180.0, 180.0, 2.0));
    public final DoubleSetting rotRoll = mainGroup.add(new DoubleSetting("Roll", "Z-axis roll degrees", 0.0, -180.0, 180.0, 2.0));

    public final BooleanSetting hideOffhand = offGroup.add(new BooleanSetting("Hide Offhand", "Completely hides the offhand item in first-person", false));
    public final DoubleSetting offX = offGroup.add(new DoubleSetting("Off X", "Offhand horizontal offset", 0.0, -1.5, 1.5, 0.05));
    public final DoubleSetting offY = offGroup.add(new DoubleSetting("Off Y", "Offhand vertical offset", 0.0, -1.5, 1.5, 0.05));
    public final DoubleSetting offZ = offGroup.add(new DoubleSetting("Off Z", "Offhand depth offset", 0.0, -1.5, 1.5, 0.05));
    public final DoubleSetting offScale = offGroup.add(new DoubleSetting("Off Scale", "Offhand scale factor", 1.0, 0.25, 2.0, 0.05));

    public final DoubleSetting swingDurationMult = addSetting(new DoubleSetting(
            "Swing Duration", "Multiplier on arm swing duration (<1 faster, >1 slower)", 1.0, 0.3, 3.0, 0.1));

    public ViewmodelModule() {
        super("Viewmodel", "Customizes hand/item position, scale, rotation, swing speed, and offhand visibility", ModuleCategory.RENDER);
    }
}
