package ru.gravit.launcher.serialize.config;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.serialize.config.entry.*;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class TextConfigWriter {
    @LauncherAPI
    public static void write(BlockConfigEntry block, Writer writer, boolean comments) throws IOException {
        new TextConfigWriter(writer, comments).writeBlock(block, false);
    }

    private final Writer writer;

    private final boolean comments;

    private TextConfigWriter(Writer writer, boolean comments) {
        this.writer = writer;
        this.comments = comments;
    }

    private void writeBlock(BlockConfigEntry block, boolean brackets) throws IOException {
        // Write start bracket
        if (brackets)
            writer.write('{');

        // Write block entries
        Map<String, ConfigEntry<?>> map = block.getValue();
        for (Entry<String, ConfigEntry<?>> mapEntry : map.entrySet()) {
            String name = mapEntry.getKey();
            ConfigEntry<?> entry = mapEntry.getValue();

            // Write entry name
            writeComment(entry.getComment(0));
            writer.write(name);
            writeComment(entry.getComment(1));
            writer.write(':');

            // Write entry value
            writeComment(entry.getComment(2));
            writeEntry(entry);
            writeComment(entry.getComment(3));
            writer.write(';');
        }
        writeComment(block.getComment(-1));

        // Write end bracket
        if (brackets)
            writer.write('}');
    }

    private void writeBoolean(BooleanConfigEntry entry) throws IOException {
        writer.write(entry.getValue().toString());
    }

    private void writeComment(String comment) throws IOException {
        if (comments && comment != null)
            writer.write(comment);
    }

    private void writeEntry(ConfigEntry<?> entry) throws IOException {
        ConfigEntry.Type type = entry.getType();
        switch (type) {
            case BLOCK:
                writeBlock((BlockConfigEntry) entry, true);
                break;
            case STRING:
                writeString((StringConfigEntry) entry);
                break;
            case INTEGER:
                writeInteger((IntegerConfigEntry) entry);
                break;
            case BOOLEAN:
                writeBoolean((BooleanConfigEntry) entry);
                break;
            case LIST:
                writeList((ListConfigEntry) entry);
                break;
            default:
                throw new AssertionError("Unsupported config entry type: " + type.name());
        }
    }

    private void writeInteger(IntegerConfigEntry entry) throws IOException {
        writer.write(Integer.toString(entry.getValue()));
    }

    private void writeList(ListConfigEntry entry) throws IOException {
        writer.write('[');

        // Write list elements
        List<ConfigEntry<?>> value = entry.getValue();
        for (int i = 0; i < value.size(); i++) {
            if (i > 0)
                writer.write(',');

            // Write element
            ConfigEntry<?> element = value.get(i);
            writeComment(element.getComment(0));
            writeEntry(element);
            writeComment(element.getComment(1));
        }
        writeComment(entry.getComment(-1));

        // Write end bracket
        writer.write(']');
    }

    private void writeString(StringConfigEntry entry) throws IOException {
        writer.write('"');

        // Quote string
        String s = entry.getValue();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\t':
                    writer.write("\\t");
                    break;
                case '\b':
                    writer.write("\\b");
                    break;
                case '\n':
                    writer.write("\\n");
                    break;
                case '\r':
                    writer.write("\\r");
                    break;
                case '\f':
                    writer.write("\\f");
                    break;
                case '"':
                case '\\':
                    writer.write('\\');
                    writer.write(ch);
                    break;
                default:
                    writer.write(ch);
                    break;
            }
        }

        // Write end quote
        writer.write('"');
    }
}
