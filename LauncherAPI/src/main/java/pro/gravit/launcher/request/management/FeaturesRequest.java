package pro.gravit.launcher.request.management;

import pro.gravit.launcher.events.request.FeaturesRequestEvent;
import pro.gravit.launcher.request.Request;

public class FeaturesRequest extends Request<FeaturesRequestEvent> {
    @Override
    public String getType() {
        return "features";
    }
}
