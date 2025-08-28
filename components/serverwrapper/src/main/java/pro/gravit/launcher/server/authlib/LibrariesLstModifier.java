package pro.gravit.launcher.server.authlib;

import pro.gravit.utils.helper.SecurityHelper;

import java.nio.charset.StandardCharsets;

public class LibrariesLstModifier implements LibrariesHashFileModifier {

    @Override
    public byte[] apply(byte[] data, InstallAuthlib.InstallAuthlibContext context) {
        String[] lines = new String(data).split("\n");
        for(int i=0;i<lines.length;++i) {
            if(lines[i].contains("com.mojang:authlib")) {
                String[] separated = lines[i].split("\t");
                separated[0] = SecurityHelper.toHex(SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256, context.repackedAuthlibBytes));
                lines[i] = String.join("\t", separated);
            }
        }
        return String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
    }
}
