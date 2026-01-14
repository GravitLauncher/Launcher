package pro.gravit.launcher.server.authlib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DownloadContextModifier implements LibrariesHashFileModifier {

    private static final Logger logger =
            LoggerFactory.getLogger(DownloadContextModifier.class);

    @Override
    public byte[] apply(byte[] data, InstallAuthlib.InstallAuthlibContext context) throws IOException {
        String[] lines = new String(data).split("\n");
        for(int i=0;i<lines.length;++i) {
            if(lines[i].contains("mojang_")) {
                String[] separated = lines[i].split("\t");
                Path path = context.workdir.resolve("cache").resolve(separated[2]);
                if(Files.notExists(path)) {
                    logger.warn("Unable to find {}. Maybe you should start the server at least once?", path);
                    return data;
                }
                separated[0] = SecurityHelper.toHex(SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256, path));
                lines[i] = String.join("\t", separated);
            }
        }
        return String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
    }
}