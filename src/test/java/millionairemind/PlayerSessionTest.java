package millionairemind;

import java.util.Map;

/** Dependency-free regression tests for session history and Bloom reporting. */
public final class PlayerSessionTest {

    private PlayerSessionTest() {
    }

    public static void main(String[] args) {
        recordsAnswersAndTalliesBloomLevels();
        recordsSessionEndStates();
        System.out.println("PlayerSessionTest passed");
    }

    private static void recordsAnswersAndTalliesBloomLevels() {
        PlayerSession session = new PlayerSession("Student");
        Question rememberedRight = BackendTestSupport.question("R1", BloomLevel.REMEMBERING, 2);
        Question rememberedWrong = BackendTestSupport.question("R2", BloomLevel.REMEMBERING, 0);
        Question understoodRight = BackendTestSupport.question("U1", BloomLevel.UNDERSTANDING, 1);
        Question appliedWrong = BackendTestSupport.question("A1", BloomLevel.APPLYING, 3);

        session.recordAnswer(1, rememberedRight, 2, true);
        session.recordAnswer(2, rememberedWrong, 1, false);
        session.recordAnswer(3, understoodRight, 1, true);
        session.recordAnswer(6, appliedWrong, 0, false);

        BackendTestSupport.check(session.playerName().equals("Student"),
                "the session should retain the player's name");
        BackendTestSupport.check(session.history().size() == 4,
                "each resolved question should be retained in history");
        PlayerSession.Result first = session.history().get(0);
        BackendTestSupport.check(first.slot == 1 && first.question == rememberedRight
                        && first.chosenIndex == 2 && first.correct && first.timestamp != null,
                "history should preserve slot, question, selected answer, result, and timestamp");
        PlayerSession.Result second = session.history().get(1);
        BackendTestSupport.check(second.slot == 2 && second.chosenIndex == 1 && !second.correct,
                "history should preserve an incorrect answer");

        Map<BloomLevel, int[]> report = session.bloomReport();
        BackendTestSupport.check(report.get(BloomLevel.REMEMBERING)[0] == 1
                        && report.get(BloomLevel.REMEMBERING)[1] == 1,
                "Remembering should report one correct and one incorrect answer");
        BackendTestSupport.check(report.get(BloomLevel.UNDERSTANDING)[0] == 1
                        && report.get(BloomLevel.UNDERSTANDING)[1] == 0,
                "Understanding should report its correct answer");
        BackendTestSupport.check(report.get(BloomLevel.APPLYING)[0] == 0
                        && report.get(BloomLevel.APPLYING)[1] == 1,
                "Applying should report its incorrect answer");
        BackendTestSupport.check(report.get(BloomLevel.CREATING)[0] == 0
                        && report.get(BloomLevel.CREATING)[1] == 0,
                "unattempted levels should report zero answers");
        BackendTestSupport.check(session.strongestLevel() == BloomLevel.UNDERSTANDING,
                "the level with a 100% score should be strongest");
        BackendTestSupport.check(session.weakestLevel() == BloomLevel.APPLYING,
                "the level with a 0% score should be weakest");
    }

    private static void recordsSessionEndStates() {
        PlayerSession walkedAway = new PlayerSession("Walker");
        walkedAway.setBankedWinnings(2_000);
        walkedAway.markWalkedAway();
        BackendTestSupport.check(walkedAway.bankedWinnings() == 2_000
                        && walkedAway.walkedAway() && walkedAway.isGameOver()
                        && !walkedAway.becameMillionaire(),
                "Walk Away should preserve winnings and set the matching terminal flags");

        PlayerSession winner = new PlayerSession("Winner");
        winner.setBankedWinnings(1_000_000);
        winner.markMillionaire();
        BackendTestSupport.check(winner.bankedWinnings() == 1_000_000
                        && winner.becameMillionaire() && winner.isGameOver()
                        && !winner.walkedAway(),
                "a millionaire result should preserve winnings and set the matching terminal flags");
    }
}
