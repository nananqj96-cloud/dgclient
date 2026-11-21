package odinclient.module.fake;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPlayerEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.effect.StatusEffects;
import net.minecraft.world.item.Items;
import net.minecraft.util.Util;

import odinclient.event.EventHandler;
import odinclient.event.TickEvent;
import odinclient.module.Category;
import odinclient.module.Module;
import odinclient.setting.Setting;
import odinclient.setting.type.StringSetting;

@Environment(EnvType.CLIENT)
public final class FakeElytra extends Module {
    private static final long DOUBLE_JUMP_WINDOW_MS = 400L;
    private static FakeElytra instance;
    private static boolean gliding;
    private static long lastJumpTime = -9223372036854775808L;
    private static int glideTicks;
    private static boolean jumpWasPressed;
    private final Setting<String> priceEach =
            this.settings
                    .getGeneral()
                    .add(
                            new StringSetting.Builder()
                                    .name("price-each")
                                    .description(
                                            "Visual price of the fake elytra, e.g. 50K or 100000.")
                                    .defaultValue("50K")
                                    .build());

    public FakeElytra(Category category) {
        super(
                category,
                "fake-elytra",
                "Locally shows a golden chestplate as an elytra and a short double-jump glide.");
        instance = this;
    }

    public static boolean isEnabled() {
        return instance != null && instance.isActive();
    }

    public static String getPriceText() {
        if (instance == null) {
            return "50K";
        } else {
            String text = instance.priceEach.getValue().strip();
            if (text.startsWith("$")) {
                text = text.substring(1).strip();
            }

            if (text.isEmpty()) {
                return "0";
            } else {
                return text.length() > 24 ? text.substring(0, 24) : text;
            }
        }
    }

    @Override
    public void onActivate() {
        resetGlide();
        jumpWasPressed = this.mc.options.jumpKey.isPressed();
    }

    @Override
    public void onDeactivate() {
        resetGlide();
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        ClientPlayerEntity player = this.mc.player;
        if (player == null) {
            resetGlide();
            jumpWasPressed = false;
        } else {
            boolean jumpPressed = this.mc.options.jumpKey.isPressed();
            if (jumpPressed && !jumpWasPressed && this.mc.currentScreen == null) {
                handleJumpPress(this.mc);
            }

            jumpWasPressed = jumpPressed;
            if (gliding) {
                if (canContinueGlide(player) && (glideTicks < 4 || !player.isOnGround())) {
                    glideTicks++;
                } else {
                    resetGlide();
                }
            }
        }
    }

    public static void handleJumpPress(Minecraft client) {
        if (isEnabled()
                && client.player != null
                && client.currentScreen == null
                && isWearingGoldenChestplate(client)) {
            long now = Util.getMeasuringTimeMs();
            if (lastJumpTime != -9223372036854775808L && now - lastJumpTime <= 400L) {
                if (canStartGlide(client.player)) {
                    gliding = true;
                    glideTicks = 0;
                }

                lastJumpTime = -9223372036854775808L;
            } else {
                lastJumpTime = now;
            }
        } else {
            lastJumpTime = -9223372036854775808L;
        }
    }

    public static boolean isGliding() {
        return isEnabled() && gliding;
    }

    public static int getGlideTicks() {
        return glideTicks;
    }

    public static boolean isFakeGliding(LivingEntity entity) {
        return isGliding() && entity == Minecraft.getInstance().player;
    }

    public static double getGlideYOffset() {
        Minecraft client = Minecraft.getInstance();
        return isGliding() && client.player != null && client.player.isOnGround() ? 0.9 : 0.0;
    }

    private static void resetGlide() {
        gliding = false;
        glideTicks = 0;
        lastJumpTime = -9223372036854775808L;
    }

    private static boolean canStartGlide(ClientPlayerEntity player) {
        return isWearingGoldenChestplate(Minecraft.getInstance())
                && !player.isOnGround()
                && !player.hasVehicle()
                && !player.hasStatusEffect(StatusEffects.LEVITATION)
                && !player.getAbilities().flying
                && !player.isTouchingWater();
    }

    private static boolean canContinueGlide(ClientPlayerEntity player) {
        return isWearingGoldenChestplate(Minecraft.getInstance())
                && !player.hasVehicle()
                && !player.hasStatusEffect(StatusEffects.LEVITATION)
                && !player.getAbilities().flying
                && !player.isTouchingWater();
    }

    private static boolean isWearingGoldenChestplate(Minecraft client) {
        return client.player != null
                && client.player
                        .getEquippedStack(EquipmentSlot.CHEST)
                        .isOf(Items.GOLDEN_CHESTPLATE);
    }
}
