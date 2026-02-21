package pro.gravit.launcher.server.authlib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.utils.helper.IOHelper;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class InstallAuthlib {

    private static final Logger logger =
            LoggerFactory.getLogger(InstallAuthlib.class);

    private static final Map<String, LibrariesHashFileModifier> modifierMap;
    private static final String tempLaunchAuthLibName = "authlib.jar";
    static {
        modifierMap = new HashMap<>();
        modifierMap.put("META-INF/libraries.list", new LibrariesLstModifier());
        modifierMap.put("patch.properties", new PatchPropertiesModifier());
        modifierMap.put("META-INF/download-context", new DownloadContextModifier());
        modifierMap.put("META-INF/patches.list", new PatchesLstModifier());
    }
    public void run(String... args) throws Exception {
        boolean deleteAuthlibAfterInstall = false;
        InstallAuthlibContext context = new InstallAuthlibContext();
        String source = null;
        if (args != null && args.length > 0) {
            source = args[0];
        }
        if (source == null || source.isEmpty()) {
            // Detect version from libraries/com/mojang/authlib/
            try {
                Path libs = context.workdir.resolve("libraries").resolve("com").resolve("mojang").resolve("authlib");
                if (Files.exists(libs) && Files.isDirectory(libs)) {
                    Optional<String> best = Files.list(libs)
                            .filter(Files::isDirectory)
                            .map(p -> p.getFileName().toString())
                            .max(InstallAuthlib::compareVersion);
                    if (best.isPresent()) {
                        String version = best.get();
                        int major = parseMajor(version);
                        String fileName;
                        if (major == 3 && inRange(version, "3.5.41", "3.18.38")) {
                            fileName = "LauncherAuthlib3-1.19.jar";
                        } else {
                            fileName = "LauncherAuthlib" + major + ".jar";
                        }
                        // Get mirrors from system property or env, fallback to default
                        String prop = System.getProperty("installAuthlib.mirrors");
                        List<String> mirrors;
                        if (prop != null && !prop.isEmpty()) {
                            mirrors = Arrays.asList(prop.split(","));
                        } else if (System.getenv("SERVERWRAPPER_MIRRORS") != null) {
                            mirrors = Arrays.asList(System.getenv("SERVERWRAPPER_MIRRORS").split(","));
                        } else {
                            mirrors = List.of("https://mirror.gravitlauncher.com/5.7.x/");
                        }
                        String chosen = null;
                        for (String base : mirrors) {
                            String b = base.endsWith("/") ? base : base + "/";
                            String urlStr = b + "authlib/" + fileName;
                            try {
                                logger.info("Try download {}", urlStr);
                                try (InputStream input = IOHelper.newInput(new URL(urlStr))) {
                                    Path tempAuthlib = Paths.get(tempLaunchAuthLibName);
                                    IOHelper.transfer(input, tempAuthlib);
                                    context.pathToAuthlib = tempAuthlib.toAbsolutePath();
                                    deleteAuthlibAfterInstall = true;
                                    chosen = urlStr;
                                    break;
                                }
                            } catch (FileNotFoundException fnf) {
                                logger.info("Not found on mirror: {}", urlStr);
                                continue;
                            } catch (IOException ioe) {
                                logger.info("Failed to download from {}: {}", urlStr, ioe.getMessage());
                                continue;
                            }
                        }
                        if (chosen == null) {
                            throw new FileNotFoundException("LauncherAuthlib for detected version " + version + " not found on mirrors. Please notify developers.");
                        }
                    } else {
                        throw new FileNotFoundException("No authlib versions found in " + libs.toString());
                    }
                } else {
                    throw new FileNotFoundException("Libraries authlib directory not found: " + libs.toString());
                }
            } catch (IOException e) {
                throw e;
            }
        } else {
            if(source.startsWith("http://") || source.startsWith("https://")) {
                Path tempAuthlib = Paths.get(tempLaunchAuthLibName);
                logger.info("Download {} to {}", source, tempAuthlib);
                try(InputStream input = IOHelper.newInput(new URL(source))) {
                    IOHelper.transfer(input, tempAuthlib);
                }
                context.pathToAuthlib = tempAuthlib.toAbsolutePath();
                deleteAuthlibAfterInstall = true;
            } else {
                context.pathToAuthlib = Paths.get(source).toAbsolutePath();
            }
        }
        if(Files.notExists(context.pathToAuthlib)) {
            throw new FileNotFoundException(context.pathToAuthlib.toString());
        }
        logger.info("Search .jar files in {}", context.workdir.toAbsolutePath());
        IOHelper.walk(context.workdir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().endsWith(".jar") && !file.equals(context.pathToAuthlib)) {
                    context.files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        }, true);
        context.files.sort(Comparator.comparingInt((Path path) -> - path.getNameCount()));
        logger.info("Search authlib in {} files", context.files.size());
        for(Path path : context.files) {
            boolean foundAuthlib = false;
            try(ZipInputStream input = IOHelper.newZipInput(path)) {
                ZipEntry e = input.getNextEntry();
                while(e != null) {
                    String name = e.getName();
                    if(!e.isDirectory() && name.contains("com/mojang/authlib") && !foundAuthlib) {
                        boolean isJarFile = name.endsWith(".jar");
                        String prefix = isJarFile ? name : name.substring(0, name.indexOf("com/mojang/authlib"));
                        context.repack.add(new RepackInfo(path, prefix, isJarFile));
                        foundAuthlib = true;
                    }
                    if(!e.isDirectory() && modifierMap.containsKey(name)) {
                        context.hashes.add(new HashFile(path, name, modifierMap.get(name)));
                    }
                    e = input.getNextEntry();
                }
            }
        }
        Path tmpFile = Paths.get("repack.tmp");
        for(RepackInfo ri : context.repack) {
            logger.info("Found authlib in {} (prefix '{}' jar {})", ri.path, ri.prefix, ri.isJarFile ? "true" : "false");
            try(ZipInputStream input = IOHelper.newZipInput(ri.path)) {
                try(ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(tmpFile))) {
                    ZipEntry e;
                    e = input.getNextEntry();
                    while(e != null) {
                        if(!e.getName().equals("META-INF") && !e.getName().equals("META-INF/")  && !e.getName().equals("META-INF/MANIFEST.MF")) {
                            break;
                        }
                        ZipEntry newEntry = IOHelper.newZipEntry(e);
                        output.putNextEntry(newEntry);
                        IOHelper.transfer(input, output);
                        e = input.getNextEntry();
                    }
                    if(!ri.isJarFile) {
                        try(ZipInputStream input2 = new ZipInputStream(IOHelper.newInput(context.pathToAuthlib))) {
                            ZipEntry e2 = input2.getNextEntry();
                            while(e2 != null) {
                                if(e2.getName().startsWith("META-INF")) {
                                    e2 = input2.getNextEntry();
                                    continue;
                                }
                                String newName = !ri.prefix.endsWith("/") && !e2.getName().startsWith("/") && !ri.prefix.isEmpty() ?
                                        ri.prefix.concat("/").concat(e2.getName()) : ri.prefix.concat(e2.getName());
                                ZipEntry newEntry = IOHelper.newZipEntry(newName);
                                output.putNextEntry(newEntry);
                                IOHelper.transfer(input2, output);
                                e2 = input2.getNextEntry();
                            }
                        }
                    }
                    while(e != null) {
                        if(e.getName().startsWith(ri.prefix)) {
                            if(ri.isJarFile) {
                                if(context.repackedAuthlibBytes == null) {
                                    byte[] orig = IOHelper.read(input);
                                    context.repackedAuthlibBytes = repackAuthlibJar(orig, context.pathToAuthlib);
                                }
                                ZipEntry newEntry = IOHelper.newZipEntry(e);
                                output.putNextEntry(newEntry);
                                output.write(context.repackedAuthlibBytes);
                                e = input.getNextEntry();
                                continue;
                            } else {
                                if(context.repackedAuthlibFiles == null) {
                                    context.repackedAuthlibFiles = getNames(context.pathToAuthlib);
                                }
                                if(context.repackedAuthlibFiles.contains(e.getName().substring(ri.prefix.length()))) {
                                    e = input.getNextEntry();
                                    continue;
                                }
                            }
                        }
                        ZipEntry newEntry = IOHelper.newZipEntry(e);
                        output.putNextEntry(newEntry);
                        IOHelper.transfer(input, output);
                        e = input.getNextEntry();
                    }
                }
            }
            Files.delete(ri.path);
            Files.move(tmpFile, ri.path);
        }
        logger.info("{} authlib files repacked", context.repack.size());
        for(HashFile hf : context.hashes) {
            logger.info("Found hash file {} in {}", hf.prefix, hf.path);
            try(ZipInputStream input = IOHelper.newZipInput(hf.path)) {
                try (ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(tmpFile))) {
                    ZipEntry e = input.getNextEntry();
                    while(e != null) {
                        ZipEntry newEntry = IOHelper.newZipEntry(e);
                        output.putNextEntry(newEntry);
                        if(e.getName().equals(hf.prefix)) {
                            byte[] orig = IOHelper.read(input);
                            byte[] bytes = hf.modifier.apply(orig, context);
                            output.write(bytes);
                        } else {
                            IOHelper.transfer(input, output);
                        }
                        e = input.getNextEntry();
                    }
                }
            }
            Files.delete(hf.path);
            Files.move(tmpFile, hf.path);
        }
        logger.info("{} hash files repacked", context.hashes.size());
        if(deleteAuthlibAfterInstall) {
            logger.info("Delete {}", context.pathToAuthlib);
            Files.delete(context.pathToAuthlib);
        }
        logger.info("Completed");
    }

    private Set<String> getNames(Path path) throws IOException {
        Set<String> set = new HashSet<>();
        try(ZipInputStream input = IOHelper.newZipInput(path)) {
            ZipEntry e = input.getNextEntry();
            while(e != null) {
                if(!e.getName().startsWith("META-INF")) {
                    set.add(e.getName());
                }
                e = input.getNextEntry();
            }
        }
        return set;
    }

    private static int compareVersion(String a, String b) {
        int[] va = parseVersion(a);
        int[] vb = parseVersion(b);
        for (int i = 0; i < Math.max(va.length, vb.length); i++) {
            int ca = i < va.length ? va[i] : 0;
            int cb = i < vb.length ? vb[i] : 0;
            if (ca != cb) return Integer.compare(ca, cb);
        }
        return 0;
    }

    private static int[] parseVersion(String v) {
        try {
            String[] parts = v.split("[.-]");
            int[] res = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                try { res[i] = Integer.parseInt(parts[i]); } catch (NumberFormatException e) { res[i] = 0; }
            }
            return res;
        } catch (Exception e) {
            return new int[]{0};
        }
    }

    private static int parseMajor(String v) {
        int[] vv = parseVersion(v);
        return vv.length > 0 ? vv[0] : 0;
    }

    private static boolean inRange(String v, String from, String to) {
        return compareVersion(v, from) >= 0 && compareVersion(v, to) <= 0;
    }

    private byte[] repackAuthlibJar(byte[] data, Path path) throws IOException {
        try(ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(data))) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try(ZipOutputStream output = new ZipOutputStream(result)) {
                Set<String> blacklist = new HashSet<>();
                try(ZipInputStream input2 = IOHelper.newZipInput(path)) {
                    ZipEntry e = input2.getNextEntry();
                    while(e != null) {
                        if(e.getName().startsWith("META-INF")) {
                            e = input2.getNextEntry();
                            continue;
                        }
                        ZipEntry newEntry = IOHelper.newZipEntry(e);
                        output.putNextEntry(newEntry);
                        IOHelper.transfer(input2, output);
                        blacklist.add(e.getName());
                        e = input2.getNextEntry();
                    }
                }
                ZipEntry e = input.getNextEntry();
                while(e != null) {
                    if(blacklist.contains(e.getName())) {
                        e = input.getNextEntry();
                        continue;
                    }
                    ZipEntry newEntry = IOHelper.newZipEntry(e);
                    output.putNextEntry(newEntry);
                    IOHelper.transfer(input, output);
                    e = input.getNextEntry();
                }
            }
            return result.toByteArray();
        }
    }

    public static class RepackInfo {
        public Path path;
        public String prefix;
        public boolean isJarFile;

        public RepackInfo(Path path, String prefix, boolean isJarFile) {
            this.path = path;
            this.prefix = prefix;
            this.isJarFile = isJarFile;
        }
    }

    public static class HashFile {
        public Path path;
        public String prefix;
        public LibrariesHashFileModifier modifier;

        public HashFile(Path path, String prefix, LibrariesHashFileModifier modifier) {
            this.path = path;
            this.prefix = prefix;
            this.modifier = modifier;
        }
    }

    public static class InstallAuthlibContext {
        public Path pathToAuthlib;
        public Path workdir = IOHelper.WORKING_DIR;
        public List<Path> files = new ArrayList<>();
        public List<RepackInfo> repack = new ArrayList<>();
        public List<HashFile> hashes = new ArrayList<>();
        public byte[] repackedAuthlibBytes = null;
        public Set<String> repackedAuthlibFiles = null;

        public LocalDateTime timestamp = LocalDateTime.now();
    }
}