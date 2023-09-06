package pro.gravit.launchserver.auth.mix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.utils.ProviderMap;

public class MixProvider {
    public static final ProviderMap<MixProvider> providers = new ProviderMap<>("MixProvider");
    private static final Logger logger = LogManager.getLogger();
    private static boolean registredProviders = false;

    public static void registerProviders() {
        if (!registredProviders) {
            registredProviders = true;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T isSupport(Class<T> clazz) {
        if (clazz.isAssignableFrom(getClass())) return (T) this;
        return null;
    }
}
