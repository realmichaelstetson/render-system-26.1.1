package com.haloclient.client.event;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight event bus for dispatching events to registered listeners.
 * Uses reflection-based discovery of @EventHandler annotated methods.
 */
public class EventBus {
    private static final EventBus INSTANCE = new EventBus();

    private final Map<Class<? extends Event>, List<Subscription>> subscriptions = new ConcurrentHashMap<>();

    public static EventBus getInstance() {
        return INSTANCE;
    }

    /**
     * Registers all @EventHandler methods in the given listener object.
     */
    public void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(EventHandler.class)) continue;
            if (method.getParameterCount() != 1) continue;
            if (!Event.class.isAssignableFrom(method.getParameterTypes()[0])) continue;

            method.setAccessible(true);
            Class<? extends Event> eventType = method.getParameterTypes()[0].asSubclass(Event.class);
            int priority = method.getAnnotation(EventHandler.class).priority();

            List<Subscription> subs = subscriptions.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
            subs.add(new Subscription(listener, method, priority));
            // Sort by priority descending (higher = first)
            subs.sort(Comparator.comparingInt(Subscription::priority).reversed());
        }
    }

    /**
     * Unregisters all event handlers belonging to the given listener.
     */
    public void unregister(Object listener) {
        subscriptions.values().forEach(list ->
                list.removeIf(sub -> sub.listener() == listener)
        );
    }

    /**
     * Posts an event to all registered handlers.
     * Returns the event (possibly modified/cancelled by handlers).
     */
    public <T extends Event> T post(T event) {
        List<Subscription> subs = subscriptions.get(event.getClass());
        if (subs == null) return event;
        for (Subscription sub : subs) {
            try {
                sub.method().invoke(sub.listener(), event);
            } catch (Exception e) {
                System.err.println("[HaloClient] Event handler error in " +
                        sub.listener().getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
            if (event.isCancelled()) break;
        }
        return event;
    }

    private record Subscription(Object listener, Method method, int priority) {}
}
