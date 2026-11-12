package dev.aether.settings;

import com.google.gson.JsonElement;
import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
   private final String name;
   private final String description;
   protected T value;
   protected final T defaultValue;
   private BooleanSupplier visibility = () -> true;

   protected Setting(String var1, String var2, T var3) {
      this.name = var1;
      this.description = var2.replace("The Aether badge", "Aether Client watermark");
      this.value = var3;
      this.defaultValue = var3;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public T get() {
      return this.value;
   }

   public void set(T var1) {
      this.value = var1;
   }

   public void reset() {
      this.value = this.defaultValue;
   }

   public void visibleWhen(BooleanSupplier var1) {
      this.visibility = var1;
   }

   public boolean isVisible() {
      return this.visibility.getAsBoolean();
   }

   public abstract JsonElement toJson();

   public abstract void fromJson(JsonElement jsonElement);
}
