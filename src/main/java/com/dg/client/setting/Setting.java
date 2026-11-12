package com.dg.client.setting;

import com.google.gson.JsonElement;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Base typed setting with default value, visibility predicate, change listener,
 * and safe JSON serialization.
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    private final T defaultValue;
    private T value;
    private BooleanSupplier visible = () -> true;
    private Consumer<T> onChanged;

    protected Setting(String name, String description, T defaultValue) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = description == null ? "" : description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T newValue) {
        T validated = validate(newValue);
        if (!Objects.equals(this.value, validated)) {
            this.value = validated;
            if (onChanged != null) {
                onChanged.accept(this.value);
            }
        }
    }

    protected T validate(T incoming) {
        return incoming == null ? defaultValue : incoming;
    }

    public void reset() {
        setValue(defaultValue);
    }

    public boolean isVisible() {
        return visible == null || visible.getAsBoolean();
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier predicate) {
        this.visible = predicate == null ? () -> true : predicate;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S onChange(Consumer<T> callback) {
        this.onChanged = callback;
        return (S) this;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement element);
}
