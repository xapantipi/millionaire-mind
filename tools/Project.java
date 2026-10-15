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
import javax.tools.ToolProvider;

/** Builds and launches the game using JDK tools in source-file mode. */
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
            if (!List.of("build", "run").contains(action)) {
                throw new IllegalArgumentException("Unknown action: " + action);
            }
            if (action.equals("build") && !options.isEmpty()) {
                throw new IllegalArgumentException("Build takes no arguments.");
            }
            project.build();
            int result = action.equals("run") ? project.runGame(options) : 0;
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

    private void compile(List<Path> sources, Path output) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IOException("Install JDK 17 or later to build from source.");
        clean(output);
        Files.createDirectories(output);
        try (var manager = compiler.getStandardFileManager(null, null, java.nio.charset.StandardCharsets.UTF_8)) {
            List<String> options = new ArrayList<>(List.of("--release", "17", "-encoding", "UTF-8",
                    "-d", output.toString()));
            if (!compiler.getTask(null, manager, null, options, null,
                    manager.getJavaFileObjectsFromPaths(sources)).call()) {
                throw new IOException("Compilation failed; the game was not launched.");
            }
        }
    }

    private void build() throws IOException {
        Path classes = build.resolve("classes");
        compile(javaFiles("src/main/java"), classes);
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

    private static String java() {
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        return Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java").toString();
    }

    private void clean(Path directory) throws IOException {
        Path target = directory.toAbsolutePath().normalize();
        if (!target.startsWith(build) || target.equals(build)) {
            throw new IOException("Refusing to clean outside generated build directories: " + target);
        }
        if (!Files.exists(target)) return;
        try (var files = Files.walk(target)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(file);
            }
        }
    }
}
