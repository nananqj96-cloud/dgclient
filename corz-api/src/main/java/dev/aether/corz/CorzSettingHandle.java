package dev.aether.corz;

import java.util.List;

public interface CorzSettingHandle {
   enum Kind {
      BOOLEAN,
      INT,
      DOUBLE,
      RANGE,
      ENUM,
      COLOR,
      STRING,
      KEYBIND,
      BUTTON
   }

   Kind kind();

   String name();

   String description();

   default boolean getBool() {
      return false;
   }

   default void setBool(boolean value) {
   }

   default double getNumber() {
      return 0.0;
   }

   default void setNumber(double value) {
   }

   default double min() {
      return 0.0;
   }

   default double max() {
      return 1.0;
   }

   default double step() {
      return 0.1;
   }

   default String formatValue() {
      return "";
   }

   default double getRangeMin() {
      return 0.0;
   }

   default double getRangeMax() {
      return 1.0;
   }

   default void setRange(double low, double high) {
   }

   default List<String> options() {
      return List.of();
   }

   default int optionIndex() {
      return 0;
   }

   default String currentOption() {
      return "";
   }

   default void setOptionIndex(int index) {
   }

   default int getColor() {
      return 0xFFFF6EC7;
   }

   default void setColor(int argb) {
   }

   default String getString() {
      return "";
   }

   default void setString(String value) {
   }

   default int getKey() {
      return -1;
   }

   default void setKey(int keyCode) {
   }

   default String buttonLabel() {
      return "";
   }

   default void pressButton() {
   }
}
