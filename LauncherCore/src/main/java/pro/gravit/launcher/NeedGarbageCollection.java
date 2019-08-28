package pro.gravit.launcher;

/**
 * Determines whether this object requires periodic garbage collection by the gc command
 * This interface has nothing to do with java garbage collection.
 */
@FunctionalInterface
public interface NeedGarbageCollection {
    void garbageCollection();
}
