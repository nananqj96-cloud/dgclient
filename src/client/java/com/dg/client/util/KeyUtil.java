package com.dg.client.util;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Converts GLFW key codes to concise display labels and parses key names for .bind commands.
 */
public final class KeyUtil {
    private static final Map<String, Integer> NAME_TO_KEY = new HashMap<>();
    private static final Map<Integer, String> KEY_TO_NAME = new HashMap<>();

    static {
        for (Field field : GLFW.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == int.class && field.getName().startsWith("GLFW_KEY_")) {
                try {
                    int code = field.getInt(null);
                    if (code > 0) {
                        String shortName = field.getName().substring("GLFW_KEY_".length());
                        NAME_TO_KEY.put(shortName.toLowerCase(Locale.ROOT), code);
                        NAME_TO_KEY.put(shortName.replace("_", "").toLowerCase(Locale.ROOT), code);
                        KEY_TO_NAME.putIfAbsent(code, formatShortName(shortName));
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        NAME_TO_KEY.put("rshift", GLFW.GLFW_KEY_RIGHT_SHIFT);
        NAME_TO_KEY.put("lshift", GLFW.GLFW_KEY_LEFT_SHIFT);
        NAME_TO_KEY.put("rctrl", GLFW.GLFW_KEY_RIGHT_CONTROL);
        NAME_TO_KEY.put("lctrl", GLFW.GLFW_KEY_LEFT_CONTROL);
        NAME_TO_KEY.put("ralt", GLFW.GLFW_KEY_RIGHT_ALT);
        NAME_TO_KEY.put("lalt", GLFW.GLFW_KEY_LEFT_ALT);
    }

    private KeyUtil() {
    }

    private static String formatShortName(String raw) {
        return switch (raw) {
            case "RIGHT_SHIFT" -> "RSHIFT";
            case "LEFT_SHIFT" -> "LSHIFT";
            case "RIGHT_CONTROL" -> "RCTRL";
            case "LEFT_CONTROL" -> "LCTRL";
            case "RIGHT_ALT" -> "RALT";
            case "LEFT_ALT" -> "LALT";
            case "GRAVE_ACCENT" -> "`";
            default -> raw.replace("KP_", "NUM");
        };
    }

    public static String getKeyName(int key) {
        if (key <= 0) {
            return "NONE";
        }
        String mapped = KEY_TO_NAME.get(key);
        if (mapped != null) {
            return mapped;
        }
        try {
            String glfwName = GLFW.glfwGetKeyName(key, 0);
            if (glfwName != null && !glfwName.isBlank()) {
                return glfwName.toUpperCase(Locale.ROOT);
            }
            return InputUtil.fromKeyCode(new net.minecraft.client.input.KeyInput(key, 0, 0)).getLocalizedText().getString().toUpperCase(Locale.ROOT);
        } catch (Throwable ignored) {
            return "KEY_" + key;
        }
    }

    public static int parseKey(String name) {
        if (name == null || name.isBlank()) {
            return -1;
        }
        String clean = name.trim().toLowerCase(Locale.ROOT);
        if (clean.equals("none") || clean.equals("unbind") || clean.equals("clear")) {
            return -1;
        }
        Integer code = NAME_TO_KEY.get(clean);
        return code != null ? code : -1;
    }
}
