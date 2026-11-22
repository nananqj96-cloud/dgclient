package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacitySpammerModule extends Module {
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Message delay in ms", 500.0, 100.0, 3000.0, 100.0, "ms"));
   public final BooleanSetting random = this.addSetting(new BooleanSetting("Random", "Send random messages", true));
   private long lastSend;

   private static final String[] MSGS = {
      "gg", "nice", "ez", "noob", "tryhard", "sucks", "bad", "lol", "wow", "hmm"
   };

   public TenacitySpammerModule() {
      super("Spammer", "Sends messages in chat at a set interval", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      long now = System.currentTimeMillis();
      if (now - lastSend < delay.getInt()) return;
      lastSend = now;
      String msg = random.get() ? MSGS[(int)(Math.random() * MSGS.length)] : "hello";
      mc.player.sendChatMessage(msg);
   }
}
