package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.module.ModuleManager;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.StringSetting;
import dev.aether.util.Amounts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public class FakePayModule extends Module {
   private static final int WHITE = 16777215;
   private static final int RED = 16733525;
   public final StringSetting command = this.addSetting(
      new StringSetting(
         "Command",
         "Command name to hijack, without the slash.",
         "pay",
         32,
         "pay"
      )
   );
   public final ModeSetting feedback = this.addSetting(
      new ModeSetting(
         "Feedback",
         "Where the confirmation shows.",
         "Both",
         "Both",
         "Action Bar",
         "Chat"
      )
   );
   public final StringSetting currency = this.addSetting(
      new StringSetting(
         "Currency",
         "Symbol before the amount (colored).",
         "$",
         4,
         "$"
      )
   );
   public final ColorSetting currencyColor = this.addSetting(
      new ColorSetting(
         "Currency Color",
         "Color of the currency symbol (DonutSMP blue by default).",
         -16740609
      )
   );
   public final BooleanSetting sounds = this.addSetting(
      new BooleanSetting("Sounds", "Level-up on success, villager 'no' when it fails.", true)
   );
   public final BooleanSetting checkBalance = this.addSetting(
      new BooleanSetting("Check Balance", "With FakeStats on, refuse pays you can't afford.", true)
   );
   public final BooleanSetting selfGuard = this.addSetting(
      new BooleanSetting("Self-Pay Guard", "Block paying your own name, like the real command.", true)
   );

   public FakePayModule() {
      super("FakePay", "Fakes a /pay for clips — blocks the real command", Category.MISC);
   }

   public boolean tryIntercept(String rawCommand) {
      if (this.isEnabled() && rawCommand != null) {
         String[] parts = rawCommand.trim().split("\\s+");
         if (parts.length == 0) {
            return false;
         } else {
            String name = parts[0];
            if (name.startsWith("/")) {
               name = name.substring(1);
            }

            String target = this.command.get().trim();
            if (!target.isEmpty() && name.equalsIgnoreCase(target)) {
               this.handle(parts);
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private void handle(String[] parts) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if (parts.length < 3) {
            this.show(mc, Component.literal("Usage: /" + parts[0] + " <player> <amount>").withColor(16733525));
            this.fail(mc);
         } else {
            String player = parts[1];
            double amount = Amounts.parse(parts[2]);
            if (Double.isNaN(amount) || amount <= 0.0) {
               this.show(mc, Component.literal("Invalid amount: " + parts[2]).withColor(16733525));
               this.fail(mc);
            } else if (this.selfGuard.get() && player.equalsIgnoreCase(mc.player.getGameProfile().name())) {
               this.show(mc, Component.literal("You can't pay yourself!").withColor(16733525));
               this.fail(mc);
            } else {
               FakeStatsModule stats = this.stats();
               boolean useStats = stats != null && stats.isEnabled() && stats.deductOnPay.get();
               if (useStats && this.checkBalance.get() && stats.getLiveBalance() < amount) {
                  this.show(mc, Component.literal("You don't have enough money!").withColor(16733525));
                  this.fail(mc);
               } else {
                  if (useStats) {
                     stats.deduct(amount);
                  }

                  MutableComponent line = Component.empty().append(Component.literal("You paid " + player).withColor(16777215));
                  String symbol = this.currency.get();
                  if (!symbol.isEmpty()) {
                     line.append(Component.literal(" " + symbol).withColor(this.currencyColor.get() & 16777215))
                        .append(Component.literal(Amounts.shortForm(amount)).withColor(16777215));
                  } else {
                     line.append(Component.literal(" " + Amounts.shortForm(amount)).withColor(16777215));
                  }

                  this.show(mc, line);
                  this.succeed(mc);
               }
            }
         }
      }
   }

   private void show(Minecraft mc, Component component) {
      if (mc.player != null) {
         String mode = this.feedback.get();
         if (mode.equals("Action Bar") || mode.equals("Both")) {
            mc.player.displayClientMessage(component, true);
         }

         if (mode.equals("Chat") || mode.equals("Both")) {
            mc.player.displayClientMessage(component, false);
         }
      }
   }

   private void succeed(Minecraft mc) {
      if (this.sounds.get()) {
         this.play(mc, SoundEvents.PLAYER_LEVELUP, 1.0F);
      }
   }

   private void fail(Minecraft mc) {
      if (this.sounds.get()) {
         this.play(mc, SoundEvents.VILLAGER_NO, 1.0F);
      }
   }

   private void play(Minecraft mc, SoundEvent event, float pitch) {
      mc.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, 1.0F));
   }

   private FakeStatsModule stats() {
      ModuleManager modules = AetherClient.modules();
      return modules == null ? null : modules.fakeStats;
   }
}
