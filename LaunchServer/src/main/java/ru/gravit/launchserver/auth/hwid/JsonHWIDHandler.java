package ru.gravit.launchserver.auth.hwid;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import ru.gravit.launcher.HWID;
import ru.gravit.launcher.OshiHWID;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;
import ru.gravit.utils.HTTPRequest;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class JsonHWIDHandler extends HWIDHandler {
    private static final Gson gson = new Gson();

    @SuppressWarnings("unused")
    private final URL url;
    private final URL urlBan;
    private final URL urlUnBan;
    @SuppressWarnings("unused")
    private final URL urlGet;

    public class banRequest {
        public banRequest(String hwid) {
            this.hwid = hwid;
        }

        String hwid;
    }

    public class checkRequest {
        public checkRequest(String username, String hwid) {
            this.username = username;
            this.hwid = hwid;
        }

        String username;
        String hwid;

    }

    public class Result {
        String error;
    }

    public class BannedResult {
        boolean isBanned;
        String error;
    }

    public class HWIDResult {
        String string;
    }

    public class HWIDRequest {
        public HWIDRequest(String username) {
            this.username = username;
        }

        String username;
    }

    JsonHWIDHandler(BlockConfigEntry block) {
        super(block);
        String configUrl = block.getEntryValue("url", StringConfigEntry.class);
        String configUrlBan = block.getEntryValue("urlBan", StringConfigEntry.class);
        String configUrlUnBan = block.getEntryValue("urlUnBan", StringConfigEntry.class);
        String configUrlGet = block.getEntryValue("urlGet", StringConfigEntry.class);
        url = IOHelper.convertToURL(configUrl);
        urlBan = IOHelper.convertToURL(configUrlBan);
        urlUnBan = IOHelper.convertToURL(configUrlUnBan);
        urlGet = IOHelper.convertToURL(configUrlGet);
    }

    @Override
    public void ban(List<HWID> l_hwid) throws HWIDException {
        for (HWID hwid : l_hwid) {
            banRequest request = new banRequest(hwid.getSerializeString());
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
        checkRequest request = new checkRequest(username, hwid.getSerializeString());
        try {
            JsonElement result = HTTPRequest.jsonRequest(gson.toJsonTree(request), urlBan);
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
    public List<HWID> getHwid(String username) throws HWIDException {
        ArrayList<HWID> hwids = new ArrayList<>();
        HWIDRequest request = new HWIDRequest(username);
        try {
            JsonElement result = HTTPRequest.jsonRequest(gson.toJsonTree(request), urlBan);
            HWIDResult[] r = gson.fromJson(result, HWIDResult[].class);
            for (HWIDResult hw : r) {
                hwids.add(OshiHWID.gson.fromJson(hw.string, OshiHWID.class));
            }
        } catch (IOException e) {
            LogHelper.error(e);
            throw new HWIDException("HWID service error");
        }
        return hwids;
    }

    @Override
    public void unban(List<HWID> l_hwid) throws HWIDException {
        for (HWID hwid : l_hwid) {
            banRequest request = new banRequest(hwid.getSerializeString());
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
