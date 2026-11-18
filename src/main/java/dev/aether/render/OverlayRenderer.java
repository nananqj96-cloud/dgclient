package dev.aether.render;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.aether.AetherClient;
import dev.aether.hud.HudDragController;
import dev.aether.hud.HudManager;
import dev.aether.module.impl.CustomCrosshairModule;
import dev.aether.module.impl.MotionBlurModule;
import dev.aether.notification.NotificationManager;
import dev.aether.render.nanovg.GlStateSnapshot;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import org.lwjgl.opengl.GL33C;

public final class OverlayRenderer {
   private static HudManager hudManager;
   private static NotificationManager notifications;
   private static int fbo = -1;
   private static int stencilRbo = -1;
   private static int cachedColorTexId = -1;
   private static int cachedWidth = -1;
   private static int cachedHeight = -1;
   private static boolean fboValid;
   private static boolean crashed;

   private OverlayRenderer() {
   }

   public static void init(HudManager hud, NotificationManager notif) {
      hudManager = hud;
      notifications = notif;
   }

   public static float uiScale() {
      Minecraft mc = Minecraft.getInstance();
      return Math.max(1.0F, (float)mc.getWindow().getHeight() / 1080.0F);
   }

   public static void render() {
      try {
         GlintTextureTinter.tick();
      } catch (Throwable ignored) {
      }

      if (!crashed && hudManager != null) {
         Minecraft mc = Minecraft.getInstance();
         RenderTarget target = mc.getMainRenderTarget();
         GpuTexture gpuTex;
         if (target != null && (gpuTex = target.getColorTexture()) instanceof GlTexture glTex) {
            boolean isNvgScreen = mc.screen instanceof NvgDrawable;
            boolean isChat = mc.screen instanceof ChatScreen;
            boolean isPauseOrOpts = mc.screen instanceof PauseScreen || mc.screen instanceof OptionsSubScreen;
            boolean drawHud = !mc.options.hideGui && mc.level != null && !isNvgScreen && !isPauseOrOpts;
            boolean drawToasts = mc.level != null && !mc.options.hideGui && !isPauseOrOpts;
            MotionBlurModule mb = AetherClient.modules() != null ? AetherClient.modules().motionBlur : null;
            boolean drawMotionBlur = mb != null && mb.isEnabled() && mc.level != null && !isNvgScreen;

            if (isNvgScreen || drawHud || drawToasts || drawMotionBlur) {
               int prevFbo = GL33C.glGetInteger(GL33C.GL_FRAMEBUFFER_BINDING);
               try {
                  GlStateSnapshot state = GlStateSnapshot.capture();
                  try {
                     if (bindOverlayFbo(glTex, target.width, target.height)) {
                        NVGRenderer vg = NVGRenderer.get();
                        if (drawMotionBlur) {
                           MotionBlurRenderer.render(vg, target.width, target.height, mb);
                        }

                        vg.setFontMode(AetherClient.modules().clickGui.font.get());
                        if (!vg.hasFont()) {
                           return;
                        }

                        float scale = uiScale();
                        float uiW = (float)target.width / scale;
                        float uiH = (float)target.height / scale;
                        vg.beginFrame((float)target.width, (float)target.height, 1.0F);
                        vg.save();
                        vg.scale(scale);

                        if (drawHud) {
                           if (isChat && HudDragController.isDragging()) {
                              HudDragController.updateDrag(vg);
                           }

                           CustomCrosshairModule xhair = AetherClient.modules().customCrosshair;
                           if (xhair.isEnabled() && mc.screen == null) {
                              xhair.render(vg, uiW / 2.0F, uiH / 2.0F);
                           }

                           if (mc.screen == null) {
                              WorldNametagRenderer.render(vg);
                              NetheriteProbabilityRenderer.renderLabels(vg);
                           }

                           hudManager.render(vg, uiW, uiH);
                           if (isChat) {
                              renderChatEditOverlay(vg, uiW, uiH);
                           }
                        }

                        if (drawToasts) {
                           notifications.renderToasts(vg, uiW, uiH);
                        }

                        if (isNvgScreen) {
                           ((NvgDrawable)mc.screen).renderNvg(vg, uiMouseX(), uiMouseY(), uiW, uiH);
                        }

                        vg.restore();
                        vg.endFrame();
                     }
                  } finally {
                     GL33C.glBindFramebuffer(GL33C.GL_FRAMEBUFFER, prevFbo);
                     state.restore();
                  }
               } catch (Throwable t) {
                  crashed = true;
                  AetherClient.LOGGER.error("AetherClient overlay renderer crashed; disabling overlay", t);
               }
            }
         }
      }
   }

   private static void renderChatEditOverlay(NVGRenderer vg, float uiW, float uiH) {
      Theme theme = AetherClient.themes().current();
      float mx = uiMouseX();
      float my = uiMouseY();
      String banner = "AETHER CLIENT  ·  DRAG HUD TO MOVE  ·  SCROLL TO SCALE";
      float bw = vg.textWidth(banner, 12.0F) + 28.0F;
      float bx = (uiW - bw) / 2.0F;
      vg.rect(bx, 12.0F, bw, 24.0F, 12.0F, Colors.withAlpha(0xFF0B0F1A, 0.86F));
      vg.rectOutline(bx, 12.0F, bw, 24.0F, 12.0F, 1.0F, Colors.withAlpha(theme.accent(), 0.45F));
      vg.text(banner, bx + 14.0F, 24.0F, 12.0F, theme.textPrimary());

      for (HudManager.Placement p : hudManager.layout(vg, uiW, uiH, true)) {
         boolean hidden = !p.component().visible();
         boolean active = p.contains(mx, my) || p.component() == HudDragController.draggedComponent();
         if (hidden) {
            vg.save();
            vg.alpha(0.35F);
            hudManager.renderPlacement(vg, p);
            vg.restore();
         }

         int outlineCol = active ? theme.accentBright() : Colors.withAlpha(theme.accent(), 0.5F);
         vg.rectOutline(p.x() - 3.0F, p.y() - 3.0F, p.w() + 6.0F, p.h() + 6.0F, 7.0F, active ? 1.6F : 1.0F, outlineCol);
         vg.rect(p.x() + p.w() - 2.0F, p.y() + p.h() - 2.0F, 6.0F, 6.0F, 2.0F, outlineCol);
         if (active) {
            String pct = Math.round(p.component().getScale() * 100.0F) + "%";
            vg.text(pct, p.x() + p.w() + 6.0F, p.y() + p.h() / 2.0F, 11.0F, theme.accentBright());
         }
      }
   }

   private static boolean bindOverlayFbo(GlTexture colorTex, int width, int height) {
      int texId = colorTex.glId();
      if (fbo == -1) {
         fbo = GL33C.glGenFramebuffers();
      }
      if (stencilRbo == -1) {
         stencilRbo = GL33C.glGenRenderbuffers();
      }

      GL33C.glBindFramebuffer(GL33C.GL_FRAMEBUFFER, fbo);
      if (texId != cachedColorTexId || width != cachedWidth || height != cachedHeight) {
         GL33C.glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0, GL33C.GL_TEXTURE_2D, texId, 0);
         if (width != cachedWidth || height != cachedHeight) {
            GL33C.glBindRenderbuffer(GL33C.GL_RENDERBUFFER, stencilRbo);
            GL33C.glRenderbufferStorage(GL33C.GL_RENDERBUFFER, GL33C.GL_DEPTH24_STENCIL8, width, height);
            GL33C.glFramebufferRenderbuffer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_STENCIL_ATTACHMENT, GL33C.GL_RENDERBUFFER, stencilRbo);
            GL33C.glBindRenderbuffer(GL33C.GL_RENDERBUFFER, 0);
         }
         fboValid = GL33C.glCheckFramebufferStatus(GL33C.GL_FRAMEBUFFER) == GL33C.GL_FRAMEBUFFER_COMPLETE;
         cachedColorTexId = texId;
         cachedWidth = width;
         cachedHeight = height;
      }

      if (!fboValid) {
         return false;
      }
      GL33C.glViewport(0, 0, width, height);
      GL33C.glDisable(GL33C.GL_SCISSOR_TEST);
      return true;
   }

   public static float uiMouseX() {
      Minecraft mc = Minecraft.getInstance();
      return (float)(mc.mouseHandler.xpos() * (double)mc.getWindow().getWidth() / (double)Math.max(1, mc.getWindow().getScreenWidth())) / uiScale();
   }

   public static float uiMouseY() {
      Minecraft mc = Minecraft.getInstance();
      return (float)(mc.mouseHandler.ypos() * (double)mc.getWindow().getHeight() / (double)Math.max(1, mc.getWindow().getScreenHeight())) / uiScale();
   }

   public static float guiToUi(double guiCoord) {
      Minecraft mc = Minecraft.getInstance();
      float px = (float)(guiCoord * (double)mc.getWindow().getGuiScale());
      return px / uiScale();
   }

   public static float uiWidth() {
      Minecraft mc = Minecraft.getInstance();
      return (float)mc.getWindow().getWidth() / uiScale();
   }

   public static float uiHeight() {
      Minecraft mc = Minecraft.getInstance();
      return (float)mc.getWindow().getHeight() / uiScale();
   }
}
