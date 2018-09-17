package ru.gravit.launcher.serialize.config;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.helper.VerifyHelper;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.BooleanConfigEntry;
import ru.gravit.launcher.serialize.config.entry.ConfigEntry;
import ru.gravit.launcher.serialize.config.entry.IntegerConfigEntry;
import ru.gravit.launcher.serialize.config.entry.ListConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;

public final class TextConfigReader {
    @LauncherAPI
    public static BlockConfigEntry read(Reader reader, boolean ro) throws IOException {
        return new TextConfigReader(reader, ro).readBlock(0);
    }
    private final LineNumberReader reader;
    private final boolean ro;
    private String skipped;

    private int ch = -1;

    private TextConfigReader(Reader reader, boolean ro) {
        this.reader = new LineNumberReader(reader);
        this.reader.setLineNumber(1);
        this.ro = ro;
    }

    private IOException newIOException(String message) {
        return new IOException(message + " (line " + reader.getLineNumber() + ')');
    }

    private int nextChar(boolean eof) throws IOException {
        ch = reader.read();
        if (eof && ch < 0)
			throw newIOException("Unexpected end of config");
        return ch;
    }

    private int nextClean(boolean eof) throws IOException {
        nextChar(eof);
        return skipWhitespace(eof);
    }

    private BlockConfigEntry readBlock(int cc) throws IOException {
        Map<String, ConfigEntry<?>> map = new LinkedHashMap<>(16);

        // Read block entries
        boolean brackets = ch == '{';
        while (nextClean(brackets) >= 0 && (!brackets || ch != '}')) {
            String preNameComment = skipped;

            // Read entry name
            String name = readToken();
            if (skipWhitespace(true) != ':')
				throw newIOException("Value start expected");
            String postNameComment = skipped;

            // Read entry value
            nextClean(true);
            String preValueComment = skipped;
            ConfigEntry<?> entry = readEntry(4);
            if (skipWhitespace(true) != ';')
				throw newIOException("Value end expected");

            // Set comments
            entry.setComment(0, preNameComment);
            entry.setComment(1, postNameComment);
            entry.setComment(2, preValueComment);
            entry.setComment(3, skipped);

            // Try add entry to map
            if (map.put(name, entry) != null)
				throw newIOException(String.format("Duplicate config entry: '%s'", name));
        }

        // Set comment after last entry and return block
        BlockConfigEntry block = new BlockConfigEntry(map, ro, cc + 1);
        block.setComment(cc, skipped);
        nextChar(false);
        return block;
    }

    private ConfigEntry<?> readEntry(int cc) throws IOException {
        // Try detect type by first char
        switch (ch) {
            case '"': // String
                return readString(cc);
            case '[': // List
                return readList(cc);
            case '{': // Block
                return readBlock(cc);
            default:
                break;
        }

        // Possibly integer value
        if (ch == '-' || ch >= '0' && ch <= '9')
			return readInteger(cc);

        // Statement?
        String statement = readToken();
        switch (statement) {
            case "true":
                return new BooleanConfigEntry(Boolean.TRUE, ro, cc);
            case "false":
                return new BooleanConfigEntry(Boolean.FALSE, ro, cc);
            default:
                throw newIOException(String.format("Unknown statement: '%s'", statement));
        }
    }

    private ConfigEntry<Integer> readInteger(int cc) throws IOException {
        return new IntegerConfigEntry(Integer.parseInt(readToken()), ro, cc);
    }

    private ConfigEntry<List<ConfigEntry<?>>> readList(int cc) throws IOException {
        List<ConfigEntry<?>> listValue = new ArrayList<>(16);

        // Read list elements
        boolean hasNextElement = nextClean(true) != ']';
        String preValueComment = skipped;
        while (hasNextElement) {
            ConfigEntry<?> element = readEntry(2);
            hasNextElement = skipWhitespace(true) != ']';
            element.setComment(0, preValueComment);
            element.setComment(1, skipped);
            listValue.add(element);

            // Prepare for next element read
            if (hasNextElement) {
                if (ch != ',')
					throw newIOException("Comma expected");
                nextClean(true);
                preValueComment = skipped;
            }
        }

        // Set in-list comment (if no elements)
        boolean additional = listValue.isEmpty();
        ConfigEntry<List<ConfigEntry<?>>> list = new ListConfigEntry(listValue, ro, additional ? cc + 1 : cc);
        if (additional)
			list.setComment(cc, skipped);

        // Return list
        nextChar(false);
        return list;
    }

    private ConfigEntry<?> readString(int cc) throws IOException {
        StringBuilder builder = new StringBuilder();

        // Read string chars
        while (nextChar(true) != '"')
			switch (ch) {
                case '\r':
                case '\n': // String termination
                    throw newIOException("String termination");
                case '\\':
                    int next = nextChar(true);
                    switch (next) {
                        case 't':
                            builder.append('\t');
                            break;
                        case 'b':
                            builder.append('\b');
                            break;
                        case 'n':
                            builder.append('\n');
                            break;
                        case 'r':
                            builder.append('\r');
                            break;
                        case 'f':
                            builder.append('\f');
                            break;
                        case '"':
                        case '\\':
                            builder.append((char) next);
                            break;
                        default:
                            throw newIOException("Illegal char escape: " + (char) next);
                    }
                    break;
                default: // Normal character
                    builder.append((char) ch);
                    break;
            }

        // Return string
        nextChar(false);
        return new StringConfigEntry(builder.toString(), ro, cc);
    }

    private String readToken() throws IOException {
        // Read token
        StringBuilder builder = new StringBuilder();
        while (VerifyHelper.isValidIDNameChar(ch)) {
            builder.append((char) ch);
            nextChar(false);
        }

        // Return token as string
        String token = builder.toString();
        if (token.isEmpty())
			throw newIOException("Not a token");
        return token;
    }

    private void skipComment(StringBuilder skippedBuilder, boolean eof) throws IOException {
        while (ch >= 0 && ch != '\r' && ch != '\n') {
            skippedBuilder.append((char) ch);
            nextChar(eof);
        }
    }

    private int skipWhitespace(boolean eof) throws IOException {
        StringBuilder skippedBuilder = new StringBuilder();
        while (Character.isWhitespace(ch) || ch == '#') {
            if (ch == '#') {
                skipComment(skippedBuilder, eof);
                continue;
            }
            skippedBuilder.append((char) ch);
            nextChar(eof);
        }
        skipped = skippedBuilder.toString();
        return ch;
    }
}
