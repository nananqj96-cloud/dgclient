package com.dg.client.module.modules.world;

import com.dg.client.core.DGLogger;
import com.dg.client.core.WaypointManager;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Coord Logger & Distance Tracker:
 * Logs death coordinates, Nether/End portal usage coordinates, and dimension transitions
 * locally to config/dgclient/coord_log.txt (never transmitted anywhere) while tracking
 * total distance traveled.
 */
public final class CoordLoggerModule extends Module {
    public final BooleanSetting logDeaths = addSetting(new BooleanSetting(
            "Log Deaths", "Appends death coordinates & dimension to coord_log.txt", true));
    public final BooleanSetting logPortals = addSetting(new BooleanSetting(
            "Log Portals", "Appends Nether/End portal coordinates when stepping through", true));

    private double sessionMeters = 0.0;
    private Vec3d prevPos = null;
    private String prevDim = "";
    private boolean deathLogged = false;
    private BlockPos lastPortalLogged = null;

    public CoordLoggerModule() {
        super("CoordLogger", "Logs deaths & portal coords locally and tracks distance traveled", ModuleCategory.WORLD);
    }

    public double getSessionMeters() {
        return sessionMeters;
    }

    @Override
    public String getDisplayInfo() {
        if (sessionMeters >= 1000.0) {
            return String.format(Locale.ROOT, "%.2fkm", sessionMeters / 1000.0);
        }
        return String.format(Locale.ROOT, "%.0fm", sessionMeters);
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) {
            prevPos = null;
            return;
        }
        Vec3d cur = mc.player.getEntityPos();
        String dim = WaypointManager.getCurrentDimensionId();

        if (prevPos != null && dim.equals(prevDim)) {
            double step = cur.distanceTo(prevPos);
            if (step > 0.001 && step < 120.0) {
                sessionMeters += step;
            }
        } else if (!prevDim.isEmpty() && !dim.equals(prevDim) && logPortals.getValue()) {
            appendLog("DIMENSION_CHANGE (" + prevDim + " -> " + dim + ")", cur);
        }
        prevPos = cur;
        prevDim = dim;

        if (logDeaths.getValue()) {
            if (mc.currentScreen instanceof DeathScreen || mc.player.isDead()) {
                if (!deathLogged) {
                    deathLogged = true;
                    appendLog("DEATH", cur);
                }
            } else {
                deathLogged = false;
            }
        }

        if (logPortals.getValue()) {
            BlockPos bp = mc.player.getBlockPos();
            var st = mc.world.getBlockState(bp);
            if ((st.isOf(Blocks.NETHER_PORTAL) || st.isOf(Blocks.END_PORTAL))
                    && (lastPortalLogged == null || lastPortalLogged.getSquaredDistance(bp) > 64.0)) {
                lastPortalLogged = bp.toImmutable();
                appendLog("PORTAL (" + st.getBlock().getName().getString() + ")", cur);
            }
        }
    }

    public void appendLog(String eventKind, Vec3d pos) {
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("dgclient");
            Files.createDirectories(dir);
            Path file = dir.resolve("coord_log.txt");
            String line = String.format(Locale.ROOT, "[%s] [%s] [%s] %s -> X:%.1f Y:%.1f Z:%.1f%n",
                    LocalDateTime.now(),
                    WaypointManager.getCurrentServerId(),
                    WaypointManager.getCurrentDimensionId(),
                    eventKind,
                    pos.x, pos.y, pos.z);
            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(line);
            }
        } catch (IOException e) {
            DGLogger.warn("CoordLogger write failed: {}", e.getMessage());
        }
    }
}
