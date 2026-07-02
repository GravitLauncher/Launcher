package pro.gravit.utils.command;

public final class CommandException extends Exception {


    public CommandException(String message) {
        super(message, null, false, false);
    }


    public CommandException(Throwable exc) {
        super(exc);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
