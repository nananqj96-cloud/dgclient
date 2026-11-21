package odinclient.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;

@Environment(EnvType.CLIENT)
public final class KeyMappingUtil {
    private KeyMappingUtil() {}

    public static boolean isPressed(KeyMapping keyBinding) {
        return keyBinding != null && keyBinding.isPressed();
    }
}
