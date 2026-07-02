package pro.gravit.launcher.base.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventBus {
    private transient final Logger logger = LoggerFactory.getLogger(EventBus.class);

    // 1. Highly optimized, lock-free cache for class hierarchies to enable polymorphic dispatch
    private static final ClassValue<List<Class<?>>> TYPE_HIERARCHY_CACHE = new ClassValue<>() {
        @Override
        protected List<Class<?>> computeValue(Class<?> type) {
            Set<Class<?>> classes = new LinkedHashSet<>();
            Queue<Class<?>> queue = new LinkedList<>();
            queue.add(type);

            // Breadth-First Search to capture superclasses and interfaces
            while (!queue.isEmpty()) {
                Class<?> current = queue.poll();
                if (current != null && classes.add(current)) {
                    queue.add(current.getSuperclass());
                    queue.addAll(Arrays.asList(current.getInterfaces()));
                }
            }
            // Store as an immutable list for fast, safe iteration
            return List.copyOf(classes);
        }
    };

    // 2. Thread-safe routing table optimized for read-heavy workloads
    private final ConcurrentMap<Class<?>, CopyOnWriteArrayList<Consumer<Object>>> handlers = new ConcurrentHashMap<>();

    /**
     * A clean API for unregistering handlers. Implement AutoCloseable for try-with-resources support.
     */
    @FunctionalInterface
    public interface Subscription extends AutoCloseable {
        void cancel();

        @Override
        default void close() {
            cancel();
        }
    }

    /**
     * Registers a handler for a specific event type.
     *
     * @param eventType The class of the event to listen for
     * @param handler The logic to execute when the event is received
     * @return A Subscription object used to unregister the handler
     */
    @SuppressWarnings("unchecked")
    public <T> Subscription register(Class<T> eventType, Consumer<T> handler) {
        // Safe cast: the publish logic guarantees we only pass instances of T or its subclasses
        var typeErasedHandler = (Consumer<Object>) handler;

        var list = handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
        list.add(typeErasedHandler);

        // Return a lambda to easily remove this specific handler
        return () -> list.remove(typeErasedHandler);
    }

    /**
     * Publishes an event to all registered handlers of its exact type, superclasses, and interfaces.
     */
    public void publish(Object event) {
        if (event == null) {
            return;
        }

        var eventType = event.getClass();
        var hierarchy = TYPE_HIERARCHY_CACHE.get(eventType);

        // Iterate through the exact class, all superclasses, and all interfaces
        for (var type : hierarchy) {
            var typeHandlers = handlers.get(type);

            if (typeHandlers != null && !typeHandlers.isEmpty()) {
                // CopyOnWriteArrayList allows lock-free concurrent iteration
                for (var handler : typeHandlers) {
                    try {
                        handler.accept(event);
                    } catch (Exception e) {
                        handleException(e, event, handler);
                    }
                }
            }
        }
    }

    /**
     * Extension point for centralized error handling without interrupting other listeners.
     */
    protected void handleException(Exception e, Object event, Consumer<Object> handler) {
        logger.error("Error dispatching event {}: {}%n", event.getClass().getSimpleName(), e.getMessage());
    }
}