package millionairemind;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Loads the compiled, human-authored question set into memory at startup,
 * grouped by Bloom's level. No network calls are made; the bank is fully
 * local and traceable to the course readings (Section 6 of the proposal).
 *
 * Expected CSV columns (header row required):
 * id,level,prompt,optionA,optionB,optionC,optionD,correctIndex,hint,sourceReading,pageNumber
 *
 * Fields containing commas or quotes must be wrapped in double quotes, with
 * embedded quotes doubled ("" ), matching standard CSV quoting.
 */
public final class QuestionBank {

    private final Map<BloomLevel, List<Question>> byLevel = new EnumMap<>(BloomLevel.class);
    private final RandomGenerator random;

    public QuestionBank(RandomGenerator random) {
        this.random = random;
        for (BloomLevel level : BloomLevel.values()) {
            byLevel.put(level, new ArrayList<>());
        }
    }

    /** Loads from a classpath resource, e.g. "/questions.csv". */
    public static QuestionBank loadFromResource(String resourcePath, RandomGenerator random) throws IOException {
        QuestionBank bank = new QuestionBank(random);
        try (InputStream in = QuestionBank.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Question bank resource not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                bank.parse(reader);
            }
        }
        return bank;
    }

    /** Loads from a plain filesystem path. */
    public static QuestionBank loadFromFile(Path path, RandomGenerator random) throws IOException {
        QuestionBank bank = new QuestionBank(random);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            bank.parse(reader);
        }
        return bank;
    }

    private void parse(BufferedReader reader) throws IOException {
        String header = reader.readLine();
        if (header == null) {
            throw new IOException("Question bank file is empty");
        }
        String line;
        int lineNo = 1;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            if (line.isBlank()) continue;
            List<String> fields = parseCsvLine(line);
            if (fields.size() != 11) {
                throw new IOException("Line " + lineNo + " has " + fields.size() + " fields, expected 11");
            }
            String id = fields.get(0).trim();
            BloomLevel level = BloomLevel.valueOf(fields.get(1).trim().toUpperCase());
            String prompt = fields.get(2);
            List<String> options = Arrays.asList(fields.get(3), fields.get(4), fields.get(5), fields.get(6));
            int correctIndex = Integer.parseInt(fields.get(7).trim());
            String hint = fields.get(8);
            String sourceReading = fields.get(9);
            String pageNumber = fields.get(10);

            Question q = new Question(id, level, prompt, options, correctIndex, hint, sourceReading, pageNumber);
            byLevel.get(level).add(q);
        }
    }

    /** Minimal RFC4180-ish CSV line parser supporting quoted fields with embedded commas/quotes. */
    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields;
    }

    public int countFor(BloomLevel level) {
        return byLevel.get(level).size();
    }

    public int totalCount() {
        return byLevel.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Draws a random, unused question from the given level.
     * @param level     the Bloom's level to draw from
     * @param usedIds   ids already used this session (excluded from the draw)
     * @return the drawn question
     * @throws IllegalStateException if every question in that level has been used
     */
    public Question drawQuestion(BloomLevel level, Set<String> usedIds) {
        List<Question> pool = new ArrayList<>(byLevel.get(level));
        pool.removeIf(q -> usedIds.contains(q.id()));
        if (pool.isEmpty()) {
            throw new IllegalStateException("No unused questions remain for level " + level.displayName());
        }
        int index = random.nextInt(pool.size());
        return pool.get(index);
    }

    /**
     * Draws an alternate unused question from the same level, for the
     * "Switch the Question" lifeline. Equivalent to drawQuestion but named
     * distinctly to match the architecture in the proposal.
     */
    public Question drawAlternate(BloomLevel level, Set<String> excludeSet) {
        return drawQuestion(level, excludeSet);
    }
}
