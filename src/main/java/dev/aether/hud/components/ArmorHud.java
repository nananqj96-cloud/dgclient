package dev.aether.hud.components;

import dev.aether.hud.HudComponent;
import dev.aether.render.nanovg.NVGImages;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ArmorHud extends HudComponent {
   private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final float ICON = 22.0F;
   private static final float PAD = 7.0F;
   private static final float GAP = 6.0F;
   private static final float BAR_H = 3.0F;
   private final ThemeManager themes;

   public ArmorHud(ThemeManager var1, BooleanSupplier var2) {
      super("armor", 0.5F, 0.8F, var2);
      this.themes = var1;
   }

   @Override
   public float measureWidth(NVGRenderer var1) {
      return 14.0F + (float)SLOTS.length * 22.0F + (float)(SLOTS.length - 1) * 6.0F;
   }

   @Override
   public float measureHeight(NVGRenderer var1) {
      return 38.0F;
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3, float var4, float var5) {
      Theme var6 = this.themes.current();
      LocalPlayer var7 = Minecraft.getInstance().player;
      if (var7 != null) {
         var1.rectGradient(var2, var3, var4, var5, 3.0F, var6.background(), var6.backgroundTo(), true);
         var1.rect(var2 + 6.0F, var3 + 5.0F, var4 - 12.0F, 1.0F, 0.5F, Colors.withAlpha(var6.accent(), 0.22F));
         float var8 = var2 + 7.0F;

         for (EquipmentSlot var12 : SLOTS) {
            ItemStack var13 = var7.getItemBySlot(var12);
            float var14 = var3 + 4.0F;
            if (var13.isEmpty()) {
               var1.rectOutline(var8, var14, 22.0F, 22.0F, 5.0F, 1.0F, Colors.withAlpha(var6.textDisabled(), 0.5F));
            } else {
               Identifier var15 = BuiltInRegistries.ITEM.getKey(var13.getItem());
               int var16 = NVGImages.fromResource(Identifier.fromNamespaceAndPath(var15.getNamespace(), "textures/item/" + var15.getPath() + ".png"));
               if (var16 > 0) {
                  var1.imagePattern(var16, var8, var14, 22.0F, 22.0F, var8, var14, 22.0F, 22.0F, 1.0F);
               } else {
                  var1.rect(var8, var14, 22.0F, 22.0F, 5.0F, Colors.withAlpha(var6.accent(), 0.3F));
               }

               if (var13.isDamageableItem()) {
                  float var17 = 1.0F - (float)var13.getDamageValue() / (float)var13.getMaxDamage();
                  int var18 = Colors.lerp(-1684147, -11671924, var17);
                  float var19 = var14 + 22.0F + 3.0F;
                  var1.rect(var8, var19, 22.0F, 3.0F, 1.5F, Colors.withAlpha(-16777216, 0.45F));
                  var1.rect(var8, var19, Math.max(3.0F, 22.0F * var17), 3.0F, 1.5F, var18);
               }
            }

            var8 += 28.0F;
         }
      }
   }
}
