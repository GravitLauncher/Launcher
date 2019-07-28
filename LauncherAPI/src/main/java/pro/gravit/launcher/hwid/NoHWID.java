package pro.gravit.launcher.hwid;

public class NoHWID implements HWID {
    @Override
    public String getSerializeString() {
        return "";
    }

    @Override
    public int getLevel() {
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
}
