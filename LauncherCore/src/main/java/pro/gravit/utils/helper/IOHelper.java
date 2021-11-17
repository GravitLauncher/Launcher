package pro.gravit.utils.helper;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class IOHelper {
    public static final long MB32 = 1 << 25;
    public static final Charset UNICODE_CHARSET = StandardCharsets.UTF_8;
    public static final Charset ASCII_CHARSET = StandardCharsets.US_ASCII;
    public static final int MAX_BATCH_SIZE = 128;
    // Charset
    public static final int SOCKET_TIMEOUT = VerifyHelper.verifyInt(
            Integer.parseUnsignedInt(System.getProperty("launcher.socketTimeout", Integer.toString(30000))),
            VerifyHelper.POSITIVE, "launcher.socketTimeout can't be <= 0");
    public static final int HTTP_TIMEOUT = VerifyHelper.verifyInt(
            Integer.parseUnsignedInt(System.getProperty("launcher.httpTimeout", Integer.toString(5000))),
            VerifyHelper.POSITIVE, "launcher.httpTimeout can't be <= 0");
    // Constants
    public static final int BUFFER_SIZE = VerifyHelper.verifyInt(
            Integer.parseUnsignedInt(System.getProperty("launcher.bufferSize", Integer.toString(4096))),
            VerifyHelper.POSITIVE, "launcher.bufferSize can't be <= 0");
    public static final String CROSS_SEPARATOR = "/";
    public static final FileSystem FS = FileSystems.getDefault();
    // Platform-dependent
    public static final String PLATFORM_SEPARATOR = FS.getSeparator();
    public static final boolean POSIX = FS.supportedFileAttributeViews().contains("posix") || FS.supportedFileAttributeViews().contains("Posix");
    public static final Path JVM_DIR = Paths.get(System.getProperty("java.home"));
    public static final Path HOME_DIR = Paths.get(System.getProperty("user.home"));
    public static final Path WORKING_DIR = Paths.get(System.getProperty("user.dir"));
    public static final String USER_AGENT = System.getProperty("launcher.userAgentDefault", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
    // Open options - as arrays
    private static final OpenOption[] READ_OPTIONS = {StandardOpenOption.READ};
    private static final OpenOption[] WRITE_OPTIONS = {StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING};
    private static final OpenOption[] APPEND_OPTIONS = {StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND};
    // Other options
    private static final LinkOption[] LINK_OPTIONS = {};
    private static final CopyOption[] COPY_OPTIONS = {StandardCopyOption.REPLACE_EXISTING};
    private static final Set<FileVisitOption> WALK_OPTIONS = Collections.singleton(FileVisitOption.FOLLOW_LINKS);
    // Other constants
    private static final Pattern CROSS_SEPARATOR_PATTERN = Pattern.compile(CROSS_SEPARATOR, Pattern.LITERAL);
    private static final Pattern PLATFORM_SEPARATOR_PATTERN = Pattern.compile(PLATFORM_SEPARATOR, Pattern.LITERAL);

    private IOHelper() {
    }

    public static void close(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception exc) {
            LogHelper.error(exc);
        }
    }

    public static void close(InputStream in) {
        try {
            in.close();
        } catch (Exception ignored) {
        }
    }

    public static void close(OutputStream out) {
        try {
            out.flush();
            out.close();
        } catch (Exception ignored) {
        }
    }

    public static URL convertToURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
    }

    public static void copy(Path source, Path target) throws IOException {
        createParentDirs(target);
        Files.copy(source, target, COPY_OPTIONS);
    }

    public static void createParentDirs(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !isDir(parent))
            Files.createDirectories(parent);
    }

    public static String decode(byte[] bytes) {
        return new String(bytes, UNICODE_CHARSET);
    }

    public static String decodeASCII(byte[] bytes) {
        return new String(bytes, ASCII_CHARSET);
    }

    public static void deleteDir(Path dir, boolean self) throws IOException {
        walk(dir, new DeleteDirVisitor(dir, self), true);
    }

    public static byte[] encode(String s) {
        return s.getBytes(UNICODE_CHARSET);
    }

    public static byte[] encodeASCII(String s) {
        return s.getBytes(ASCII_CHARSET);
    }

    public static boolean exists(Path path) {
        return Files.exists(path, LINK_OPTIONS);
    }

    public static Path getCodeSource(Class<?> clazz) {
        return Paths.get(toURI(clazz.getProtectionDomain().getCodeSource().getLocation()));
    }

    public static String getFileName(Path path) {
        return path.getFileName().toString();
    }

    public static String getIP(SocketAddress address) {
        return ((InetSocketAddress) address).getAddress().getHostAddress();
    }

    public static Path getRoot() {
        switch (JVMHelper.OS_TYPE) {
            case MUSTDIE: {
                String drive = System.getenv("SystemDrive").concat("\\");
                return Paths.get(drive);
            }
            case LINUX:
            case MACOSX: {
                return Paths.get("/");
            }
        }
        throw new UnsupportedOperationException();
    }

    public static byte[] getResourceBytes(String name) throws IOException {
        return read(getResourceURL(name));
    }

    public static URL getResourceURL(String name) throws NoSuchFileException {
        URL url = IOHelper.class.getResource('/' + name);
        if (url == null)
            throw new NoSuchFileException(name);
        return url;
    }

    public static boolean hasExtension(Path file, String extension) {
        return getFileName(file).endsWith('.' + extension);
    }

    public static boolean isDir(Path path) {
        return Files.isDirectory(path, LINK_OPTIONS);
    }

    public static boolean isEmpty(Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            return !stream.iterator().hasNext();
        }
    }

    public static boolean isFile(Path path) {
        return Files.isRegularFile(path, LINK_OPTIONS);
    }

    public static boolean isValidFileName(String fileName) {
        return !fileName.equals(".") && !fileName.equals("..") &&
                fileName.chars().noneMatch(ch -> ch == '/' || ch == '\\') && isValidPath(fileName);
    }

    public static boolean isValidPath(String path) {
        try {
            toPath(path);
            return true;
        } catch (InvalidPathException ignored) {
            return false;
        }
    }

    public static boolean isValidTextureBounds(int width, int height, boolean cloak) {
        return width % 64 == 0 && (height << 1 == width || !cloak && height == width) && width <= 1024 ||
                cloak && width % 22 == 0 && height % 17 == 0 && width / 22 == height / 17;
    }

    public static void move(Path source, Path target) throws IOException {
        IOHelper.walk(source, new MoveFileVisitor(source, target), true);
    }

    public static byte[] newBuffer() {
        return new byte[BUFFER_SIZE];
    }

    public static ByteArrayOutputStream newByteArrayOutput() {
        return new ByteArrayOutputStream();
    }

    public static char[] newCharBuffer() {
        return new char[BUFFER_SIZE];
    }

    public static URLConnection newConnection(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpURLConnection) {
            connection.setReadTimeout(HTTP_TIMEOUT);
            connection.setConnectTimeout(HTTP_TIMEOUT);
            connection.addRequestProperty("User-Agent", USER_AGENT); // Fix for stupid servers
        } else
            connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(false);
        return connection;
    }

    public static HttpURLConnection newConnectionPost(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) newConnection(url);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        return connection;
    }

    public static Deflater newDeflater() {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        deflater.setStrategy(Deflater.DEFAULT_STRATEGY);
        return deflater;
    }

    public static Inflater newInflater() {
        return new Inflater(true);
    }

    public static InputStream newInput(Path file) throws IOException {
        return Files.newInputStream(file, READ_OPTIONS);
    }

    public static InputStream newBufferedInput(Path file) throws IOException {
        return new BufferedInputStream(Files.newInputStream(file, READ_OPTIONS));
    }

    public static InputStream newInput(URL url) throws IOException {
        return newConnection(url).getInputStream();
    }

    public static BufferedInputStream newBufferedInput(URL url) throws IOException {
        return new BufferedInputStream(newConnection(url).getInputStream());
    }

    public static OutputStream newOutput(Path file) throws IOException {
        return newOutput(file, false);
    }

    public static OutputStream newBufferedOutput(Path file) throws IOException {
        return newBufferedOutput(file, false);
    }

    public static OutputStream newOutput(Path file, boolean append) throws IOException {
        createParentDirs(file);
        return Files.newOutputStream(file, append ? APPEND_OPTIONS : WRITE_OPTIONS);
    }

    public static OutputStream newBufferedOutput(Path file, boolean append) throws IOException {
        createParentDirs(file);
        return new BufferedOutputStream(Files.newOutputStream(file, append ? APPEND_OPTIONS : WRITE_OPTIONS));
    }

    public static BufferedReader newReader(InputStream input) {
        return newReader(input, UNICODE_CHARSET);
    }

    public static BufferedReader newReader(InputStream input, Charset charset) {
        return new BufferedReader(new InputStreamReader(input, charset));
    }

    public static BufferedReader newReader(Path file) throws IOException {
        return Files.newBufferedReader(file, UNICODE_CHARSET);
    }

    public static BufferedReader newReader(URL url) throws IOException {
        URLConnection connection = newConnection(url);
        String charset = connection.getContentEncoding();
        return newReader(connection.getInputStream(), charset == null ? UNICODE_CHARSET : Charset.forName(charset));
    }

    public static Socket newSocket() throws SocketException {
        Socket socket = new Socket();
        setSocketFlags(socket);
        return socket;
    }

    public static BufferedWriter newWriter(FileDescriptor fd) {
        return newWriter(new FileOutputStream(fd));
    }

    public static BufferedWriter newWriter(OutputStream output) {
        return new BufferedWriter(new OutputStreamWriter(output, UNICODE_CHARSET));
    }

    public static BufferedWriter newWriter(Path file) throws IOException {
        return newWriter(file, false);
    }

    public static BufferedWriter newWriter(Path file, boolean append) throws IOException {
        createParentDirs(file);
        return Files.newBufferedWriter(file, UNICODE_CHARSET, append ? APPEND_OPTIONS : WRITE_OPTIONS);
    }

    public static ZipEntry newZipEntry(String name) {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0);
        return entry;
    }

    public static ZipEntry newZipEntry(ZipEntry entry) {
        return newZipEntry(entry.getName());
    }

    public static ZipInputStream newZipInput(InputStream input) {
        return new ZipInputStream(input, UNICODE_CHARSET);
    }

    public static ZipInputStream newZipInput(Path file) throws IOException {
        return newZipInput(newInput(file));
    }

    public static ZipInputStream newZipInput(URL url) throws IOException {
        return newZipInput(newInput(url));
    }

    public static byte[] read(InputStream input) throws IOException {
        try (ByteArrayOutputStream output = newByteArrayOutput()) {
            transfer(input, output);
            return output.toByteArray();
        }
    }

    public static void read(InputStream input, byte[] bytes) throws IOException {
        int offset = 0;
        while (offset < bytes.length) {
            int length = input.read(bytes, offset, bytes.length - offset);
            if (length < 0)
                throw new EOFException(String.format("%d bytes remaining", bytes.length - offset));
            offset += length;
        }
    }

    public static byte[] read(Path file) throws IOException {
        long size = readAttributes(file).size();
        if (size > Integer.MAX_VALUE)
            throw new IOException("File too big");

        // Read bytes from file
        byte[] bytes = new byte[(int) size];
        try (InputStream input = newInput(file)) {
            read(input, bytes);
        }

        // Return result
        return bytes;
    }

    public static byte[] read(URL url) throws IOException {
        try (InputStream input = newInput(url)) {
            return read(input);
        }
    }

    public static BasicFileAttributes readAttributes(Path path) throws IOException {
        return Files.readAttributes(path, BasicFileAttributes.class, LINK_OPTIONS);
    }

    public static BufferedImage readTexture(Object input, boolean cloak) throws IOException {
        ImageReader reader = ImageIO.getImageReadersByMIMEType("image/png").next();
        try {
            reader.setInput(ImageIO.createImageInputStream(input), false, false);

            // Verify texture bounds
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            if (!isValidTextureBounds(width, height, cloak))
                throw new IOException(String.format("Invalid texture bounds: %dx%d", width, height));

            // Read image
            return reader.read(0);
        } finally {
            reader.dispose();
        }
    }

    public static String request(URL url) throws IOException {
        return decode(read(url)).trim();
    }

    public static InetSocketAddress resolve(InetSocketAddress address) {
        if (address.isUnresolved())
            return new InetSocketAddress(address.getHostString(), address.getPort());
        return address;
    }

    public static Path resolveIncremental(Path dir, String name, String extension) {
        Path original = dir.resolve(name + '.' + extension);
        if (!exists(original))
            return original;

        // Incremental resolve
        int counter = 1;
        while (true) {
            Path path = dir.resolve(String.format("%s (%d).%s", name, counter, extension));
            if (exists(path)) {
                counter++;
                continue;
            }
            return path;
        }
    }

    public static Path resolveJavaBin(Path javaDir) {
        return resolveJavaBin(javaDir, false);
    }

    public static Path resolveJavaBin(Path javaDir, boolean isConsole) {
        // Get Java binaries path
        Path javaBinDir = (javaDir == null ? JVM_DIR : javaDir).resolve("bin");

        // Verify has "javaw.exe" file
        if (!isConsole && !LogHelper.isDebugEnabled()) {
            Path javawExe = javaBinDir.resolve("javaw.exe");
            if (isFile(javawExe))
                return javawExe;
        }

        // Verify has "java.exe" file
        Path javaExe = javaBinDir.resolve("java.exe");
        if (isFile(javaExe))
            return javaExe;

        // Verify has "java" file
        Path java = javaBinDir.resolve("java");
        if (isFile(java))
            return java;

        // Throw exception as no runnable found
        throw new RuntimeException("Java binary wasn't found");
    }

    public static void setSocketFlags(Socket socket) throws SocketException {
        // Set socket flags
        socket.setKeepAlive(false);
        socket.setTcpNoDelay(false);
        socket.setReuseAddress(true);

        // Set socket options
        socket.setSoTimeout(SOCKET_TIMEOUT);
        try {
            socket.setTrafficClass(0b11100);
        } catch (SocketException ignored) {
            // Windows XP has no support for that
        }
        socket.setPerformancePreferences(1, 0, 2);
    }

    public static String toAbsPathString(Path path) {
        return toAbsPath(path).toFile().getAbsolutePath();
    }

    public static Path toAbsPath(Path path) {
        return path.normalize().toAbsolutePath();
    }

    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());
        IOHelper.transfer(in, out);
        return out.toByteArray();
    }

    public static Path toPath(String path) {
        return Paths.get(CROSS_SEPARATOR_PATTERN.matcher(path).replaceAll(Matcher.quoteReplacement(PLATFORM_SEPARATOR)));
    }

    public static String toString(Path path) {
        return PLATFORM_SEPARATOR_PATTERN.matcher(path.toString()).replaceAll(Matcher.quoteReplacement(CROSS_SEPARATOR));
    }

    public static URI toURI(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static URL toURL(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new InternalError(e);
        }
    }

    public static void transfer(byte[] write, Path file, boolean append) throws IOException {
        try (OutputStream out = newOutput(file, append)) {
            out.write(write);
        }
    }

    public static long transfer(InputStream input, OutputStream output) throws IOException {
        long transferred = 0;
        byte[] buffer = newBuffer();
        for (int length = input.read(buffer); length >= 0; length = input.read(buffer)) {
            output.write(buffer, 0, length);
            transferred += length;
        }
        return transferred;
    }

    public static long transfer(InputStream input, Path file) throws IOException {
        return transfer(input, file, false);
    }

    public static long transfer(InputStream input, Path file, boolean append) throws IOException {
        try (OutputStream output = newOutput(file, append)) {
            return transfer(input, output);
        }
    }

    public static void transfer(Path file, OutputStream output) throws IOException {
        try (InputStream input = newInput(file)) {
            transfer(input, output);
        }
    }

    public static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, UNICODE_CHARSET.name());
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(e);
        }
    }

    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, UNICODE_CHARSET.name());
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(e);
        }
    }

    public static String verifyFileName(String fileName) {
        return VerifyHelper.verify(fileName, IOHelper::isValidFileName, String.format("Invalid file name: '%s'", fileName));
    }

    public static int verifyLength(int length, int max) throws IOException {
        if (length < 0 || max < 0 && length != -max || max > 0 && length > max)
            throw new IOException("Illegal length: " + length);
        return length;
    }

    public static BufferedImage verifyTexture(BufferedImage skin, boolean cloak) {
        return VerifyHelper.verify(skin, i -> isValidTextureBounds(i.getWidth(), i.getHeight(), cloak),
                String.format("Invalid texture bounds: %dx%d", skin.getWidth(), skin.getHeight()));
    }

    public static String verifyURL(String url) {
        try {
            new URL(url).toURI();
            return url;
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
    }

    public static void walk(Path dir, FileVisitor<Path> visitor, boolean hidden) throws IOException {
        Files.walkFileTree(dir, WALK_OPTIONS, Integer.MAX_VALUE, hidden ? visitor : new SkipHiddenVisitor(visitor));
    }

    public static void write(Path file, byte[] bytes) throws IOException {
        createParentDirs(file);
        Files.write(file, bytes, WRITE_OPTIONS);
    }

    public static InputStream nonClosing(InputStream in) {
        return new FilterInputStream(in) {
            @Override
            public void close() {
                // ignore
            }
        };
    }

    public static OutputStream nonClosing(OutputStream out) {
        return new FilterOutputStream(out) {
            @Override
            public void write(byte[] b, int offset, int len) throws IOException {
                super.out.write(b, offset, len);
            }

            @Override
            public void close() {
                // ignore
            }
        };
    }

    private static class MoveFileVisitor implements FileVisitor<Path> {
        private final Path from, to;

        private MoveFileVisitor(Path from, Path to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs) throws IOException {
            Path dir = to.resolve(from.relativize(file));
            if (!isDir(dir))
                Files.createDirectories(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.move(file, to.resolve(from.relativize(file)), COPY_OPTIONS);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            throw exc;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc != null) throw exc;
            if (!this.from.equals(dir)) Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }

    private static final class DeleteDirVisitor extends SimpleFileVisitor<Path> {
        private final Path dir;
        private final boolean self;

        private DeleteDirVisitor(Path dir, boolean self) {
            this.dir = dir;
            this.self = self;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            FileVisitResult result = super.postVisitDirectory(dir, exc);
            if (self || !this.dir.equals(dir))
                Files.delete(dir);
            return result;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return super.visitFile(file, attrs);
        }
    }

    private static final class SkipHiddenVisitor implements FileVisitor<Path> {
        private final FileVisitor<Path> visitor;

        private SkipHiddenVisitor(FileVisitor<Path> visitor) {
            this.visitor = visitor;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return Files.isHidden(dir) ? FileVisitResult.CONTINUE : visitor.postVisitDirectory(dir, exc);
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return Files.isHidden(dir) ? FileVisitResult.SKIP_SUBTREE : visitor.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            return Files.isHidden(file) ? FileVisitResult.CONTINUE : visitor.visitFile(file, attrs);
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return visitor.visitFileFailed(file, exc);
        }
    }
}
