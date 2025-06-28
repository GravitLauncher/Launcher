package pro.gravit.launchserver.binary.tasks;

import pro.gravit.launchserver.binary.PipelineContext;

import java.io.IOException;
import java.nio.file.Path;

public interface LauncherBuildTask {
    String getName();

    Path process(PipelineContext context) throws IOException;
}
