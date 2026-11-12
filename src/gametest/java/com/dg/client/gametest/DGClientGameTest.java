package com.dg.client.gametest;

import com.dg.client.DGClient;
import com.dg.client.core.DGLogger;
import com.dg.client.core.detection.DetectionEntry;
import com.dg.client.core.detection.SuspiciousChunkAnalyzer;
import com.dg.client.gui.ClickGuiScreen;
import com.dg.client.module.Module;
import com.dg.client.module.modules.esp.StorageBlockTracker;
import com.dg.client.module.modules.esp.StorageEspModule;
import com.dg.client.module.modules.esp.StorageTracersModule;
import com.dg.client.module.modules.esp.TracersModule;
import com.dg.client.module.modules.hud.ClickGuiModule;
import com.dg.client.module.modules.render.BlockHighlightModule;
import com.dg.client.module.modules.render.ChunkBordersModule;
import com.dg.client.module.modules.render.CustomCrosshairModule;
import com.dg.client.module.modules.render.FullbrightModule;
import com.dg.client.module.modules.world.BaseFinderModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.Arrays;
import java.util.List;

/**
 * End-to-end automated integration test executed inside a real booted Minecraft 1.21.11 client
 * via {@code fabric-client-gametest-api-v1}.
 *
 * <p>Verifies:
 * <ol>
 *     <li>Runtime SpongePowered Mixin audit across all 24 Mixins on Minecraft 1.21.11 bytecode</li>
 *     <li>DGClient v1.0.2 initialization, official KeyBinding registration, GUI opening via RSHIFT,
 *         staying open across multiple rendered frames (verifying same-frame close fix), rebinding,
 *         command opening (.gui), and closing</li>
 *     <li>Live in-world Suspicious Chunk detection across isolated player blocks, 1.21 Trial Chamber
 *         structures, real underground bases/stashes, cluster deduplication, and 3D render styles</li>
 *     <li>Live in-world StorageESP + StorageTracers detection, double-chest deduplication, batched
 *         3D line rendering, and immediate line removal when blocks are broken</li>
 *     <li>Regression check across existing DGClient modules</li>
 * </ol>
 */
@SuppressWarnings("resource")
public final class DGClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        DGLogger.info("[GameTest] Phase 1: Auditing Mixins and verifying DGClient v1.0.2 bootstrap...");
        context.runOnClient(mc -> {
            MixinEnvironment.getCurrentEnvironment().audit();

            DGClient dg = DGClient.getInstance();
            assertState(dg != null && dg.isReady(), "DGClient instance must be initialized and ready");
            assertState("1.0.2".equals(DGClient.VERSION), "Expected DGClient.VERSION == 1.0.2, got " + DGClient.VERSION);
            assertState(dg.getModuleManager().getModules().size() >= 46,
                    "Expected >= 46 registered modules, got " + dg.getModuleManager().getModules().size());

            KeyBinding guiKb = dg.getOpenGuiKeyBinding();
            assertState(guiKb != null, "Official openGuiKeyBinding must be registered");
            boolean inOptions = Arrays.asList(mc.options.allKeys).contains(guiKb);
            assertState(inOptions, "openGuiKeyBinding must be present in Minecraft GameOptions.allKeys");
            assertState(dg.getBoundGuiKeyCode() == GLFW.GLFW_KEY_RIGHT_SHIFT,
                    "Default GUI keybind must be RIGHT_SHIFT, got " + dg.getBoundGuiKeyCode());
        });

        try (TestSingleplayerContext sp = context.worldBuilder().create()) {
            sp.getClientWorld().waitForChunksRender();
            context.waitTicks(5);

            // =========================================================================
            // Phase 2: Real In-Game GUI Open, Stay-Open, Rebind & Close Verification
            // =========================================================================
            DGLogger.info("[GameTest] Phase 2: Testing ClickGUI keybind open, multi-frame stability, rebind & close...");

            // Press RIGHT_SHIFT via real simulated GLFW input
            context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
            context.waitTicks(4);

            context.runOnClient(mc -> assertState(
                    mc.currentScreen instanceof ClickGuiScreen,
                    "ClickGUI must open and STAY open after pressing RIGHT_SHIFT (currentScreen=" + mc.currentScreen + ")"
            ));
            context.takeScreenshot("dgclient_v102_clickgui_open");

            // Wait another 4 rendered frames (>100ms) and verify GUI is still open, then press RIGHT_SHIFT to close
            context.waitTicks(4);
            context.runOnClient(mc -> assertState(
                    mc.currentScreen instanceof ClickGuiScreen,
                    "ClickGUI must remain open across subsequent ticks"
            ));

            context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
            context.waitTicks(3);
            context.runOnClient(mc -> assertState(
                    mc.currentScreen == null,
                    "Pressing RIGHT_SHIFT while ClickGUI is open must cleanly close it"
            ));

            // Test rebinding GUI key via .bind command -> pressing new key -> closing with ESC -> restoring RIGHT_SHIFT
            context.runOnClient(mc -> {
                boolean handled = DGClient.getInstance().getCommandManager().tryExecute(".bind ClickGUI INSERT");
                assertState(handled, ".bind ClickGUI INSERT command must be handled");
                assertState(DGClient.getInstance().getBoundGuiKeyCode() == GLFW.GLFW_KEY_INSERT,
                        "Bound GUI key must sync to GLFW_KEY_INSERT");
            });
            context.waitTick();

            context.getInput().pressKey(GLFW.GLFW_KEY_INSERT);
            context.waitTicks(3);
            context.runOnClient(mc -> assertState(
                    mc.currentScreen instanceof ClickGuiScreen,
                    "ClickGUI must open via rebound INSERT key"
            ));

            context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
            context.waitTicks(2);
            context.runOnClient(mc -> {
                assertState(mc.currentScreen == null, "ESCAPE must close ClickGUI");
                DGClient.getInstance().getCommandManager().tryExecute(".bind ClickGUI RSHIFT");
                assertState(DGClient.getInstance().getBoundGuiKeyCode() == GLFW.GLFW_KEY_RIGHT_SHIFT,
                        "GUI key must restore to RIGHT_SHIFT");
            });

            // Test .gui command opening
            context.runOnClient(mc -> DGClient.getInstance().getCommandManager().tryExecute(".gui"));
            context.waitTicks(3);
            context.runOnClient(mc -> {
                assertState(mc.currentScreen instanceof ClickGuiScreen, ".gui command must open ClickGUI");
                mc.setScreen(null);
            });
            context.waitTicks(2);

            // =========================================================================
            // Phase 3: Multi-Scenario Live In-World Suspicious Chunk Detection
            // =========================================================================
            DGLogger.info("[GameTest] Phase 3: Building live world chunk scenarios & verifying BaseFinder...");

            int[] playerChunk = context.computeOnClient(mc -> new int[]{
                    mc.player.getChunkPos().x,
                    mc.player.getChunkPos().z,
                    mc.world.getBottomY()
            });
            int pcx = playerChunk[0];
            int pcz = playerChunk[1];
            int baseY = playerChunk[2] + 1;

            int isolatedCx = pcx + 2, isolatedCz = pcz;
            int trialCx = pcx, trialCz = pcz + 2;
            int baseACx = pcx - 2, baseACz = pcz;
            int baseBCx = pcx - 2, baseBCz = pcz + 1;

            sp.getServer().runOnServer(server -> {
                ServerWorld sw = server.getOverworld();

                // Scenario A: Isolated crafting table + cobblestone + 2 torches -> MUST NOT FLAG
                BlockPos isoOrigin = new BlockPos((isolatedCx << 4) + 6, baseY + 3, (isolatedCz << 4) + 6);
                sw.setBlockState(isoOrigin, Blocks.CRAFTING_TABLE.getDefaultState());
                sw.setBlockState(isoOrigin.east(), Blocks.COBBLESTONE.getDefaultState());
                sw.setBlockState(isoOrigin.up(), Blocks.TORCH.getDefaultState());

                // Scenario B: Simulated 1.21 Trial Chamber -> MUST NOT FLAG
                Block tuffBricks = block("tuff_bricks");
                Block waxedCopper = block("waxed_copper_block");
                Block copperBulb = block("copper_bulb");
                Block trialSpawner = block("trial_spawner");
                BlockPos tcOrigin = new BlockPos((trialCx << 4) + 3, baseY + 3, (trialCz << 4) + 3);
                for (int dx = 0; dx < 8; dx++) {
                    for (int dz = 0; dz < 8; dz++) {
                        sw.setBlockState(tcOrigin.add(dx, 0, dz), (dx + dz) % 2 == 0
                                ? tuffBricks.getDefaultState()
                                : waxedCopper.getDefaultState());
                    }
                }
                sw.setBlockState(tcOrigin.add(1, 1, 1), Blocks.BARREL.getDefaultState());
                sw.setBlockState(tcOrigin.add(2, 1, 1), Blocks.BARREL.getDefaultState());
                sw.setBlockState(tcOrigin.add(3, 1, 1), Blocks.BARREL.getDefaultState());
                sw.setBlockState(tcOrigin.add(4, 1, 4), trialSpawner.getDefaultState());
                sw.setBlockState(tcOrigin.add(5, 1, 4), copperBulb.getDefaultState());

                // Scenario C: Real Player Base Primary Chunk (-2, 0) -> MUST FLAG HIGH CONFIDENCE
                BlockPos baseA = new BlockPos((baseACx << 4) + 4, baseY + 3, (baseACz << 4) + 4);
                BlockState rotatedDeepslate = Blocks.DEEPSLATE.getDefaultState().with(Properties.AXIS, Direction.Axis.X);
                for (int i = 0; i < 8; i++) {
                    sw.setBlockState(baseA.add(i, 0, 0), rotatedDeepslate);
                }
                sw.setBlockState(baseA.add(0, 1, 1), Blocks.SHULKER_BOX.getDefaultState());
                sw.setBlockState(baseA.add(1, 1, 1), Blocks.CYAN_SHULKER_BOX.getDefaultState());
                sw.setBlockState(baseA.add(2, 1, 1), Blocks.ENDER_CHEST.getDefaultState());
                sw.setBlockState(baseA.add(3, 1, 1), Blocks.CHEST.getDefaultState());
                sw.setBlockState(baseA.add(4, 1, 1), Blocks.BARREL.getDefaultState());
                sw.setBlockState(baseA.add(0, 1, 2), Blocks.CRAFTING_TABLE.getDefaultState());
                sw.setBlockState(baseA.add(1, 1, 2), Blocks.ENCHANTING_TABLE.getDefaultState());
                sw.setBlockState(baseA.add(2, 1, 2), Blocks.ANVIL.getDefaultState());
                sw.setBlockState(baseA.add(3, 1, 2), Blocks.OBSERVER.getDefaultState());
                sw.setBlockState(baseA.add(4, 1, 2), Blocks.PISTON.getDefaultState());

                // Scenario D: Adjacent Player Base Farm/Smelter Room (-2, 1) -> MUST MERGE INTO CLUSTER
                BlockPos baseB = new BlockPos((baseBCx << 4) + 4, baseY + 3, (baseBCz << 4) + 4);
                for (int i = 0; i < 6; i++) {
                    sw.setBlockState(baseB.add(i, 0, 0), rotatedDeepslate);
                }
                sw.setBlockState(baseB.add(0, 1, 1), Blocks.SHULKER_BOX.getDefaultState());
                sw.setBlockState(baseB.add(1, 1, 1), Blocks.BARREL.getDefaultState());
                sw.setBlockState(baseB.add(2, 1, 1), Blocks.BARREL.getDefaultState());
                sw.setBlockState(baseB.add(3, 1, 1), Blocks.HOPPER.getDefaultState());
                sw.setBlockState(baseB.add(0, 1, 2), Blocks.BLAST_FURNACE.getDefaultState());
                sw.setBlockState(baseB.add(1, 1, 2), Blocks.SMOKER.getDefaultState());
                sw.setBlockState(baseB.add(2, 1, 2), Blocks.OBSERVER.getDefaultState());
            });

            // Wait for block updates to sync to client chunks
            context.waitTicks(8);

            context.runOnClient(mc -> {
                DGClient dg = DGClient.getInstance();
                dg.getDetectionRegistry().clearAll();
                BaseFinderModule bf = dg.getModuleManager().get(BaseFinderModule.class);
                assertState(bf != null, "BaseFinderModule must exist");
                bf.sensitivity.setValue(SuspiciousChunkAnalyzer.Sensitivity.BALANCED);

                WorldChunk isoChunk = mc.world.getChunkManager().getWorldChunk(isolatedCx, isolatedCz);
                WorldChunk tcChunk = mc.world.getChunkManager().getWorldChunk(trialCx, trialCz);
                WorldChunk bAChunk = mc.world.getChunkManager().getWorldChunk(baseACx, baseACz);
                WorldChunk bBChunk = mc.world.getChunkManager().getWorldChunk(baseBCx, baseBCz);

                assertState(isoChunk != null && tcChunk != null && bAChunk != null && bBChunk != null,
                        "All 4 test scenario chunks must be loaded on client");

                DetectionEntry isoEntry = bf.scanChunkImmediate(isoChunk);
                assertState(isoEntry == null,
                        "Isolated crafting table + cobble chunk MUST NOT be flagged as a base!");

                DetectionEntry tcEntry = bf.scanChunkImmediate(tcChunk);
                assertState(tcEntry == null,
                        "1.21 Trial Chamber chunk MUST NOT be flagged as a player base!");

                DetectionEntry baseAEntry = bf.scanChunkImmediate(bAChunk);
                assertState(baseAEntry != null, "Real player base chunk A must be detected!");
                assertState(baseAEntry.getConfidence() == SuspiciousChunkAnalyzer.Confidence.HIGH,
                        "Player base chunk A must have HIGH confidence, got " + baseAEntry.getConfidence()
                                + " (" + baseAEntry.getScore() + "%)");

                DetectionEntry baseBEntry = bf.scanChunkImmediate(bBChunk);
                assertState(baseBEntry != null, "Adjacent base room chunk B must be detected!");

                List<DetectionEntry> clusters = dg.getDetectionRegistry().getDeduplicatedClusters();
                assertState(clusters.size() == 1,
                        "Adjacent base chunks A & B must deduplicate into 1 cluster, got " + clusters.size());
                assertState(clusters.getFirst().getClusterSize() == 2,
                        "Deduplicated cluster size must be 2, got " + clusters.getFirst().getClusterSize());

                bf.setEnabled(true, true);
            });

            // Exercise all 3 redesigned chunk visual styles through real 3D OpenGL rendering frames
            for (BaseFinderModule.RenderStyle style : BaseFinderModule.RenderStyle.values()) {
                context.runOnClient(mc -> {
                    BaseFinderModule bf = DGClient.getInstance().getModuleManager().get(BaseFinderModule.class);
                    bf.renderStyle.setValue(style);
                });
                context.waitTicks(3);
            }
            context.takeScreenshot("dgclient_v102_basefinder_render");

            // =========================================================================
            // Phase 4: Storage ESP, Storage Tracers, Double-Chest Merge & Live Removal
            // =========================================================================
            DGLogger.info("[GameTest] Phase 4: Verifying StorageESP + StorageTracers + live block removal...");

            BlockPos nearOrigin = context.computeOnClient(mc -> mc.player.getBlockPos().add(4, 0, 4));
            BlockPos shulkerPos = nearOrigin.add(3, 0, 0);

            sp.getServer().runOnServer(server -> {
                ServerWorld sw = server.getOverworld();
                // Place a merged double chest facing NORTH (LEFT at nearOrigin, RIGHT at nearOrigin.east())
                BlockState leftChest = Blocks.CHEST.getDefaultState()
                        .with(ChestBlock.FACING, Direction.NORTH)
                        .with(ChestBlock.CHEST_TYPE, ChestType.LEFT);
                BlockState rightChest = Blocks.CHEST.getDefaultState()
                        .with(ChestBlock.FACING, Direction.NORTH)
                        .with(ChestBlock.CHEST_TYPE, ChestType.RIGHT);
                sw.setBlockState(nearOrigin, leftChest);
                sw.setBlockState(nearOrigin.east(), rightChest);
                sw.setBlockState(shulkerPos, block("magenta_shulker_box").getDefaultState());
                sw.setBlockState(nearOrigin.add(4, 0, 0), block("ender_chest").getDefaultState());
                sw.setBlockState(nearOrigin.add(5, 0, 0), block("oak_shelf").getDefaultState());
            });

            context.waitTicks(6);

            context.runOnClient(mc -> {
                DGClient dg = DGClient.getInstance();
                StorageEspModule esp = dg.getModuleManager().get(StorageEspModule.class);
                StorageTracersModule tracers = dg.getModuleManager().get(StorageTracersModule.class);
                assertState(esp != null && tracers != null, "StorageESP and StorageTracers must be registered");

                esp.shelves.setValue(true);
                tracers.shelves.setValue(true);
                esp.setEnabled(true, true);
                tracers.setEnabled(true, true);

                // Verify tracker within 16m around player: 2 double-chest blocks must merge into 1 CHEST target
                // + 1 SHULKER_BOX + 1 ENDER_CHEST + 1 SHELF = 4 resolved targets from 5 block entities!
                List<StorageBlockTracker.ResolvedStorage> nearby = dg.getStorageBlockTracker()
                        .collectVisible(16.0, 64, true, tracers::acceptsKind);
                assertState(nearby.size() == 4,
                        "Expected 4 resolved nearby storage targets (with merged double chest), got " + nearby.size());
            });

            // Let WorldRenderEvents.END_MAIN render StorageESP boxes + batched StorageTracers lines
            context.waitTicks(4);
            context.takeScreenshot("dgclient_v102_storage_tracers");

            context.runOnClient(mc -> {
                StorageTracersModule tracers = DGClient.getInstance().getModuleManager().get(StorageTracersModule.class);
                int renderedLines = Integer.parseInt(tracers.getDisplayInfo());
                assertState(renderedLines >= 4,
                        "StorageTracers must render >= 4 active lines, got " + renderedLines);
            });

            // Break the shulker box on the server and verify immediate removal on next rendered ticks
            sp.getServer().runOnServer(server ->
                    server.getOverworld().setBlockState(shulkerPos, Blocks.AIR.getDefaultState()));
            context.waitTicks(4);

            context.runOnClient(mc -> {
                DGClient dg = DGClient.getInstance();
                StorageTracersModule tracers = dg.getModuleManager().get(StorageTracersModule.class);
                List<StorageBlockTracker.ResolvedStorage> nearbyAfterBreak = dg.getStorageBlockTracker()
                        .collectVisible(16.0, 64, true, tracers::acceptsKind);
                assertState(nearbyAfterBreak.size() == 3,
                        "Broken shulker box must immediately disappear from StorageBlockTracker (expected 3, got "
                                + nearbyAfterBreak.size() + ")");
            });

            // =========================================================================
            // Phase 5: Regression Check Across Existing Modules
            // =========================================================================
            DGLogger.info("[GameTest] Phase 5: Running live regression check across existing visual/HUD/ESP modules...");
            List<Class<? extends Module>> regressionModules = List.of(
                    FullbrightModule.class,
                    CustomCrosshairModule.class,
                    BlockHighlightModule.class,
                    ChunkBordersModule.class,
                    TracersModule.class
            );
            context.runOnClient(mc -> {
                for (Class<? extends Module> cls : regressionModules) {
                    Module m = DGClient.getInstance().getModuleManager().get(cls);
                    assertState(m != null, "Missing regression module: " + cls.getSimpleName());
                    m.setEnabled(true, true);
                }
            });
            context.waitTicks(5);
            context.runOnClient(mc -> {
                for (Class<? extends Module> cls : regressionModules) {
                    Module m = DGClient.getInstance().getModuleManager().get(cls);
                    assertState(m.isEnabled(),
                            "Module " + m.getName() + " must remain enabled without fault-handler crash!");
                    m.setEnabled(false, true);
                }
            });

            DGLogger.info("[GameTest] ALL DGClient v1.0.2 in-game verification checks PASSED!");
        }
    }

    private static Block block(String id) {
        return Registries.BLOCK.get(Identifier.ofVanilla(id));
    }

    private static void assertState(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("[DGClientGameTest] " + message);
        }
    }
}
