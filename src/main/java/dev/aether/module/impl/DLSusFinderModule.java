package dev.aether.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.AetherClient;
import dev.aether.donut.BaseStore;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.render.DonutChunkRenderer;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Marks the rough area where a base is on DonutLarp, using the DonutLarp {@code donutlarp:bases}
 * data. Detection logic (area radius + fuzzed centre) is ported from the original DLSusFinder;
 * the rendering is the shared minimalistic purple rounded/glow design.
 */
public class DLSusFinderModule extends Module {
   public final SliderSetting areaRadius = this.addSetting(
      new SliderSetting("Area Radius", "Size of the sus area in chunks around its centre (2 = 5x5)", 2.0, 1.0, 8.0, 1.0)
   );
   public final SliderSetting fuzz = this.addSetting(
      new SliderSetting("Fuzz", "How far (chunks) the area centre may shift from the real base", 2.0, 0.0, 8.0, 1.0)
   );
   public final BooleanSetting toast = this.addSetting(
      new BooleanSetting("Toast", "Show a notification when a sus area is found", true)
   );
   public final BooleanSetting chat = this.addSetting(
      new BooleanSetting("Chat Message", "Also write the sus area into the chat", false)
   );
   public final BooleanSetting sound = this.addSetting(
      new BooleanSetting("Sound", "Play a sound when a sus area is found", true)
   );
   public final SliderSetting maxDistance = this.addSetting(
      new SliderSetting("Max Distance", "Only render sus areas within this many chunks", 40.0, 1.0, 64.0, 1.0)
   );
   public final SliderSetting yLevel = this.addSetting(
      new SliderSetting("Y Level", "Height the sus area is rendered at", 63.0, -64.0, 320.0, 1.0)
   );
   public final SliderSetting height = this.addSetting(
      new SliderSetting("Height", "Vertical thickness of the sus area in blocks", 1.0, 0.0, 64.0, 1.0)
   );
   public final ColorSetting fillColor = this.addSetting(
      new ColorSetting("Fill Color", "Fill color of the sus area", 0x968C44DC)
   );
   public final ColorSetting outlineColor = this.addSetting(
      new ColorSetting("Outline Color", "Outline color of the sus area", 0xFFB478FF)
   );
   public final SliderSetting glowStrength = this.addSetting(
      new SliderSetting("Glow", "Soft outer glow spread in blocks", 4.0, 0.0, 16.0, 0.5)
   );
   public final SliderSetting cornerRadius = this.addSetting(
      new SliderSetting("Corner Radius", "Rounded corner radius in blocks", 3.0, 0.0, 8.0, 0.5)
   );
   public final SliderSetting opacity = this.addSetting(
      new SliderSetting("Opacity", "Fill opacity", 0.5, 0.0, 1.0, 0.05)
   );

   public DLSusFinderModule() {
      super("DL Sus Finder", "Marks the rough area where a base is on DonutLarp.", Category.DONUT);
      BaseStore.onNewBase(b -> {
         if (this.isEnabled()) {
            this.announce(b);
         }
      });
   }

   private int[] center(BaseStore.Base b) {
      int f = this.fuzz.getInt();
      long h = (long) b.id() * -7046029254386353131L;
      int ox = f == 0 ? 0 : (int) Math.floorMod(h >>> 7, 2L * f + 1L) - f;
      int oz = f == 0 ? 0 : (int) Math.floorMod(h >>> 29, 2L * f + 1L) - f;
      return new int[]{b.chunkX() + ox, b.chunkZ() + oz};
   }

   private void announce(BaseStore.Base b) {
      int[] c = this.center(b);
      int bx = c[0] * 16 + 8;
      int bz = c[1] * 16 + 8;
      Minecraft mc = Minecraft.getInstance();
      if (this.toast.get() && AetherClient.notifications() != null) {
         AetherClient.notifications().push("Sus Area Detected", true);
      }
      if (this.chat.get() && mc.player != null) {
         mc.player.displayClientMessage(Component.literal("Sus area around X " + bx + "  Z " + bz), false);
      }
      if (this.sound.get() && mc.player != null) {
         mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING, 0.6F, 0.8F);
      }
   }

   @Override
   protected void onEnable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if (BaseStore.size() == 0) {
            mc.player.displayClientMessage(Component.literal("Waiting for base data from the server..."), false);
         } else {
            mc.player.displayClientMessage(Component.literal(BaseStore.size() + " base chunks known."), false);
         }
      }
   }

   public void render(BufferSource bufferSource, PoseStack poseStack, Vec3 camera) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return;
      }
      int pcx = (int) (mc.player.getX() >> 4);
      int pcz = (int) (mc.player.getZ() >> 4);
      int max = this.maxDistance.getInt();
      int r = this.areaRadius.getInt();
      double y = this.yLevel.getFloat();
      double h = this.height.getFloat();
      int fill = this.fillColor.get();
      int outline = this.outlineColor.get();
      float glow = this.glowStrength.getFloat();
      float radius = this.cornerRadius.getFloat();
      float op = this.opacity.getFloat();
      for (BaseStore.Base b : BaseStore.all()) {
         int[] c = this.center(b);
         if (Math.abs(c[0] - pcx) > max || Math.abs(c[1] - pcz) > max) {
            continue;
         }
         double x1 = (c[0] - r) * 16.0;
         double z1 = (c[1] - r) * 16.0;
         double x2 = (c[0] + r + 1) * 16.0;
         double z2 = (c[1] + r + 1) * 16.0;
         DonutChunkRenderer.renderRect(bufferSource, poseStack, camera, x1, z1, x2, z2, y, h, fill, outline, glow, radius, op);
      }
      DonutChunkRenderer.flush(bufferSource);
   }
}
