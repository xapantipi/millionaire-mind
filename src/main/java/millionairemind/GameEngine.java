package millionairemind;

import java.util.HashSet;
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

    private int currentSlot = 1; // 1-indexed, 1-15
    private Question currentQuestion;
    private boolean gameOver = false;

    public GameEngine(QuestionBank questionBank, RandomGenerator random, String playerName) {
        this.questionBank = questionBank;
        this.lifelineManager = new LifelineManager(random);
        this.session = new PlayerSession(playerName);
        drawCurrentQuestion();
    }

    private void drawCurrentQuestion() {
        BloomLevel level = BloomLevel.forSlot(currentSlot);
        this.currentQuestion = questionBank.drawQuestion(level, usedQuestionIds);
        usedQuestionIds.add(currentQuestion.id());
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
        this.currentQuestion = replacement;
        return replacement;
    }
}
