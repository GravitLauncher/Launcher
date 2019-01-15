package ru.gravit.launchserver.manangers;

import ru.gravit.utils.helper.IOHelper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MirrorManager {
    public class Mirror {
        URL url;
        String assetsURLMask;
        String clientsURLMask;
        boolean enabled;

        Mirror(String url) {
            assetsURLMask = url.concat("assets/%s.zip");
            clientsURLMask = url.concat("clients/%s.zip");
        }

        private URL formatArg(String mask, String arg) throws MalformedURLException {
            return new URL(String.format(mask, IOHelper.urlEncode(arg)));
        }

        public URL getAssetsURL(String assets) throws MalformedURLException {
            return formatArg(assetsURLMask, assets);
        }

        public URL getClientsURL(String client) throws MalformedURLException {
            return formatArg(clientsURLMask, client);
        }
    }

    protected ArrayList<Mirror> list = new ArrayList<>();
    private Mirror defaultMirror;

    public void addMirror(String mirror) {
        Mirror m = new Mirror(mirror);
        m.enabled = true;
        if (defaultMirror == null) defaultMirror = m;
    }

    public void addMirror(String mirror, boolean enabled) throws MalformedURLException {
        Mirror m = new Mirror(mirror);
        m.url = new URL(mirror);
        m.enabled = enabled;
        if (defaultMirror == null && enabled) defaultMirror = m;
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
}
