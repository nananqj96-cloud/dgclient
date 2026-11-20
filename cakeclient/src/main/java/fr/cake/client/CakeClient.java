package fr.cake.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CakeClient implements ClientModInitializer {
 public static final Logger LOG=LoggerFactory.getLogger("CakeClient");
 public static Settings settings;
 public static String effectError="";
 @Override public void onInitializeClient(){settings=Settings.load();CosmeticNetwork.init();LOG.info("Cake Client 6.9.4 / Minecraft 1.21.11");}
 public static boolean toggleMenu(){
  Minecraft mc=Minecraft.getInstance();if(settings==null)return false;
  PreviewTrial.cancel();
  if(mc.screen instanceof ClientScreen screen){screen.closeAnimated();return true;}
  if(true){
   if(settings.spatial&&mc.player!=null&&mc.level!=null){if(mc.screen!=null)mc.setScreen(null);SpatialPanel.toggle();return true;}
   mc.setScreen(new ClientScreen(mc.screen));return true;
  }return false;
 }
 public static void tick(){
  if(settings==null)return;
  Minecraft mc=Minecraft.getInstance();Edition.enforce(settings);PvpCustomizer.tick();RealisticMode.tick();
  SpotifyVisualizer.tick();CosmeticNetwork.tick();CakeOptimize.tick();CosmeticFx.tick(mc);PlayerFollow.tick(mc);PreviewTrial.tick(mc);Fireflies.tick(mc);Ambience.tick(mc);SpatialPanel.tick(mc);LyricsService.tick();
 }
 public static void shutdown(){CakeOptimize.flush();PreviewTrial.cancel();LyricsService.close();MotionBlur.close();SkyEffect.close();Atmosphere.close();SpotifyVisualizer.close();SpotifyHud.close();CosmeticFx.close();WorldQuad.close();InsectBatch.close();Comets.reset();Knives.close();ClientScreen.canvas.close();LyricsSky.close();}
}
