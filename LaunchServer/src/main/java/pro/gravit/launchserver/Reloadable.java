package pro.gravit.launchserver;

@FunctionalInterface
@Deprecated
public interface Reloadable {
    void reload() throws Exception;
}
