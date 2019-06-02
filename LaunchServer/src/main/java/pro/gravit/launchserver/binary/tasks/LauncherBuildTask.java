package pro.gravit.launchserver.binary.tasks;

import java.io.IOException;
import java.nio.file.Path;

public interface LauncherBuildTask {
    String getName();

    Path process(Path inputFile) throws IOException;

    boolean allowDelete();
}
