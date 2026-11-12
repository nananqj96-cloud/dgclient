package dev.aether.hud.components;

import dev.aether.hud.HudComponent;
import dev.aether.render.nanovg.NVGImages;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public class PotionsHud extends HudComponent {
   private static final float ROW = 24.0F;
   private static final float ICON = 18.0F;
   private static final float PAD = 7.0F;
   private static final float FONT = 12.5F;
   private final ThemeManager themes;

   public PotionsHud(ThemeManager var1, BooleanSupplier var2) {
      super("potions", 0.995F, 0.6F, var2);
      this.themes = var1;
   }

   private List<MobEffectInstance> effects() {
      LocalPlayer var1 = Minecraft.getInstance().player;
      if (var1 == null) {
         return List.of();
      } else {
         ArrayList<MobEffectInstance> var2 = new ArrayList<>(var1.getActiveEffects());
         var2.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());
         return var2;
      }
   }

   private static String label(MobEffectInstance var0) {
      String var1 = ((MobEffect)var0.getEffect().value()).getDisplayName().getString();
      int var2 = var0.getAmplifier();
      return var2 > 0 ? var1 + " " + (var2 + 1) : var1;
   }

   private static String timer(MobEffectInstance var0) {
      if (var0.isInfiniteDuration()) {
         return "∞";
      } else {
         int var1 = var0.getDuration() / 20;
         return String.format("%d:%02d", var1 / 60, var1 % 60);
      }
   }

   @Override
   public float measureWidth(NVGRenderer var1) {
      float var2 = 110.0F;

      for (MobEffectInstance var4 : this.effects()) {
         var2 = Math.max(var2, 31.0F + var1.textWidth(label(var4), 12.5F) + 10.0F + var1.textWidth(timer(var4), 12.5F) + 7.0F);
      }

      return var2;
   }

   @Override
   public float measureHeight(NVGRenderer var1) {
      return Math.max(24.0F, (float)this.effects().size() * 24.0F);
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3, float var4, float var5) {
      Theme var6 = this.themes.current();
      List<MobEffectInstance> var7 = this.effects();
      if (!var7.isEmpty()) {
         boolean var8 = this.rightAnchored();
         float var9 = var3;

         for (MobEffectInstance var11 : var7) {
            float var13 = var1.textWidth(label(var11), 12.5F);
            float var14 = var1.textWidth(timer(var11), 12.5F);
            float var15 = 31.0F + var13 + 10.0F + var14 + 7.0F;
            float var16 = var8 ? var2 + var4 - var15 : var2;
            float var17 = var9 + 12.0F;
            var1.rect(var16, var9 + 1.0F, var15, 22.0F, 3.0F, Colors.withAlpha(-15462118, 0.84F));
            var1.rect(var16 + 2.0F, var9 + 6.0F, 2.0F, 12.0F, 1.0F, Colors.withAlpha(var6.accent(), 0.72F));
            Identifier var18 = var11.getEffect().unwrapKey().map(var0 -> var0.identifier()).orElse(null);
            int var12;
            if (var18 != null
               && (var12 = NVGImages.fromResource(Identifier.fromNamespaceAndPath(var18.getNamespace(), "textures/mob_effect/" + var18.getPath() + ".png")))
                  > 0) {
               var1.imagePattern(var12, var16 + 7.0F, var17 - 9.0F, 18.0F, 18.0F, var16 + 7.0F, var17 - 9.0F, 18.0F, 18.0F, 1.0F);
            }

            var1.text(label(var11), var16 + 7.0F + 18.0F + 6.0F, var17, 12.5F, var6.textPrimary());
            var1.textGradient(timer(var11), var16 + var15 - 7.0F - var14, var17, 12.5F, var6.accentBright(), var6.accent());
            var9 += 24.0F;
         }
      }
   }
}
