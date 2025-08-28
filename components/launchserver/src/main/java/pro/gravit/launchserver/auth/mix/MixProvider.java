package pro.gravit.launchserver.auth.mix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.utils.ProviderMap;

public abstract class MixProvider implements  AutoCloseable{
    public static final ProviderMap<MixProvider> providers = new ProviderMap<>("MixProvider");
    private static final Logger logger = LogManager.getLogger();
    private static boolean registredProviders = false;

    public static void registerProviders() {
        if (!registredProviders) {
            providers.register("uploadAsset", UploadAssetMixProvider.class);
            registredProviders = true;
        }
    }

    public abstract void init(LaunchServer server, AuthCoreProvider core);

    @SuppressWarnings("unchecked")
    public <T> T isSupport(Class<T> clazz) {
        if (clazz.isAssignableFrom(getClass())) return (T) this;
        return null;
    }

    @Override
    public abstract void close();
}
