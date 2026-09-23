package millionairemind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.random.RandomGenerator;

/** Shared deterministic fixtures for the backend rule tests. */
final class BackendTestSupport {

    private static final String HEADER = String.join(",",
            "id", "level", "prompt", "optionA", "optionB", "optionC", "optionD",
            "correctIndex", "hint", "sourceReading", "pageNumber");

    private BackendTestSupport() {
    }

    static QuestionBank questionBank() throws IOException {
        Path path = Files.createTempFile("millionaire-mind-backend-tests-", ".csv");
        try {
            Files.writeString(path, questionBankCsv(), StandardCharsets.UTF_8);
            return QuestionBank.loadFromFile(path, new java.util.Random(17));
        } finally {
            Files.deleteIfExists(path);
        }
    }

    static Question question(String id, BloomLevel level, int correctIndex) {
        return new Question(id, level, "Prompt for " + id,
                List.of("Option A", "Option B", "Option C", "Option D"),
                correctIndex, "Hint for " + id, "Reading for " + id, "12");
    }

    static RandomGenerator fixedRandom(double doubleValue, int intValue) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0L;
            }

            @Override
            public int nextInt(int bound) {
                return Math.floorMod(intValue, bound);
            }

            @Override
            public double nextDouble() {
                return doubleValue;
            }
        };
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void expectIllegalState(Runnable action, String messagePart) {
        try {
            action.run();
            throw new AssertionError("expected IllegalStateException containing: " + messagePart);
        } catch (IllegalStateException e) {
            check(e.getMessage().contains(messagePart),
                    "expected error containing '" + messagePart + "' but got '" + e.getMessage() + "'");
        }
    }

    private static String questionBankCsv() {
        StringBuilder csv = new StringBuilder(HEADER).append('\n');
        for (BloomLevel level : BloomLevel.values()) {
            int required = 0;
            for (int slot = 1; slot <= BloomLevel.totalSlots(); slot++) {
                if (BloomLevel.forSlot(slot) == level) {
                    required++;
                }
            }

            int questionCount = Math.max(4, required + 1);
            String prefix = switch (level) {
                case REMEMBERING -> "R";
                case UNDERSTANDING -> "U";
                case APPLYING -> "A";
                case ANALYZING -> "N";
                case EVALUATING -> "E";
                case CREATING -> "C";
            };
            for (int i = 1; i <= questionCount; i++) {
                String id = prefix + i;
                csv.append(id).append(',').append(level.name()).append(',')
                        .append("Prompt ").append(id).append(',')
                        .append("Option A,").append("Option B,").append("Option C,").append("Option D,")
                        .append("1,").append("Hint ").append(id).append(',')
                        .append("Reading ").append(prefix).append(",12\n");
            }
        }
        return csv.toString();
    }
}
