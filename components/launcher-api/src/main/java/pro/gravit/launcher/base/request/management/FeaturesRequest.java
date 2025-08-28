package pro.gravit.launcher.base.request.management;

import pro.gravit.launcher.base.events.request.FeaturesRequestEvent;
import pro.gravit.launcher.base.request.Request;

public class FeaturesRequest extends Request<FeaturesRequestEvent> {
    @Override
    public String getType() {
        return "features";
    }
}
