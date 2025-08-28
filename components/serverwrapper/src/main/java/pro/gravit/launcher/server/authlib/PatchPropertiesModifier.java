package pro.gravit.launcher.server.authlib;

import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PatchPropertiesModifier implements LibrariesHashFileModifier {
    @Override
    public byte[] apply(byte[] data, InstallAuthlib.InstallAuthlibContext context) throws IOException {
        String[] lines = new String(data).split("\n");
        String version = null;
        int linePatchedHashIndex = -1;
        int lineOriginalHashIndex = -1;
        for(int i=0;i<lines.length;++i) {
            if(lines[i].startsWith("version=")) {
                version = lines[i].split("=")[1];
            } else if(lines[i].startsWith("patchedHash=")) {
                linePatchedHashIndex = i;
            } else if(lines[i].startsWith("originalHash=")) {
                lineOriginalHashIndex = i;
            }
        }
        if(version == null) {
            LogHelper.warning("Unable to parse version from patch.properties");
            return data;
        }
        if(linePatchedHashIndex < 0) {
            LogHelper.warning("Unable to parse patchedHash from patch.properties");
            return data;
        }
        if(lineOriginalHashIndex < 0) {
            LogHelper.warning("Unable to parse originalHash from patch.properties");
            return data;
        }
        Path patchedFile = context.workdir.resolve("cache").resolve("patched_".concat(version).concat(".jar"));
        Path originalFile = context.workdir.resolve("cache").resolve("mojang_".concat(version).concat(".jar"));
        if(Files.notExists(patchedFile)) {
            LogHelper.warning("Unable to find %s. Maybe you should start the server at least once?", patchedFile);
            return data;
        }
        if(Files.notExists(originalFile)) {
            LogHelper.warning("Unable to find %s. Maybe you should start the server at least once?", originalFile);
            return data;
        }
        String newPatchedHash = SecurityHelper.toHex(SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256, patchedFile)).toUpperCase();
        String newOriginalHash = SecurityHelper.toHex(SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256, originalFile)).toUpperCase();
        lines[linePatchedHashIndex] = "patchedHash=".concat(newPatchedHash);
        lines[lineOriginalHashIndex] = "originalHash=".concat(newOriginalHash);
        return String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
    }
}
