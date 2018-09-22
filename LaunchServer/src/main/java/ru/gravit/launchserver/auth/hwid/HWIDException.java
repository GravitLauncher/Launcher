package ru.gravit.launchserver.auth.hwid;

public class HWIDException extends Exception {
    /**
     *
     */
    private static final long serialVersionUID = -5307315891121889972L;

    public HWIDException() {
    }

    public HWIDException(String s) {
        super(s);
    }

    public HWIDException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public HWIDException(Throwable throwable) {
        super(throwable);
    }
}
