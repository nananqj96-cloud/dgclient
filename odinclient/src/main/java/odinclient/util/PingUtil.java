package odinclient.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerEntity;

@Environment(EnvType.CLIENT)
public final class PingUtil {
    private PingUtil() {}

    public static int getPing(PlayerEntity player) {
        Minecraft client = Minecraft.getInstance();
        if (player != null && client.getConnection() != null) {
            PlayerInfo entry = client.getConnection().getPlayerInfo(player.getUuid());
            return entry == null ? 0 : entry.getLatency();
        } else {
            return 0;
        }
    }
}
