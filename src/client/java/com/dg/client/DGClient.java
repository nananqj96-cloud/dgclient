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
import com.dg.client.hud.HudManager;
import com.dg.client.module.ModuleManager;
import com.dg.client.module.modules.esp.EntityEspModule;
import com.dg.client.module.modules.esp.PlayerEspModule;
import com.dg.client.module.modules.esp.SpawnerEspModule;
import com.dg.client.module.modules.esp.StorageEspModule;
import com.dg.client.module.modules.esp.TracersModule;
import com.dg.client.module.modules.hud.ClickGuiModule;
import com.dg.client.module.modules.hud.HudModule;
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
import com.dg.client.render.Render2DUtil;
import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.util.ColorUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * Primary client-only entrypoint for DGClient (Minecraft 1.21.1, Fabric).
 */
public final class DGClient implements ClientModInitializer {
    public static final String MOD_ID = "dgclient";
    public static final String NAME = "DGClient";
    public static final String VERSION = "1.0.0";

    private static DGClient instance;

    private final EventBus eventBus = new EventBus();
    private final NotificationManager notificationManager = new NotificationManager();
    private final FriendManager friendManager = new FriendManager();
    private final WaypointManager waypointManager = new WaypointManager();
    private final DetectionRegistry detectionRegistry = new DetectionRegistry();
    private final AsyncScannerPool scannerPool = new AsyncScannerPool();
    private final ModuleManager moduleManager = new ModuleManager();
    private final HudManager hudManager = new HudManager();
    private final CommandManager commandManager = new CommandManager();
    private final ServerRateTracker serverRateTracker = new ServerRateTracker();
    private final ConfigManager configManager = new ConfigManager();

    private ClickGuiScreen clickGuiScreen;
    private volatile boolean ready = false;

    public static DGClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        DGLogger.info("Initializing {} v{} for Minecraft 1.21.1...", NAME, VERSION);

        moduleManager.init();
        registerModules();

        hudManager.init();
        commandManager.init();
        serverRateTracker.init();
        configManager.loadAll();

        registerFabricHooks();
        this.ready = true;
        DGLogger.info("{} v{} initialized ({} modules loaded, profile='{}').",
                NAME, VERSION, moduleManager.getModules().size(), configManager.getActiveProfile());
    }

    private void registerModules() {
        // Core HUD & GUI modules
        moduleManager.register(new ClickGuiModule());
        moduleManager.register(new HudModule());

        // Phase 2: 18 Visual Modules
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

        // Phase 3: ESP & Tracers
        moduleManager.register(new PlayerEspModule());
        moduleManager.register(new EntityEspModule());
        moduleManager.register(new StorageEspModule());
        moduleManager.register(new SpawnerEspModule());
        moduleManager.register(new TracersModule());
    }

    private void registerFabricHooks() {
        // Sodium-safe block outline suppression when BlockHighlightModule is active
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hitResult) -> {
            BlockHighlightModule bh = moduleManager.get(BlockHighlightModule.class);
            return bh == null || !bh.isEnabled();
        });

        // 3D World Render hook (compatible with Sodium & Iris)
        WorldRenderEvents.LAST.register(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.player == null) {
                return;
            }
            MatrixStack matrices = context.matrixStack() != null ? context.matrixStack() : new MatrixStack();
            float delta = context.tickCounter().getTickDelta(false);
            eventBus.post(new Render3DEvent(
                    matrices,
                    context.positionMatrix(),
                    context.projectionMatrix(),
                    context.camera(),
                    context.frustum(),
                    context.world(),
                    delta
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
                    tickCounter.getTickDelta(false)
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
                        btn -> client.setScreen(getClickGuiScreen())
                ).dimensions(scaledWidth - 88, 8, 80, 20).build();
                Screens.getButtons(screen).add(dgBtn);

                ScreenEvents.afterRender(screen).register((scr, drawContext, mouseX, mouseY, tickDelta) -> {
                    MatrixStack ms = drawContext.getMatrices();
                    Render2DUtil.drawGaussianGlow(ms, 8.0f, 8.0f, 152.0f, 22.0f, 6.0f, 8.0f, ColorUtil.NEON_GREEN, 0.85f);
                    Render2DUtil.drawRoundedRect(ms, 8.0f, 8.0f, 152.0f, 22.0f, 6.0f, ColorUtil.PANEL_DARK);
                    Render2DUtil.drawRoundedOutline(ms, 8.0f, 8.0f, 152.0f, 22.0f, 6.0f, 1.0f, ColorUtil.NEON_GREEN);
                    DGFontRenderer.drawAnimatedGradientText(drawContext, "DGClient v" + VERSION, 15.0f, 15.0f, true, 0.96f, 1.2f, 0.8f);
                    DGFontRenderer.drawScaledText(drawContext, "RSHIFT", 122.0f, 15.5f, ColorUtil.SOFT_WHITE, false, 0.78f, false);
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

    public boolean isReady() {
        return ready;
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
