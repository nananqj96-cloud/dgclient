package com.corz.client.bridge;

import ai.auth.Module;
import ai.auth.ModuleRegistry;
import com.corz.client.categories.settings.License;
import com.corz.client.categories.settings.SelfDestruct;
import com.corz.client.categories.theme.ThemeCustomizer;
import com.corz.client.config.BooleanSetting;
import com.corz.client.config.ColorSetting;
import com.corz.client.config.ConfigListSetting;
import com.corz.client.config.ConfigSetting;
import com.corz.client.config.DoubleSetting;
import com.corz.client.config.EnumSetting;
import com.corz.client.config.IntegerSetting;
import com.corz.client.config.KeybindSetting;
import com.corz.client.config.ModuleConfigAutosave;
import com.corz.client.config.RadioSetting;
import com.corz.client.config.RadioStationRegistry;
import com.corz.client.config.RangeSetting;
import com.corz.client.config.StringBase;
import com.corz.client.gui.ButtonBase;
import com.corz.client.gui.MatrixMatricesScreen;
import com.corz.client.inventory.ItemList;
import com.corz.client.utils.DelegatingAutoEspBase;
import com.corz.client.utils.FriendListBase;
import com.corz.client.utils.IconsPngType;
import com.corz.client.utils.PaletteBase;
import dev.aether.corz.CorzModuleHandle;
import dev.aether.corz.CorzPortProvider;
import dev.aether.corz.CorzSettingHandle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;

public final class CorzPortProviderImpl implements CorzPortProvider {
   private static final List<String> ORDERED_CATEGORIES = List.of(
      "Bots", "DonutSMP", "Combat", "Movement", "Network", "Misc", "Visuals", "Socials"
   );

   private final List<CorzModuleHandle> allHandles = new ArrayList<>();
   private final Map<String, List<CorzModuleHandle>> byCategory = new LinkedHashMap<>();
   private final Map<String, Module> rawById = new LinkedHashMap<>();
   private int lastSyncedAccent = 0;

   public CorzPortProviderImpl() {
      for (String cat : ORDERED_CATEGORIES) {
         this.byCategory.put(cat, new ArrayList<>());
      }
      for (Module mod : ModuleRegistry.getModules()) {
         if (mod instanceof License || mod instanceof SelfDestruct || mod instanceof ThemeCustomizer) {
            continue;
         }
         IconsPngType page = mod.getPageType();
         if (page == IconsPngType.CONFIGS || page == IconsPngType.THEME || page == IconsPngType.SETTING) {
            if (mod.isHiddenModule()) {
               continue;
            }
         }
         String catName = mapCategoryName(page);
         if (!this.byCategory.containsKey(catName)) {
            continue;
         }
         String id = mod.getClass().getSimpleName();
         this.rawById.put(id, mod);
         CorzModuleHandle handle = wrapModule(id, catName, mod);
         this.allHandles.add(handle);
         this.byCategory.get(catName).add(handle);
      }
   }

   private static String mapCategoryName(IconsPngType page) {
      if (page == null) {
         return "Misc";
      }
      return switch (page) {
         case BOTS -> "Bots";
         case DONUTSMP -> "DonutSMP";
         case COMBAT -> "Combat";
         case MOVEMENT -> "Movement";
         case NETWORK -> "Network";
         case VISUAL -> "Visuals";
         case SOCIALS -> "Socials";
         default -> "Misc";
      };
   }

   @Override
   public List<String> categories() {
      return ORDERED_CATEGORIES;
   }

   @Override
   public List<CorzModuleHandle> allModules() {
      return this.allHandles;
   }

   @Override
   public List<CorzModuleHandle> modulesInCategory(String category) {
      return this.byCategory.getOrDefault(category, List.of());
   }

   @Override
   public void syncTheme(int accentArgb, int accentBrightArgb, int backgroundArgb) {
      if (accentArgb == this.lastSyncedAccent) {
         return;
      }
      this.lastSyncedAccent = accentArgb;
      if (ModuleRegistry.getModule(ThemeCustomizer.class) instanceof ThemeCustomizer tc) {
         tc.main.setColor(accentArgb);
         tc.primary.setColor(accentArgb);
         tc.secondary.setColor(accentBrightArgb);
      }
   }

   @Override
   public void openNativeStudio(String moduleId) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Module raw = this.rawById.get(moduleId);
      if (mc != null) {
         if (raw != null) {
            mc.setScreen(new MatrixMatricesScreen(raw));
         } else {
            mc.setScreen(new MatrixMatricesScreen());
         }
      }
   }

   @Override
   public void saveConfig() {
      ModuleConfigAutosave.saveModuleConfig();
   }

   private CorzModuleHandle wrapModule(String id, String catName, Module mod) {
      boolean customStudio = mod.hasExtraData();
      List<CorzSettingHandle> wrappedSettings = new ArrayList<>();
      for (ConfigSetting s : mod.getSettings()) {
         if (s == null || s.isHiddenFromList()) {
            continue;
         }
         if (s instanceof ItemList
            || s instanceof FriendListBase
            || s instanceof ConfigListSetting
            || s instanceof DelegatingAutoEspBase
            || s instanceof PaletteBase) {
            customStudio = true;
            String label = s.getSettingName();
            wrappedSettings.add(new CorzSettingHandle() {
               @Override
               public Kind kind() {
                  return Kind.BUTTON;
               }

               @Override
               public String name() {
                  return label;
               }

               @Override
               public String description() {
                  return s.getDescription();
               }

               @Override
               public String buttonLabel() {
                  return "Open Studio \u2197";
               }

               @Override
               public void pressButton() {
                  CorzPortProviderImpl.this.openNativeStudio(id);
               }
            });
            continue;
         }
         CorzSettingHandle sh = wrapSetting(s);
         if (sh != null) {
            wrappedSettings.add(sh);
         }
      }
      final boolean finalCustomStudio = customStudio;
      final String prettyTitle = humanizeTitle(mod.getCategory(), id);

      return new CorzModuleHandle() {
         @Override
         public String id() {
            return id;
         }

         @Override
         public String displayName() {
            return prettyTitle;
         }

         @Override
         public String category() {
            return catName;
         }

         @Override
         public boolean isEnabled() {
            return mod.isEnabled();
         }

         @Override
         public void setEnabled(boolean enabled) {
            mod.setEnabled(enabled);
            ModuleConfigAutosave.updateFlag2();
         }

         @Override
         public int getBindKey() {
            return mod.getBindKey();
         }

         @Override
         public void setBindKey(int keyCode) {
            mod.setBindKey(keyCode);
         }

         @Override
         public boolean hasCustomStudio() {
            return finalCustomStudio;
         }

         @Override
         public List<CorzSettingHandle> settings() {
            return wrappedSettings;
         }
      };
   }

   private static String humanizeTitle(String rawTitle, String fallbackId) {
      String base = (rawTitle != null && !rawTitle.isBlank() && !"Unknown".equals(rawTitle)) ? rawTitle : fallbackId;
      if (base.contains(" ")) {
         return base;
      }
      StringBuilder sb = new StringBuilder(base.length() + 4);
      for (int i = 0; i < base.length(); i++) {
         char c = base.charAt(i);
         if (i > 0 && Character.isUpperCase(c)) {
            char prev = base.charAt(i - 1);
            boolean nextLower = (i + 1 < base.length()) && Character.isLowerCase(base.charAt(i + 1));
            if (Character.isLowerCase(prev) || (Character.isUpperCase(prev) && nextLower)) {
               sb.append(' ');
            }
         }
         sb.append(c);
      }
      return sb.toString();
   }

   private static CorzSettingHandle wrapSetting(ConfigSetting s) {
      if (s instanceof BooleanSetting b) {
         return new CorzSettingHandle() {
            @Override
            public Kind kind() {
               return Kind.BOOLEAN;
            }

            @Override
            public String name() {
               return b.getSettingName();
            }

            @Override
            public String description() {
               return b.getDescription();
            }

            @Override
            public boolean getBool() {
               return b.isEnabled();
            }

            @Override
            public void setBool(boolean value) {
               b.handle(value);
            }
         };
      }
      if (s instanceof IntegerSetting iv) {
         return new CorzSettingHandle() {
            @Override
            public Kind kind() {
               return Kind.INT;
            }

            @Override
            public String name() {
               return iv.getSettingName();
            }

            @Override
            public String description() {
               return iv.getDescription();
            }

            @Override
            public double getNumber() {
               return iv.getValue();
            }

            @Override
            public void setNumber(double value) {
               int step = Math.max(1, iv.getStepSize());
               int snapped = (int)Math.round((value - iv.getMinValue()) / step) * step + iv.getMinValue();
               iv.handleMax(snapped);
            }

            @Override
            public double min() {
               return iv.getMinValue();
            }

            @Override
            public double max() {
               return iv.getMaxValue();
            }

            @Override
            public double step() {
               return Math.max(1, iv.getStepSize());
            }

            @Override
            public String formatValue() {
               String hint = iv.getHintLabel();
               return hint != null ? hint : Integer.toString(iv.getValue());
            }
         };
      }
      if (s instanceof DoubleSetting dv) {
         return new CorzSettingHandle() {
            @Override
            public Kind kind() {
               return Kind.DOUBLE;
            }

            @Override
            public String name() {
               return dv.getSettingName();
            }

            @Override
            public String description() {
               return dv.getDescription();
            }

            @Override
            public double getNumber() {
               return dv.getValue();
            }

            @Override
            public void setNumber(double value) {
               double step = dv.getStepSize() > 0.0 ? dv.getStepSize() : 0.01;
               double snapped = Math.round((value - dv.getMinValue()) / step) * step + dv.getMinValue();
               dv.handleMaxAmount(snapped);
            }

            @Override
            public double min() {
               return dv.getMinValue();
            }

            @Override
            public double max() {
               return dv.getMaxValue();
            }

            @Override
            public double step() {
               return dv.getStepSize() > 0.0 ? dv.getStepSize() : 0.05;
            }

            @Override
            public String formatValue() {
               double step = dv.getStepSize();
               if (step >= 1.0) {
                  return Integer.toString((int)Math.round(dv.getValue()));
               }
               if (step >= 0.1) {
                  return String.format(Locale.ROOT, "%.1f", dv.getValue());
               }
               return String.format(Locale.ROOT, "%.2f", dv.getValue());
            }
         };
      }
      if (s instanceof RangeSetting rv) {
         return new CorzSettingHandle() {
            @Override
            public Kind kind() {
               return Kind.RANGE;
            }

            @Override
            public String name() {
               return rv.getSettingName();
            }

            @Override
            public String description() {
               return rv.getDescription();
            }

            @Override
            public double min() {
               return rv.getRangeMin();
            }

            @Override
            public double max() {
               return rv.getRangeMax();
            }

            @Override
            public double step() {
               return rv.getStep() > 0.0 ? rv.getStep() : 0.1;
            }

            @Override
            public double getRangeMin() {
               return rv.getMin();
            }

            @Override
            public double getRangeMax() {
               return rv.getMax();
            }

            @Override
            public void setRange(double low, double high) {
               double lo = Math.min(low, high);
               double hi = Math.max(low, high);
               if (lo > rv.getMax()) {
                  rv.handleMaxAmount(hi);
                  rv.handleMaxAmount2(lo);
               } else {
                  rv.handleMaxAmount2(lo);
                  rv.handleMaxAmount(hi);
               }
            }

            @Override
            public String formatValue() {
               if (rv.getStep() >= 1.0) {
                  return (int)Math.round(rv.getMin()) + " - " + (int)Math.round(rv.getMax());
               }
               return String.format(Locale.ROOT, "%.1f - %.1f", rv.getMin(), rv.getMax());
            }
         };
      }
      if (s instanceof EnumSetting ev) {
         return new CorzSettingHandle() {
            @Override
            public Kind kind() {
               return Kind.ENUM;
            }

            @Override
            public String name() {
               return ev.getSettingName();
            }

            @Override
            public String description() {
               return ev.getDescription();
            }

            @Override
            public List<String> options() {
               return ev.getOptions();
            }

            @Override
            public int optionIndex() {
               return ev.getCurrentIndex();
            }

            @Override
            public String currentOption() {
               return ev.getCurrentOption();
            }

            @Override
            public void setOptionIndex(int index) {
               ev.handle(index);
            }
         };
      }
      if (s instanceof RadioSetting rad) {
         return new CorzSettingHandle() {
            @Override
            public Kind kind() {
               return Kind.ENUM;
            }

            @Override
            public String name() {
               return rad.getSettingName();
            }

            @Override
            public String description() {
               return rad.getDescription();
            }

            @Override
            public List<String> options() {
               List<RadioStationRegistry.Station> sts = rad.getStations();
               if (sts == null || sts.isEmpty()) {
                  return List.of("None");
               }
               List<String> names = new ArrayList<>(sts.size());
               for (RadioStationRegistry.Station st : sts) {
                  names.add(st.title().isEmpty() ? st.key() : st.title());
               }
               return names;
            }

            @Override
            public int optionIndex() {
               List<RadioStationRegistry.Station> sts = rad.getStations();
               if (sts == null) {
                  return 0;
               }
               for (int i = 0; i < sts.size(); i++) {
                  if (rad.isSelected(sts.get(i))) {
                     return i;
                  }
               }
               return 0;
            }

            @Override
            public String currentOption() {
               List<String> opts = this.options();
               int idx = this.optionIndex();
               return idx >= 0 && idx < opts.size() ? opts.get(idx) : "None";
            }

            @Override
            public void setOptionIndex(int index) {
               List<RadioStationRegistry.Station> sts = rad.getStations();
               if (sts != null && index >= 0 && index < sts.size()) {
                  rad.setSelectedStationKey(sts.get(index).key());
               }
            }
         };
      }
      if (s instanceof ColorSetting cv) {
         return new CorzSettingHandle() {
            @Override
            public Kind kind() {
               return Kind.COLOR;
            }

            @Override
            public String name() {
               return cv.getSettingName();
            }

            @Override
            public String description() {
               return cv.getDescription();
            }

            @Override
            public int getColor() {
               return cv.getColor();
            }

            @Override
            public void setColor(int argb) {
               cv.setColor(argb);
            }
         };
      }
      if (s instanceof StringBase sv) {
         return new CorzSettingHandle() {
            @Override
            public Kind kind() {
               return Kind.STRING;
            }

            @Override
            public String name() {
               return sv.getSettingName();
            }

            @Override
            public String description() {
               return sv.getDescription();
            }

            @Override
            public String getString() {
               return sv.getCurrentText();
            }

            @Override
            public void setString(String value) {
               sv.handleText(value);
               ModuleConfigAutosave.updateFlag2();
            }
         };
      }
      if (s instanceof KeybindSetting kv) {
         return new CorzSettingHandle() {
            @Override
            public Kind kind() {
               return Kind.KEYBIND;
            }

            @Override
            public String name() {
               return kv.getSettingName();
            }

            @Override
            public String description() {
               return kv.getDescription();
            }

            @Override
            public int getKey() {
               return kv.getKeyCode();
            }

            @Override
            public void setKey(int keyCode) {
               kv.setValue2(keyCode);
               ModuleConfigAutosave.updateFlag2();
            }
         };
      }
      if (s instanceof ButtonBase bv) {
         return new CorzSettingHandle() {
            @Override
            public Kind kind() {
               return Kind.BUTTON;
            }

            @Override
            public String name() {
               return bv.formatSupplier();
            }

            @Override
            public String description() {
               return bv.getDescription();
            }

            @Override
            public String buttonLabel() {
               return bv.getText();
            }

            @Override
            public void pressButton() {
               bv.updateAction();
            }
         };
      }
      return null;
   }
}
