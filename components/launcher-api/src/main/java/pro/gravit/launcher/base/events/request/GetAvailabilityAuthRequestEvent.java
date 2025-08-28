package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.base.request.auth.details.AuthLoginOnlyDetails;
import pro.gravit.launcher.base.request.auth.details.AuthWebViewDetails;
import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.base.events.RequestEvent;
import pro.gravit.launcher.core.api.method.AuthMethod;
import pro.gravit.launcher.core.api.method.AuthMethodDetails;
import pro.gravit.launcher.core.api.method.details.AuthPasswordDetails;
import pro.gravit.launcher.core.api.method.details.AuthWebDetails;
import pro.gravit.utils.TypeSerializeInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GetAvailabilityAuthRequestEvent extends RequestEvent {
    @LauncherNetworkAPI
    public final List<AuthAvailability> list;
    @LauncherNetworkAPI
    public final long features;

    public GetAvailabilityAuthRequestEvent(List<AuthAvailability> list) {
        this.list = list;
        this.features = ServerFeature.FEATURE_SUPPORT.val;
    }

    public GetAvailabilityAuthRequestEvent(List<AuthAvailability> list, long features) {
        this.list = list;
        this.features = features;
    }

    @Override
    public String getType() {
        return "getAvailabilityAuth";
    }

    public enum ServerFeature {
        FEATURE_SUPPORT(1);
        public final int val;

        ServerFeature(int val) {
            this.val = val;
        }
    }

    public interface AuthAvailabilityDetails extends AuthMethodDetails, TypeSerializeInterface {
        AuthMethodDetails toAuthMethodDetails();
    }

    public static class AuthAvailability implements AuthMethod {
        public final List<AuthAvailabilityDetails> details;
        @LauncherNetworkAPI
        public String name;
        @LauncherNetworkAPI
        public String displayName;

        @LauncherNetworkAPI
        public boolean visible;
        @LauncherNetworkAPI
        public Set<String> features;

        public AuthAvailability(List<AuthAvailabilityDetails> details, String name, String displayName, boolean visible, Set<String> features) {
            this.details = details;
            this.name = name;
            this.displayName = displayName;
            this.visible = visible;
            this.features = features;
        }

        @Override
        public List<AuthMethodDetails> getDetails() {
            List<AuthMethodDetails> convert = new ArrayList<>();
            for(var e : details) {
                convert.add(e.toAuthMethodDetails());
            }
            return convert;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public Set<String> getFeatures() {
            return features;
        }
    }
}
