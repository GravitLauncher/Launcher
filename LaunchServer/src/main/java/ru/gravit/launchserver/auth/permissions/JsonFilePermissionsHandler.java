package ru.gravit.launchserver.auth.permissions;

import com.google.gson.reflect.TypeToken;
import ru.gravit.launcher.ClientPermissions;
import ru.gravit.launcher.Launcher;
import ru.gravit.launchserver.Reloadable;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class JsonFilePermissionsHandler extends PermissionsHandler implements Reloadable {
    public String filename = "permissions.json";
    public static Map<String, ClientPermissions> map;

    @Override
    public void reload() {
        map.clear();
        Path path = Paths.get(filename);
        Type type = new TypeToken<Map<String, ClientPermissions>>() {
        }.getType();
        try (Reader reader = IOHelper.newReader(path)) {
            map = Launcher.gson.fromJson(reader, type);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    @Override
    public void close() throws Exception {

    }

    public static class Enity {
        public String username;
        public ClientPermissions permissions;
    }

    @Override
    public ClientPermissions getPermissions(String username) {
        return map.getOrDefault(username, ClientPermissions.DEFAULT);
    }

    public JsonFilePermissionsHandler() {
        Type type = new TypeToken<Map<String, ClientPermissions>>() {
        }.getType();
        Path path = Paths.get(filename);
        if (!IOHelper.exists(path)) {
            map = new HashMap<>();
            try (Writer writer = IOHelper.newWriter(path)) {
                Launcher.gson.toJson(map, writer);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        try (Reader reader = IOHelper.newReader(path)) {
            map = Launcher.gson.fromJson(reader, type);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }
}
