package pro.gravit.launcher.profiles.optional.actions;

public class OptionalActionClassPath extends OptionalAction {
    public String[] args;
    public boolean useAltClasspath = false;

    public OptionalActionClassPath() {
    }

    public OptionalActionClassPath(String[] args) {
        this.args = args;
    }

    public OptionalActionClassPath(String[] args, boolean useAltClasspath) {
        this.args = args;
        this.useAltClasspath = useAltClasspath;
    }
}
