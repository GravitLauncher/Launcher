package ru.gravit.launchserver.auth.hwid;

import ru.gravit.launcher.HWID;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;

import java.util.ArrayList;
import java.util.List;

public class AcceptHWIDHandler extends HWIDHandler {

    public AcceptHWIDHandler(BlockConfigEntry block) {
        super(block);
    }

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
    public List<HWID> getHwid(String username) {
        return new ArrayList<>();
    }

    @Override
    public void unban(List<HWID> hwid) {
        //SKIP
    }

}
