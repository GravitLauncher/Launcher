package ru.gravit.launchserver.auth.hwid;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import ru.gravit.launcher.helper.IOHelper;
import ru.gravit.launcher.helper.LogHelper;
import ru.gravit.launcher.helper.VerifyHelper;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class JsonHWIDHandler extends HWIDHandler {
    private static final int TIMEOUT = Integer.parseInt(
            System.getProperty("launcher.connection.timeout", Integer.toString(1500)));

    private final URL url;
    private final URL urlBan;
    private final URL urlUnBan;
    private final URL urlGet;
    private final String loginKeyName;
    private final String hddKeyName;
    private final String cpuKeyName;
    private final String biosKeyName;
    private final String isBannedKeyName;

    JsonHWIDHandler(BlockConfigEntry block) {
        super(block);
        String configUrl = block.getEntryValue("url", StringConfigEntry.class);
        String configUrlBan = block.getEntryValue("urlBan", StringConfigEntry.class);
        String configUrlUnBan = block.getEntryValue("urlUnBan", StringConfigEntry.class);
        String configUrlGet = block.getEntryValue("urlGet", StringConfigEntry.class);
        loginKeyName = VerifyHelper.verify(block.getEntryValue("loginKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "Login key name can't be empty");
        hddKeyName = VerifyHelper.verify(block.getEntryValue("hddKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "HDD key name can't be empty");
        cpuKeyName = VerifyHelper.verify(block.getEntryValue("cpuKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "CPU key can't be empty");
        biosKeyName = VerifyHelper.verify(block.getEntryValue("biosKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "Bios key can't be empty");
        isBannedKeyName = VerifyHelper.verify(block.getEntryValue("isBannedKeyName", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "Response username key can't be empty");
        url = IOHelper.convertToURL(configUrl);
        urlBan = IOHelper.convertToURL(configUrlBan);
        urlUnBan = IOHelper.convertToURL(configUrlUnBan);
        urlGet = IOHelper.convertToURL(configUrlGet);
    }

    @Override
    public void ban(List<HWID> l_hwid) throws HWIDException {
        for(HWID hwid : l_hwid) {
            JsonObject request = Json.object().add(hddKeyName, hwid.getHwid_hdd()).add(cpuKeyName, hwid.getHwid_cpu()).add(biosKeyName, hwid.getHwid_bios());
            try {
                request(request,urlBan);
            } catch (IOException e) {
                LogHelper.error(e);
                throw new HWIDException("HWID service error");
            }
        }
    }
    public JsonObject request(JsonObject request, URL url) throws HWIDException, IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        if (TIMEOUT > 0)
            connection.setConnectTimeout(TIMEOUT);

        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), Charset.forName("UTF-8"));
        writer.write(request.toString());
        writer.flush();
        writer.close();

        InputStreamReader reader;
        int statusCode = connection.getResponseCode();

        if (200 <= statusCode && statusCode < 300)
            reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
        else
            reader = new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8);
        JsonValue content = Json.parse(reader);
        if (!content.isObject())
            throw new HWIDException("HWID server response is malformed");

        JsonObject response = content.asObject();
        return  response;
    }
    @Override
    public void check0(HWID hwid, String username) throws HWIDException {
        JsonObject request = Json.object().add(loginKeyName,username).add(hddKeyName,hwid.getHwid_hdd()).add(cpuKeyName,hwid.getHwid_cpu()).add(biosKeyName,hwid.getHwid_bios());
        JsonObject response = null;
        try {
            response = request(request,url);
        } catch (IOException e) {
            LogHelper.error(e);
            throw new HWIDException("HWID service error");
        }
        boolean isBanned = response.getBoolean(isBannedKeyName,false);
        if(isBanned) throw new HWIDException("You will BANNED!");
    }

    @Override
    public void close() {
        // pass
    }

    @Override
    public List<HWID> getHwid(String username) throws HWIDException {
        JsonObject request = Json.object().add(loginKeyName,username);
        JsonObject responce;
        try {
            responce = request(request,urlGet);
        } catch (IOException e) {
            LogHelper.error(e);
            throw new HWIDException("HWID service error");
        }
        JsonArray array = responce.get("hwids").asArray();
        ArrayList<HWID> hwids = new ArrayList<>();
        for(JsonValue i : array)
        {
            long hdd=0,cpu=0,bios=0;
            JsonObject object = i.asObject();
            hdd = object.getLong(hddKeyName,0);
            cpu = object.getLong(cpuKeyName,0);
            bios = object.getLong(biosKeyName,0);
            HWID hwid = HWID.gen(hdd,cpu,bios);
            hwids.add(hwid);
        }
        return hwids;
    }

    @Override
    public void unban(List<HWID> l_hwid) throws HWIDException {
        for(HWID hwid : l_hwid) {
            JsonObject request = Json.object().add(hddKeyName, hwid.getHwid_hdd()).add(cpuKeyName, hwid.getHwid_cpu()).add(biosKeyName, hwid.getHwid_bios());
            try {
                request(request,urlUnBan);
            } catch (IOException e) {
                LogHelper.error(e);
                throw new HWIDException("HWID service error");
            }
        }
    }
}
