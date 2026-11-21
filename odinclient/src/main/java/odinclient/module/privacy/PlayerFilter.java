package odinclient.module.privacy;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;

public final class PlayerFilter {
    private PlayerFilter() {}

    public static boolean isOtherPlayer(Minecraft client, PlayerEntity player) {
        return client.player != null
                && player != client.player
                && player.isAlive()
                && !player.getGameProfile().id().equals(client.player.getGameProfile().id());
    }
}
