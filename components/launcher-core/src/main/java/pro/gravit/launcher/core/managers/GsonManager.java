package pro.gravit.launcher.core.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import pro.gravit.launcher.core.hasher.HashedEntry;
import pro.gravit.launcher.core.hasher.HashedEntryAdapter;
import pro.gravit.utils.helper.CommonHelper;

public class GsonManager {
    public volatile GsonBuilder gsonBuilder;
    public volatile Gson gson;
    public volatile GsonBuilder configGsonBuilder;
    public volatile Gson configGson;

    public void initGson() {
        gsonBuilder = CommonHelper.newBuilder();
        configGsonBuilder = CommonHelper.newBuilder();
        configGsonBuilder.setPrettyPrinting().disableHtmlEscaping();
        registerAdapters(gsonBuilder);
        registerAdapters(configGsonBuilder);
        preConfigGson(configGsonBuilder);
        preGson(gsonBuilder);
        gson = gsonBuilder.create();
        configGson = configGsonBuilder.create();
    }

    public void registerAdapters(GsonBuilder builder) {
        builder.registerTypeAdapter(HashedEntry.class, new HashedEntryAdapter());
    }

    public void preConfigGson(GsonBuilder gsonBuilder) {
        //skip
    }

    public void preGson(GsonBuilder gsonBuilder) {
        //skip
    }
}
