package millionairemind;

import java.util.HashSet;
import java.util.Set;

/** Dependency-free regression tests for game progression and terminal rules. */
public final class GameEngineTest {

    private GameEngineTest() {
    }

    public static void main(String[] args) throws Exception {
        dropsWrongAnswersToTheLastCheckpoint();
        losesBeforeTheFirstCheckpointAndWalksAwayAtTheCheckpointBank();
        switchesToAnUnusedQuestionAtTheSameLevel();
        lifelineVisibilityPreservesCombinedAndUntimedBehavior();
        awardsTheMillionAfterFifteenCorrectAnswers();
        System.out.println("GameEngineTest passed");
    }

    private static void dropsWrongAnswersToTheLastCheckpoint() throws Exception {
        GameEngine afterFirstCheckpoint = new GameEngine(
                BackendTestSupport.questionBank(), new java.util.Random(1), "Checkpoint Player");
        answerCorrectlyThrough(afterFirstCheckpoint, 5);
        BackendTestSupport.check(afterFirstCheckpoint.currentSlot() == 6,
                "a correct Q5 answer should advance to Q6");
        BackendTestSupport.check(afterFirstCheckpoint.currentPrize() == 4_000,
                "Q6 should be worth $4,000");
        BackendTestSupport.check(afterFirstCheckpoint.guaranteedBank() == 2_000,
                "Q6 should carry the Q5 checkpoint bank");

        Question q6 = afterFirstCheckpoint.currentQuestion();
        GameEngine.AnswerOutcome q6Outcome = afterFirstCheckpoint.answer(wrongIndex(q6));
        BackendTestSupport.check(q6Outcome == GameEngine.AnswerOutcome.WRONG_DROPPED_TO_CHECKPOINT,
                "a wrong Q6 answer should drop to the Q5 checkpoint");
        BackendTestSupport.check(afterFirstCheckpoint.session().bankedWinnings() == 2_000,
                "a wrong Q6 answer should bank $2,000");
        BackendTestSupport.check(afterFirstCheckpoint.session().history().size() == 6,
                "the wrong answer should be included in session history");
        BackendTestSupport.check(!afterFirstCheckpoint.session().history().get(5).correct,
                "the recorded Q6 answer should be wrong");
        BackendTestSupport.check(afterFirstCheckpoint.isGameOver()
                        && afterFirstCheckpoint.session().isGameOver(),
                "a wrong answer should end both the engine and session");
        BackendTestSupport.expectIllegalState(() -> afterFirstCheckpoint.answer(0), "already over");

        GameEngine afterSecondCheckpoint = new GameEngine(
                BackendTestSupport.questionBank(), new java.util.Random(2), "Second Checkpoint Player");
        answerCorrectlyThrough(afterSecondCheckpoint, 10);
        BackendTestSupport.check(afterSecondCheckpoint.currentSlot() == 11,
                "a correct Q10 answer should advance to Q11");
        BackendTestSupport.check(afterSecondCheckpoint.guaranteedBank() == 64_000,
                "Q11 should carry the Q10 checkpoint bank");
        Question q11 = afterSecondCheckpoint.currentQuestion();
        afterSecondCheckpoint.answer(wrongIndex(q11));
        BackendTestSupport.check(afterSecondCheckpoint.session().bankedWinnings() == 64_000,
                "a wrong Q11 answer should bank $64,000");
    }

    private static void losesBeforeTheFirstCheckpointAndWalksAwayAtTheCheckpointBank()
            throws Exception {
        GameEngine earlyLoss = new GameEngine(
                BackendTestSupport.questionBank(), new java.util.Random(3), "Early Player");
        GameEngine.AnswerOutcome outcome = earlyLoss.answer(wrongIndex(earlyLoss.currentQuestion()));
        BackendTestSupport.check(outcome == GameEngine.AnswerOutcome.WRONG_GAME_OVER,
                "a wrong answer before Q5 should end the game without a checkpoint");
        BackendTestSupport.check(earlyLoss.session().bankedWinnings() == 0,
                "a pre-checkpoint loss should bank $0");
        BackendTestSupport.check(!earlyLoss.session().walkedAway(),
                "a wrong answer should not be recorded as Walk Away");

        GameEngine earlyWalker = new GameEngine(
                BackendTestSupport.questionBank(), new java.util.Random(7), "Early Walker");
        earlyWalker.walkAway();
        BackendTestSupport.check(earlyWalker.session().walkedAway()
                        && earlyWalker.session().bankedWinnings() == 0,
                "walking away before the first checkpoint should bank $0");

        GameEngine walker = new GameEngine(
                BackendTestSupport.questionBank(), new java.util.Random(4), "Walker");
        answerCorrectlyThrough(walker, 7);
        BackendTestSupport.check(walker.currentSlot() == 8 && walker.guaranteedBank() == 2_000,
                "after Q7, the Q5 checkpoint should remain guaranteed");
        walker.walkAway();
        BackendTestSupport.check(walker.isGameOver() && walker.session().isGameOver(),
                "walking away should end the game and session");
        BackendTestSupport.check(walker.session().walkedAway(),
                "the session should record Walk Away");
        BackendTestSupport.check(walker.session().bankedWinnings() == 2_000,
                "walking away after Q7 should bank the Q5 checkpoint");
        BackendTestSupport.check(walker.session().history().size() == 7,
                "walking away should not add an unanswered question to history");
        BackendTestSupport.expectIllegalState(walker::walkAway, "already over");
    }

    private static void switchesToAnUnusedQuestionAtTheSameLevel() throws Exception {
        GameEngine engine = new GameEngine(
                BackendTestSupport.questionBank(), new java.util.Random(5), "Switcher");
        Question original = engine.currentQuestion();
        Question replacement = engine.switchQuestion();

        BackendTestSupport.check(!replacement.id().equals(original.id()),
                "switching should replace the current question");
        BackendTestSupport.check(replacement.level() == original.level()
                        && engine.currentLevel() == original.level(),
                "the replacement should stay at the current Bloom level");
        BackendTestSupport.check(engine.currentSlot() == 1,
                "switching should not advance the question slot");
        BackendTestSupport.check(!engine.lifelines().isAvailable(LifelineManager.Lifeline.SWITCH_QUESTION),
                "switching should consume its lifeline");
        BackendTestSupport.expectIllegalState(engine::switchQuestion, "already been used");

        engine.answer(replacement.correctIndex());
        BackendTestSupport.check(engine.currentSlot() == 2,
                "a correct answer after switching should advance normally");
        BackendTestSupport.check(!engine.currentQuestion().id().equals(original.id())
                        && !engine.currentQuestion().id().equals(replacement.id()),
                "the next slot should not reuse either the discarded or replacement question");
    }

    private static void awardsTheMillionAfterFifteenCorrectAnswers() throws Exception {
        GameEngine engine = new GameEngine(
                BackendTestSupport.questionBank(), new java.util.Random(6), "Winner");
        answerCorrectlyThrough(engine, PrizeLadder.totalSlots());

        BackendTestSupport.check(engine.isGameOver() && engine.session().isGameOver(),
                "a correct final answer should end the game");
        BackendTestSupport.check(engine.session().becameMillionaire(),
                "a correct final answer should mark the player as a millionaire");
        BackendTestSupport.check(!engine.session().walkedAway(),
                "winning should not be recorded as Walk Away");
        BackendTestSupport.check(engine.session().bankedWinnings() == 1_000_000,
                "a perfect run should bank $1,000,000");
        BackendTestSupport.check(engine.session().history().size() == PrizeLadder.totalSlots(),
                "the session should contain all 15 answers");
    }

    private static void lifelineVisibilityPreservesCombinedAndUntimedBehavior() throws Exception {
        GameEngine combined = new GameEngine(BackendTestSupport.questionBank(),
                BackendTestSupport.fixedRandom(0.2, 0), "Combined");
        combined.useFiftyFifty();
        BackendTestSupport.check(visibleCount(combined) == 2,
                "50:50 should leave two visible choices");
        BackendTestSupport.check(combined.useSpinTheWheel()
                        == LifelineManager.SpinOutcome.REVEAL_ONE_WRONG_OPTION
                        && visibleCount(combined) == 1,
                "Spin should remove the remaining wrong option after 50:50");
        combined.switchQuestion();
        BackendTestSupport.check(visibleCount(combined) == 4,
                "switching should restore all choices for the replacement question");
        BackendTestSupport.check(combined.usePhoneAFriend().equals(combined.currentQuestion().hint()),
                "Phone a Friend should return the replacement question's hint");

        GameEngine reverseOrder = new GameEngine(BackendTestSupport.questionBank(),
                BackendTestSupport.fixedRandom(0.2, 0), "Reverse");
        reverseOrder.useSpinTheWheel();
        BackendTestSupport.check(visibleCount(reverseOrder) == 3,
                "reveal-one spin should hide one wrong choice");
        reverseOrder.useFiftyFifty();
        BackendTestSupport.check(visibleCount(reverseOrder) == 2
                        && reverseOrder.isOptionVisible(0),
                "50:50 after Spin should replace the mask as the console previously did");

        GameEngine untimed = new GameEngine(BackendTestSupport.questionBank(),
                BackendTestSupport.fixedRandom(0.9, 0), "Untimed");
        BackendTestSupport.check(untimed.useSpinTheWheel()
                        == LifelineManager.SpinOutcome.GRANT_BONUS_TIME
                        && visibleCount(untimed) == 4,
                "bonus time should leave choices unchanged in untimed play");
    }

    private static int visibleCount(GameEngine engine) {
        int count = 0;
        for (int i = 0; i < 4; i++) if (engine.isOptionVisible(i)) count++;
        return count;
    }

    private static void answerCorrectlyThrough(GameEngine engine, int slotCount) {
        Set<String> seenQuestionIds = new HashSet<>();
        for (int slot = 1; slot <= slotCount; slot++) {
            BackendTestSupport.check(engine.currentSlot() == slot,
                    "expected current slot " + slot + " but got " + engine.currentSlot());
            BackendTestSupport.check(engine.currentLevel() == BloomLevel.forSlot(slot),
                    "slot " + slot + " should use its configured Bloom level");
            BackendTestSupport.check(seenQuestionIds.add(engine.currentQuestion().id()),
                    "a question should not be repeated during a run");
            GameEngine.AnswerOutcome outcome = engine.answer(engine.currentQuestion().correctIndex());
            BackendTestSupport.check(outcome == GameEngine.AnswerOutcome.CORRECT,
                    "the correct answer at Q" + slot + " should be accepted");
        }
    }

    private static int wrongIndex(Question question) {
        return (question.correctIndex() + 1) % question.options().size();
    }
}
