package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

public class ChatMacroModule extends Module {
   private static final int SLOTS = 3;
   public final ModeSetting slot = this.addSetting(
      new ModeSetting(
         "Slot",
         "Macro slot to edit.",
         "1",
         "1",
         "2",
         "3"
      )
   );
   public final BooleanSetting sendInstantly = this.addSetting(
      new BooleanSetting("Send Instantly", "Skip the chat preview and send immediately.", true)
   );
   private final StringSetting[] messages = new StringSetting[3];
   private final KeybindSetting[] keys = new KeybindSetting[3];

   public ChatMacroModule() {
      super("ChatMacro", "Bindable chat command macros.", Category.CLIENT);

      for (int i = 0; i < 3; i++) {
         String num = Integer.toString(i + 1);
         this.messages[i] = this.addSetting(
            new StringSetting("Message " + num, "Message or /command for slot " + num, "", 256, "/say hi")
         );
         this.keys[i] = this.addSetting(new KeybindSetting("Key " + num, "Key that runs slot " + num, -1));
         this.messages[i].visibleWhen(() -> this.slot.is(num));
         this.keys[i].visibleWhen(() -> this.slot.is(num));
      }
   }

   @Override
   public boolean onKeyPress(int keyCode) {
      boolean handled = false;

      for (int i = 0; i < 3; i++) {
         if (this.keys[i].matches(keyCode)) {
            this.run(this.messages[i].get());
            handled = true;
         }
      }

      return handled;
   }

   private void run(String message) {
      if (message != null && !message.isBlank()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.player.connection != null) {
            if (!this.sendInstantly.get()) {
               mc.setScreen(new ChatScreen(message, false));
            } else if (message.startsWith("/")) {
               mc.player.connection.sendCommand(message.substring(1));
            } else {
               mc.player.connection.sendChat(message);
            }
         }
      }
   }
}
