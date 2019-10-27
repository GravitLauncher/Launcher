package pro.gravit.utils.command;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;

import java.io.BufferedReader;
import java.io.IOException;

public class StdCommandHandler extends CommandHandler {
    private final BufferedReader reader;

    public StdCommandHandler(boolean readCommands) {
        super();
        reader = readCommands ? IOHelper.newReader(System.in) : null;
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
}
