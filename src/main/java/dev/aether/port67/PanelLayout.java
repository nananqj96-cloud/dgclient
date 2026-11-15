package dev.aether.port67;

import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Ported from the 67port source ({@code sixseven.client.ui.PanelLayout}). The category list is
 * supplied by the caller (Aether's categories) instead of the 67client's {@code JsonSupport2}, so
 * the real 67client layout logic runs unchanged against Aether's module categories.
 */
public class PanelLayout {
   public final Map<String, PanelLayoutSupport> map = new LinkedHashMap<>();
   public final Set<String> set = new HashSet<>();
   public boolean enabled;
   public boolean enabled2;
   public float amount = -1.0F;
   public float amount2 = -1.0F;
   public static final int count = 4;
   public static final float amount3 = 210.0F;

   private final List<String> categoryNames;

   public PanelLayout(List<String> categoryNames) {
      this.categoryNames = categoryNames;
      float floatValue = 16.0F;
      for (String name : categoryNames) {
         this.map.put(name, new PanelLayoutSupport(floatValue, 64.0F));
         floatValue += 222.0F;
      }
   }

   public void update() {
      this.enabled2 = true;
   }

   public void ensureDefaultLayout(float amount4, float amount22) {
      if (!this.enabled2 && (!this.enabled || amount4 != this.amount || amount22 != this.amount2)) {
         this.enabled = true;
         this.amount = amount4;
         this.amount2 = amount22;
         String[] textArray = this.categoryNames.toArray(new String[0]);
         float floatValue = textArray.length * 222.0F + 12.0F;
         if (amount4 >= floatValue) {
            float floatValue2 = (amount4 - textArray.length * 210.0F) / (textArray.length + 1);
            float amount5 = floatValue2;
            for (int index = 0; index < textArray.length; index++) {
               PanelLayoutSupport panelLayoutSupport = this.panel(textArray[index]);
               panelLayoutSupport.amount = amount5;
               panelLayoutSupport.amount2 = 64.0F;
               panelLayoutSupport.enabled = false;
               amount5 += 210.0F + floatValue2;
            }
         } else {
            int intValue = Math.max(1, (int) ((amount4 - 24.0F) / 222.0F));
            for (int index2 = 0; index2 < textArray.length; index2++) {
               PanelLayoutSupport panelLayoutSupport2 = this.panel(textArray[index2]);
               panelLayoutSupport2.amount = 12.0F + index2 % intValue * 222.0F;
               int intValue2 = (textArray.length + intValue - 1) / intValue;
               panelLayoutSupport2.amount2 =
                   64.0F + index2 / intValue * ((amount22 - 122.0F) / intValue2);
               panelLayoutSupport2.enabled = false;
            }
         }
      }
   }

   public PanelLayoutSupport panel(String text) {
      return this.map.computeIfAbsent(text, value -> new PanelLayoutSupport(16.0F, 16.0F));
   }

   public void update2() {
      this.enabled2 = false;
      this.enabled = false;
      this.set.clear();
   }

   public boolean test(String text) {
      return this.set.contains(text);
   }

   public void apply(String text, boolean flag) {
      if (flag) {
         this.set.add(text);
      } else {
         this.set.remove(text);
      }
   }

   public JsonObject getJsonObject() {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("v", 4);
      jsonObject.addProperty("custom", this.enabled2);
      for (Entry<String, PanelLayoutSupport> entry : this.map.entrySet()) {
         JsonObject jsonObject2 = new JsonObject();
         jsonObject2.addProperty("x", entry.getValue().amount);
         jsonObject2.addProperty("y", entry.getValue().amount2);
         jsonObject2.addProperty("collapsed", entry.getValue().enabled);
         jsonObject.add(entry.getKey(), jsonObject2);
      }
      return jsonObject;
   }

   public void apply(JsonObject jsonObject) {
      if (jsonObject.has("v") && jsonObject.get("v").getAsInt() >= 4) {
         if (jsonObject.has("custom") && jsonObject.get("custom").getAsBoolean()) {
            this.enabled = true;
            this.enabled2 = true;
            for (Entry<String, PanelLayoutSupport> entry : this.map.entrySet()) {
               JsonObject asJsonObject = jsonObject.getAsJsonObject(entry.getKey());
               if (asJsonObject != null) {
                  if (asJsonObject.has("x")) {
                     entry.getValue().amount = asJsonObject.get("x").getAsFloat();
                  }
                  if (asJsonObject.has("y")) {
                     entry.getValue().amount2 = asJsonObject.get("y").getAsFloat();
                  }
                  if (asJsonObject.has("collapsed")) {
                     entry.getValue().enabled = asJsonObject.get("collapsed").getAsBoolean();
                  }
               }
            }
         }
      }
   }
}
