package com.dg.client;

import com.dg.client.command.CommandManager;
import com.dg.client.core.AsyncScannerPool;
import com.dg.client.core.ConfigManager;
import com.dg.client.core.DGLogger;
import com.dg.client.core.FriendManager;
import com.dg.client.core.NotificationManager;
import com.dg.client.core.ServerRateTracker;
import com.dg.client.core.WaypointManager;
import com.dg.client.core.detection.DetectionRegistry;
import com.dg.client.event.EventBus;
import com.dg.client.event.events.ChunkDataEvent;
import com.dg.client.event.events.EntityEvent;
import com.dg.client.event.events.Render2DEvent;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.gui.ClickGuiScreen;
import com.dg.client.hud.HudEditorScreen;
import com.dg.client.hud.HudManager;
import com.dg.client.module.ModuleManager;
import com.dg.client.module.modules.esp.EntityEspModule;
import com.dg.client.module.modules.esp.PlayerEspModule;
import com.dg.client.module.modules.esp.SpawnerEspModule;
import com.dg.client.module.modules.esp.StorageBlockTracker;
import com.dg.client.module.modules.esp.StorageEspModule;
import com.dg.client.module.modules.esp.StorageTracersModule;
import com.dg.client.module.modules.esp.TracersModule;
import com.dg.client.module.modules.fun.HeadAnimationsModule;
import com.dg.client.module.modules.fun.QueueNotifierModule;
import com.dg.client.module.modules.fun.ScreenshotToolModule;
import com.dg.client.module.modules.fun.VisualRangeNotifierModule;
import com.dg.client.module.modules.hud.ClickGuiModule;
import com.dg.client.module.modules.hud.HudModule;
import com.dg.client.module.modules.misc.AutoArmorModule;
import com.dg.client.module.modules.misc.AutoEatModule;
import com.dg.client.module.modules.misc.AutoLogModule;
import com.dg.client.module.modules.misc.AutoReconnectModule;
import com.dg.client.module.modules.misc.AutoTotemModule;
import com.dg.client.module.modules.misc.ChatUtilsModule;
import com.dg.client.module.modules.misc.InventoryHelpersModule;
import com.dg.client.module.modules.misc.SessionStatsModule;
import com.dg.client.module.modules.movement.AutoWalkModule;
import com.dg.client.module.modules.movement.ElytraHelperModule;
import com.dg.client.module.modules.movement.FreecamModule;
import com.dg.client.module.modules.movement.HighwayHelperModule;
import com.dg.client.module.modules.movement.NoFallModule;
import com.dg.client.module.modules.render.BetterTabListModule;
import com.dg.client.module.modules.render.BlockHighlightModule;
import com.dg.client.module.modules.render.BloomModule;
import com.dg.client.module.modules.render.ChunkBordersModule;
import com.dg.client.module.modules.render.CustomCrosshairModule;
import com.dg.client.module.modules.render.DamageIndicatorsModule;
import com.dg.client.module.modules.render.FullbrightModule;
import com.dg.client.module.modules.render.HandItemGlowModule;
import com.dg.client.module.modules.render.HitGlowModule;
import com.dg.client.module.modules.render.ItemGlowModule;
import com.dg.client.module.modules.render.NametagsModule;
import com.dg.client.module.modules.render.SkyFogModule;
import com.dg.client.module.modules.render.TimeChangerModule;
import com.dg.client.module.modules.render.TotemPopEffectsModule;
import com.dg.client.module.modules.render.TrailsModule;
import com.dg.client.module.modules.render.ViewmodelModule;
import com.dg.client.module.modules.render.WeatherParticleReducerModule;
import com.dg.client.module.modules.render.ZoomModule;
import com.dg.client.module.modules.world.BaseFinderModule;
import com.dg.client.module.modules.world.CoordLoggerModule;
import com.dg.client.module.modules.world.FastUseBreakModule;
import com.dg.client.module.modules.world.WaypointsModule;
import com.dg.client.module.modules.world.YZeroBypassModule;
import com.dg.client.module.modules.world.YZeroDiagnosticsModule;
import com.dg.client.render.Render2DUtil;
import com.dg.client.render.Render3DUtil;
import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.util.ColorUtil;
import com.dg.client.util.KeyUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Primary client-only entrypoint for DGClient (Minecraft 1.21.11, Fabric).
 */
public final class DGClient implements ClientModInitializer {
    public static final String MOD_ID = "dgclient";
    public static final String NAME = "DGClient";
    public static final String VERSION = "1.0.2";

    private static DGClient instance;

    private Matrix4f lastViewMatrix = new Matrix4f();
    private Matrix4f lastProjectionMatrix = new Matrix4f();
    private Frustum lastFrustum;
    private Camera lastCamera;
    private float lastTickProgress = 1.0f;

    private final EventBus eventBus = new EventBus();
    private final NotificationManager notificationManager = new NotificationManager();
    private final FriendManager friendManager = new FriendManager();
    private final WaypointManager waypointManager = new WaypointManager();
    private final DetectionRegistry detectionRegistry = new DetectionRegistry();
    private final AsyncScannerPool scannerPool = new AsyncScannerPool();
    private final StorageBlockTracker storageBlockTracker = new StorageBlockTracker();
    private final ModuleManager moduleManager = new ModuleManager();
    private final HudManager hudManager = new HudManager();
    private final CommandManager commandManager = new CommandManager();
    private final ServerRateTracker serverRateTracker = new ServerRateTracker();
    private final ConfigManager configManager = new ConfigManager();

    private KeyBinding.Category keyCategory;
    private KeyBinding openGuiKeyBinding;
    private KeyBinding hudEditorKeyBinding;
    private int lastSyncedGuiKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private ClickGuiScreen clickGuiScreen;
    private volatile boolean ready = false;

    public static DGClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        DGLogger.info("Initializing {} v{} for Minecraft 1.21.11...", NAME, VERSION);

        Render3DUtil.initPipelines();
        registerKeyBindings();
        eventBus.subscribe(storageBlockTracker);
        moduleManager.init();
        registerModules();

        hudManager.init();
        commandManager.init();
        serverRateTracker.init();
        configManager.loadAll();
        syncGuiKeybindFromModule();

        registerFabricHooks();
        this.ready = true;
        DGLogger.info("{} v{} initialized ({} modules loaded, profile='{}', guiKey='{}').",
                NAME, VERSION, moduleManager.getModules().size(),
                configManager.getActiveProfile(), KeyUtil.getKeyName(getBoundGuiKeyCode()));
    }

    private void registerKeyBindings() {
        keyCategory = KeyBinding.Category.create(Identifier.of(MOD_ID, "general"));
        openGuiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dgclient.open_gui",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                keyCategory
        ));
        hudEditorKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dgclient.hud_editor",
                GLFW.GLFW_KEY_UNKNOWN,
                keyCategory
        ));
    }

    private void registerModules() {
        // Core HUD & GUI modules
        moduleManager.register(new ClickGuiModule());
        moduleManager.register(new HudModule());

        // Visual Modules
        moduleManager.register(new HandItemGlowModule());
        moduleManager.register(new FullbrightModule());
        moduleManager.register(new CustomCrosshairModule());
        moduleManager.register(new BlockHighlightModule());
        moduleManager.register(new HitGlowModule());
        moduleManager.register(new TotemPopEffectsModule());
        moduleManager.register(new DamageIndicatorsModule());
        moduleManager.register(new ItemGlowModule());
        moduleManager.register(new ZoomModule());
        moduleManager.register(new ViewmodelModule());
        moduleManager.register(new SkyFogModule());
        moduleManager.register(new TimeChangerModule());
        moduleManager.register(new BloomModule());
        moduleManager.register(new NametagsModule());
        moduleManager.register(new TrailsModule());
        moduleManager.register(new ChunkBordersModule());
        moduleManager.register(new WeatherParticleReducerModule());
        moduleManager.register(new BetterTabListModule());

        // ESP & Tracers
        moduleManager.register(new PlayerEspModule());
        moduleManager.register(new EntityEspModule());
        moduleManager.register(new StorageEspModule());
        moduleManager.register(new StorageTracersModule());
        moduleManager.register(new SpawnerEspModule());
        moduleManager.register(new TracersModule());

        // Y-0 Diagnostics & Underground Bypass
        moduleManager.register(new YZeroDiagnosticsModule());
        moduleManager.register(new YZeroBypassModule());

        // Suspicious Chunk Detector ("BaseFinder")
        moduleManager.register(new BaseFinderModule());

        // Movement, World, Fun & Utility Modules
        moduleManager.register(new AutoWalkModule());
        moduleManager.register(new ElytraHelperModule());
        moduleManager.register(new NoFallModule());
        moduleManager.register(new HighwayHelperModule());
        moduleManager.register(new FreecamModule());

        moduleManager.register(new FastUseBreakModule());
        moduleManager.register(new WaypointsModule());
        moduleManager.register(new CoordLoggerModule());

        moduleManager.register(new HeadAnimationsModule());
        moduleManager.register(new ScreenshotToolModule());
        moduleManager.register(new VisualRangeNotifierModule());
        moduleManager.register(new QueueNotifierModule());

        moduleManager.register(new AutoTotemModule());
        moduleManager.register(new AutoArmorModule());
        moduleManager.register(new AutoEatModule());
        moduleManager.register(new InventoryHelpersModule());
        moduleManager.register(new ChatUtilsModule());
        moduleManager.register(new AutoReconnectModule());
        moduleManager.register(new AutoLogModule());
        moduleManager.register(new SessionStatsModule());
    }

    private void registerFabricHooks() {
        // Bi-directional sync and reliable tick polling for official 1.21.11 KeyBindings
        ClientTickEvents.END_CLIENT_TICK.register(this::handleKeybindingTick);

        // Sodium-safe block outline suppression when BlockHighlightModule is active
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outlineRenderState) -> {
            BlockHighlightModule bh = moduleManager.get(BlockHighlightModule.class);
            return bh == null || !bh.isEnabled();
        });

        // Capture per-frame view/projection/frustum state at end of world extraction
        WorldRenderEvents.END_EXTRACTION.register(context -> {
            lastViewMatrix = new Matrix4f(context.viewMatrix());
            lastProjectionMatrix = new Matrix4f(context.cullProjectionMatrix());
            lastFrustum = context.frustum();
            lastCamera = context.camera();
            lastTickProgress = context.tickCounter().getTickProgress(false);
        });

        // 3D World Render hook at END_MAIN (compatible with Sodium & Iris on 1.21.11)
        WorldRenderEvents.END_MAIN.register(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.player == null) {
                return;
            }
            MatrixStack matrices = context.matrices() != null ? context.matrices() : new MatrixStack();
            Camera cam = lastCamera != null ? lastCamera : context.gameRenderer().getCamera();
            eventBus.post(new Render3DEvent(
                    matrices,
                    lastViewMatrix,
                    lastProjectionMatrix,
                    cam,
                    lastFrustum,
                    mc.world,
                    lastTickProgress
            ));
        });

        // 2D HUD Render hook
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getWindow() == null) {
                return;
            }
            eventBus.post(new Render2DEvent(
                    drawContext,
                    mc.getWindow().getScaledWidth(),
                    mc.getWindow().getScaledHeight(),
                    tickCounter.getTickProgress(false)
            ));
        });

        // Chunk Load / Unload hooks
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) ->
                eventBus.post(new ChunkDataEvent.Load(world, chunk)));
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) ->
                eventBus.post(new ChunkDataEvent.Unload(world, chunk)));

        // Entity Add / Remove hooks
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) ->
                eventBus.post(new EntityEvent.Add(entity, world)));
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) ->
                eventBus.post(new EntityEvent.Remove(entity, world)));

        // Custom Main Menu emblem & DGClient quick-launch button
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                ButtonWidget dgBtn = ButtonWidget.builder(
                        Text.literal("DGClient"),
                        btn -> openClickGui()
                ).dimensions(scaledWidth - 88, 8, 80, 20).build();
                Screens.getButtons(screen).add(dgBtn);

                ScreenEvents.afterRender(screen).register((scr, drawContext, mouseX, mouseY, tickDelta) -> {
                    Render2DUtil.setContext(drawContext);
                    Matrix3x2fStack ms = drawContext.getMatrices();
                    String keyBadge = KeyUtil.getKeyName(getBoundGuiKeyCode());
                    float boxW = Math.max(156.0f, 126.0f + DGFontRenderer.getWidth(keyBadge, false, 0.78f));
                    Render2DUtil.drawGaussianGlow(ms, 8.0f, 8.0f, boxW, 22.0f, 6.0f, 8.0f, ColorUtil.NEON_GREEN, 0.85f);
                    Render2DUtil.drawRoundedRect(ms, 8.0f, 8.0f, boxW, 22.0f, 6.0f, ColorUtil.PANEL_DARK);
                    Render2DUtil.drawRoundedOutline(ms, 8.0f, 8.0f, boxW, 22.0f, 6.0f, 1.0f, ColorUtil.NEON_GREEN);
                    DGFontRenderer.drawAnimatedGradientText(drawContext, "DGClient v" + VERSION, 15.0f, 15.0f, true, 0.96f, 1.2f, 0.8f);
                    DGFontRenderer.drawScaledText(drawContext, keyBadge, 122.0f, 15.5f, ColorUtil.SOFT_WHITE, false, 0.78f, false);
                });
            }
        });

        // Clean shutdown persistence
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            DGLogger.info("Saving DGClient configuration and shutting down worker threads...");
            configManager.saveAll();
            scannerPool.shutdown();
        });
    }

    private void handleKeybindingTick(MinecraftClient client) {
        if (openGuiKeyBinding == null) {
            return;
        }
        // Keep Minecraft Controls screen & ClickGuiModule's KeybindSetting in sync
        ClickGuiModule guiModule = moduleManager.get(ClickGuiModule.class);
        int boundCode = getBoundGuiKeyCode();
        if (guiModule != null) {
            int moduleCode = guiModule.getKeybind().getValue();
            if (boundCode != lastSyncedGuiKey) {
                guiModule.getKeybind().setValue(boundCode);
                lastSyncedGuiKey = boundCode;
            } else if (moduleCode != lastSyncedGuiKey) {
                syncGuiKeybindFromModule();
            }
        }

        boolean wantOpenGui = false;
        while (openGuiKeyBinding.wasPressed()) {
            wantOpenGui = true;
        }
        if (wantOpenGui && client.currentScreen == null) {
            openClickGui();
        }

        if (hudEditorKeyBinding != null) {
            boolean wantHudEditor = false;
            while (hudEditorKeyBinding.wasPressed()) {
                wantHudEditor = true;
            }
            if (wantHudEditor && client.currentScreen == null) {
                client.setScreen(new HudEditorScreen(false));
            }
        }
    }

    /**
     * Synchronizes {@link ClickGuiModule#getKeybind()} into the registered Minecraft {@link KeyBinding}.
     */
    public void syncGuiKeybindFromModule() {
        if (openGuiKeyBinding == null) {
            return;
        }
        ClickGuiModule guiModule = moduleManager.get(ClickGuiModule.class);
        int code = guiModule != null ? guiModule.getKeybind().getValue() : GLFW.GLFW_KEY_RIGHT_SHIFT;
        InputUtil.Key key = code <= 0
                ? InputUtil.UNKNOWN_KEY
                : InputUtil.Type.KEYSYM.createFromCode(code);
        openGuiKeyBinding.setBoundKey(key);
        KeyBinding.updateKeysByCode();
        lastSyncedGuiKey = code;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null) {
            mc.options.write();
        }
    }

    public int getBoundGuiKeyCode() {
        if (openGuiKeyBinding == null) {
            return GLFW.GLFW_KEY_RIGHT_SHIFT;
        }
        InputUtil.Key k = KeyBindingHelper.getBoundKeyOf(openGuiKeyBinding);
        return k == null || k == InputUtil.UNKNOWN_KEY ? GLFW.GLFW_KEY_UNKNOWN : k.getCode();
    }

    public void drainGuiKeyBindings() {
        if (openGuiKeyBinding != null) {
            openGuiKeyBinding.setPressed(false);
            while (openGuiKeyBinding.wasPressed()) {
                // drain queued presses so opening via KeyboardMixin never triggers a second toggle on tick
            }
        }
    }

    public void openClickGui() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return;
        }
        drainGuiKeyBindings();
        ClickGuiScreen screen = getClickGuiScreen();
        if (mc.currentScreen != screen) {
            mc.setScreen(screen);
        }
    }

    public KeyBinding getOpenGuiKeyBinding() {
        return openGuiKeyBinding;
    }

    public boolean isReady() {
        return ready;
    }

    public Matrix4f getLastViewMatrix() {
        return lastViewMatrix;
    }

    public Matrix4f getLastProjectionMatrix() {
        return lastProjectionMatrix;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }

    public WaypointManager getWaypointManager() {
        return waypointManager;
    }

    public DetectionRegistry getDetectionRegistry() {
        return detectionRegistry;
    }

    public AsyncScannerPool getScannerPool() {
        return scannerPool;
    }

    public StorageBlockTracker getStorageBlockTracker() {
        return storageBlockTracker;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ServerRateTracker getServerRateTracker() {
        return serverRateTracker;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ClickGuiScreen getClickGuiScreen() {
        if (clickGuiScreen == null) {
            clickGuiScreen = new ClickGuiScreen();
        }
        return clickGuiScreen;
    }
}
