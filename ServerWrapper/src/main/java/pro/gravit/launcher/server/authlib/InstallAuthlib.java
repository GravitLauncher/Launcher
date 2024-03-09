package pro.gravit.launcher.server.authlib;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

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
        if(args[0].startsWith("http://") || args[0].startsWith("https://")) {
            Path tempAuthlib = Paths.get(tempLaunchAuthLibName);
            LogHelper.info("Download %s to %s", args[0], tempAuthlib);
            try(InputStream input = IOHelper.newInput(new URL(args[0]))) {
                IOHelper.transfer(input, tempAuthlib);
            }
            context.pathToAuthlib = tempAuthlib.toAbsolutePath();
            deleteAuthlibAfterInstall = true;
        } else {
            context.pathToAuthlib = Paths.get(args[0]).toAbsolutePath();
        }
        if(Files.notExists(context.pathToAuthlib)) {
            throw new FileNotFoundException(context.pathToAuthlib.toString());
        }
        LogHelper.info("Search .jar files in %s", context.workdir.toAbsolutePath());
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
        LogHelper.info("Search authlib in %d files", context.files.size());
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
            LogHelper.info("Found authlib in %s (prefix '%s' jar %s)", ri.path, ri.prefix, ri.isJarFile ? "true" : "false");
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
        LogHelper.info("%d authlib files repacked", context.repack.size());
        for(HashFile hf : context.hashes) {
            LogHelper.info("Found hash file %s in %s", hf.prefix, hf.path);
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
        LogHelper.info("%d hash files repacked", context.hashes.size());
        if(deleteAuthlibAfterInstall) {
            LogHelper.info("Delete %s", context.pathToAuthlib);
            Files.delete(context.pathToAuthlib);
        }
        LogHelper.info("Completed");
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
