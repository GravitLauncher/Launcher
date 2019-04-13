package ru.gravit.utils.command;

import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.*;

public abstract class CommandHandler implements Runnable {
    private final List<Category> categories = new ArrayList<>();
    private final CommandCategory baseCategory = new BaseCommandCategory();

    public static class Category
    {
        public CommandCategory category;
        public String name;
        public String description;

        public Category(CommandCategory category, String name) {
            this.category = category;
            this.name = name;
        }

        public Category(CommandCategory category, String name, String description) {
            this.category = category;
            this.name = name;
            this.description = description;
        }
    }

    public void eval(String line, boolean bell) {
        LogHelper.info("Command '%s'", line);

        // Parse line to tokens
        String[] args;
        try {
            args = CommonHelper.parseCommand(line);
            if (args.length > 0) args[0] = args[0].toLowerCase();
        } catch (Exception e) {
            LogHelper.error(e);
            return;
        }

        // Evaluate command
        eval(args, bell);
    }


    public void eval(String[] args, boolean bell) {
        if (args.length == 0)
            return;

        // Measure start time and invoke command
        long startTime = System.currentTimeMillis();
        try {
            lookup(args[0]).invoke(Arrays.copyOfRange(args, 1, args.length));
        } catch (Exception e) {
            LogHelper.error(e);
        }

        // Bell if invocation took > 1s
        long endTime = System.currentTimeMillis();
        if (bell && endTime - startTime >= 5000)
            try {
                bell();
            } catch (IOException e) {
                LogHelper.error(e);
            }
    }


    public Command lookup(String name) throws CommandException {
        Command command = findCommand(name);
        if (command == null)
            throw new CommandException(String.format("Unknown command: '%s'", name));
        return command;
    }
    public Command findCommand(String name)
    {
        Command cmd = baseCategory.findCommand(name);
        if(cmd == null)
        {
            for(Category entry : categories)
            {
                cmd = entry.category.findCommand(name);
                if(cmd != null) return cmd;
            }
        }
        return cmd;
    }


    public abstract String readLine() throws IOException;

    private void readLoop() throws IOException {
        for (String line = readLine(); line != null; line = readLine())
            eval(line, true);
    }


    public void registerCommand(String name, Command command) {
        baseCategory.registerCommand(name, command);
    }

    public void registerCategory(Category category)
    {
        categories.add(category);
    }
    public boolean unregisterCategory(Category category)
    {
        return categories.remove(category);
    }
    public Category findCategory(String name)
    {
        for(Category category : categories) if(category.name.equals(name)) return category;
        return null;
    }

    public Command unregisterCommand(String name) {
        return baseCategory.unregisterCommand(name);
    }

    @Override
    public void run() {
        try {
            readLoop();
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    public CommandCategory getBaseCategory() {
        return baseCategory;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public abstract void bell() throws IOException;


    public abstract void clear() throws IOException;
}
