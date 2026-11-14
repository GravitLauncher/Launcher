package pro.gravit.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pro.gravit.launcher.impl.Test2Command;
import pro.gravit.launcher.impl.TestCommand;
import pro.gravit.utils.command.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandHandlerTest {
    public static CommandHandler commandHandler;

    @BeforeAll
    public static void prepare() {
        commandHandler = new StdCommandHandler(false);
        commandHandler.registerCommand("test", new TestCommand());
    }

    @Test
    public void stopCommandDoesNotReadAnotherLine() {
        AtomicInteger reads = new AtomicInteger();
        var handler = new StdCommandHandler(new BufferedReader(new StringReader("stop\nunexpected\n"))) {
            @Override
            public String readLine() throws IOException {
                reads.incrementAndGet();
                return super.readLine();
            }
        };
        handler.registerCommand("stop", new Command() {
            @Override
            public String getArgsDescription() { return ""; }

            @Override
            public String getUsageDescription() { return "Stop the console"; }

            @Override
            public void invoke(String... args) { handler.requestStop(); }
        });

        handler.run();
        handler.run();

        Assertions.assertEquals(1, reads.get());
    }

    @Test
    public void baseTest() throws Exception {
        commandHandler.evalNative("test test1 \"test 2\" \"test\\\" 3\" \"test\\\\ 4\"", false);
        Assertions.assertTrue(((TestCommand) commandHandler.findCommand("test")).ok);
    }

    @Test
    public void failNumberTest() throws Exception {
        Command cmd = commandHandler.findCommand("test");
        try {
            cmd.invoke("test1");
            Assertions.fail("CommandException not throw");
        } catch (CommandException ignored) {

        }
    }

    @Test
    public void categoryTest() throws Exception {
        BaseCommandCategory category = new BaseCommandCategory();
        category.registerCommand("test2", new Test2Command());
        CommandHandler.Category category1 = new CommandHandler.Category(category, "testCat");
        commandHandler.registerCategory(category1);
        commandHandler.evalNative("test2", false);
        commandHandler.unregisterCategory(category1);
        try {
            commandHandler.evalNative("test2", false);
            Assertions.fail("CommandException not throw");
        } catch (CommandException ignored) {

        }
    }
}
