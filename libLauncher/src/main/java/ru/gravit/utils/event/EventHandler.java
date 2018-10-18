package ru.gravit.utils.event;

@FunctionalInterface
public interface EventHandler {
    void run(EventInterface event);
}