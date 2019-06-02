package pro.gravit.launchserver.auth.hwid;

import pro.gravit.launcher.HWID;

import java.util.ArrayList;
import java.util.List;

public class AcceptHWIDHandler extends HWIDHandler {

    @Override
    public void ban(List<HWID> hwid) {
        //SKIP
    }

    @Override
    public void check0(HWID hwid, String username) {
        //SKIP
    }

    @Override
    public void close() {
        //SKIP
    }

    @Override
    public void init() {

    }

    @Override
    public List<HWID> getHwid(String username) {
        return new ArrayList<>();
    }

    @Override
    public void unban(List<HWID> hwid) {
        //SKIP
    }

}
