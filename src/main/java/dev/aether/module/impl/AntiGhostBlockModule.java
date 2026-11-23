package dev.aether.module.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import dev.aether.module.Module;
import dev.aether.module.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;

public class AntiGhostBlockModule extends Module {
    private static final int MAX_PENDING = 256;
    private static AntiGhostBlockModule instance;
    private final Map<Long, AntiGhostBlockModule.Pending> pending = new LinkedHashMap<>();
    private ClientLevel world;

    public AntiGhostBlockModule() {
        super("anti-ghost-block", "Stops invisible ghost blocks when mining: a broken block stays visible until the server confirms the break.", Category.MISC);
        instance = this;
    }

    @Override
    protected void onEnable() {
        this.pending.clear();
        this.world = Minecraft.getInstance().level;
    }

    @Override
    protected void onDisable() {
        this.pending.clear();
        this.world = null;
    }

    private static AntiGhostBlockModule active(ClientLevel level) {
        AntiGhostBlockModule module = instance;
        if (module != null && module.isEnabled()) {
            Minecraft mc = Minecraft.getInstance();
            if (module.world != mc.level) {
                module.pending.clear();
                module.world = mc.level;
            }

            return level != null && level == mc.level && mc.player != null && mc.getConnection() != null && !mc.isLocalServer() ? module : null;
        } else {
            return null;
        }
    }

    private static long timeoutNanos(Minecraft mc) {
        PlayerInfo info = mc.getConnection() != null && mc.player != null ? mc.getConnection().getPlayerInfo(mc.player.getUUID()) : null;
        long ping = info == null ? 0L : Math.max(0, info.getLatency());
        return Math.max(2000L, ping * 3L + 500L) * 1000000L;
    }

//     @EventHandler
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        AntiGhostBlockModule module = active(mc.level);
        if (module != null) {
            long now = System.nanoTime();
            long timeout = timeoutNanos(mc);
            module.pending.values().removeIf(entry -> now - entry.since >= timeout);
        }
    }

    public static boolean holdBreak(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        AntiGhostBlockModule module = active(mc.level);
        if (module != null && !mc.level.getBlockState(pos).isAir()) {
            int sequence = ((AccessorNetheriteClientLevel)mc.level).anubis$blockStatePredictionHandler().currentSequence();
            long key = pos.asLong();
            if (!module.pending.containsKey(key)) {
                if (module.pending.size() >= 256) {
                    Iterator<Long> oldest = module.pending.keySet().iterator();
                    oldest.next();
                    oldest.remove();
                }

                module.pending.put(key, new AntiGhostBlockModule.Pending(sequence, System.nanoTime()));
            }

            return true;
        } else {
            return false;
        }
    }

    public static boolean waiting(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        AntiGhostBlockModule module = active(mc.level);
        if (module == null) {
            return false;
        }

        AntiGhostBlockModule.Pending entry = module.pending.get(pos.asLong());
        if (entry == null) {
            return false;
        }

        if (System.nanoTime() - entry.since < timeoutNanos(mc)) {
            return true;
        }

        module.pending.remove(pos.asLong());
        return false;
    }

    public static boolean serverAnswered(ClientLevel level, BlockPos pos) {
        AntiGhostBlockModule module = active(level);
        return module != null && module.pending.remove(pos.asLong()) != null;
    }

    public static void acknowledged(ClientLevel level, int sequence) {
        AntiGhostBlockModule module = active(level);
        if (module != null) {
            module.pending.values().removeIf(entry -> entry.sequence <= sequence);
        }
    }

        private record Pending(int sequence, long since) {
    }
}
