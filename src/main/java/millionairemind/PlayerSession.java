package millionairemind;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks a single player's progress through a run: banked winnings,
 * current slot, and the per-question history that feeds both the
 * end-of-game Bloom's report and the session replay log.
 */
public final class PlayerSession {

    /** One resolved question in the session history. */
    public static final class Result {
        public final int slot;
        public final Question question;
        public final int chosenIndex;
        public final boolean correct;
        public final Instant timestamp;

        Result(int slot, Question question, int chosenIndex, boolean correct, Instant timestamp) {
            this.slot = slot;
            this.question = question;
            this.chosenIndex = chosenIndex;
            this.correct = correct;
            this.timestamp = timestamp;
        }
    }

    private final String playerName;
    private final List<Result> history = new ArrayList<>();
    private long bankedWinnings = 0;
    private boolean walkedAway = false;
    private boolean gameOver = false;
    private boolean becameMillionaire = false;

    public PlayerSession(String playerName) {
        this.playerName = playerName;
    }

    public String playerName() { return playerName; }

    public void recordAnswer(int slot, Question question, int chosenIndex, boolean correct) {
        history.add(new Result(slot, question, chosenIndex, correct, Instant.now()));
    }

    public List<Result> history() { return history; }

    public void setBankedWinnings(long amount) { this.bankedWinnings = amount; }
    public long bankedWinnings() { return bankedWinnings; }

    public void markWalkedAway() { this.walkedAway = true; this.gameOver = true; }
    public void markGameOver() { this.gameOver = true; }
    public void markMillionaire() { this.becameMillionaire = true; this.gameOver = true; }

    public boolean walkedAway() { return walkedAway; }
    public boolean isGameOver() { return gameOver; }
    public boolean becameMillionaire() { return becameMillionaire; }

    /**
     * Tallies correct/incorrect answers per Bloom's level, giving the player
     * a locally computed recap of their strongest and weakest cognitive
     * levels (Section 5, "Post-Game Bloom's Report").
     */
    public Map<BloomLevel, int[]> bloomReport() {
        // int[0] = correct, int[1] = incorrect
        Map<BloomLevel, int[]> report = new EnumMap<>(BloomLevel.class);
        for (BloomLevel level : BloomLevel.values()) {
            report.put(level, new int[]{0, 0});
        }
        for (Result r : history) {
            int[] tally = report.get(r.question.level());
            if (r.correct) tally[0]++; else tally[1]++;
        }
        return report;
    }

    public BloomLevel strongestLevel() {
        return extremeLevel(true);
    }

    public BloomLevel weakestLevel() {
        return extremeLevel(false);
    }

    private BloomLevel extremeLevel(boolean strongest) {
        Map<BloomLevel, int[]> report = bloomReport();
        BloomLevel best = null;
        double bestScore = strongest ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        for (Map.Entry<BloomLevel, int[]> e : report.entrySet()) {
            int correct = e.getValue()[0];
            int incorrect = e.getValue()[1];
            int attempts = correct + incorrect;
            if (attempts == 0) continue;
            double score = (double) correct / attempts;
            if (strongest ? score > bestScore : score < bestScore) {
                bestScore = score;
                best = e.getKey();
            }
        }
        return best;
    }
}
