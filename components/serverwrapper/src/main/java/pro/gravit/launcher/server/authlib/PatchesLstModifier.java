package pro.gravit.launcher.server.authlib;

import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PatchesLstModifier implements LibrariesHashFileModifier {
    @Override
    public byte[] apply(byte[] data, InstallAuthlib.InstallAuthlibContext context) throws IOException {
        String[] lines = new String(data).split("\n");
        for(int i=0;i<lines.length;++i) {
            if(lines[i].contains("paper-")) {
                String[] separated = lines[i].split("\t");
                Path path = context.workdir.resolve("versions").resolve(separated[6]);
                if(Files.notExists(path)) {
                    LogHelper.warning("Unable to find %s. Maybe you should start the server at least once?", path);
                    return data;
                }
                separated[3] = SecurityHelper.toHex(SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256, path));
                lines[i] = String.join("\t", separated);
            }
        }
        return String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
    }
}
