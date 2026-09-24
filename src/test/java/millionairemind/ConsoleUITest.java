package millionairemind;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Dependency-free integration check for lifeline option visibility in the console. */
public final class ConsoleUITest {

    private ConsoleUITest() {
    }

    public static void main(String[] args) throws Exception {
        keepsFiftyFiftyOptionsRemovedUntilTheQuestionChanges();
        switchingQuestionRestoresAllOptionsForTheReplacement();
        perfectWinReportsTheTieInsteadOfStrongestAndWeakest();
        System.out.println("ConsoleUITest passed");
    }

    private static void keepsFiftyFiftyOptionsRemovedUntilTheQuestionChanges() throws Exception {
        String output = runConsole("1\nLifelineCheck\n5\nW\n3\n");
        int lifelineMessage = output.indexOf("[50:50] Two incorrect options removed.");
        BackendTestSupport.check(lifelineMessage >= 0,
                "the console should report that 50:50 was used");

        int redrawnQuestion = output.indexOf("Question 1/15", lifelineMessage);
        BackendTestSupport.check(redrawnQuestion > lifelineMessage,
                "the current question should be redrawn after using 50:50");
        int answerPrompt = output.indexOf("Answer (A-D)", redrawnQuestion);
        BackendTestSupport.check(answerPrompt > redrawnQuestion,
                "the redrawn question should prompt for an answer");

        String redrawnQuestionScreen = output.substring(redrawnQuestion, answerPrompt);
        long removedOptionCount = countRemovedOptions(redrawnQuestionScreen);
        BackendTestSupport.check(removedOptionCount == 2,
                "both options removed by 50:50 should stay removed until the answer");
    }

    private static void switchingQuestionRestoresAllOptionsForTheReplacement() throws Exception {
        String output = runConsole("1\nLifelineCheck\n5\nS\nW\n3\n");
        int switchMessage = output.indexOf("[Switch the Question] New question drawn from the same level.");
        BackendTestSupport.check(switchMessage >= 0,
                "the console should report that Switch the Question was used");

        int replacementScreen = output.lastIndexOf("Question 1/15");
        BackendTestSupport.check(replacementScreen > switchMessage,
                "the replacement question should be displayed after switching");
        int answerPrompt = output.indexOf("Answer (A-D)", replacementScreen);
        BackendTestSupport.check(answerPrompt > replacementScreen,
                "the replacement question should prompt for an answer");
        BackendTestSupport.check(countRemovedOptions(output.substring(replacementScreen, answerPrompt)) == 0,
                "a replacement question should start with all four options visible");
    }

    private static void perfectWinReportsTheTieInsteadOfStrongestAndWeakest() throws Exception {
        // Choice order is now shuffled per question, so the correct letter
        // can no longer be assumed to always be "A"; drive the console
        // interactively and read each question's own options to find it.
        String output = runConsoleAnsweringEveryQuestionCorrectly(perfectRunQuestionBank());

        BackendTestSupport.check(output.contains("Congrats! You became a MILLIONAIRE!!!"),
                "answering every fixture question correctly should complete the game");
        BackendTestSupport.check(output.contains(
                        "All Bloom levels tied at 100%; no unique strongest or weakest."),
                "a perfect win should explain that no level is uniquely strongest or weakest");
        BackendTestSupport.check(!output.contains("Strongest level:")
                        && !output.contains("Weakest level:"),
                "a perfect win should not assign misleading strongest and weakest labels");

        for (BloomLevel level : BloomLevel.values()) {
            int questionsInLevel = 0;
            for (int slot = 1; slot <= BloomLevel.totalSlots(); slot++) {
                if (BloomLevel.forSlot(slot) == level) questionsInLevel++;
            }
            String tally = String.format("  %-14s %d/%d correct",
                    level.displayName(), questionsInLevel, questionsInLevel);
            BackendTestSupport.check(output.contains(tally),
                    level.displayName() + " should retain its perfect score in the report");
        }
    }

    private static String runConsole(String scriptedInput) throws Exception {
        return runConsole(scriptedInput, null);
    }

    private static String runConsole(String scriptedInput, String questionBankCsv) throws Exception {
        Path temporaryDirectory = Files.createTempDirectory("millionaire-mind-console-test-");
        try {
            Path outputFile = temporaryDirectory.resolve("console-output.txt");
            Path questionBankFile = null;
            if (questionBankCsv != null) {
                questionBankFile = temporaryDirectory.resolve("question-bank.csv");
                Files.writeString(questionBankFile, questionBankCsv, StandardCharsets.UTF_8);
            }
            String javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                    isWindows() ? "java.exe" : "java").toString();
            String classPath = absoluteClassPath();
            ProcessBuilder processBuilder = new ProcessBuilder(javaExecutable, "-cp", classPath,
                    "millionairemind.ConsoleUI");
            if (questionBankFile != null) processBuilder.command().add(questionBankFile.toString());
            Process process = processBuilder.directory(temporaryDirectory.toFile())
                    .redirectErrorStream(true).redirectOutput(outputFile.toFile()).start();

            try (OutputStream input = process.getOutputStream()) {
                input.write(scriptedInput.getBytes(StandardCharsets.UTF_8));
            }

            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new AssertionError("console game did not finish scripted input");
            }
            BackendTestSupport.check(process.exitValue() == 0,
                    "console game should exit successfully after the scripted run");

            return Files.readString(outputFile, StandardCharsets.UTF_8);
        } finally {
            deleteRecursively(temporaryDirectory);
        }
    }

    /**
     * Drives the console interactively, answering each of the 15 questions
     * with its own correct letter. Since choice order is shuffled per
     * question, the letter is discovered fresh each time from that
     * question's freshly-printed options rather than assumed in advance.
     */
    private static String runConsoleAnsweringEveryQuestionCorrectly(String questionBankCsv) throws Exception {
        Path temporaryDirectory = Files.createTempDirectory("millionaire-mind-console-test-");
        try {
            Path questionBankFile = temporaryDirectory.resolve("question-bank.csv");
            Files.writeString(questionBankFile, questionBankCsv, StandardCharsets.UTF_8);
            String javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                    isWindows() ? "java.exe" : "java").toString();
            String classPath = absoluteClassPath();
            ProcessBuilder processBuilder = new ProcessBuilder(javaExecutable, "-cp", classPath,
                    "millionairemind.ConsoleUI", questionBankFile.toString());
            Process process = processBuilder.directory(temporaryDirectory.toFile())
                    .redirectErrorStream(true).start();

            StringBuilder transcript = new StringBuilder();
            try (InputStream stdout = process.getInputStream();
                 OutputStream stdin = process.getOutputStream()) {
                writeLine(stdin, "1");
                writeLine(stdin, "PerfectPlayer");

                for (int slot = 1; slot <= PrizeLadder.totalSlots(); slot++) {
                    String questionScreen = readUntilMarker(stdout, transcript, "Answer (A-D)");
                    writeLine(stdin, findCorrectAnswerLetter(questionScreen));
                }

                readUntilMarker(stdout, transcript, "Choose an option");
                writeLine(stdin, "3");
                drainRemaining(stdout, transcript);
            }

            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new AssertionError("console game did not finish scripted input");
            }
            BackendTestSupport.check(process.exitValue() == 0,
                    "console game should exit successfully after the scripted run");

            return transcript.toString();
        } finally {
            deleteRecursively(temporaryDirectory);
        }
    }

    private static void writeLine(OutputStream stdin, String line) throws IOException {
        stdin.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        stdin.flush();
    }

    /** Reads (and appends to the running transcript) until the marker text has just been printed. */
    private static String readUntilMarker(InputStream stdout, StringBuilder transcript, String marker) throws IOException {
        StringBuilder chunk = new StringBuilder();
        int ch;
        while ((ch = stdout.read()) != -1) {
            chunk.append((char) ch);
            transcript.append((char) ch);
            if (chunk.length() >= marker.length()
                    && chunk.substring(chunk.length() - marker.length()).equals(marker)) {
                return chunk.toString();
            }
        }
        throw new AssertionError("console output ended before printing: " + marker);
    }

    private static void drainRemaining(InputStream stdout, StringBuilder transcript) throws IOException {
        int ch;
        while ((ch = stdout.read()) != -1) {
            transcript.append((char) ch);
        }
    }

    /** Finds the letter of whichever option reads "Correct answer" on the freshly-printed question screen. */
    private static String findCorrectAnswerLetter(String questionScreen) {
        Matcher matcher = Pattern.compile("(?m)^\\s*([A-D])\\)\\s*Correct answer\\s*$").matcher(questionScreen);
        if (!matcher.find()) {
            throw new AssertionError("could not locate the correct-answer option in:\n" + questionScreen);
        }
        return matcher.group(1);
    }

    private static String perfectRunQuestionBank() {
        StringBuilder csv = new StringBuilder("id,level,prompt,optionA,optionB,optionC,optionD,"
                + "correctIndex,hint,sourceReading,pageNumber\n");
        int id = 1;
        for (BloomLevel level : BloomLevel.values()) {
            for (int slot = 1; slot <= BloomLevel.totalSlots(); slot++) {
                if (BloomLevel.forSlot(slot) != level) continue;
                csv.append("PERF-").append(id).append(',').append(level.name()).append(',')
                        .append("Prompt ").append(id).append(',')
                        .append("Correct answer,Wrong B,Wrong C,Wrong D,0,Hint,Test reading,1\n");
                id++;
            }
        }
        return csv.toString();
    }

    private static long countRemovedOptions(String screen) {
        return screen.lines().filter(line -> line.contains("[removed]")).count();
    }

    private static String absoluteClassPath() {
        return Arrays.stream(System.getProperty("java.class.path").split(
                        java.util.regex.Pattern.quote(File.pathSeparator)))
                .map(entry -> Path.of(entry).toAbsolutePath().normalize().toString())
                .collect(Collectors.joining(File.pathSeparator));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static void deleteRecursively(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
