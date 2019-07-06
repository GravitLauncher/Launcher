package pro.gravit.launcher.events;

//Используется, что бы послать короткое сообщение, которое вмещается в int
public class SignalEvent {
    public int signal;

    public SignalEvent(int signal) {
        this.signal = signal;
    }
}
