package dev.aether.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.AetherClient;
import dev.aether.donut.ActivityStore;
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

import java.util.HashSet;
import java.util.Set;

/**
 * Marks chunks where a player is underground on DonutLarp from the {@code donutlarp:activity}
 * data. Detection/tick logic is ported from the original DLActivityInspector; rendering uses the
 * shared minimalistic rounded/glow design in a red palette so it reads differently from the sus
 * area (purple) and base chunk (gold).
 */
public class ActivityInspectorModule extends Module {
   public final BooleanSetting chat = this.addSetting(
      new BooleanSetting("Chat Message", "Write into the chat when new activity shows up", true)
   );
   public final BooleanSetting sound = this.addSetting(
      new BooleanSetting("Sound", "Play a sound when new activity shows up", false)
   );
   public final SliderSetting yLevel = this.addSetting(
      new SliderSetting("Y Level", "Height the activity chunk is rendered at", 64.0, -64.0, 320.0, 1.0)
   );
   public final SliderSetting height = this.addSetting(
      new SliderSetting("Height", "Vertical thickness of the chunk in blocks", 1.0, 0.0, 64.0, 1.0)
   );
   public final ColorSetting fillColor = this.addSetting(
      new ColorSetting("Fill Color", "Fill color of the activity chunk", 0x96E05050)
   );
   public final ColorSetting outlineColor = this.addSetting(
      new ColorSetting("Outline Color", "Outline color of the activity chunk", 0xFFFF6060)
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

   private final Set<Long> seen = new HashSet<>();

   public ActivityInspectorModule() {
      super("Activity Inspector", "Marks a chunk where a player is underground on DonutLarp.", Category.DONUT);
   }

   @Override
   protected void onEnable() {
      this.seen.clear();
   }

   @Override
   public void onTick() {
      HashSet<Long> now = new HashSet<>();
      Minecraft mc = Minecraft.getInstance();
      for (ActivityStore.Activity a : ActivityStore.all()) {
         long key = (long) a.chunkX() << 32 ^ (long) a.chunkZ() & 0xFFFFFFFFL;
         now.add(key);
         if (this.seen.contains(key)) {
            continue;
         }
         if (this.chat.get() && mc.player != null) {
            String plural = a.players() == 1 ? "player" : "players";
            mc.player.displayClientMessage(
               Component.literal("Activity below chunk " + (a.chunkX() * 16 + 8) + ", " + (a.chunkZ() * 16 + 8)
                  + " (" + a.players() + " " + plural + ")"), false);
         }
         if (this.sound.get() && mc.player != null) {
            mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.8F, 0.6F);
         }
      }
      this.seen.clear();
      this.seen.addAll(now);
   }

   public void render(BufferSource bufferSource, PoseStack poseStack, Vec3 camera) {
      double y = this.yLevel.getFloat();
      double h = this.height.getFloat();
      int fill = this.fillColor.get();
      int outline = this.outlineColor.get();
      float glow = this.glowStrength.getFloat();
      float radius = this.cornerRadius.getFloat();
      float op = this.opacity.getFloat();
      for (ActivityStore.Activity a : ActivityStore.all()) {
         double x1 = a.chunkX() * 16.0;
         double z1 = a.chunkZ() * 16.0;
         DonutChunkRenderer.renderRect(bufferSource, poseStack, camera, x1, z1, x1 + 16.0, z1 + 16.0, y, h, fill, outline, glow, radius, op);
      }
      DonutChunkRenderer.flush(bufferSource);
   }
}
