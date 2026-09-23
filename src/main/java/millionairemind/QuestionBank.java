package millionairemind;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Loads the compiled, human-authored question set into memory at startup,
 * grouped by Bloom's level. No network calls are made; the bank is fully
 * local and traceable to the course readings.
 *
 * Expected CSV columns (header row required):
 * id,level,prompt,optionA,optionB,optionC,optionD,correctIndex,hint,sourceReading,pageNumber
 *
 * Fields containing commas, quotes, or line breaks must be wrapped in double
 * quotes, with embedded quotes doubled (""), matching standard CSV quoting.
 * Quoted fields may contain line breaks, so one CSV record is not necessarily
 * one physical text line.
 *
 * The correct answer is stored on {@link Question} by CONTENT rather than by
 * the CSV's positional {@code correctIndex}, so the four displayed choices
 * can be shuffled per draw without losing track of which one is correct.
 */
public final class QuestionBank {

    private static final List<String> EXPECTED_HEADER = List.of(
            "id", "level", "prompt", "optionA", "optionB", "optionC", "optionD",
            "correctIndex", "hint", "sourceReading", "pageNumber");

    private final Map<BloomLevel, List<Question>> byLevel = new EnumMap<>(BloomLevel.class);
    private final RandomGenerator random;

    public QuestionBank(RandomGenerator random) {
        if (random == null) {
            throw new IllegalArgumentException("Random generator cannot be null.");
        }

        this.random = random;
        for (BloomLevel level : BloomLevel.values()) {
            byLevel.put(level, new ArrayList<>());
        }
    }

    /** Loads from a classpath resource, e.g. "/Question Bank.csv". */
    public static QuestionBank loadFromResource(String resourcePath, RandomGenerator random) throws IOException {
        QuestionBank bank = new QuestionBank(random);
        try (InputStream in = QuestionBank.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Question bank resource not found: " + resourcePath);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                bank.parse(reader);
            }
        }
        return bank;
    }

    /** Loads from a plain filesystem path. */
    public static QuestionBank loadFromFile(Path path, RandomGenerator random) throws IOException {
        QuestionBank bank = new QuestionBank(random);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            bank.parse(reader);
        }
        return bank;
    }

    private void parse(Reader reader) throws IOException {
        String csv = readAll(reader);
        if (!csv.isEmpty() && csv.charAt(0) == '\uFEFF') {
            csv = csv.substring(1);
        }

        CsvRecordReader records = new CsvRecordReader(csv);
        CsvRecord header = readNextNonBlankRecord(records);
        if (header == null) {
            throw new IOException("Question bank file is empty");
        }

        if (!EXPECTED_HEADER.equals(header.fields())) {
            throw invalidRecord(header, "header must be " + String.join(",", EXPECTED_HEADER));
        }

        Set<String> ids = new HashSet<>();
        CsvRecord record;
        while ((record = records.readRecord()) != null) {
            if (record.isBlank()) {
                continue;
            }
            parseQuestion(record, ids);
        }

        validateMinimumQuestionCounts();
    }

    private void parseQuestion(CsvRecord record, Set<String> ids) throws IOException {
        List<String> fields = record.fields();
        if (fields.size() != EXPECTED_HEADER.size()) {
            throw invalidRecord(record, "has " + fields.size() + " fields, expected " + EXPECTED_HEADER.size());
        }

        String id = requiredField(record, fields, 0, "id").trim();
        if (!ids.add(id)) {
            throw invalidRecord(record, "duplicate id '" + id + "'");
        }

        String levelText = requiredField(record, fields, 1, "level").trim();
        BloomLevel level;
        try {
            level = BloomLevel.fromCsvValue(levelText);
        } catch (IllegalArgumentException e) {
            throw invalidRecord(record, "invalid Bloom level '" + levelText + "'");
        }

        String prompt = requiredField(record, fields, 2, "prompt");
        List<String> options = List.of(
                requiredField(record, fields, 3, "optionA").trim(),
                requiredField(record, fields, 4, "optionB").trim(),
                requiredField(record, fields, 5, "optionC").trim(),
                requiredField(record, fields, 6, "optionD").trim());

        String correctIndexText = requiredField(record, fields, 7, "correctIndex").trim();
        int correctIndex;
        try {
            correctIndex = Integer.parseInt(correctIndexText);
        } catch (NumberFormatException e) {
            throw invalidRecord(record, "correctIndex must be an integer from 0 to 3");
        }
        if (correctIndex < 0 || correctIndex > 3) {
            throw invalidRecord(record, "correctIndex must be from 0 to 3");
        }

        String hint = requiredField(record, fields, 8, "hint");
        String sourceReading = requiredField(record, fields, 9, "sourceReading");
        String pageNumber = requiredField(record, fields, 10, "pageNumber");

        // Question stores the correct answer by content (not position) so its
        // options can be shuffled on every draw; translate the CSV's
        // positional correctIndex into that content once, here, at load time.
        String correctAnswer = options.get(correctIndex);

        try {
            byLevel.get(level).add(new Question(
                    id, level, prompt, options, correctAnswer, hint, sourceReading, pageNumber));
        } catch (IllegalArgumentException e) {
            throw invalidRecord(record, e.getMessage());
        }
    }

    private static String requiredField(CsvRecord record, List<String> fields,
                                         int index, String name) throws IOException {
        String value = fields.get(index);
        if (value.isBlank()) {
            throw invalidRecord(record, name + " is required");
        }
        return value;
    }

    private void validateMinimumQuestionCounts() throws IOException {
        for (BloomLevel level : BloomLevel.values()) {
            int required = 0;
            for (int slot = 1; slot <= BloomLevel.totalSlots(); slot++) {
                if (BloomLevel.forSlot(slot) == level) {
                    required++;
                }
            }
            int actual = countFor(level);
            if (actual < required) {
                throw new IOException("Question bank has " + actual + " question(s) for "
                        + level.displayName() + "; at least " + required + " required");
            }
        }
    }

    private static CsvRecord readNextNonBlankRecord(CsvRecordReader records) throws IOException {
        CsvRecord record;
        while ((record = records.readRecord()) != null) {
            if (!record.isBlank()) {
                return record;
            }
        }
        return null;
    }

    private static IOException invalidRecord(CsvRecord record, String message) {
        return new IOException("Question bank record at line " + record.startLine() + ": " + message);
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder contents = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            contents.append(buffer, 0, read);
        }
        return contents.toString();
    }

    public int countFor(BloomLevel level) {
        return byLevel.get(level).size();
    }

    public int totalCount() {
        return byLevel.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Draws a random, unused question from the given level and immediately
     * shuffles its choices, so the same question can resurface later in a
     * replay with a different option order.
     *
     * @param level     the Bloom's level to draw from
     * @param usedIds   ids already used this session (excluded from the draw)
     * @return the drawn question, with freshly shuffled options
     * @throws IllegalStateException if every question in that level has been used
     */
    public Question drawQuestion(BloomLevel level, Set<String> usedIds) {
        List<Question> pool = new ArrayList<>(byLevel.get(level));
        pool.removeIf(question -> usedIds.contains(question.id()));
        if (pool.isEmpty()) {
            throw new IllegalStateException("No unused questions remain for level " + level.displayName());
        }

        int index = random.nextInt(pool.size());
        Question question = pool.get(index);
        question.shuffleOptions(random);
        return question;
    }

    /**
     * Draws an alternate unused question from the same level, for the
     * "Switch the Question" lifeline. Equivalent to drawQuestion but named
     * distinctly to match the architecture in the proposal.
     */
    public Question drawAlternate(BloomLevel level, Set<String> excludeSet) {
        return drawQuestion(level, excludeSet);
    }

    private static final class CsvRecord {
        private final List<String> fields;
        private final int startLine;
        private final boolean containsQuotedField;

        private CsvRecord(List<String> fields, int startLine, boolean containsQuotedField) {
            this.fields = List.copyOf(fields);
            this.startLine = startLine;
            this.containsQuotedField = containsQuotedField;
        }

        private List<String> fields() {
            return fields;
        }

        private int startLine() {
            return startLine;
        }

        private boolean isBlank() {
            return !containsQuotedField && fields.size() == 1 && fields.get(0).isBlank();
        }
    }

    /** Reads logical CSV records, so quoted fields may contain commas or line breaks. */
    private static final class CsvRecordReader {
        private final String csv;
        private int position;
        private int line = 1;

        private CsvRecordReader(String csv) {
            this.csv = csv;
        }

        private CsvRecord readRecord() throws IOException {
            if (position >= csv.length()) {
                return null;
            }

            int startLine = line;
            List<String> fields = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            boolean afterClosingQuote = false;
            boolean fieldStarted = false;
            boolean containsQuotedField = false;

            while (position < csv.length()) {
                char c = csv.charAt(position);

                if (inQuotes) {
                    if (c == '"') {
                        position++;
                        if (position < csv.length() && csv.charAt(position) == '"') {
                            current.append('"');
                            position++;
                        } else {
                            inQuotes = false;
                            afterClosingQuote = true;
                        }
                    } else if (c == '\r') {
                        current.append('\r');
                        position++;
                        if (position < csv.length() && csv.charAt(position) == '\n') {
                            current.append('\n');
                            position++;
                        }
                        line++;
                    } else if (c == '\n') {
                        current.append('\n');
                        position++;
                        line++;
                    } else {
                        current.append(c);
                        position++;
                    }
                    continue;
                }

                if (afterClosingQuote) {
                    if (c == ',') {
                        fields.add(current.toString());
                        current.setLength(0);
                        fieldStarted = false;
                        afterClosingQuote = false;
                        position++;
                    } else if (c == '\r' || c == '\n') {
                        fields.add(current.toString());
                        finishRecord(c);
                        return new CsvRecord(fields, startLine, containsQuotedField);
                    } else {
                        throw new IOException("Malformed CSV record at line " + startLine
                                + ": unexpected character after closing quote");
                    }
                    continue;
                }

                if (c == '"') {
                    if (fieldStarted) {
                        throw new IOException("Malformed CSV record at line " + startLine
                                + ": quote inside an unquoted field");
                    }
                    inQuotes = true;
                    fieldStarted = true;
                    containsQuotedField = true;
                    position++;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                    fieldStarted = false;
                    position++;
                } else if (c == '\r' || c == '\n') {
                    fields.add(current.toString());
                    finishRecord(c);
                    return new CsvRecord(fields, startLine, containsQuotedField);
                } else {
                    current.append(c);
                    fieldStarted = true;
                    position++;
                }
            }

            if (inQuotes) {
                throw new IOException("Malformed CSV record at line " + startLine
                        + ": unterminated quoted field");
            }

            fields.add(current.toString());
            return new CsvRecord(fields, startLine, containsQuotedField);
        }

        private void finishRecord(char lineEnding) {
            position++;
            if (lineEnding == '\r' && position < csv.length() && csv.charAt(position) == '\n') {
                position++;
            }
            line++;
        }
    }
}
