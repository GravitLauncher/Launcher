package pro.gravit.launchserver.auth.protect.hwid;

import pro.gravit.launcher.HTTPRequest;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.request.secure.HardwareReportRequest;
import pro.gravit.launchserver.socket.Client;

import java.net.URL;

public class JsonHWIDProvider extends HWIDProvider {
    public URL findHardwareInfoByPublicKeyRequest;
    public URL createHardwareInfoRequest;
    public URL addPublicKeyToHardwareInfoRequest;
    public String apiKey;

    @Override
    public HardwareReportRequest.HardwareInfo findHardwareInfoByPublicKey(byte[] publicKey, Client client) throws HWIDException {
        try {
            RequestFind req = new RequestFind();
            req.publicKey = publicKey;
            req.client = client;
            req.apiKey = apiKey;
            ResultFind r = Launcher.gsonManager.gson.fromJson(HTTPRequest.jsonRequest(Launcher.gsonManager.gson.toJsonTree(req), findHardwareInfoByPublicKeyRequest), ResultFind.class);
            if (r.error != null) throw new HWIDException(r.error);
            return r.info;
        } catch (HWIDException t) {
            throw t;
        } catch (Throwable t) {
            throw new HWIDException(t);
        }
    }

    @Override
    public void createHardwareInfo(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey, Client client) throws HWIDException {
        try {
            RequestCreate req = new RequestCreate();
            req.publicKey = publicKey;
            req.client = client;
            req.hardwareInfo = hardwareInfo;
            req.apiKey = apiKey;
            ResultCreate r = Launcher.gsonManager.gson.fromJson(HTTPRequest.jsonRequest(Launcher.gsonManager.gson.toJsonTree(req), createHardwareInfoRequest), ResultCreate.class);
            if (r.error != null) throw new HWIDException(r.error);
        } catch (HWIDException t) {
            throw t;
        } catch (Throwable t) {
            throw new HWIDException(t);
        }
    }

    @Override
    public boolean addPublicKeyToHardwareInfo(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey, Client client) throws HWIDException {
        try {
            RequestAddKey req = new RequestAddKey();
            req.publicKey = publicKey;
            req.client = client;
            req.hardwareInfo = hardwareInfo;
            req.apiKey = apiKey;
            ResultAddKey r = Launcher.gsonManager.gson.fromJson(HTTPRequest.jsonRequest(Launcher.gsonManager.gson.toJsonTree(req), addPublicKeyToHardwareInfoRequest), ResultAddKey.class);
            if (r.error != null) throw new HWIDException(r.error);
            return r.success;
        } catch (HWIDException t) {
            throw t;
        } catch (Throwable t) {
            throw new HWIDException(t);
        }
    }

    public static class RequestFind {
        public byte[] publicKey;
        public Client client;
        public String apiKey;
    }

    public static class ResultFind {
        public String error;
        public HardwareReportRequest.HardwareInfo info;
    }

    public static class RequestCreate {
        public byte[] publicKey;
        public Client client;
        public HardwareReportRequest.HardwareInfo hardwareInfo;
        public String apiKey;
    }

    public static class ResultCreate {
        public String error;
    }

    public static class RequestAddKey {
        public byte[] publicKey;
        public Client client;
        public HardwareReportRequest.HardwareInfo hardwareInfo;
        public String apiKey;
    }

    public static class ResultAddKey {
        public String error;
        public boolean success;
    }
}
