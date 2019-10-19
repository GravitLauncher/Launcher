package pro.gravit.launchserver.auth.hwid;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import pro.gravit.launcher.hwid.HWID;
import pro.gravit.launcher.hwid.OshiHWID;
import pro.gravit.utils.HTTPRequest;
import pro.gravit.utils.helper.LogHelper;

public final class JsonHWIDHandler extends HWIDHandler {
    private static final Gson gson = new Gson();

    private URL url;
    private URL urlBan;
    private URL urlUnBan;
    private URL urlGet;
    private String apiKey;

    public static class banRequest {
        public banRequest(OshiHWID hwid) {
            this.hwid = hwid;
        }

        final OshiHWID hwid;
        String apiKey;

        public banRequest(OshiHWID hwid, String apiKey) {
            this.hwid = hwid;
            this.apiKey = apiKey;
        }
    }

    public static class checkRequest {
        public checkRequest(String username, OshiHWID hwid) {
            this.username = username;
            this.hwid = hwid;
        }

        final String username;
        final OshiHWID hwid;
        String apiKey;

        public checkRequest(String username, OshiHWID hwid, String apiKey) {
            this.username = username;
            this.hwid = hwid;
            this.apiKey = apiKey;
        }
    }

    public static class Result {
        String error;
    }

    public static class BannedResult {
        boolean isBanned;
        String error;
    }

    public static class HWIDRequest {
        public HWIDRequest(String username) {
            this.username = username;
        }

        final String username;
        String apiKey;

        public HWIDRequest(String username, String apiKey) {
            this.username = username;
            this.apiKey = apiKey;
        }
    }

    @Override
    public void ban(List<HWID> l_hwid) throws HWIDException {
        for (HWID hwid : l_hwid) {
            banRequest request = new banRequest((OshiHWID) hwid, apiKey);
            try {
                JsonElement result = HTTPRequest.jsonRequest(gson.toJsonTree(request), urlBan);
                Result r = gson.fromJson(result, Result.class);
                if (r.error != null) throw new HWIDException(r.error);
            } catch (IOException e) {
                LogHelper.error(e);
                throw new HWIDException("HWID service error");
            }
        }
    }

    @Override
    public void check0(HWID hwid, String username) throws HWIDException {
        checkRequest request = new checkRequest(username, (OshiHWID) hwid, apiKey);
        try {
            JsonElement result = HTTPRequest.jsonRequest(gson.toJsonTree(request), url);
            BannedResult r = gson.fromJson(result, BannedResult.class);
            if (r.error != null) throw new HWIDException(r.error);
            boolean isBanned = r.isBanned;
            if (isBanned) throw new HWIDException("You will BANNED!");
        } catch (IOException e) {
            LogHelper.error(e);
            throw new HWIDException("HWID service error");
        }
    }

    @Override
    public void close() {
        // pass
    }

    @Override
    public void init() {

    }

    @Override
    public List<HWID> getHwid(String username) throws HWIDException {
        ArrayList<HWID> hwids = new ArrayList<>();
        HWIDRequest request = new HWIDRequest(username, apiKey);
        try {
            JsonElement result = HTTPRequest.jsonRequest(gson.toJsonTree(request), urlGet);
            OshiHWID[] r = gson.fromJson(result, OshiHWID[].class);
            hwids.addAll(Arrays.asList(r));
        } catch (IOException e) {
            LogHelper.error(e);
            throw new HWIDException("HWID service error");
        }
        return hwids;
    }

    @Override
    public void unban(List<HWID> l_hwid) throws HWIDException {
        for (HWID hwid : l_hwid) {
            banRequest request = new banRequest((OshiHWID) hwid, apiKey);
            try {
                JsonElement result = HTTPRequest.jsonRequest(gson.toJsonTree(request), urlUnBan);
                Result r = gson.fromJson(result, Result.class);
                if (r.error != null) throw new HWIDException(r.error);
            } catch (IOException e) {
                LogHelper.error(e);
                throw new HWIDException("HWID service error");
            }
        }
    }
}
