package millionairemind;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Represents one multiple-choice question.
 *
 * The correct answer is stored as the actual answer text rather than
 * as a fixed option index. This allows the four choices to be shuffled
 * without changing which answer is correct.
 */
public final class Question {

    private final String id;
    private final BloomLevel level;
    private final String prompt;
    private final List<String> options;
    private final String correctAnswer;
    private final String hint;
    private final String sourceReading;
    private final String pageNumber;

    public Question(
            String id,
            BloomLevel level,
            String prompt,
            List<String> options,
            String correctAnswer,
            String hint,
            String sourceReading,
            String pageNumber
    ) {

        if (options == null || options.size() != 4) {
            throw new IllegalArgumentException(
                    "Question " + id
                            + " must have exactly 4 options."
            );
        }

        if (correctAnswer == null
                || correctAnswer.isBlank()) {

            throw new IllegalArgumentException(
                    "Question " + id
                            + " must have a correct answer."
            );
        }

        if (!options.contains(correctAnswer)) {
            throw new IllegalArgumentException(
                    "Correct answer for question "
                            + id
                            + " does not match any option."
            );
        }

        this.id = id;
        this.level = level;
        this.prompt = prompt;

        /*
         * Copy the list so the Question object owns
         * its option ordering.
         */
        this.options =
                new ArrayList<>(options);

        this.correctAnswer = correctAnswer;
        this.hint = hint;
        this.sourceReading = sourceReading;
        this.pageNumber = pageNumber;
    }

    public String id() {
        return id;
    }

    public BloomLevel level() {
        return level;
    }

    public String prompt() {
        return prompt;
    }

    public List<String> options() {
        return options;
    }

    /**
     * Returns the correct answer by CONTENT.
     *
     * This value does not change when choices are shuffled.
     */
    public String correctAnswer() {
        return correctAnswer;
    }

    /**
     * Finds the correct answer's CURRENT position.
     *
     * Because options can be shuffled, this must not be
     * stored as a permanent integer.
     */
    public int correctIndex() {

        int index =
                options.indexOf(correctAnswer);

        if (index < 0) {
            throw new IllegalStateException(
                    "Correct answer is missing from "
                            + "the options for question "
                            + id
            );
        }

        return index;
    }

    /**
     * Returns the actual correct answer text.
     */
    public String correctOptionText() {
        return correctAnswer;
    }

    /**
     * Randomizes the order of the four options.
     *
     * Uses Fisher-Yates shuffling.
     */
    public void shuffleOptions(
            RandomGenerator random
    ) {

        if (random == null) {
            throw new IllegalArgumentException(
                    "Random generator cannot be null."
            );
        }

        for (
                int i = options.size() - 1;
                i > 0;
                i--
        ) {

            int j =
                    random.nextInt(i + 1);

            String temp =
                    options.get(i);

            options.set(
                    i,
                    options.get(j)
            );

            options.set(
                    j,
                    temp
            );
        }
    }

    /**
     * Checks an answer using its CURRENT position.
     *
     * The selected position is translated into its
     * actual answer text, which is then compared with
     * the stored correct answer.
     */
    public boolean isCorrect(
            int chosenIndex
    ) {

        if (
                chosenIndex < 0
                        || chosenIndex >= options.size()
        ) {
            return false;
        }

        return options
                .get(chosenIndex)
                .equals(correctAnswer);
    }

    public String hint() {
        return hint;
    }

    public String sourceReading() {
        return sourceReading;
    }

    public String pageNumber() {
        return pageNumber;
    }

    public String citation() {

        if (
                pageNumber == null
                        || pageNumber.isBlank()
        ) {
            return sourceReading;
        }

        return sourceReading
                + ", p. "
                + pageNumber;
    }

    @Override
    public String toString() {

        return "["
                + id
                + "/"
                + level.displayName()
                + "] "
                + prompt;
    }
}