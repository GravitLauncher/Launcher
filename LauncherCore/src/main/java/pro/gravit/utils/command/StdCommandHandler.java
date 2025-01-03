package pro.gravit.utils.command;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class StdCommandHandler extends CommandHandler {
    private final BufferedReader reader;

    public StdCommandHandler(boolean readCommands) {
        super();
        reader = readCommands ? IOHelper.newReader(System.in) : null;
    }

    public StdCommandHandler(InputStream stream) {
        super();
        this.reader = IOHelper.newReader(stream);
    }

    public StdCommandHandler(BufferedReader reader) {
        super();
        this.reader = reader;
    }

    protected StdCommandHandler(List<Category> categories, CommandCategory baseCategory, boolean readCommands) {
        super(categories, baseCategory);
        this.reader = readCommands ? IOHelper.newReader(System.in) : null;
    }

    protected StdCommandHandler(List<Category> categories, CommandCategory baseCategory, InputStream stream) {
        super(categories, baseCategory);
        this.reader = IOHelper.newReader(stream);
    }

    protected StdCommandHandler(List<Category> categories, CommandCategory baseCategory, BufferedReader reader) {
        super(categories, baseCategory);
        this.reader = reader;
    }

    @Override
    public void bell() {
    }

    @Override
    public void clear() throws IOException {
        System.out.flush();
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
            try {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } catch (InterruptedException ex) {
                throw new IOException(ex);
            }
        } else {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        }
    }

    @Override
    public String readLine() throws IOException {
        return reader == null ? null : reader.readLine();
    }

    public StdCommandHandler ofHandler(CommandHandler commandHandler, boolean readCommands) {
        return new StdCommandHandler(commandHandler.categories, commandHandler.baseCategory, readCommands);
    }

    public StdCommandHandler ofHandler(CommandHandler commandHandler, InputStream stream) {
        return new StdCommandHandler(commandHandler.categories, commandHandler.baseCategory, stream);
    }

    public StdCommandHandler ofHandler(CommandHandler commandHandler, BufferedReader reader) {
        return new StdCommandHandler(commandHandler.categories, commandHandler.baseCategory, reader);
    }
}
