package dev.aether;

import dev.aether.config.ConfigManager;
import dev.aether.config.ConfigStore;
import dev.aether.gui.ClickGuiScreen;
import dev.aether.hud.HudManager;
import dev.aether.module.Category;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.FreecamModule;
import dev.aether.notification.NotificationManager;
import dev.aether.render.BlockEspRenderer;
import dev.aether.render.ClusterEspRenderer;
import dev.aether.render.MotionBlurRenderer;
import dev.aether.render.OverlayRenderer;
import dev.aether.render.StorageEspRenderer;
import dev.aether.render.SusChunkRenderer;
import dev.aether.spotify.SpotifyService;
import dev.aether.suschunk.ServerLightCache;
import dev.aether.theme.SoundSettings;
import dev.aether.theme.ThemeManager;
import dev.aether.util.UiSoundEvents;
import dev.aether.util.UiSounds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AetherClient implements ClientModInitializer {
   public static final String MOD_ID = "aetherclient";
   public static final String NAME = "Aether Client";
   public static final String VERSION = "2.0.0";
   public static final Logger LOGGER = LoggerFactory.getLogger("AetherClient");
   private static ModuleManager modules;
   private static ThemeManager themes;
   private static ConfigManager config;
   private static ConfigStore configStore;
   private static HudManager hud;
   private static NotificationManager notifications;
   private static SpotifyService spotify;
   private static SoundSettings soundSettings;
   private static boolean startupSoundPlayed;

   public static ModuleManager modules() {
      return modules;
   }

   public static ThemeManager themes() {
      return themes;
   }

   public static ConfigManager config() {
      return config;
   }

   public static ConfigStore configStore() {
      return configStore;
   }

   public static HudManager hud() {
      return hud;
   }

   public static SpotifyService spotify() {
      return spotify;
   }

   public static SoundSettings sounds() {
      return soundSettings;
   }

   public static NotificationManager notifications() {
      return notifications;
   }

   @Override
   public void onInitializeClient() {
      LOGGER.info("{} {} initializing", NAME, VERSION);
      UiSoundEvents.bootstrap();
      themes = new ThemeManager();
      soundSettings = new SoundSettings();
      modules = new ModuleManager();
      spotify = new SpotifyService();
      notifications = new NotificationManager(themes, modules.hud);
      hud = new HudManager(modules, themes, spotify, notifications);
      config = new ConfigManager(modules, themes);
      config.addSection("hud", hud::toJson, hud::fromJson);
      config.addSection("panels", ClickGuiScreen.state()::toJson, ClickGuiScreen.state()::fromJson);
      config.addSection("sounds", soundSettings::toJson, soundSettings::fromJson);
      config.load();
      configStore = new ConfigStore(config);
      configStore.loadAll();
      UiSounds.init(soundSettings);
      modules.setOpenGuiAction(() -> Minecraft.getInstance().setScreen(new ClickGuiScreen()));
      modules.setToggleListener((mod, enabled) -> {
         if (!ConfigStore.applying && mod.getCategory() != Category.CLIENT && Minecraft.getInstance().level != null && modules.hud.notifications.get()) {
            notifications.push(mod.getName(), enabled);
            UiSounds.notification(enabled);
         }
      });
      OverlayRenderer.init(hud, notifications);
      ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
         if (screen instanceof TitleScreen && !startupSoundPlayed) {
            startupSoundPlayed = true;
            UiSounds.playStartup();
         }
      });
      spotify.start();
      ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> clearSusState());
      ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearSusState());
      ClientTickEvents.END_CLIENT_TICK.register(client -> modules.onTick());
      ClientTickEvents.START_CLIENT_TICK.register(client -> {
         FreecamModule freecam = modules.freecam;
         if (freecam != null && freecam.isActive()) {
            FreecamModule.reapplyBodyInput(client);
         }
         modules.onCombatTick();
      });
      ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
         config.save();
         spotify.stop();
      });
   }

   private static void clearSusState() {
      BlockEspRenderer.clear();
      ClusterEspRenderer.clear();
      StorageEspRenderer.clear();
      ServerLightCache.get().clear();
      modules.susChunkFinder.scanner.clear();
      SusChunkRenderer.reset();
      if (modules.chunkFinder != null) {
         modules.chunkFinder.clear();
      }
      if (modules.spawnerNametags != null) {
         modules.spawnerNametags.clear();
      }
      if (modules.jumpCircles != null) {
         modules.jumpCircles.clear();
      }
      if (modules.hitParticles != null) {
         modules.hitParticles.clear();
      }
      if (modules.customAccessories != null) {
         modules.customAccessories.clear();
      }
      MotionBlurRenderer.reset();
   }
}
