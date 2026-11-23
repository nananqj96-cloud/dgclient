package dev.aether.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.SectionPos;
import org.lwjgl.glfw.GLFW;

public class AutoWalkModule extends Module {
    private final ModeSetting direction = this.addSetting(
        "simple-direction", new ModeSetting("Simple Direction", "Forwards", "Forwards", "Backwards", "Left", "Right")
    );
    private final BooleanSetting disableOnInput = this.addSetting(new BooleanSetting("Disable On Input", false));
    private final BooleanSetting disableOnY = this.addSetting(new BooleanSetting("Disable On Y Change", false));
    private final BooleanSetting waitForChunks = this.addSetting(new BooleanSetting("No Unloaded Chunks", true));

    public AutoWalkModule() {
        super("auto-walk", "Automatically walks forward.", Category.MOVEMENT);
            }
        });
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
        releaseKeys();
    }

    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && mc.level != null) {
            if (this.disableOnY.get() && player.yo != player.getY()) {
                this.requestDisable();
            } else {
                this.keyFor(mc.options).setDown(true);
            }
        }
    }

//     private void onKey(KeyInputEvent event) {
        if (event.action == 1 && this.disableOnInput.get()) {
            if (event.key != -1) {
                Options options = Minecraft.getInstance().options;
                KeyEvent key = new KeyEvent(event.key, event.scancode, event.mods);
                if (options.keyUp.matches(key)
                    || options.keyDown.matches(key)
                    || options.keyLeft.matches(key)
                    || options.keyRight.matches(key)
                    || options.keyShift.matches(key)
                    || options.keyJump.matches(key)) {
                    this.onMovementInput();
                }
            }
        }
    }

//     private void onMouseButton(MouseInputEvent event) {
        if (event.action == 1 && this.disableOnInput.get()) {
            Options options = Minecraft.getInstance().options;
            MouseButtonEvent click = new MouseButtonEvent(0.0, 0.0, new MouseButtonInfo(event.button, event.mods));
            if (options.keyUp.matchesMouse(click)
                || options.keyDown.matchesMouse(click)
                || options.keyLeft.matchesMouse(click)
                || options.keyRight.matchesMouse(click)
                || options.keyShift.matchesMouse(click)
                || options.keyJump.matchesMouse(click)) {
                this.onMovementInput();
            }
        }
    }

    private void onMovementInput() {
        if (Minecraft.getInstance().screen == null) {
            this.requestDisable();
        }
    }

//     private void onPlayerMove(PlayerMoveEvent event) {
        if (this.waitForChunks.get()) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            ClientLevel level = mc.level;
            if (player != null && level != null) {
                int chunkX = SectionPos.blockToSectionCoord(player.getX() + event.x * 2.0);
                int chunkZ = SectionPos.blockToSectionCoord(player.getZ() + event.z * 2.0);
                if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                    event.x = 0.0;
                    event.z = 0.0;
                }
            }
        }
    }

    private KeyMapping keyFor(Options options) {
        return switch (this.direction.get()) {
            case "Backwards" -> options.keyDown;
            case "Left" -> options.keyLeft;
            case "Right" -> options.keyRight;
            default -> options.keyUp;
        };
    }

    private static void releaseKeys() {
        Minecraft mc = Minecraft.getInstance();
        Options options = mc.options;
        if (options != null) {
            boolean inGame = mc.screen == null && !isFreecamActive();
            restore(mc, options.keyUp, inGame);
            restore(mc, options.keyDown, inGame);
            restore(mc, options.keyLeft, inGame);
            restore(mc, options.keyRight, inGame);
        }
    }

    private static boolean isFreecamActive() {
//         FreecamModule freecam = ModuleRegistry.get(FreecamModule.class);
        return freecam != null && freecam.isEnabled();
    }

    private static void restore(Minecraft mc, KeyMapping mapping, boolean inGame) {
        mapping.setDown(inGame && isPhysicallyHeld(mc, mapping));
    }

    private static boolean isPhysicallyHeld(Minecraft mc, KeyMapping mapping) {
        if (mapping.isUnbound()) {
            return false;
        } else {
            Key key = InputConstants.getKey(mapping.saveString());
            Type type = key.getType();
            if (type == Type.KEYSYM) {
//                 return AnubisKeyPolling.isKeyDown(mc.getWindow(), key.getValue());
            } else {
                return type == Type.MOUSE ? GLFW.glfwGetMouseButton(mc.getWindow().handle(), key.getValue()) == 1 : false;
            }
        }
    }
}
