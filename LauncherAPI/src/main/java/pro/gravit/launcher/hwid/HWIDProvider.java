package pro.gravit.launcher.hwid;

import pro.gravit.utils.ProviderMap;

public class HWIDProvider {
    public static final ProviderMap<HWID> hwids = new ProviderMap<>();

    public static void registerHWIDs() {
        hwids.register("oshi", OshiHWID.class);
        hwids.register("no", NoHWID.class);
    }
}
