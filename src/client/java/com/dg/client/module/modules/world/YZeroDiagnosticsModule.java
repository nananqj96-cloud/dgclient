package com.dg.client.module.modules.world;

import com.dg.client.DGClient;
import com.dg.client.core.DGLogger;
import com.dg.client.core.NotificationManager;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.ChunkDataEvent;
import com.dg.client.event.events.PacketEvent;
import com.dg.client.event.events.Render2DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render2DUtil;
import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.IntSetting;
import com.dg.client.util.ChatUtil;
import com.dg.client.util.ColorUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Section 6.1 Diagnostic Module:
 * Analyzes incoming packets and below-Y=0 chunk sections to determine exactly how the
 * server hides underground terrain, displays live findings in a neon green/white panel,
 * runs N-chunk test sampling, writes timestamped reports to config/dgclient/diagnostics/,
 * and auto-configures YZeroBypassModule strategies based on measured leaks.
 */
public final class YZeroDiagnosticsModule extends Module {
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public final BooleanSetting renderPanel = addSetting(new BooleanSetting(
            "Diagnostic Panel", "Displays live Y < 0 obfuscation findings on screen", true));
    public final BooleanSetting autoConfigureBypass = addSetting(new BooleanSetting(
            "Auto-Drive Bypass", "Automatically enables viable YZeroBypass strategies from findings", true));
    public final IntSetting sampleTargetChunks = addSetting(new IntSetting(
            "Test Sample Chunks", "Number of chunks sampled in Test Mode", 16, 4, 64, 4));
    public final BooleanSetting startSampleRun = addSetting(new BooleanSetting(
            "Run Test Sample Now", "Toggle ON to sample N chunks and save a timestamped report", false));

    private final AtomicInteger chunksInspected = new AtomicInteger();
    private final AtomicInteger airMaskedChunks = new AtomicInteger();
    private final AtomicInteger solidMaskedChunks = new AtomicInteger();
    private final AtomicInteger oreDecoyChunks = new AtomicInteger();
    private final AtomicInteger normalChunks = new AtomicInteger();
    private final AtomicInteger missingSectionChunks = new AtomicInteger();

    private final AtomicInteger lightLeakPackets = new AtomicInteger();
    private final AtomicInteger blockUpdateLeaks = new AtomicInteger();
    private final AtomicInteger blockEntityLeaks = new AtomicInteger();
    private final AtomicInteger entityBelowZeroLeaks = new AtomicInteger();
    private final AtomicInteger soundParticleLeaks = new AtomicInteger();

    private volatile boolean samplingActive = false;
    private int sampleStartChunkCount = 0;
    private volatile String lastSavedReportName = "None";

    public YZeroDiagnosticsModule() {
        super("Y0Diagnostics", "Analyzes how the server hides Y<0 data, logs leaks, and drives Y0Bypass", ModuleCategory.DETECT);
        startSampleRun.onChange(val -> {
            if (Boolean.TRUE.equals(val)) {
                beginSampleTest(sampleTargetChunks.getValue());
                startSampleRun.setValue(false);
            }
        });
    }

    @Override
    public String getDisplayInfo() {
        return getDominantVerdictShort();
    }

    public void beginSampleTest(int targetChunks) {
        if (!isEnabled()) {
            setEnabled(true, false);
        }
        samplingActive = true;
        sampleStartChunkCount = chunksInspected.get();
        sampleTargetChunks.setValue(targetChunks);
        ChatUtil.accent("Y-0 Test Mode:", "Sampling next " + targetChunks + " loaded chunks (walk/fly to load chunks or inspect current area)...");
        // Immediately inspect already-loaded nearby chunks as baseline
        if (mc.world != null && mc.player != null) {
            int cx = mc.player.getChunkPos().x;
            int cz = mc.player.getChunkPos().z;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    WorldChunk wc = mc.world.getChunkManager().getWorldChunk(cx + dx, cz + dz);
                    if (wc != null && !wc.isEmpty()) {
                        inspectLoadedChunk(wc);
                    }
                }
            }
        }
    }

    public void resetStats() {
        chunksInspected.set(0);
        airMaskedChunks.set(0);
        solidMaskedChunks.set(0);
        oreDecoyChunks.set(0);
        normalChunks.set(0);
        missingSectionChunks.set(0);
        lightLeakPackets.set(0);
        blockUpdateLeaks.set(0);
        blockEntityLeaks.set(0);
        entityBelowZeroLeaks.set(0);
        soundParticleLeaks.set(0);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        Packet<?> pkt = event.getPacket();
        if (pkt instanceof ChunkDataS2CPacket chunkPkt) {
            inspectLightData(chunkPkt.getLightData());
            // Check block entities inside ChunkData packet
            chunkPkt.getChunkData().getBlockEntities(chunkPkt.getChunkX(), chunkPkt.getChunkZ())
                    .accept((pos, type, nbt) -> {
                        if (pos.getY() < 0) {
                            blockEntityLeaks.incrementAndGet();
                        }
                    });
        } else if (pkt instanceof LightUpdateS2CPacket lightPkt) {
            inspectLightData(lightPkt.getData());
        } else if (pkt instanceof BlockUpdateS2CPacket bu) {
            if (bu.getPos().getY() < 0) {
                blockUpdateLeaks.incrementAndGet();
            }
        } else if (pkt instanceof ChunkDeltaUpdateS2CPacket cdu) {
            cdu.visitUpdates((pos, state) -> {
                if (pos.getY() < 0) {
                    blockUpdateLeaks.incrementAndGet();
                }
            });
        } else if (pkt instanceof BlockEntityUpdateS2CPacket beu) {
            if (beu.getPos().getY() < 0) {
                blockEntityLeaks.incrementAndGet();
            }
        } else if (pkt instanceof EntitySpawnS2CPacket esp) {
            if (esp.getY() < 0.0) {
                entityBelowZeroLeaks.incrementAndGet();
            }
        } else if (pkt instanceof EntityPositionS2CPacket epos && mc.world != null) {
            if (epos.getY() < 0.0) {
                entityBelowZeroLeaks.incrementAndGet();
            }
        } else if (pkt instanceof PlaySoundS2CPacket snd) {
            if (snd.getY() < 0.0) {
                soundParticleLeaks.incrementAndGet();
            }
        } else if (pkt instanceof ParticleS2CPacket part) {
            if (part.getY() < 0.0) {
                soundParticleLeaks.incrementAndGet();
            }
        } else if (pkt instanceof WorldEventS2CPacket we) {
            if (we.getPos().getY() < 0) {
                soundParticleLeaks.incrementAndGet();
            }
        }
    }

    private void inspectLightData(LightData lightData) {
        if (lightData == null) {
            return;
        }
        BitSet initedBlock = lightData.getInitedBlock();
        // Lighting bit 0 is section below bottomY (-5), bits 1..4 are sections Y=-64..-1
        for (int bit = 1; bit <= 4; bit++) {
            if (initedBlock.get(bit)) {
                lightLeakPackets.incrementAndGet();
                break;
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkDataEvent.Load event) {
        inspectLoadedChunk(event.getChunk());
    }

    public void inspectLoadedChunk(WorldChunk chunk) {
        if (chunk == null) {
            return;
        }
        int bottomY = chunk.getBottomY();
        if (bottomY >= 0) {
            missingSectionChunks.incrementAndGet();
            onChunkSampleCompleted(chunk.getPos().x, chunk.getPos().z, UndergroundStore.ObfuscationMode.MISSING_SECTIONS);
            return;
        }

        // Snapshot summary counts on calling thread fast (sampling 4 sections x 512 stride points = 2048 reads, <0.08ms)
        ChunkSection[] sections = chunk.getSectionArray();
        int subZeroSections = Math.min(sections.length, (0 - bottomY) >> 4);
        int totalSampled = 0;
        int airCount = 0;
        int solidFillerCount = 0;
        int oreCount = 0;

        for (int secIdx = 0; secIdx < subZeroSections; secIdx++) {
            ChunkSection sec = sections[secIdx];
            if (sec == null || sec.isEmpty()) {
                airCount += 512;
                totalSampled += 512;
                continue;
            }
            for (int y = 0; y < 16; y += 2) {
                for (int z = 0; z < 16; z += 2) {
                    for (int x = 0; x < 16; x += 2) {
                        BlockState st = sec.getBlockState(x, y, z);
                        totalSampled++;
                        if (st.isAir()) {
                            airCount++;
                        } else {
                            Block b = st.getBlock();
                            if (b == Blocks.DEEPSLATE || b == Blocks.STONE || b == Blocks.BEDROCK || b == Blocks.TUFF) {
                                solidFillerCount++;
                            } else if (isOreBlock(st, b)) {
                                oreCount++;
                            }
                        }
                    }
                }
            }
        }

        UndergroundStore.ObfuscationMode mode;
        if (totalSampled == 0 || airCount >= totalSampled * 0.992) {
            airMaskedChunks.incrementAndGet();
            mode = UndergroundStore.ObfuscationMode.ALL_AIR;
        } else if (oreCount >= totalSampled * 0.085) {
            oreDecoyChunks.incrementAndGet();
            mode = UndergroundStore.ObfuscationMode.RANDOM_ORE_DECOYS;
        } else if (solidFillerCount >= totalSampled * 0.992 && airCount == 0 && oreCount == 0) {
            solidMaskedChunks.incrementAndGet();
            mode = UndergroundStore.ObfuscationMode.SOLID_DEEPSLATE;
        } else {
            normalChunks.incrementAndGet();
            mode = UndergroundStore.ObfuscationMode.UNMASKED;
        }

        onChunkSampleCompleted(chunk.getPos().x, chunk.getPos().z, mode);
    }

    private void onChunkSampleCompleted(int cx, int cz, UndergroundStore.ObfuscationMode mode) {
        YZeroBypassModule bypass = DGClient.getInstance().getModuleManager().get(YZeroBypassModule.class);
        if (bypass != null) {
            bypass.getStore().getOrCreateChunk(cx, cz).obfuscationMode = mode;
            if (autoConfigureBypass.getValue()) {
                applyRecommendationsToBypass(bypass);
            }
        }

        int total = chunksInspected.incrementAndGet();
        if (samplingActive && (total - sampleStartChunkCount) >= sampleTargetChunks.getValue()) {
            samplingActive = false;
            Path saved = saveDiagnosticReport();
            String fileName = saved != null ? saved.getFileName().toString() : "failed";
            lastSavedReportName = fileName;
            DGClient.getInstance().getNotificationManager().push(
                    NotificationManager.Type.DETECTION,
                    "Y-0 Diagnostic Complete",
                    getDominantVerdictShort() + " -> " + fileName
            );
            ChatUtil.accent("Y-0 Report Saved:", "config/dgclient/diagnostics/" + fileName);
        }
    }

    private void applyRecommendationsToBypass(YZeroBypassModule bypass) {
        bypass.packetLeakReader.setValue(true);
        bypass.antiDecoyFilter.setValue(oreDecoyChunks.get() > 0 || solidMaskedChunks.get() > 0);
        bypass.lightHeightmapInference.setValue(lightLeakPackets.get() > 0);
        bypass.soundParticleInference.setValue(true);
    }

    public String getDominantVerdict() {
        int total = chunksInspected.get();
        if (total == 0) {
            return "Waiting for chunk packets...";
        }
        int air = airMaskedChunks.get();
        int solid = solidMaskedChunks.get();
        int decoy = oreDecoyChunks.get();
        int missing = missingSectionChunks.get();
        int normal = normalChunks.get();

        int max = Math.max(Math.max(air, solid), Math.max(Math.max(decoy, missing), normal));
        if (max == air) return "Zeroed / Air-masked sections";
        if (max == solid) return "Fake uniform Deepslate/Stone";
        if (max == decoy) return "Randomized decoy ores (Anti-Xray)";
        if (max == missing) return "Missing sections (bottomY >= 0)";
        return "Unmasked real terrain";
    }

    public String getDominantVerdictShort() {
        int total = chunksInspected.get();
        if (total == 0) return "Idle";
        if (airMaskedChunks.get() >= total / 2) return "AirMask";
        if (solidMaskedChunks.get() >= total / 2) return "FakeStone";
        if (oreDecoyChunks.get() >= total / 2) return "OreDecoy";
        if (missingSectionChunks.get() >= total / 2) return "NoSec";
        return "Real";
    }

    public List<String> buildSummaryLines() {
        List<String> lines = new ArrayList<>();
        lines.add("Below Y=0 blocks arrive as: " + getDominantVerdict());
        lines.add(String.format(Locale.ROOT, "Chunks analyzed: %d (Air:%d  FakeStone:%d  Decoys:%d  Real:%d)",
                chunksInspected.get(), airMaskedChunks.get(), solidMaskedChunks.get(), oreDecoyChunks.get(), normalChunks.get()));
        lines.add("Light data leaks: " + (lightLeakPackets.get() > 0 ? "YES (" + lightLeakPackets.get() + " pkts)" : "NO"));
        lines.add("Block updates below Y=0 sent: " + (blockUpdateLeaks.get() > 0 ? "YES (" + blockUpdateLeaks.get() + ")" : "NO"));
        lines.add("Block entities below Y=0 sent: " + (blockEntityLeaks.get() > 0 ? "YES (" + blockEntityLeaks.get() + ")" : "NO"));
        lines.add("Entities below Y=0 sent: " + (entityBelowZeroLeaks.get() > 0 ? "YES (" + entityBelowZeroLeaks.get() + ")" : "NO"));
        lines.add("Sound/Particles below Y=0: " + (soundParticleLeaks.get() > 0 ? "YES (" + soundParticleLeaks.get() + ")" : "NO"));
        lines.add("Last saved log: " + lastSavedReportName);
        return lines;
    }

    public Path saveDiagnosticReport() {
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("dgclient").resolve("diagnostics");
            Files.createDirectories(dir);
            String stamp = LocalDateTime.now().format(FILE_TS);
            Path file = dir.resolve("y0_diagnostic_" + stamp + ".log");
            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write("=== DGClient Y-0 Underground Diagnostic Report ===\n");
                w.write("Timestamp: " + LocalDateTime.now() + "\n");
                w.write("Server: " + com.dg.client.core.WaypointManager.getCurrentServerId() + "\n");
                w.write("Dimension: " + com.dg.client.core.WaypointManager.getCurrentDimensionId() + "\n\n");
                for (String line : buildSummaryLines()) {
                    w.write(line + "\n");
                }
                w.write("\n--- Recommended Strategy Assessment ---\n");
                if (airMaskedChunks.get() > normalChunks.get() || solidMaskedChunks.get() > normalChunks.get()) {
                    w.write("* Server strips or replaces sub-zero chunk sections prior to transmission.\n");
                    w.write("* Client CANNOT invent blocks never transmitted by the server.\n");
                    w.write("* Viable strategies: Packet-Leak Reader, BlockEntity/Entity Leak Capture, Light/Heightmap & Sound Inference.\n");
                } else if (oreDecoyChunks.get() > 0) {
                    w.write("* Server uses Anti-Xray decoy injection. Enable Anti-Decoy Statistical Filter + Adjacent Exposure check.\n");
                } else {
                    w.write("* Sub-zero sections appear unmasked in sampled chunks.\n");
                }
            }
            lastSavedReportName = file.getFileName().toString();
            return file;
        } catch (IOException e) {
            DGLogger.error("Failed saving Y-0 diagnostic report", e);
            return null;
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (!renderPanel.getValue() || mc.options.hudHidden) {
            return;
        }
        DrawContext ctx = event.getContext();
        MatrixStack ms = ctx.getMatrices();
        List<String> lines = buildSummaryLines();

        float maxW = 230.0f;
        for (String line : lines) {
            maxW = Math.max(maxW, DGFontRenderer.getWidth(line, false, 0.8f) + 18.0f);
        }
        float h = 22.0f + lines.size() * 11.5f;
        float x = 8.0f;
        float y = 38.0f;

        Render2DUtil.drawGaussianGlow(ms, x, y, maxW, h, 6.0f, 7.0f, ColorUtil.NEON_GREEN, 0.65f);
        Render2DUtil.drawRoundedRect(ms, x, y, maxW, h, 6.0f, ColorUtil.PANEL_DARK);
        Render2DUtil.drawRoundedOutline(ms, x, y, maxW, h, 6.0f, 1.1f, ColorUtil.NEON_GREEN);

        String hdr = samplingActive ? "Y-0 DIAGNOSTICS [SAMPLING...]" : "Y-0 DIAGNOSTICS";
        DGFontRenderer.drawScaledText(ctx, hdr, x + 8.0f, y + 5.5f, ColorUtil.NEON_GREEN, true, 0.86f, false);

        float ly = y + 18.5f;
        for (String line : lines) {
            int col = line.contains(": YES") ? ColorUtil.MINT_GREEN : ColorUtil.PURE_WHITE;
            DGFontRenderer.drawScaledText(ctx, line, x + 8.0f, ly, col, false, 0.78f, false);
            ly += 11.5f;
        }
    }

    private static boolean isOreBlock(BlockState state, Block b) {
        return state.isIn(BlockTags.COAL_ORES)
                || state.isIn(BlockTags.IRON_ORES)
                || state.isIn(BlockTags.GOLD_ORES)
                || state.isIn(BlockTags.DIAMOND_ORES)
                || state.isIn(BlockTags.REDSTONE_ORES)
                || state.isIn(BlockTags.LAPIS_ORES)
                || state.isIn(BlockTags.COPPER_ORES)
                || state.isIn(BlockTags.EMERALD_ORES)
                || b == Blocks.ANCIENT_DEBRIS;
    }
}
