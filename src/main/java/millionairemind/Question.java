package millionairemind;

import java.util.List;

/** An immutable, source-cited question tagged with a Bloom level. */
public final class Question {
    private static final java.util.regex.Pattern MODULE = java.util.regex.Pattern.compile(
            "^Module\\s+(\\d+)\\s*/", java.util.regex.Pattern.CASE_INSENSITIVE);

    private final String id;
    private final BloomLevel level;
    private final String prompt;
    private final List<String> options; // exactly 4, A-D
    private final int correctIndex;     // 0-based index into options
    private final String hint;
    private final String sourceReading;
    private final String pageNumber;

    public Question(String id, BloomLevel level, String prompt, List<String> options,
                     int correctIndex, String hint, String sourceReading, String pageNumber) {
        if (options == null || options.size() != 4) {
            throw new IllegalArgumentException("Question " + id + " must have exactly 4 options");
        }
        if (correctIndex < 0 || correctIndex > 3) {
            throw new IllegalArgumentException("Question " + id + " has an out-of-range correct index");
        }
        this.id = id;
        this.level = level;
        this.prompt = prompt;
        this.options = List.copyOf(options);
        this.correctIndex = correctIndex;
        this.hint = hint;
        this.sourceReading = sourceReading;
        this.pageNumber = pageNumber;
    }

    public String id() { return id; }
    public BloomLevel level() { return level; }
    public String prompt() { return prompt; }
    public List<String> options() { return options; }
    public int correctIndex() { return correctIndex; }
    public String correctOptionText() { return options.get(correctIndex); }
    public String hint() { return hint; }
    public String sourceReading() { return sourceReading; }
    public String module() {
        var match = MODULE.matcher(sourceReading.trim());
        return match.find() ? "Module " + match.group(1).replaceFirst("^0+(?!$)", "") : "Unspecified";
    }
    public String pageNumber() { return pageNumber; }

    public boolean isCorrect(int chosenIndex) {
        return chosenIndex == correctIndex;
    }

    public String citation() {
        return sourceReading + (pageNumber.startsWith("PDF p") ? ", " : ", p. ") + pageNumber;
    }

    @Override
    public String toString() {
        return "[" + id + "/" + level.displayName() + "] " + prompt;
    }
}
