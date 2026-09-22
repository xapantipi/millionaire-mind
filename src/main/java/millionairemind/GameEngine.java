package millionairemind;

import java.util.HashSet;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Owns game state and enforces the game rules.
 *
 * Dynamic question behavior:
 *
 * 1. A random unused question is selected from
 *    the required Bloom level.
 *
 * 2. QuestionBank shuffles its choices immediately
 *    after selection.
 *
 * 3. Question stores the correct answer by CONTENT,
 *    so answer checking remains correct after shuffling.
 */
public final class GameEngine {

    public enum AnswerOutcome {
        CORRECT,
        WRONG_DROPPED_TO_CHECKPOINT,
        WRONG_GAME_OVER
    }

    private final QuestionBank questionBank;
    private final LifelineManager lifelineManager;
    private final PlayerSession session;

    private final Set<String> usedQuestionIds =
            new HashSet<>();

    private final RandomGenerator random;

    private int currentSlot = 1;

    private Question currentQuestion;

    private boolean gameOver = false;

    public GameEngine(
            QuestionBank questionBank,
            RandomGenerator random,
            String playerName
    ) {

        if (questionBank == null) {
            throw new IllegalArgumentException(
                    "Question bank cannot be null."
            );
        }

        if (random == null) {
            throw new IllegalArgumentException(
                    "Random generator cannot be null."
            );
        }

        this.questionBank =
                questionBank;

        this.random =
                random;

        this.lifelineManager =
                new LifelineManager(
                        random
                );

        this.session =
                new PlayerSession(
                        playerName
                );

        drawCurrentQuestion();
    }

    /**
     * Selects a random unused question from
     * the Bloom level assigned to the current slot.
     *
     * QuestionBank also shuffles the choices.
     */
    private void drawCurrentQuestion() {

        BloomLevel level =
                BloomLevel.forSlot(
                        currentSlot
                );

        currentQuestion =
                questionBank.drawQuestion(
                        level,
                        usedQuestionIds
                );

        usedQuestionIds.add(
                currentQuestion.id()
        );
    }

    public int currentSlot() {
        return currentSlot;
    }

    public Question currentQuestion() {
        return currentQuestion;
    }

    public BloomLevel currentLevel() {

        return BloomLevel.forSlot(
                currentSlot
        );
    }

    public long currentPrize() {

        return PrizeLadder.prizeFor(
                currentSlot
        );
    }

    public long guaranteedBank() {

        return PrizeLadder
                .checkpointBankFor(
                        currentSlot - 1
                );
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public LifelineManager lifelines() {
        return lifelineManager;
    }

    public PlayerSession session() {
        return session;
    }

    public QuestionBank questionBank() {
        return questionBank;
    }

    /**
     * Submits the currently displayed choice.
     *
     * chosenIndex refers to the CURRENT shuffled
     * order of the options.
     */
    public AnswerOutcome answer(
            int chosenIndex
    ) {

        if (gameOver) {

            throw new IllegalStateException(
                    "Game is already over."
            );
        }

        boolean correct =
                currentQuestion.isCorrect(
                        chosenIndex
                );

        session.recordAnswer(
                currentSlot,
                currentQuestion,
                chosenIndex,
                correct
        );

        if (correct) {

            if (
                    currentSlot
                            == PrizeLadder
                                    .totalSlots()
            ) {

                session.setBankedWinnings(
                        PrizeLadder.prizeFor(
                                currentSlot
                        )
                );

                session.markMillionaire();

                gameOver = true;

                return AnswerOutcome.CORRECT;
            }

            currentSlot++;

            drawCurrentQuestion();

            return AnswerOutcome.CORRECT;
        }

        long banked =
                PrizeLadder
                        .checkpointBankFor(
                                currentSlot - 1
                        );

        session.setBankedWinnings(
                banked
        );

        session.markGameOver();

        gameOver = true;

        return banked > 0
                ? AnswerOutcome
                        .WRONG_DROPPED_TO_CHECKPOINT
                : AnswerOutcome
                        .WRONG_GAME_OVER;
    }

    /**
     * Walk away from the current game.
     */
    public void walkAway() {

        if (gameOver) {

            throw new IllegalStateException(
                    "Game is already over."
            );
        }

        long banked =
                PrizeLadder
                        .checkpointBankFor(
                                currentSlot - 1
                        );

        session.setBankedWinnings(
                banked
        );

        session.markWalkedAway();

        gameOver = true;
    }

    /**
     * Switch the current question with another
     * unused question from the SAME Bloom level.
     *
     * QuestionBank also shuffles the new choices.
     */
    public Question switchQuestion() {

        if (gameOver) {

            throw new IllegalStateException(
                    "Game is already over."
            );
        }

        lifelineManager
                .useSwitchQuestion();

        BloomLevel level =
                currentLevel();

        Question replacement =
                questionBank.drawAlternate(
                        level,
                        usedQuestionIds
                );

        usedQuestionIds.add(
                replacement.id()
        );

        currentQuestion =
                replacement;

        return replacement;
    }
}