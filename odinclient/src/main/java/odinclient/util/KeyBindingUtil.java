package odinclient.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.KeyMapping;

@Environment(EnvType.CLIENT)
public final class KeyMappingUtil {
    private KeyMappingUtil() {}

    public static boolean isPressed(KeyMapping keyBinding) {
        return keyBinding != null && keyBinding.isPressed();
    }
}
