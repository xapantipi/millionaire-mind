import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.tools.ToolProvider;

/** Shared Windows/Bash build driver; runs in JDK source-file mode. */
public final class Project {
    private final Path root;
    private final Path build;
    private final Path jar;

    private Project(Path root) {
        this.root = root.toAbsolutePath().normalize();
        build = this.root.resolve("build");
        jar = this.root.resolve("dist/millionaire-mind.jar");
    }

    public static void main(String[] args) {
        try {
            Project project = new Project(Path.of(args[0]));
            String action = args[1];
            List<String> options = List.of(args).subList(2, args.length);
            if (!List.of("build", "run", "test", "release").contains(action)) {
                throw new IllegalArgumentException("Unknown action: " + action);
            }
            if (action.equals("test") && !(options.isEmpty() || options.equals(List.of("--gui")))) {
                throw new IllegalArgumentException("Usage: test.cmd [--gui] or ./test.sh [--gui]");
            }
            if ((action.equals("build") || action.equals("release")) && !options.isEmpty()) {
                throw new IllegalArgumentException("Build/release take no arguments.");
            }
            project.build();
            int result = switch (action) {
                case "run" -> project.runGame(options);
                case "test" -> project.test(options.contains("--gui"));
                case "release" -> project.release();
                default -> 0;
            };
            if (result != 0) System.exit(result);
        } catch (Exception e) {
            System.err.println("Millionaire Mind: " + e.getMessage());
            System.exit(1);
        }
    }

    private List<Path> javaFiles(String directory) throws IOException {
        try (var files = Files.walk(root.resolve(directory))) {
            return files.filter(file -> file.toString().endsWith(".java")).sorted().toList();
        }
    }

    private void compile(List<Path> sources, Path output, String classPath) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IOException("Install JDK 17 or later to build from source.");
        clean(output);
        Files.createDirectories(output);
        try (var manager = compiler.getStandardFileManager(null, null, java.nio.charset.StandardCharsets.UTF_8)) {
            List<String> options = new ArrayList<>(List.of("--release", "17", "-encoding", "UTF-8",
                    "-d", output.toString()));
            if (classPath != null) options.addAll(List.of("-classpath", classPath));
            if (!compiler.getTask(null, manager, null, options, null,
                    manager.getJavaFileObjectsFromPaths(sources)).call()) {
                throw new IOException("Compilation failed; the game was not launched.");
            }
        }
    }

    private void build() throws IOException {
        Path classes = build.resolve("classes");
        compile(javaFiles("src/main/java"), classes, null);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "millionairemind.Main");
        Files.createDirectories(jar.getParent());
        Path staged = Files.createTempFile(jar.getParent(), "game-", ".jar");
        try {
            try (JarOutputStream archive = new JarOutputStream(Files.newOutputStream(staged), manifest)) {
                try (var files = Files.walk(classes)) {
                    for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                        addEntry(archive, file, classes.relativize(file).toString().replace('\\', '/'));
                    }
                }
                Path resources = root.resolve("resources");
                addEntry(archive, root.resolve("MCQ/Question Bank.csv"), "MCQ/Question Bank.csv");
                try (var files = Files.walk(resources)) {
                    for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                        String name = resources.relativize(file).toString().replace('\\', '/');
                        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".wav") || name.equals("splash.gif")) {
                            addEntry(archive, file, name);
                        }
                    }
                }
            }
            Files.move(staged, jar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staged);
        }
        System.out.println("Built " + jar);
    }

    private static void addEntry(JarOutputStream archive, Path file, String name) throws IOException {
        JarEntry entry = new JarEntry(name);
        entry.setTime(0);
        archive.putNextEntry(entry);
        Files.copy(file, archive);
        archive.closeEntry();
    }

    private int runGame(List<String> options) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(java(), "-jar", jar.toString()));
        command.addAll(options);
        return new ProcessBuilder(command).inheritIO().start().waitFor();
    }

    private int test(boolean gui) throws IOException, InterruptedException {
        Path tests = build.resolve("test-classes");
        List<Path> sources = javaFiles("src/test/java");
        compile(sources, tests, jar.toString());
        String classPath = jar + java.io.File.pathSeparator + tests;
        for (Path source : sources) {
            String name = source.getFileName().toString();
            if (!name.endsWith("Test.java")) continue;
            if (name.equals("SwingUITest.java") && !gui) {
                System.out.println("SwingUITest not run: use test --gui on a graphical desktop.");
                continue;
            }
            String type = "millionairemind." + name.substring(0, name.length() - 5);
            Process process = new ProcessBuilder(java(), "-cp", classPath, type).inheritIO().start();
            if (!process.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException(type + " timed out after 120 seconds");
            }
            if (process.exitValue() != 0) throw new IOException(type + " failed");
        }
        return 0;
    }

    private static String java() {
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        return Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java").toString();
    }

    private int release() throws IOException, InterruptedException {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            throw new IOException("Create the Windows submission on Windows using JDK 17+ and release.cmd.");
        }
        Path jpackage = Path.of(System.getProperty("java.home"), "bin", "jpackage.exe");
        if (!Files.isRegularFile(jpackage)) throw new IOException("This JDK does not include jpackage.exe.");
        test(false);
        Path input = build.resolve("native-input");
        Path windows = build.resolve("windows");
        clean(input);
        clean(windows);
        Files.createDirectories(input);
        Files.createDirectories(windows);
        Files.copy(jar, input.resolve(jar.getFileName()));
        Path consoleProperties = build.resolve("console-launcher.properties");
        Files.writeString(consoleProperties, "arguments=--console\nwin-console=true\n");
        List<String> command = List.of(jpackage.toString(), "--type", "app-image", "--name", "MillionaireMind",
                "--app-version", "1.0", "--description", "AI fundamentals quiz across Bloom's Taxonomy",
                "--input", input.toString(), "--main-jar", jar.getFileName().toString(),
                "--main-class", "millionairemind.Main", "--dest", windows.toString(),
                "--add-modules", "java.desktop,jdk.random", "--java-options", "-Dfile.encoding=UTF-8",
                "--add-launcher", "MillionaireMind-Console=" + consoleProperties);
        Process packaging = new ProcessBuilder(command).inheritIO().start();
        if (!packaging.waitFor(5, java.util.concurrent.TimeUnit.MINUTES)) {
            packaging.destroyForcibly();
            throw new IOException("Windows packaging timed out.");
        }
        if (packaging.exitValue() != 0) throw new IOException("Windows packaging failed.");
        Path image = windows.resolve("MillionaireMind");
        Files.copy(root.resolve("ReadMeFirst.txt"), image.resolve("ReadMeFirst.txt"));
        verifyNativeConsole(image);

        List<Path> sourceFiles = new ArrayList<>();
        for (String directory : List.of("src/main/java", "resources")) {
            try (var files = Files.walk(root.resolve(directory))) {
                sourceFiles.addAll(files.filter(Files::isRegularFile).filter(file -> {
                    String name = file.getFileName().toString();
                    return directory.equals("src/main/java") ? name.endsWith(".java")
                            : name.toLowerCase(java.util.Locale.ROOT).endsWith(".wav") || name.equals("splash.gif");
                }).sorted().toList());
            }
        }
        for (String name : List.of("MCQ/Question Bank.csv", "tools/Project.java",
                "ReadMeFirst.txt", "build.cmd", "run.cmd")) {
            sourceFiles.add(root.resolve(name));
        }
        Path sourceZip = jar.getParent().resolve("millionaire-mind-source.zip");
        zip(sourceZip, root, "MillionaireMind-source/", sourceFiles);
        Path executableZip = jar.getParent().resolve("millionaire-mind-windows.zip");
        try (var files = Files.walk(image)) {
            zip(executableZip, image, "MillionaireMind/", files.filter(Files::isRegularFile).sorted().toList());
        }
        System.out.println("Source submission: " + sourceZip);
        System.out.println("Windows submission: " + executableZip);
        System.out.println("Standalone game: " + image.resolve("MillionaireMind.exe"));
        return 0;
    }

    private void verifyNativeConsole(Path image) throws IOException, InterruptedException {
        Path output = build.resolve("native-smoke-output.txt");
        ProcessBuilder command = new ProcessBuilder(image.resolve("MillionaireMind-Console.exe").toString())
                .directory(build.toFile()).redirectErrorStream(true).redirectOutput(output.toFile());
        command.environment().remove("JAVA_HOME");
        command.environment().put("PATH", Path.of(System.getenv("SystemRoot"), "System32").toString());
        Process game = command.start();
        try (var input = game.getOutputStream()) {
            input.write("3\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        if (!game.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
            game.destroyForcibly();
            throw new IOException("Standalone executable failed to finish its smoke test.");
        }
        if (game.exitValue() != 0 || !Files.readString(output).contains("Goodbye")) {
            throw new IOException("Standalone executable failed: " + Files.readString(output));
        }
        System.out.println("Standalone EXE passed with JAVA_HOME unset and Java removed from PATH.");
    }

    private static void zip(Path output, Path base, String prefix, List<Path> files) throws IOException {
        Path staged = Files.createTempFile(output.getParent(), "submission-", ".zip");
        try {
            try (ZipOutputStream archive = new ZipOutputStream(Files.newOutputStream(staged))) {
                for (Path file : files) {
                    ZipEntry entry = new ZipEntry(prefix + base.relativize(file).toString().replace('\\', '/'));
                    entry.setTime(0);
                    archive.putNextEntry(entry);
                    Files.copy(file, archive);
                    archive.closeEntry();
                }
            }
            Files.move(staged, output, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(staged); }
    }

    private void clean(Path directory) throws IOException {
        Path target = directory.toAbsolutePath().normalize();
        if (!target.startsWith(build) || target.equals(build)) {
            throw new IOException("Refusing to clean outside generated build directories: " + target);
        }
        if (!Files.exists(target)) return;
        try (var files = Files.walk(target)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                // jpackage marks generated Windows launchers read-only.
                if (System.getProperty("os.name").startsWith("Windows") && !Files.isSymbolicLink(file)) {
                    Files.setAttribute(file, "dos:readonly", false, java.nio.file.LinkOption.NOFOLLOW_LINKS);
                }
                Files.delete(file);
            }
        }
    }
}
