package millionairemind;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Owns game state (current question index, prize ladder position,
 * checkpoints, lifelines used) and enforces the rules in Section 3 of the
 * proposal: 15 questions across six Bloom's tiers, two guaranteed
 * checkpoints (Q5, Q10), drop-to-last-checkpoint on a wrong answer, and
 * walk-away banking.
 */
public final class GameEngine {

    public enum AnswerOutcome { CORRECT, WRONG_DROPPED_TO_CHECKPOINT, WRONG_GAME_OVER }

    private final QuestionBank questionBank;
    private final LifelineManager lifelineManager;
    private final PlayerSession session;
    private final Set<String> usedQuestionIds = new HashSet<>();
    private final boolean[] visibleOptions = new boolean[4];
    private final RandomGenerator choiceShuffleRandom;

    private int currentSlot = 1; // 1-indexed, 1-15
    private Question currentQuestion;
    private boolean gameOver = false;

    public GameEngine(QuestionBank questionBank, RandomGenerator random, String playerName) {
        this.questionBank = questionBank;
        this.lifelineManager = new LifelineManager(random);
        this.session = new PlayerSession(playerName);
        // A dedicated, independently-seeded generator so shuffling choice
        // order never shares (and skews) the exact random draws that the
        // lifelines rely on.
        this.choiceShuffleRandom = new Random(random.nextLong());
        resetVisibleOptions();
        drawCurrentQuestion();
    }

    private void drawCurrentQuestion() {
        BloomLevel level = BloomLevel.forSlot(currentSlot);
        Question drawn = questionBank.drawQuestion(level, usedQuestionIds);
        usedQuestionIds.add(drawn.id());
        this.currentQuestion = drawn.shuffledOptions(choiceShuffleRandom);
    }

    public int currentSlot() { return currentSlot; }
    public Question currentQuestion() { return currentQuestion; }
    public BloomLevel currentLevel() { return BloomLevel.forSlot(currentSlot); }
    public long currentPrize() { return PrizeLadder.prizeFor(currentSlot); }
    public long guaranteedBank() { return PrizeLadder.checkpointBankFor(currentSlot - 1); }
    public boolean isGameOver() { return gameOver; }
    public LifelineManager lifelines() { return lifelineManager; }
    public PlayerSession session() { return session; }
    public QuestionBank questionBank() { return questionBank; }

    public boolean isOptionVisible(int index) { return visibleOptions[index]; }

    /** 50:50 replaces the current option mask, matching the console behavior. */
    public void useFiftyFifty() {
        ensureActive();
        int[] kept = lifelineManager.fiftyFifty(currentQuestion);
        Arrays.fill(visibleOptions, false);
        for (int index : kept) visibleOptions[index] = true;
    }

    /** Applies the visual option effect; bonus time has no effect in untimed play. */
    public LifelineManager.SpinOutcome useSpinTheWheel() {
        ensureActive();
        LifelineManager.SpinOutcome outcome = lifelineManager.spinTheWheel();
        switch (outcome) {
            case REVEAL_ONE_WRONG_OPTION -> {
                for (int i = 0; i < visibleOptions.length; i++) {
                    if (visibleOptions[i] && i != currentQuestion.correctIndex()) {
                        visibleOptions[i] = false;
                        break;
                    }
                }
            }
            case REDUCE_TWO_OPTIONS -> {
                int correct = currentQuestion.correctIndex();
                int kept = correct;
                for (int i = 0; i < visibleOptions.length; i++) {
                    if (i != correct && visibleOptions[i]) {
                        kept = i;
                        break;
                    }
                }
                Arrays.fill(visibleOptions, false);
                visibleOptions[correct] = true;
                visibleOptions[kept] = true;
            }
            case GRANT_BONUS_TIME -> { /* Timer mode is not active. */ }
        }
        return outcome;
    }

    public String usePhoneAFriend() {
        ensureActive();
        return lifelineManager.phoneAFriend(currentQuestion);
    }

    private void resetVisibleOptions() {
        Arrays.fill(visibleOptions, true);
    }

    private void ensureActive() {
        if (gameOver) throw new IllegalStateException("Game is already over");
    }

    /**
     * Submits an answer for the current question and advances state.
     * @param chosenIndex 0-based index (0=A, 1=B, 2=C, 3=D)
     */
    public AnswerOutcome answer(int chosenIndex) {
        if (gameOver) throw new IllegalStateException("Game is already over");
        boolean correct = currentQuestion.isCorrect(chosenIndex);
        session.recordAnswer(currentSlot, currentQuestion, chosenIndex, correct);

        if (correct) {
            if (currentSlot == PrizeLadder.totalSlots()) {
                session.setBankedWinnings(PrizeLadder.prizeFor(currentSlot));
                session.markMillionaire();
                gameOver = true;
                return AnswerOutcome.CORRECT;
            }
            currentSlot++;
            drawCurrentQuestion();
            resetVisibleOptions();
            return AnswerOutcome.CORRECT;
        } else {
            long banked = PrizeLadder.checkpointBankFor(currentSlot - 1);
            session.setBankedWinnings(banked);
            session.markGameOver();
            gameOver = true;
            return banked > 0 ? AnswerOutcome.WRONG_DROPPED_TO_CHECKPOINT : AnswerOutcome.WRONG_GAME_OVER;
        }
    }

    /** Banks current guaranteed winnings and ends the game (Section 3, "Walk Away"). */
    public void walkAway() {
        if (gameOver) throw new IllegalStateException("Game is already over");
        long banked = PrizeLadder.checkpointBankFor(currentSlot - 1);
        session.setBankedWinnings(banked);
        session.markWalkedAway();
        gameOver = true;
    }

    /**
     * "Switch the Question" lifeline: discards the current question and
     * draws another unused one from the same Bloom's level.
     */
    public Question switchQuestion() {
        if (gameOver) throw new IllegalStateException("Game is already over");
        lifelineManager.useSwitchQuestion();
        BloomLevel level = currentLevel();
        Question replacement = questionBank.drawAlternate(level, usedQuestionIds);
        usedQuestionIds.add(replacement.id());
        this.currentQuestion = replacement.shuffledOptions(choiceShuffleRandom);
        resetVisibleOptions();
        return this.currentQuestion;
    }
}
