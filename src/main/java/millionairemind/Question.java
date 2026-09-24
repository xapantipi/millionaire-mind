package millionairemind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * A single, source-cited multiple-choice question tagged with a Bloom's
 * Taxonomy level, per Section 4 of the proposal.
 */
public final class Question {

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
        this.options = options;
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
    public String pageNumber() { return pageNumber; }

    public boolean isCorrect(int chosenIndex) {
        return chosenIndex == correctIndex;
    }

    /**
     * Returns a copy of this question with its four options presented in a
     * random order, so the correct choice is not always tied to the slot the
     * CSV happened to declare it in (proposal requirement: choice order must
     * be dynamic, not just question selection). The correct option's text
     * travels with it, so {@link #correctIndex()} and {@link #isCorrect(int)}
     * on the returned copy stay accurate for whatever position it lands on.
     */
    public Question shuffledOptions(RandomGenerator random) {
        List<String> newOptions = new ArrayList<>(options);
        List<Integer> originalIndices = new ArrayList<>(List.of(0, 1, 2, 3));
        for (int i = newOptions.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(newOptions, i, j);
            Collections.swap(originalIndices, i, j);
        }
        int newCorrectIndex = originalIndices.indexOf(correctIndex);
        return new Question(id, level, prompt, newOptions, newCorrectIndex, hint, sourceReading, pageNumber);
    }

    public String citation() {
        return sourceReading + ", p. " + pageNumber;
    }

    @Override
    public String toString() {
        return "[" + id + "/" + level.displayName() + "] " + prompt;
    }
}
