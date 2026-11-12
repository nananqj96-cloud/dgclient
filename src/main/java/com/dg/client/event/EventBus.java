package com.dg.client.event;

import com.dg.client.core.DGLogger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * High-throughput, crash-isolated event bus supporting both {@link EventHandler}
 * annotated methods and functional lambda listeners with {@link EventPriority}.
 */
public final class EventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<ListenerEntry<?>>> listeners = new ConcurrentHashMap<>();
    private volatile BiConsumer<Object, Throwable> faultHandler = (owner, throwable) ->
            DGLogger.error("Unhandled listener exception in " + owner, throwable);

    public void setFaultHandler(BiConsumer<Object, Throwable> faultHandler) {
        if (faultHandler != null) {
            this.faultHandler = faultHandler;
        }
    }

    public void subscribe(Object subscriber) {
        if (subscriber == null) {
            return;
        }
        Class<?> type = subscriber.getClass();
        while (type != null && type != Object.class) {
            for (Method method : type.getDeclaredMethods()) {
                EventHandler annotation = method.getAnnotation(EventHandler.class);
                if (annotation == null) {
                    continue;
                }
                if (method.getParameterCount() != 1) {
                    DGLogger.warn("Ignoring @EventHandler {}#{} with invalid parameter count",
                            type.getSimpleName(), method.getName());
                    continue;
                }
                Class<?> eventType = method.getParameterTypes()[0];
                method.setAccessible(true);
                Method targetMethod = method;
                ListenerEntry<Object> entry = new ListenerEntry<>(
                        subscriber,
                        annotation.priority(),
                        annotation.receiveCancelled(),
                        event -> {
                            try {
                                targetMethod.invoke(subscriber, event);
                            } catch ( java.lang.reflect.InvocationTargetException ite) {
                                Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
                                throw new ListenerInvocationException(cause);
                            } catch (IllegalAccessException iae) {
                                throw new ListenerInvocationException(iae);
                            }
                        }
                );
                insertSorted(eventType, entry);
            }
            type = type.getSuperclass();
        }
    }

    public void unsubscribe(Object subscriber) {
        if (subscriber == null) {
            return;
        }
        for (CopyOnWriteArrayList<ListenerEntry<?>> list : listeners.values()) {
            list.removeIf(entry -> entry.owner == subscriber);
        }
    }

    public <E> void register(Class<E> eventType, EventPriority priority, Consumer<E> consumer) {
        register( consumer, eventType, priority, false, consumer);
    }

    public <E> void register(Object owner, Class<E> eventType, EventPriority priority, boolean receiveCancelled, Consumer<E> consumer) {
        ListenerEntry<E> entry = new ListenerEntry<>(owner, priority, receiveCancelled, consumer);
        insertSorted(eventType, entry);
    }

    @SuppressWarnings("unchecked")
    public <E> E post(E event) {
        if (event == null) {
            return null;
        }
        CopyOnWriteArrayList<ListenerEntry<?>> list = listeners.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return event;
        }
        boolean cancellable = event instanceof CancellableEvent;
        for (ListenerEntry<?> raw : list) {
            if (cancellable && ((CancellableEvent) event).isCancelled() && !raw.receiveCancelled) {
                continue;
            }
            ListenerEntry<E> entry = (ListenerEntry<E>) raw;
            try {
                entry.consumer.accept(event);
            } catch (ListenerInvocationException lie) {
                faultHandler.accept(entry.owner, lie.getCause());
            } catch (Throwable t) {
                faultHandler.accept(entry.owner, t);
            }
        }
        return event;
    }

    private void insertSorted(Class<?> eventType, ListenerEntry<?> newEntry) {
        listeners.compute(eventType, (key, existing) -> {
            List<ListenerEntry<?>> temp = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            temp.removeIf(e -> e.owner == newEntry.owner && e.consumer == newEntry.consumer);
            temp.add(newEntry);
            temp.sort(Comparator.comparingInt(e -> e.priority.getOrder()));
            return new CopyOnWriteArrayList<>(temp);
        });
    }

    private static final class ListenerEntry<E> {
        final Object owner;
        final EventPriority priority;
        final boolean receiveCancelled;
        final Consumer<E> consumer;

        ListenerEntry(Object owner, EventPriority priority, boolean receiveCancelled, Consumer<E> consumer) {
            this.owner = owner;
            this.priority = priority;
            this.receiveCancelled = receiveCancelled;
            this.consumer = consumer;
        }
    }

    private static final class ListenerInvocationException extends RuntimeException {
        ListenerInvocationException(Throwable cause) {
            super(cause);
        }
    }
}
