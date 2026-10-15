package millionairemind;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/** Runs the distributed JAR away from the repository, including a complete console win. */
public final class PackagingTest {
    public static void main(String[] args) throws Exception {
        Path jar = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        BackendTestSupport.check(jar.toString().endsWith(".jar"), "tests must exercise the packaged JAR");
        try (JarFile archive = new JarFile(jar.toFile())) {
            for (String resource : List.of("questions.csv", "splash.gif", "splash.wav", "bg.wav",
                    "sfx/suspense.WAV", "sfx/q12.WAV", "sfx/wrong.WAV",
                    "sfx/regular winning.WAV", "sfx/supreme victory.WAV", "sfx/Clapping Sound Effects.WAV",
                    "sfx/niera_sound_5.wav", "sfx/niera_sound_2.wav", "sfx/select_005.wav",
                    "sfx/confirmation_004.wav", "sfx/error_006.wav")) {
                BackendTestSupport.check(archive.getEntry(resource) != null, "missing resource: " + resource);
                if (resource.toLowerCase(java.util.Locale.ROOT).endsWith(".wav")) {
                    javax.sound.sampled.AudioSystem.getAudioFileFormat(Main.class.getResource("/" + resource));
                }
            }
            BackendTestSupport.check(archive.stream().noneMatch(entry -> entry.getName().startsWith("logs/")
                            || entry.getName().endsWith("Test.class") || entry.getName().endsWith(".java")
                            || entry.getName().endsWith(".ogg")), "JAR contains development files");
        }
        Path temp = Files.createTempDirectory("millionaire portable game ");
        try {
            Path copiedJar = temp.resolve("millionaire-mind.jar");
            Files.copy(jar, copiedJar);
            checkRun(temp, copiedJar, List.of("--help"), "", 0, "Usage:");
            checkRun(temp, copiedJar, List.of("--unknown"), "", 2, "Invalid arguments");
            checkRun(temp, copiedJar, List.of("--console", "missing.csv"), "", 1, "Cannot start");
            checkRun(temp, copiedJar, List.of("--console"), "3\n", 0, "Goodbye");
            checkRun(temp, copiedJar, List.of("--console"), "1\nEOF Player\n", 0, "Session log saved");
            checkRun(temp, copiedJar, List.of(), "", 1, "graphical desktop");
            Path blocked = Files.createDirectory(temp.resolve("blocked logs"));
            Files.writeString(blocked.resolve("logs"), "A file is blocking the log directory.");
            checkRun(blocked, copiedJar, List.of("--console"), "", 1, "Cannot start");
            winsWithShuffledAnswers(temp, copiedJar);
        } finally {
            try (var files = Files.walk(temp)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
        System.out.println("PackagingTest passed");
    }

    private static ProcessBuilder command(Path directory, Path jar, List<String> options) {
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        String java = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java").toString();
        var command = new java.util.ArrayList<>(List.of(java, "-Djava.awt.headless=true", "-jar", jar.toString()));
        command.addAll(options);
        return new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true);
    }

    private static void checkRun(Path directory, Path jar, List<String> options, String input,
            int expectedExit, String expectedText) throws Exception {
        Path output = directory.resolve("output.txt");
        Process process = command(directory, jar, options).redirectOutput(output.toFile()).start();
        try (var stdin = process.getOutputStream()) { stdin.write(input.getBytes(StandardCharsets.UTF_8)); }
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("packaged game hung: " + options);
        }
        String text = Files.readString(output);
        BackendTestSupport.check(process.exitValue() == expectedExit && text.contains(expectedText),
                "unexpected packaged result for " + options + ": " + text);
    }

    private static void winsWithShuffledAnswers(Path directory, Path jar) throws Exception {
        Path bank = directory.resolve("custom question bank.csv");
        StringBuilder csv = new StringBuilder("id,level,prompt,optionA,optionB,optionC,optionD,correctIndex,hint,sourceReading,pageNumber\n");
        for (int slot = 1; slot <= 15; slot++) {
            csv.append(slot).append(',').append(BloomLevel.forSlot(slot))
                    .append(",Prompt,CORRECT,Wrong B,Wrong C,Wrong D,0,Hint,Test reading,1\n");
        }
        Files.writeString(bank, csv);
        Process process = command(directory, jar, List.of("--console", bank.toString())).start();
        var worker = java.util.concurrent.Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "packaged-console-test");
            thread.setDaemon(true);
            return thread;
        });
        try {
            var result = worker.submit(() -> {
                var input = process.getOutputStream();
                input.write("1\nPortable Winner\n".getBytes(StandardCharsets.UTF_8));
                input.flush();
                var output = new StringBuilder();
                var screen = new StringBuilder();
                var reader = new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
                int character;
                while ((character = reader.read()) != -1) {
                    output.append((char) character);
                    screen.append((char) character);
                    if (screen.toString().endsWith("Walk Away (W): ")) {
                        var correct = Pattern.compile("(?m)^  ([A-D])\\) CORRECT\\r?$").matcher(screen);
                        if (!correct.find()) throw new AssertionError("missing shuffled correct answer");
                        input.write((correct.group(1) + "\n").getBytes(StandardCharsets.UTF_8));
                        input.flush();
                        screen.setLength(0);
                    } else if (screen.toString().endsWith("Choose an option: ")
                            && output.toString().contains("Session log saved")) {
                        input.write("3\n".getBytes(StandardCharsets.UTF_8));
                        input.flush();
                        screen.setLength(0);
                    }
                }
                return output.toString();
            });
            String text = result.get(20, TimeUnit.SECONDS);
            BackendTestSupport.check(process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0
                            && text.contains("became a MILLIONAIRE") && text.contains("Session log saved"),
                    "standalone JAR did not finish a full game: " + text);
        } finally {
            process.destroyForcibly();
            worker.shutdownNow();
        }
    }
}
