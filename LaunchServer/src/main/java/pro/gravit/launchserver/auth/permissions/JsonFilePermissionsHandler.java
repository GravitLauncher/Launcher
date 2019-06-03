package pro.gravit.launchserver.auth.permissions;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.reflect.TypeToken;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.Launcher;
import pro.gravit.launchserver.Reloadable;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

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
            map = Launcher.gsonManager.gson.fromJson(reader, type);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    @Override
    public void close() {

    }

    public static class Enity {
        public String username;
        public ClientPermissions permissions;
    }

    @Override
    public void init() {
        Type type = new TypeToken<Map<String, ClientPermissions>>() {
        }.getType();
        Path path = Paths.get(filename);
        if (!IOHelper.exists(path)) {
            map = new HashMap<>();
            try (Writer writer = IOHelper.newWriter(path)) {
                Launcher.gsonManager.gson.toJson(map, writer);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        try (Reader reader = IOHelper.newReader(path)) {
            map = Launcher.gsonManager.gson.fromJson(reader, type);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    @Override
    public ClientPermissions getPermissions(String username) {
        return map.getOrDefault(username, ClientPermissions.DEFAULT);
    }

    @Override
    public void setPermissions(String username, ClientPermissions permissions) {
        map.put(username, permissions);
    }

    public JsonFilePermissionsHandler() {

    }
}
