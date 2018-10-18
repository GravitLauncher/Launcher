package ru.gravit.utils.event;

@FunctionalInterface
public interface EventHandler<T extends EventInterface> {
    void run(T event);
}
