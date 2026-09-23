package millionairemind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Dependency-free regression tests for replay file creation and contents. */
public final class ReplayLoggerTest {

    private ReplayLoggerTest() {
    }

    public static void main(String[] args) throws Exception {
        logsSessionHistoryAndEachOutcome();
        System.out.println("ReplayLoggerTest passed");
    }

    private static void logsSessionHistoryAndEachOutcome() throws IOException {
        Path temporaryDirectory = Files.createTempDirectory("millionaire-mind-replay-tests-");
        try {
            Path logDirectory = temporaryDirectory.resolve("nested").resolve("logs");
            ReplayLogger logger = new ReplayLogger(logDirectory);
            BackendTestSupport.check(Files.isDirectory(logDirectory),
                    "the logger should create its target directory");

            PlayerSession missed = new PlayerSession("Ken / Smith");
            Question correctQuestion = new Question("R1", BloomLevel.REMEMBERING,
                    "Name the correct choice", java.util.List.of("Choice A", "Choice B", "Choice C", "Choice D"),
                    2, "Use the hint", "AI Foundations", "12");
            Question wrongQuestion = new Question("R2", BloomLevel.REMEMBERING,
                    "Choose another answer", java.util.List.of("Choice A", "Choice B", "Choice C", "Choice D"),
                    0, "Review the reading", "AI Foundations", "13");
            missed.recordAnswer(1, correctQuestion, 2, true);
            missed.recordAnswer(2, wrongQuestion, 1, false);
            missed.setBankedWinnings(2_000);
            missed.markGameOver();

            Path missedLog = logger.logSession(missed);
            BackendTestSupport.check(missedLog.getParent().equals(logDirectory),
                    "the replay should be written inside the configured log directory");
            BackendTestSupport.check(missedLog.getFileName().toString()
                            .matches("replay-\\d{6}-\\d{6}(?:-\\d+)?\\.log"),
                    "the replay filename should be short and independent of player names");
            Path secondLog = logger.logSession(missed);
            BackendTestSupport.check(!secondLog.equals(missedLog)
                            && Files.isRegularFile(secondLog),
                    "replays finished in the same second should use distinct files");
            String missedContents = Files.readString(missedLog, StandardCharsets.UTF_8);
            BackendTestSupport.check(missedContents.contains("Player: Ken / Smith"),
                    "the replay should preserve the player's display name");
            BackendTestSupport.check(missedContents.contains("Outcome: Missed a question")
                            && missedContents.contains("Final winnings: $2000"),
                    "the replay should include the loss outcome and final bank");
            BackendTestSupport.check(missedContents.contains("Q01 [Remembering] (R1) - CORRECT")
                            && missedContents.contains("Q02 [Remembering] (R2) - WRONG"),
                    "the replay should include each question result in order");
            BackendTestSupport.check(missedContents.contains("Chosen: C) Choice C")
                            && missedContents.contains("Correct answer: C) Choice C")
                            && missedContents.contains("Source: AI Foundations, p. 12"),
                    "the replay should include selected and correct answers with citations");
            BackendTestSupport.check(missedContents.contains("Remembering: 1/2 correct"),
                    "the replay should include the Bloom-level tally");

            PlayerSession walker = new PlayerSession("Walker");
            walker.setBankedWinnings(2_000);
            walker.markWalkedAway();
            String walkerContents = Files.readString(logger.logSession(walker), StandardCharsets.UTF_8);
            BackendTestSupport.check(walkerContents.contains("Outcome: Walked away"),
                    "the replay should label a Walk Away session");

            PlayerSession winner = new PlayerSession("Winner");
            winner.setBankedWinnings(1_000_000);
            winner.markMillionaire();
            String winnerContents = Files.readString(logger.logSession(winner), StandardCharsets.UTF_8);
            BackendTestSupport.check(winnerContents.contains("Outcome: MILLIONAIRE!")
                            && winnerContents.contains("Final winnings: $1000000"),
                    "the replay should label a millionaire result and prize");
        } finally {
            deleteRecursively(temporaryDirectory);
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
