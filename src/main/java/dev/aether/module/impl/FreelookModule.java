package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public class FreelookModule extends Module {
    private static final String PLAYER = "Player";
    private static final String CAMERA = "Camera";
    private static final float MIN_SENSITIVITY = 0.1F;
    private final ModeSetting mode = this.addSetting(new ModeSetting("Mode", "Player", "Player", "Camera"));
    private final BooleanSetting togglePerspective = this.addSetting(new BooleanSetting("Toggle Perspective", true));
    private final SliderSetting sensitivity = this.addSetting(new SliderSetting("Camera Sensitivity", 8.0, 0.0, 10.0, 0.1F));
    private final BooleanSetting arrows = this.addSetting(new BooleanSetting("Arrows Control Opposite", true));
    private final SliderSetting arrowSpeed = this.addSetting(new SliderSetting("Arrow Speed", 4.0, 0.0, 10.0, 0.5F));
    private float cameraYaw;
    private float cameraPitch;
    private CameraType previousView;

    public FreelookModule() {
        super("freelook", "Turns the camera and the player separately.", Category.RENDER);
        this.sensitivity.visibleWhen(() -> this.mode.is("Camera"));
        this.arrowSpeed.visibleWhen(this.arrows::get);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            this.requestDisable();
        } else {
            this.cameraYaw = mc.player.getYRot();
            this.cameraPitch = mc.player.getXRot();
            this.previousView = mc.options.getCameraType();
            if (this.previousView != CameraType.THIRD_PERSON_BACK && this.togglePerspective.get()) {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (this.previousView != null && mc.options.getCameraType() != this.previousView && this.togglePerspective.get()) {
            mc.options.setCameraType(this.previousView);
        }

        this.previousView = null;
    }

    private void onTick(TickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            if (this.arrows.get() && mc.screen == null) {
                float turn = this.arrowSpeed.get();
                float yawTurn = (held(mc, 262) ? turn : 0.0F) - (held(mc, 263) ? turn : 0.0F);
                float pitchTurn = (held(mc, 264) ? turn : 0.0F) - (held(mc, 265) ? turn : 0.0F);
                if (this.mode.is("Player")) {
                    this.cameraYaw += yawTurn;
                    this.cameraPitch += pitchTurn;
                } else if (yawTurn != 0.0F || pitchTurn != 0.0F) {
                    player.setYRot(player.getYRot() + yawTurn);
                    player.setXRot(player.getXRot() + pitchTurn);
                }
            }

            this.cameraPitch = Mth.clamp(this.cameraPitch, -90.0F, 90.0F);
        }
    }

    private static boolean held(Minecraft mc, int key) {
//         return AnubisKeyPolling.isKeyDown(mc.getWindow(), key);
    }

    public boolean cameraMode() {
        return this.isEnabled() && this.mode.is("Camera");
    }

    public static boolean throughBlocks() {
//         FreelookModule freelook = ModuleRegistry.get(FreelookModule.class);
        return freelook != null && freelook.isEnabled() && FreecamModule.active == null;
    }

    public void turnCamera(double yRot, double xRot) {
        float divisor = Math.max(this.sensitivity.get(), 0.1F);
        this.cameraYaw += (float)(yRot / divisor);
        this.cameraPitch += (float)(xRot / divisor);
        if (Math.abs(this.cameraPitch) > 90.0F) {
            this.cameraPitch = this.cameraPitch > 0.0F ? 90.0F : -90.0F;
        }
    }

    public float cameraYaw() {
        return this.cameraYaw;
    }

    public float cameraPitch() {
        return this.cameraPitch;
    }
}
