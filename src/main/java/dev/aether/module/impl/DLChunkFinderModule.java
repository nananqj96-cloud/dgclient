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
 * Marks the golden chunk above every base on DonutLarp from the {@code donutlarp:bases} data.
 * Detection/announce logic is ported from the original DLChunkFinder; rendering uses the shared
 * minimalistic rounded/glow design in a gold/amber palette so it reads differently from the sus
 * area (purple).
 */
public class DLChunkFinderModule extends Module {
   public final BooleanSetting toast = this.addSetting(
      new BooleanSetting("Toast", "Show a notification when a base chunk is found", true)
   );
   public final BooleanSetting chat = this.addSetting(
      new BooleanSetting("Chat Message", "Also write the found chunk into the chat", true)
   );
   public final BooleanSetting sound = this.addSetting(
      new BooleanSetting("Sound", "Play a sound when a base chunk is found", true)
   );
   public final SliderSetting maxDistance = this.addSetting(
      new SliderSetting("Max Distance", "Only render base chunks within this many chunks", 32.0, 1.0, 64.0, 1.0)
   );
   public final SliderSetting yLevel = this.addSetting(
      new SliderSetting("Y Level", "Height the base chunk is rendered at", 64.0, -64.0, 320.0, 1.0)
   );
   public final SliderSetting height = this.addSetting(
      new SliderSetting("Height", "Vertical thickness of the chunk in blocks", 1.0, 0.0, 64.0, 1.0)
   );
   public final ColorSetting fillColor = this.addSetting(
      new ColorSetting("Fill Color", "Fill color of the base chunk", 0x96DCAA28)
   );
   public final ColorSetting outlineColor = this.addSetting(
      new ColorSetting("Outline Color", "Outline color of the base chunk", 0xFFFFD750)
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

   public DLChunkFinderModule() {
      super("DL Chunk Finder", "Marks a golden chunk above every base on DonutLarp.", Category.DONUT);
      BaseStore.onNewBase(b -> {
         if (this.isEnabled()) {
            this.announce(b);
         }
      });
   }

   private void announce(BaseStore.Base b) {
      int bx = b.chunkX() * 16 + 8;
      int bz = b.chunkZ() * 16 + 8;
      Minecraft mc = Minecraft.getInstance();
      if (this.toast.get() && AetherClient.notifications() != null) {
         AetherClient.notifications().push("Base Chunk Found!", true);
      }
      if (this.chat.get() && mc.player != null) {
         mc.player.displayClientMessage(Component.literal("Golden chunk found at X " + bx + "  Z " + bz), false);
      }
      if (this.sound.get() && mc.player != null) {
         mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 1.2F);
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
      double y = this.yLevel.getFloat();
      double h = this.height.getFloat();
      int fill = this.fillColor.get();
      int outline = this.outlineColor.get();
      float glow = this.glowStrength.getFloat();
      float radius = this.cornerRadius.getFloat();
      float op = this.opacity.getFloat();
      for (BaseStore.Base b : BaseStore.all()) {
         int cx = b.chunkX();
         int cz = b.chunkZ();
         if (Math.abs(cx - pcx) > max || Math.abs(cz - pcz) > max) {
            continue;
         }
         double x1 = cx * 16.0;
         double z1 = cz * 16.0;
         DonutChunkRenderer.renderRect(bufferSource, poseStack, camera, x1, z1, x1 + 16.0, z1 + 16.0, y, h, fill, outline, glow, radius, op);
      }
      DonutChunkRenderer.flush(bufferSource);
   }
}
