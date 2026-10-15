package millionairemind;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;

/** Regression coverage for the finalization fixes; no clock sleeps needed. */
public final class GameRulesTest {
    public static void main(String[] args) throws Exception {
        timersAndCheckpoints();
        bonusAndSwitchReset();
        combinedLifelinesNeverRestoreOptions();
        rejectsInvalidAnswers();
        switchReservesEnoughQuestions();
        shufflingPreservesAnswerIdentity();
        System.out.println("GameRulesTest passed");
    }

    private static GameEngine game(boolean timed, double spin) throws Exception {
        return new GameEngine(BackendTestSupport.questionBank(),
                BackendTestSupport.fixedRandom(spin, 0), "Rules", timed);
    }

    private static void timersAndCheckpoints() throws Exception {
        for (int slot : new int[]{1, 5, 6, 10, 11, 15}) {
            GameEngine engine = game(true, 0.2);
            while (engine.currentSlot() < slot) engine.answer(engine.currentQuestion().correctIndex());
            engine.advanceTime(29);
            check(!engine.isGameOver() && engine.secondsRemaining() == 1, "29 seconds must leave 1 second");
            engine.advanceTime(1);
            check(engine.isGameOver() && engine.session().history().size() == slot, "timeout must record once");
            check(engine.session().bankedWinnings() == PrizeLadder.checkpointBankFor(slot - 1),
                    "timeout must pay only the passed checkpoint");
            check(engine.session().history().get(slot - 1).chosenIndex == -1, "timeout has no chosen option");
            engine.advanceTime(100);
            BackendTestSupport.expectIllegalState(engine::timeExpired, "already over");
            BackendTestSupport.expectIllegalState(() -> engine.answer(0), "already over");
            check(engine.session().history().size() == slot, "late callbacks must not duplicate results");
        }
        GameEngine untimed = game(false, 0.9);
        untimed.advanceTime(1000);
        untimed.useSpinTheWheel();
        check(!untimed.isGameOver() && untimed.secondsRemaining() == 0, "untimed games have no countdown");
        BackendTestSupport.expectIllegalState(untimed::timeExpired, "untimed");
    }

    private static void bonusAndSwitchReset() throws Exception {
        GameEngine engine = game(true, 0.9);
        engine.advanceTime(10);
        engine.useSpinTheWheel();
        check(engine.secondsRemaining() == 35, "bonus must add 15 to the remaining time");
        engine.switchQuestion();
        check(engine.secondsRemaining() == 30, "switch must reset rather than carry a bonus");
        engine.advanceTime(20);
        engine.answer(engine.currentQuestion().correctIndex());
        check(engine.secondsRemaining() == 30, "advancing must reset the timer");
        engine.walkAway();
        engine.advanceTime(100);
        check(engine.session().history().size() == 1, "walk away must stop timeout recording");
    }

    private static void combinedLifelinesNeverRestoreOptions() throws Exception {
        for (double spin : new double[]{0.2, 0.6}) {
            for (boolean fiftyFirst : new boolean[]{true, false}) {
                GameEngine engine = game(false, spin);
                if (fiftyFirst) engine.useFiftyFifty(); else engine.useSpinTheWheel();
                boolean[] before = new boolean[4];
                for (int i = 0; i < 4; i++) before[i] = engine.isOptionVisible(i);
                if (fiftyFirst) engine.useSpinTheWheel(); else engine.useFiftyFifty();
                for (int i = 0; i < 4; i++) check(before[i] || !engine.isOptionVisible(i), "hidden option restored");
                check(engine.isOptionVisible(engine.currentQuestion().correctIndex()), "correct answer removed");
            }
        }
    }

    private static void rejectsInvalidAnswers() throws Exception {
        GameEngine engine = game(false, 0.2);
        engine.useFiftyFifty();
        for (int answer : new int[]{-1, 4, Integer.MAX_VALUE}) expectBadAnswer(engine, answer);
        for (int answer = 0; answer < 4; answer++) {
            if (!engine.isOptionVisible(answer)) expectBadAnswer(engine, answer);
        }
        check(engine.currentSlot() == 1 && engine.session().history().isEmpty(), "invalid answer mutated state");
        engine.answer(engine.currentQuestion().correctIndex());
        try {
            engine.session().history().clear();
            throw new AssertionError("history should be read-only");
        } catch (UnsupportedOperationException expected) { }
    }

    private static void expectBadAnswer(GameEngine engine, int answer) {
        try {
            engine.answer(answer);
            throw new AssertionError("invalid answer accepted");
        } catch (IllegalArgumentException expected) { }
    }

    private static void switchReservesEnoughQuestions() throws Exception {
        var file = Files.createTempFile("minimum-bank-", ".csv");
        try {
            StringBuilder csv = new StringBuilder("id,level,prompt,optionA,optionB,optionC,optionD,correctIndex,hint,sourceReading,pageNumber\n");
            for (int slot = 1; slot <= 15; slot++) {
                csv.append(slot).append(',').append(BloomLevel.forSlot(slot)).append(",Prompt,A,B,C,D,0,Hint,Reading,1\n");
            }
            Files.writeString(file, csv);
            GameEngine engine = new GameEngine(QuestionBank.loadFromFile(file, new Random(1)), new Random(2), "Minimum", true);
            Question original = engine.currentQuestion();
            engine.advanceTime(5);
            BackendTestSupport.expectIllegalState(engine::switchQuestion, "No spare");
            check(engine.currentQuestion() == original && engine.secondsRemaining() == 25,
                    "failed switch changed question or timer");
            check(engine.lifelines().isAvailable(LifelineManager.Lifeline.SWITCH_QUESTION), "failed switch consumed lifeline");
            while (!engine.isGameOver()) engine.answer(engine.currentQuestion().correctIndex());
            check(engine.session().becameMillionaire(), "failed switch must leave all slots playable");
        } finally { Files.deleteIfExists(file); }
    }

    private static void shufflingPreservesAnswerIdentity() throws Exception {
        Set<Integer> positions = new HashSet<>();
        var bank = BackendTestSupport.questionBank();
        for (int draw = 0; draw < 40; draw++) {
            Question question = bank.drawQuestion(BloomLevel.REMEMBERING, Set.of());
            positions.add(question.correctIndex());
            check(question.correctOptionText().equals("Option B"), "shuffle moved correctness to another text");
            check(new HashSet<>(question.options()).size() == 4, "shuffle duplicated or lost an option");
        }
        check(positions.size() == 4, "seeded draws should exercise all answer positions");
    }

    private static void check(boolean condition, String message) { BackendTestSupport.check(condition, message); }
}
