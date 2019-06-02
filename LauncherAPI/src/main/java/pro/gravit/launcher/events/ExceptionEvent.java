package pro.gravit.launcher.events;

public class ExceptionEvent extends RequestEvent {
    public ExceptionEvent(Exception e) {
        this.message = e.getMessage();
        this.clazz = e.getClass().getName();
    }

    public final String message;
    public final String clazz;

    @Override
    public String getType() {
        return "exception";
    }
}
