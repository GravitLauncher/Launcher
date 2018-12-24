package ru.gravit.launchserver.auth.permissions;

import com.google.gson.reflect.TypeToken;
import ru.gravit.launcher.Launcher;
import ru.gravit.launchserver.auth.ClientPermissions;
import ru.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class JsonFilePermissionsHandler {
    public static String FILE_NAME = "permissions.json";
    public static Map<String,ClientPermissions> map;
    public static class Enity
    {
        public String username;
        public ClientPermissions permissions;
    }
    public static ClientPermissions getPermissions(String username)
    {
        return map.getOrDefault(username,ClientPermissions.DEFAULT);
    }
    public static void init() throws IOException {
        Type type = new TypeToken<Map<String,ClientPermissions>>(){}.getType();
        Path path = Paths.get(FILE_NAME);
        if(!IOHelper.exists(path))
        {
            map = new HashMap<>();
            try(Writer writer = IOHelper.newWriter(path))
            {
                Launcher.gson.toJson(map,writer);
            }
        }
        try(Reader reader = IOHelper.newReader(path))
        {
            map = Launcher.gson.fromJson(reader,type);
        }
    }
}
