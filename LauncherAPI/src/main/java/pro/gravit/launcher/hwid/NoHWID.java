package pro.gravit.launcher.hwid;

public class NoHWID implements HWID {

    @Override
    public int getLevel() {
        return 0;
    }

    @Override
    public int getAntiLevel() {
        return 0;
    }

    @Override
    public int compare(HWID hwid) {
        return 0;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public void normalize() {
        //Skip
    }
}
