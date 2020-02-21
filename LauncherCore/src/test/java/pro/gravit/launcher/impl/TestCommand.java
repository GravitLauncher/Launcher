package pro.gravit.launcher.impl;

import pro.gravit.utils.command.Command;

public class TestCommand extends Command {
    public boolean ok = true;

    @Override
    public String getArgsDescription() {
        return "TEST ARGS";
    }

    @Override
    public String getUsageDescription() {
        return "TEST USAGE";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 4);
        if (!args[0].equals("test1")) {
            throw new IllegalArgumentException(args[0]);
        }
        if (!args[1].equals("test 2")) {
            throw new IllegalArgumentException(args[1]);
        }
        if (!args[2].equals("test\" 3")) {
            throw new IllegalArgumentException(args[2]);
        }
        if (!args[3].equals("test\\ 4")) {
            throw new IllegalArgumentException(args[3]);
        }
        ok = true;
    }
}
