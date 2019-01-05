package ru.gravit.launchserver.binary.tasks;

import java.io.IOException;
import java.nio.file.Path;

public interface LauncherBuildTask {
    String getName();
    int priority();
    Path process(Path inputFile) throws IOException;
}
