package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public class FakeRolesModule extends Module {
   public static final String ROLE_NONE = "None";
   public static final String ROLE_SRMOD = "SR.MOD";
   public static final String ROLE_MEDIA = "MEDIA";
   public static final String ROLE_SRADMIN = "SR.ADMIN";
   private static final int GRAY = 8355711;
   private static final int GREEN = 5635925;
   private static final int PINK = 16733695;
   private static final int RED = 16733269;
   private static final int WHITE = 16777215;
   public final ModeSetting role = this.addSetting(
      new ModeSetting(
         "Role",
         "Which fake rank tag to wear in front of your name",
         "None",
         "None",
         "SR.MOD",
         "MEDIA",
         "SR.ADMIN"
      )
   );
   public final BooleanSetting nametag = this.addSetting(
      new BooleanSetting("Nametag", "Show the tag on your floating nametag (3rd person)", true)
   );
   public final BooleanSetting tabList = this.addSetting(
      new BooleanSetting(
         "Tab List", "Show the tag before your name in the player list (Tab)", true
      )
   );
   public final BooleanSetting chat = this.addSetting(
      new BooleanSetting("Chat", "Show the tag before your name in chat messages", true)
   );

   public FakeRolesModule() {
      super("FakeRoles", "Fake [SR.MOD] / [MEDIA] / [SR.ADMIN] rank tag on your own name", Category.MISC);
   }

   public boolean isActive() {
      Minecraft mc = Minecraft.getInstance();
      return this.isEnabled() && !this.role.is("None") && mc.player != null;
   }

   private String selfName() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == null ? null : mc.player.getGameProfile().name();
   }

   private boolean isSelf(String exactName) {
      String me = this.selfName();
      return me != null && !me.isEmpty() && me.equals(exactName);
   }

   private int roleColor() {
      String var1 = this.role.get();

      return switch (var1) {
         case "SR.MOD" -> 5635925;
         case "MEDIA" -> 16733695;
         case "SR.ADMIN" -> 16733269;
         default -> 16777215;
      };
   }

   private Style bracketStyle() {
      return Style.EMPTY.withColor(8355711).withBold(false);
   }

   private Style tagStyle() {
      return Style.EMPTY.withColor(this.roleColor()).withBold(true);
   }

   private Style nameStyle() {
      String var1 = this.role.get();

      return switch (var1) {
         case "SR.MOD" -> Style.EMPTY.withColor(5635925).withBold(true);
         case "SR.ADMIN" -> Style.EMPTY.withColor(16733269).withBold(true);
         case "MEDIA" -> Style.EMPTY.withColor(16777215).withBold(false);
         default -> Style.EMPTY;
      };
   }

   public Component tagComponent() {
      return Component.empty()
         .append(Component.literal("[").setStyle(this.bracketStyle()))
         .append(Component.literal(this.role.get()).setStyle(this.tagStyle()))
         .append(Component.literal("] ").setStyle(this.bracketStyle()));
   }

   public Component buildPrefixedDisplayName(String name) {
      return this.isActive() && name != null ? Component.empty().append(this.tagComponent()).append(Component.literal(name).setStyle(this.nameStyle())) : null;
   }

   public Component decorateNametag(Component tag) {
      return this.isActive() && this.nametag.get() ? this.modifyText(tag) : tag;
   }

   public Component decorateTab(Component display, String realName) {
      if (this.isActive() && this.tabList.get()) {
         Component prefixed = this.isSelf(realName) ? this.buildPrefixedDisplayName(realName) : null;
         return prefixed != null ? prefixed : display;
      } else {
         return display;
      }
   }

   public Component decorateChat(Component input) {
      return this.isActive() && this.chat.get() ? this.modifyText(input) : input;
   }

   private Component modifyText(Component input) {
      if (input == null) {
         return null;
      } else {
         String me = this.selfName();
         return me != null && !me.isBlank() && input.getString().contains(me) ? this.splice(input, me, new boolean[]{false}) : input;
      }
   }

   private Component splice(Component c, String name, boolean[] done) {
      ComponentContents contents = c.getContents();
      MutableComponent result;
      if (!done[0] && contents instanceof PlainTextContents ptc) {
         String text = ptc.text();
         int idx = text.indexOf(name);
         if (idx >= 0) {
            done[0] = true;
            result = Component.empty();
            if (idx > 0) {
               result.append(Component.literal(text.substring(0, idx)).setStyle(c.getStyle()));
            }

            result.append(this.buildPrefixedDisplayName(name));
            int end = idx + name.length();
            if (end < text.length()) {
               result.append(Component.literal(text.substring(end)).setStyle(c.getStyle()));
            }
         } else {
            result = MutableComponent.create(contents).setStyle(c.getStyle());
         }
      } else if (!done[0] && contents instanceof TranslatableContents tc) {
         Object[] args = tc.getArgs();
         Object[] out = new Object[args.length];

         for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Component ac) {
               out[i] = this.splice(ac, name, done);
            } else {
               if (!done[0] && arg instanceof String) {
                  String s = (String)arg;
                  if (s.contains(name)) {
                     done[0] = true;
                     int idx = s.indexOf(name);
                     MutableComponent split = Component.empty();
                     if (idx > 0) {
                        split.append(Component.literal(s.substring(0, idx)));
                     }

                     split.append(this.buildPrefixedDisplayName(name));
                     int end = idx + name.length();
                     if (end < s.length()) {
                        split.append(Component.literal(s.substring(end)));
                     }

                     out[i] = split;
                     continue;
                  }
               }

               out[i] = arg;
            }
         }

         result = MutableComponent.create(new TranslatableContents(tc.getKey(), tc.getFallback(), out)).setStyle(c.getStyle());
      } else {
         result = MutableComponent.create(contents).setStyle(c.getStyle());
      }

      for (Component sibling : c.getSiblings()) {
         result.append(this.splice(sibling, name, done));
      }

      return result;
   }
}
