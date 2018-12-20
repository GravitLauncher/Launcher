package ru.gravit.launcher.hasher;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.stream.EnumSerializer;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Map.Entry;

public final class HashedDir extends HashedEntry {
    public static final class Diff {
        @LauncherAPI
        public final HashedDir mismatch;
        @LauncherAPI
        public final HashedDir extra;

        private Diff(HashedDir mismatch, HashedDir extra) {
            this.mismatch = mismatch;
            this.extra = extra;
        }

        @LauncherAPI
        public boolean isSame() {
            return mismatch.isEmpty() && extra.isEmpty();
        }
    }

    private final class HashFileVisitor extends SimpleFileVisitor<Path> {
        private final Path dir;
        private final FileNameMatcher matcher;
        private final boolean allowSymlinks;
        private final boolean digest;

        // State
        private HashedDir current = HashedDir.this;
        private final Deque<String> path = new LinkedList<>();
        private final Deque<HashedDir> stack = new LinkedList<>();

        private HashFileVisitor(Path dir, FileNameMatcher matcher, boolean allowSymlinks, boolean digest) {
            this.dir = dir;
            this.matcher = matcher;
            this.allowSymlinks = allowSymlinks;
            this.digest = digest;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            FileVisitResult result = super.postVisitDirectory(dir, exc);
            if (this.dir.equals(dir))
                return result;

            // Add directory to parent
            HashedDir parent = stack.removeLast();
            parent.map.put(path.removeLast(), current);
            current = parent;

            // We're done
            return result;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            FileVisitResult result = super.preVisitDirectory(dir, attrs);
            if (this.dir.equals(dir))
                return result;

            // Verify is not symlink
            // Symlinks was disallowed because modification of it's destination are ignored by DirWatcher
            if (!allowSymlinks && attrs.isSymbolicLink())
                throw new SecurityException("Symlinks are not allowed");

            // Add child
            stack.add(current);
            current = new HashedDir();
            path.add(IOHelper.getFileName(dir));

            // We're done
            return result;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            // Verify is not symlink
            if (!allowSymlinks && attrs.isSymbolicLink())
                throw new SecurityException("Symlinks are not allowed");

            // Add file (may be unhashed, if exclusion)
            path.add(IOHelper.getFileName(file));
            boolean doDigest = digest && (matcher == null || matcher.shouldUpdate(path));
            current.map.put(path.removeLast(), new HashedFile(file, attrs.size(), doDigest));
            return super.visitFile(file, attrs);
        }
    }

    private final Map<String, HashedEntry> map = new HashMap<>(32);

    @LauncherAPI
    public HashedDir() {
    }

    @LauncherAPI
    public HashedDir(HInput input) throws IOException {
        int entriesCount = input.readLength(0);
        for (int i = 0; i < entriesCount; i++) {
            String name = IOHelper.verifyFileName(input.readString(255));

            // Read entry
            HashedEntry entry;
            Type type = Type.read(input);
            switch (type) {
                case FILE:
                    entry = new HashedFile(input);
                    break;
                case DIR:
                    entry = new HashedDir(input);
                    break;
                default:
                    throw new AssertionError("Unsupported hashed entry type: " + type.name());
            }

            // Try add entry to map
            VerifyHelper.putIfAbsent(map, name, entry, String.format("Duplicate dir entry: '%s'", name));
        }
    }

    @LauncherAPI
    public HashedDir(Path dir, FileNameMatcher matcher, boolean allowSymlinks, boolean digest) throws IOException {
        IOHelper.walk(dir, new HashFileVisitor(dir, matcher, allowSymlinks, digest), true);
    }

    @LauncherAPI
    public Diff diff(HashedDir other, FileNameMatcher matcher) {
        HashedDir mismatch = sideDiff(other, matcher, new LinkedList<>(), true);
        HashedDir extra = other.sideDiff(this, matcher, new LinkedList<>(), false);
        return new Diff(mismatch, extra);
    }

    @LauncherAPI
    public Diff compare(HashedDir other, FileNameMatcher matcher) {
        HashedDir mismatch = sideDiff(other, matcher, new LinkedList<>(), true);
        HashedDir extra = other.sideDiff(this, matcher, new LinkedList<>(), false);
        return new Diff(mismatch, extra);
    }

    public void remove(String name) {
        map.remove(name);
    }

    public void removeR(String name) {
        LinkedList<String> dirs = new LinkedList<>();
        StringTokenizer t = new StringTokenizer(name, "/");
        while (t.hasMoreTokens()) {
            dirs.add(t.nextToken());
        }
        Map<String, HashedEntry> current = map;
        for (String s : dirs) {
            HashedEntry e = current.get(s);
            if (e == null) {
                LogHelper.debug("Null %s", s);
                for (String x : current.keySet()) LogHelper.debug("Contains %s", x);
                break;
            }
            if (e.getType() == Type.DIR) {
                current = ((HashedDir) e).map;
                LogHelper.debug("Found dir %s", s);
            } else {
                current.remove(s);
                LogHelper.debug("Found filename %s", s);
                break;
            }
        }
    }

    @LauncherAPI
    public HashedEntry getEntry(String name) {
        return map.get(name);
    }

    @Override
    public Type getType() {
        return Type.DIR;
    }

    @LauncherAPI
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @LauncherAPI
    public Map<String, HashedEntry> map() {
        return Collections.unmodifiableMap(map);
    }

    @LauncherAPI
    public HashedEntry resolve(Iterable<String> path) {
        HashedEntry current = this;
        for (String pathEntry : path) {
            if (current instanceof HashedDir) {
                current = ((HashedDir) current).map.get(pathEntry);
                continue;
            }
            return null;
        }
        return current;
    }

    private HashedDir sideDiff(HashedDir other, FileNameMatcher matcher, Deque<String> path, boolean mismatchList) {
        HashedDir diff = new HashedDir();
        for (Entry<String, HashedEntry> mapEntry : map.entrySet()) {
            String name = mapEntry.getKey();
            HashedEntry entry = mapEntry.getValue();
            path.add(name);

            // Should update?
            boolean shouldUpdate = matcher == null || matcher.shouldUpdate(path);

            // Not found or of different type
            Type type = entry.getType();
            HashedEntry otherEntry = other.map.get(name);
            if (otherEntry == null || otherEntry.getType() != type) {
                if (shouldUpdate || mismatchList && otherEntry == null) {
                    diff.map.put(name, entry);

                    // Should be deleted!
                    if (!mismatchList)
                        entry.flag = true;
                }
                path.removeLast();
                continue;
            }

            // Compare entries based on type
            switch (type) {
                case FILE:
                    HashedFile file = (HashedFile) entry;
                    HashedFile otherFile = (HashedFile) otherEntry;
                    if (mismatchList && shouldUpdate && !file.isSame(otherFile))
                        diff.map.put(name, entry);
                    break;
                case DIR:
                    HashedDir dir = (HashedDir) entry;
                    HashedDir otherDir = (HashedDir) otherEntry;
                    if (mismatchList || shouldUpdate) { // Maybe isn't need to go deeper?
                        HashedDir mismatch = dir.sideDiff(otherDir, matcher, path, mismatchList);
                        if (!mismatch.isEmpty())
                            diff.map.put(name, mismatch);
                    }
                    break;
                default:
                    throw new AssertionError("Unsupported hashed entry type: " + type.name());
            }

            // Remove this path entry
            path.removeLast();
        }
        return diff;
    }

    public HashedDir sideCompare(HashedDir other, FileNameMatcher matcher, Deque<String> path, boolean mismatchList) {
        HashedDir diff = new HashedDir();
        for (Entry<String, HashedEntry> mapEntry : map.entrySet()) {
            String name = mapEntry.getKey();
            HashedEntry entry = mapEntry.getValue();
            path.add(name);

            // Should update?
            boolean shouldUpdate = matcher == null || matcher.shouldUpdate(path);

            // Not found or of different type
            Type type = entry.getType();
            HashedEntry otherEntry = other.map.get(name);
            if (otherEntry == null || otherEntry.getType() != type) {
                if (shouldUpdate || mismatchList && otherEntry == null) {
                    diff.map.put(name, entry);

                    // Should be deleted!
                    if (!mismatchList)
                        entry.flag = true;
                }
                path.removeLast();
                continue;
            }

            // Compare entries based on type
            switch (type) {
                case FILE:
                    HashedFile file = (HashedFile) entry;
                    HashedFile otherFile = (HashedFile) otherEntry;
                    if (mismatchList && shouldUpdate && file.isSame(otherFile))
                        diff.map.put(name, entry);
                    break;
                case DIR:
                    HashedDir dir = (HashedDir) entry;
                    HashedDir otherDir = (HashedDir) otherEntry;
                    if (mismatchList || shouldUpdate) { // Maybe isn't need to go deeper?
                        HashedDir mismatch = dir.sideCompare(otherDir, matcher, path, mismatchList);
                        if (!mismatch.isEmpty())
                            diff.map.put(name, mismatch);
                    }
                    break;
                default:
                    throw new AssertionError("Unsupported hashed entry type: " + type.name());
            }

            // Remove this path entry
            path.removeLast();
        }
        return diff;
    }

    @Override
    public long size() {
        return map.values().stream().mapToLong(HashedEntry::size).sum();
    }

    @Override
    public void write(HOutput output) throws IOException {
        Set<Entry<String, HashedEntry>> entries = map.entrySet();
        output.writeLength(entries.size(), 0);
        for (Entry<String, HashedEntry> mapEntry : entries) {
            output.writeString(mapEntry.getKey(), 255);

            // Write hashed entry
            HashedEntry entry = mapEntry.getValue();
            EnumSerializer.write(output, entry.getType());
            entry.write(output);
        }
    }
}
