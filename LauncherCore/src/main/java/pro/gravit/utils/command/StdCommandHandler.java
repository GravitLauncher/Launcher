package pro.gravit.utils.command;

import java.io.BufferedReader;
import java.io.IOException;

import pro.gravit.utils.helper.IOHelper;

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
    public void clear() throws IOException, InterruptedException {
        //throw new UnsupportedOperationException("clear terminal");
		String os = System.getProperty("os.name");

        if (os.contains("Windows"))
        {
            
		new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        }
        else
        {
            Runtime.getRuntime().exec("clear");
        }
    }

    @Override
    public String readLine() throws IOException {
        return reader == null ? null : reader.readLine();
    }
}
