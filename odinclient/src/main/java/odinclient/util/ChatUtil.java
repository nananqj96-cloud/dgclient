package odinclient.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public final class ChatUtil {
    private ChatUtil() {}

    public static void sendChat(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.getNetworkHandler() != null && message != null && !message.isBlank()) {
            if (message.startsWith("/")) {
                client.getNetworkHandler().sendChatCommand(message.substring(1));
            } else {
                client.getNetworkHandler().sendChatMessage(message);
            }
        }
    }
}
