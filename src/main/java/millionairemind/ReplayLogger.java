package millionairemind;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Persists session results, the full question/answer history, and the
 * per-level score breakdown to a local log file (Section 6, ReplayLogger).
 * Used both for the readme demo walkthrough and for repeat-play statistics.
 */
public final class ReplayLogger {

    private final Path logDirectory;

    public ReplayLogger(Path logDirectory) {
        this.logDirectory = logDirectory;
        try {
            Files.createDirectories(logDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes a full session log and returns the path written to.
     */
    public Path logSession(PlayerSession session) {
        String stamp = DateTimeFormatter.ofPattern("yyMMdd-HHmmss")
                .format(java.time.LocalDateTime.now());
        String basename = "replay-" + stamp;

        StringBuilder sb = new StringBuilder();
        sb.append("Millionaire Mind - Session Replay Log\n");
        sb.append("Player: ").append(session.playerName()).append("\n");
        sb.append("Ended: ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("Outcome: ");
        if (session.becameMillionaire()) sb.append("MILLIONAIRE!\n");
        else if (session.walkedAway()) sb.append("Walked away\n");
        else sb.append("Missed a question\n");
        sb.append("Final winnings: $").append(session.bankedWinnings()).append("\n");
        sb.append("----------------------------------------\n");
        sb.append("Question history:\n");

        for (PlayerSession.Result r : session.history()) {
            sb.append(String.format(
                "Q%02d [%s] (%s) - %s%n",
                r.slot, r.question.level().displayName(), r.question.id(),
                r.correct ? "CORRECT" : "WRONG"
            ));
            sb.append("  Prompt: ").append(r.question.prompt()).append("\n");
            sb.append("  Chosen: ").append(indexLetter(r.chosenIndex))
              .append(") ").append(r.question.options().get(r.chosenIndex)).append("\n");
            sb.append("  Correct answer: ").append(indexLetter(r.question.correctIndex()))
              .append(") ").append(r.question.correctOptionText()).append("\n");
            sb.append("  Source: ").append(r.question.citation()).append("\n");
        }

        sb.append("----------------------------------------\n");
        sb.append("Bloom's report:\n");
        Map<BloomLevel, int[]> report = session.bloomReport();
        for (BloomLevel level : BloomLevel.values()) {
            int[] tally = report.get(level);
            int attempts = tally[0] + tally[1];
            sb.append("  ").append(level.displayName()).append(": ")
              .append(tally[0]).append("/").append(attempts).append(" correct\n");
        }

        for (int suffix = 1; ; suffix++) {
            String filename = basename + (suffix == 1 ? "" : "-" + suffix) + ".log";
            Path file = logDirectory.resolve(filename);
            try {
                Files.writeString(file, sb.toString(),
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                return file;
            } catch (FileAlreadyExistsException e) {
                // A second session can finish within the same second.
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static String indexLetter(int index) {
        return String.valueOf((char) ('A' + index));
    }
}
