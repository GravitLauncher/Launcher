package pro.gravit.launcher.core.api.features;

public interface RestoreTempTokensFeatureAPI extends FeatureAPI {
    Object collectTempTokens();
    Class<?> getTempTokensObjectClass();
    void setTempTokens(Object o);
}
