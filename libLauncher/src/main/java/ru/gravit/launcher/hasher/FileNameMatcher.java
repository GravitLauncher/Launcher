package ru.gravit.launcher.hasher;

import java.util.Arrays;
import java.util.Collection;

import ru.gravit.launcher.LauncherAPI;

public final class FileNameMatcher {
    private static final String[] NO_ENTRIES = new String[0];

    private static boolean anyMatch(String[] entries, Collection<String> path) {
        return path.stream().anyMatch(e -> Arrays.stream(entries).anyMatch(p -> p.endsWith(e)));
        //for(String p : path)
        //{
        //    for(String e : entries)
        //    {
        //        if(p.endsWith(e)) return true;
        //    }
        //}
    }

    // Instance
    private final String[] update;
    private final String[] verify;

    private final String[] exclusions;


    @LauncherAPI
    public FileNameMatcher(String[] update, String[] verify, String[] exclusions) {
        this.update = update;
        this.verify = verify;
        this.exclusions = exclusions;
    }


    public boolean shouldUpdate(Collection<String> path) {
        return (anyMatch(update, path) || anyMatch(verify, path)) && !anyMatch(exclusions, path);
    }


    public boolean shouldVerify(Collection<String> path) {
        return anyMatch(verify, path) && !anyMatch(exclusions, path);
    }

    public FileNameMatcher verifyOnly() {
        return new FileNameMatcher(NO_ENTRIES, verify, exclusions);
    }
}
