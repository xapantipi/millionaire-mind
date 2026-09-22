package millionairemind;

import java.util.Arrays;
import java.util.Set;

/** Dependency-free regression tests for lifeline effects and one-use rules. */
public final class LifelineManagerTest {

    private LifelineManagerTest() {
    }

    public static void main(String[] args) {
        fiftyFiftyKeepsTheCorrectAnswerAndOneWrongOption();
        spinWheelUsesTheConfiguredOutcomeRanges();
        eachLifelineCanOnlyBeUsedOnce();
        System.out.println("LifelineManagerTest passed");
    }

    private static void fiftyFiftyKeepsTheCorrectAnswerAndOneWrongOption() {
        Question question = BackendTestSupport.question("L1", BloomLevel.REMEMBERING, 1);
        LifelineManager manager = new LifelineManager(BackendTestSupport.fixedRandom(0.1, 3));

        int[] remaining = manager.fiftyFifty(question);
        BackendTestSupport.check(Arrays.equals(remaining, new int[]{1, 3}),
                "50:50 should preserve the correct option and one selected wrong option in order");
        BackendTestSupport.check(!manager.isAvailable(LifelineManager.Lifeline.FIFTY_FIFTY),
                "using 50:50 should mark it unavailable");
        BackendTestSupport.expectIllegalState(() -> manager.fiftyFifty(question), "already been used");
    }

    private static void spinWheelUsesTheConfiguredOutcomeRanges() {
        assertSpinOutcome(0.449, LifelineManager.SpinOutcome.REVEAL_ONE_WRONG_OPTION);
        assertSpinOutcome(0.45, LifelineManager.SpinOutcome.REDUCE_TWO_OPTIONS);
        assertSpinOutcome(0.799, LifelineManager.SpinOutcome.REDUCE_TWO_OPTIONS);
        assertSpinOutcome(0.80, LifelineManager.SpinOutcome.GRANT_BONUS_TIME);
    }

    private static void assertSpinOutcome(double roll, LifelineManager.SpinOutcome expected) {
        LifelineManager manager = new LifelineManager(BackendTestSupport.fixedRandom(roll, 0));
        BackendTestSupport.check(manager.spinTheWheel() == expected,
                "spin roll " + roll + " should produce " + expected);
        BackendTestSupport.expectIllegalState(manager::spinTheWheel, "already been used");
    }

    private static void eachLifelineCanOnlyBeUsedOnce() {
        Question question = BackendTestSupport.question("L2", BloomLevel.REMEMBERING, 0);
        LifelineManager manager = new LifelineManager(BackendTestSupport.fixedRandom(0.5, 2));
        BackendTestSupport.check(manager.remaining().equals(Set.of(LifelineManager.Lifeline.values())),
                "all four lifelines should be available at the start");

        manager.fiftyFifty(question);
        manager.spinTheWheel();
        manager.useSwitchQuestion();
        BackendTestSupport.check(manager.phoneAFriend(question).equals(question.hint()),
                "Phone a Friend should return the question's authored hint");

        BackendTestSupport.check(manager.remaining().isEmpty(),
                "using all lifelines should leave none remaining");
        BackendTestSupport.expectIllegalState(() -> manager.fiftyFifty(question), "already been used");
        BackendTestSupport.expectIllegalState(manager::spinTheWheel, "already been used");
        BackendTestSupport.expectIllegalState(manager::useSwitchQuestion, "already been used");
        BackendTestSupport.expectIllegalState(() -> manager.phoneAFriend(question), "already been used");
    }
}
