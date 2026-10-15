package millionairemind;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.random.RandomGenerator;

/** Owns progression, lifeline effects, timing, and checkpoint payouts. */
public final class GameEngine {

    public enum AnswerOutcome { CORRECT, WRONG_DROPPED_TO_CHECKPOINT, WRONG_GAME_OVER, TIMEOUT_DROPPED_TO_CHECKPOINT, TIMEOUT_GAME_OVER }
    
    public static final int TIMER_SECONDS = 30;
    public static final int BONUS_TIME_SECONDS = 15;

    private final QuestionBank questionBank;
    private final LifelineManager lifelineManager;
    private final PlayerSession session;
    private final Set<String> usedQuestionIds = new HashSet<>();
    private final boolean[] visibleOptions = new boolean[4];
    private final boolean timedMode;
    private int secondsRemaining;

    private int currentSlot = 1; // 1-indexed, 1-15
    private Question currentQuestion;

    private boolean gameOver = false;

    public GameEngine(QuestionBank questionBank, RandomGenerator random, String playerName) {
        this(questionBank, random, playerName, false);
    }

    public GameEngine(QuestionBank questionBank, RandomGenerator random, String playerName, boolean timedMode) {
        this.questionBank = questionBank;
        this.lifelineManager = new LifelineManager(random);
        this.session = new PlayerSession(playerName);
        this.timedMode = timedMode;
        resetVisibleOptions();
        drawCurrentQuestion();
    }

    private void drawCurrentQuestion() {
        BloomLevel level = BloomLevel.forSlot(currentSlot);
        this.currentQuestion = questionBank.drawQuestion(level, usedQuestionIds);
        usedQuestionIds.add(currentQuestion.id());
        secondsRemaining = timedMode ? TIMER_SECONDS : 0;
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
    public boolean isTimedMode() { return timedMode; }
    public int secondsRemaining() { return secondsRemaining; }

    /** Applies elapsed gameplay seconds; stopped UI screens must not advance time. */
    public void advanceTime(int seconds) {
        if (seconds < 0) throw new IllegalArgumentException("Elapsed seconds cannot be negative");
        if (!timedMode || gameOver) return;
        secondsRemaining = Math.max(0, secondsRemaining - seconds);
        if (secondsRemaining == 0) timeExpired();
    }

    public boolean isOptionVisible(int index) { return visibleOptions[index]; }

    /** Intersect with the existing mask so earlier lifelines stay effective. */
    public void useFiftyFifty() {
        ensureActive();
        int[] kept = lifelineManager.fiftyFifty(currentQuestion);
        for (int i = 0; i < visibleOptions.length; i++) {
            visibleOptions[i] &= i == kept[0] || i == kept[1];
        }
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
            case GRANT_BONUS_TIME -> {
                if (timedMode) secondsRemaining += BONUS_TIME_SECONDS;
            }
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
        if (chosenIndex < 0 || chosenIndex >= visibleOptions.length || !visibleOptions[chosenIndex]) {
            throw new IllegalArgumentException("Choose a visible answer from A to D");
        }
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

    public AnswerOutcome timeExpired() {
        if (gameOver) throw new IllegalStateException("Game is already over");
        if (!timedMode) throw new IllegalStateException("The game is untimed");
        secondsRemaining = 0;
        session.recordAnswer(currentSlot, currentQuestion, -1, false);
        long banked = PrizeLadder.checkpointBankFor(currentSlot - 1);
        session.setBankedWinnings(banked);
        session.markGameOver();
        gameOver = true;
        return banked > 0 ? AnswerOutcome.TIMEOUT_DROPPED_TO_CHECKPOINT : AnswerOutcome.TIMEOUT_GAME_OVER;
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
        if (!lifelineManager.isAvailable(LifelineManager.Lifeline.SWITCH_QUESTION)) {
            throw new IllegalStateException("SWITCH_QUESTION has already been used this game");
        }
        BloomLevel level = currentLevel();
        if (questionBank.unusedCount(level, usedQuestionIds) <= remainingSlotsForLevel(level)) {
            throw new IllegalStateException("No spare question remains for this level");
        }
        Question replacement = questionBank.drawAlternate(level, usedQuestionIds);
        lifelineManager.useSwitchQuestion();
        usedQuestionIds.add(replacement.id());
        this.currentQuestion = replacement;
        resetVisibleOptions();
        secondsRemaining = timedMode ? TIMER_SECONDS : 0;
        return replacement;
    }

    private int remainingSlotsForLevel(BloomLevel level) {
        int remaining = 0;
        for (int slot = currentSlot + 1; slot <= PrizeLadder.totalSlots(); slot++) {
            if (BloomLevel.forSlot(slot) == level) remaining++;
        }
        return remaining;
    }
}
