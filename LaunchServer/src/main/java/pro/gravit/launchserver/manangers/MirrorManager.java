package pro.gravit.launchserver.manangers;

import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.HTTPRequest;
import pro.gravit.utils.HttpDownloader;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class MirrorManager {
    protected final ArrayList<Mirror> list = new ArrayList<>();
    private transient final Logger logger = LogManager.getLogger();
    private Mirror defaultMirror;

    public void addMirror(String mirror) {
        Mirror m = new Mirror(mirror);
        m.enabled = true;
        if (defaultMirror == null) defaultMirror = m;
        list.add(m);
    }

    public void addMirror(String mirror, boolean enabled) {
        Mirror m = new Mirror(mirror);
        m.enabled = enabled;
        if (defaultMirror == null && enabled) defaultMirror = m;
        list.add(m);
    }

    public Mirror getDefaultMirror() {
        return defaultMirror;
    }

    public void setDefaultMirror(Mirror m) {
        defaultMirror = m;
    }

    public void disableMirror(int index) {
        list.get(index).enabled = false;
    }

    public void enableMirror(int index) {
        list.get(index).enabled = true;
    }

    public int size() {
        return list.size();
    }

    public boolean downloadZip(Mirror mirror, Path path, String mask, Object... args) throws IOException {
        if (!mirror.enabled) return false;
        URL url = mirror.getURL(mask, args);
        logger.debug("Try download {}", url.toString());
        try {
            HttpDownloader.downloadZip(url, path);
        } catch (IOException e) {
            logger.error("Download {} failed({}: {})", url.toString(), e.getClass().getName(), e.getMessage());
            return false;
        }
        return true;
    }

    public void downloadZip(Path path, String mask, Object... args) throws IOException {
        if (downloadZip(defaultMirror, path, mask, args)) {
            return;
        }
        for (Mirror mirror : list) {
            if (mirror != defaultMirror) {
                if (downloadZip(mirror, path, mask, args)) return;
            }
        }
        throw new IOException(String.format("Error download %s. All mirrors return error", path.toString()));
    }

    public JsonElement jsonRequest(Mirror mirror, JsonElement request, String method, String mask, Object... args) throws IOException {
        if (!mirror.enabled) return null;
        URL url = mirror.getURL(mask, args);
        try {
            return HTTPRequest.jsonRequest(request, method, url);
        } catch (IOException e) {
            logger.error("JsonRequest {} failed({}: {})", url.toString(), e.getClass().getName(), e.getMessage());
            return null;
        }
    }

    public JsonElement jsonRequest(JsonElement request, String method, String mask, Object... args) throws IOException {
        JsonElement result = jsonRequest(defaultMirror, request, method, mask, args);
        if (result != null) return result;
        for (Mirror mirror : list) {
            if (mirror != defaultMirror) {
                result = jsonRequest(mirror, request, method, mask, args);
                if (result != null) return result;
            }
        }
        throw new IOException("Error jsonRequest. All mirrors return error");
    }

    public static class Mirror {
        final String baseUrl;
        boolean enabled;

        Mirror(String url) {
            baseUrl = url;
        }

        private URL formatArgs(String mask, Object... args) throws MalformedURLException {
            Object[] data = Arrays.stream(args).map(e -> IOHelper.urlEncode(e.toString())).toArray();
            return new URL(baseUrl.concat(String.format(mask, data)));
        }

        public URL getURL(String mask, Object... args) throws MalformedURLException {
            return formatArgs(mask, args);
        }
    }
}
