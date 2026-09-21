package millionairemind;

import java.util.EnumSet;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Tracks and applies the four reworked lifelines described in Section 5:
 * 50:50, Spin the Wheel, Switch the Question, and Phone a Friend.
 * Each lifeline may be used at most once per game.
 */
public final class LifelineManager {

    public enum Lifeline { FIFTY_FIFTY, SPIN_THE_WHEEL, SWITCH_QUESTION, PHONE_A_FRIEND }

    /** Outcomes a Spin the Wheel result can land on. */
    public enum SpinOutcome {
        REVEAL_ONE_WRONG_OPTION,
        REDUCE_TWO_OPTIONS,
        GRANT_BONUS_TIME
    }

    private final Set<Lifeline> used = EnumSet.noneOf(Lifeline.class);
    private final RandomGenerator random;

    public LifelineManager(RandomGenerator random) {
        this.random = random;
    }

    public boolean isAvailable(Lifeline lifeline) {
        return !used.contains(lifeline);
    }

    public Set<Lifeline> remaining() {
        Set<Lifeline> all = EnumSet.allOf(Lifeline.class);
        all.removeAll(used);
        return all;
    }

    private void markUsed(Lifeline lifeline) {
        if (used.contains(lifeline)) {
            throw new IllegalStateException(lifeline + " has already been used this game");
        }
        used.add(lifeline);
    }

    /**
     * 50:50 - removes two incorrect options. Returns the indices (0-3) that
     * should remain visible: the correct index plus one randomly-kept wrong one.
     */
    public int[] fiftyFifty(Question question) {
        markUsed(Lifeline.FIFTY_FIFTY);
        int correct = question.correctIndex();
        int keptWrong;
        do {
            keptWrong = random.nextInt(4);
        } while (keptWrong == correct);
        int[] remainingIdx = {correct, keptWrong};
        java.util.Arrays.sort(remainingIdx);
        return remainingIdx;
    }

    /**
     * Spin the Wheel - a random, weighted assist replacing "Ask the Audience".
     * Weights: 45% reveal one wrong option, 35% reduce two options (same
     * effect as 50:50 but doesn't consume that lifeline), 20% grant bonus time.
     */
    public SpinOutcome spinTheWheel() {
        markUsed(Lifeline.SPIN_THE_WHEEL);
        double roll = random.nextDouble();
        if (roll < 0.45) return SpinOutcome.REVEAL_ONE_WRONG_OPTION;
        if (roll < 0.80) return SpinOutcome.REDUCE_TWO_OPTIONS;
        return SpinOutcome.GRANT_BONUS_TIME;
    }

    /** Marks Switch the Question as used. The actual re-draw happens via QuestionBank.drawAlternate. */
    public void useSwitchQuestion() {
        markUsed(Lifeline.SWITCH_QUESTION);
    }

    /** Phone a Friend - reveals the pre-written hint authored alongside the question. */
    public String phoneAFriend(Question question) {
        markUsed(Lifeline.PHONE_A_FRIEND);
        return question.hint();
    }
}
