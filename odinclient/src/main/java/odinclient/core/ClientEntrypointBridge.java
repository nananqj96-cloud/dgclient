package odinclient.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public final class ClientEntrypointBridge {
    private ClientEntrypointBridge() {}

    public static void initialize(Minecraft client) {
        OdinClient.initialize();
    }
}
