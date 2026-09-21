package millionairemind;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/**
 * Console-based presentation layer. Mirrors the wireframes' screen flow:
 * Main Menu -> Instructions -> Question screens (with lifeline popups) ->
 * Results screen. Talks only to GameEngine; holds no game rules itself.
 */
public final class ConsoleUI {

    private static final String DIVIDER = "==========================================================";

    private final Scanner scanner = new Scanner(System.in);
    private final RandomGenerator random = RandomGeneratorFactory.getDefault().create();
    private QuestionBank questionBank;
    private ReplayLogger replayLogger;

    public static void main(String[] args) {
        new ConsoleUI().run(args);
    }

    private void run(String[] args) {
        String bankPath = args.length > 0 ? args[0] : null;
        try {
            questionBank = loadBank(bankPath);
        } catch (IOException e) {
            System.err.println("Failed to load question bank: " + e.getMessage());
            System.exit(1);
            return;
        }
        replayLogger = new ReplayLogger(Paths.get("logs"));

        printBanner();
        mainMenuLoop();
        System.out.println("\nThanks for playing Millionaire Mind. Goodbye!");
    }

    private QuestionBank loadBank(String bankPath) throws IOException {
        if (bankPath != null) {
            return QuestionBank.loadFromFile(Paths.get(bankPath), random);
        }
        return QuestionBank.loadFromResource("/questions.csv", random);
    }

    private void printBanner() {
        System.out.println(DIVIDER);
        System.out.println("   WHO WANTS TO BE A MILLIONAIRE MIND -- AI FUNDAMENTALS EDITION");
        System.out.println(DIVIDER);
    }

    private void mainMenuLoop() {
        while (true) {
            System.out.println();
            System.out.println("[1] PLAY");
            System.out.println("[2] INSTRUCTIONS");
            System.out.println("[3] QUIT");
            String choice = prompt("Choose an option: ");
            switch (choice.trim()) {
                case "1":
                    playGame();
                    break;
                case "2":
                    showInstructions();
                    break;
                case "3":
                    return;
                default:
                    System.out.println("Please enter 1, 2, or 3.");
            }
        }
    }

    private void showInstructions() {
        System.out.println();
        System.out.println("-------------------- INSTRUCTIONS --------------------");
        System.out.println("Answer 15 multiple-choice questions on AI fundamentals,");
        System.out.println("organized into six Bloom's Taxonomy tiers of rising");
        System.out.println("difficulty: Remembering, Understanding, Applying,");
        System.out.println("Analyzing, Evaluating, and Creating.");
        System.out.println();
        System.out.println("Checkpoints lock in your winnings at Q5 and Q10 -- a wrong");
        System.out.println("answer drops you back to the last checkpoint you passed");
        System.out.println("(or to $0 if you haven't reached one yet).");
        System.out.println();
        System.out.println("You may Walk Away at any time to bank your current");
        System.out.println("guaranteed winnings. Four lifelines are available, each");
        System.out.println("usable once per game:");
        System.out.println("  50:50            - removes two incorrect options");
        System.out.println("  Spin the Wheel    - a random weighted assist");
        System.out.println("  Switch the Question - swaps in a new question, same level");
        System.out.println("  Phone a Friend    - reveals a hint toward the source reading");
        System.out.println("--------------------------------------------------------");
    }

    private void playGame() {
        String name = prompt("\nEnter your name: ").trim();
        if (name.isEmpty()) name = "Player";

        GameEngine engine = new GameEngine(questionBank, random, name);
        boolean[] optionMask = {true, true, true, true};
        boolean bonusTimeGranted = false;

        while (!engine.isGameOver()) {
            optionMask = new boolean[]{true, true, true, true};
            Question q = engine.currentQuestion();
            printQuestionScreen(engine, q, optionMask);

            String action = promptAction(engine);
            if (action.equalsIgnoreCase("W")) {
                engine.walkAway();
                break;
            } else if (action.equalsIgnoreCase("5")) {
                if (engine.lifelines().isAvailable(LifelineManager.Lifeline.FIFTY_FIFTY)) {
                    int[] kept = engine.lifelines().fiftyFifty(q);
                    optionMask = new boolean[]{false, false, false, false};
                    for (int idx : kept) optionMask[idx] = true;
                    System.out.println("\n[50:50] Two incorrect options removed.");
                    printOptions(q, optionMask);
                } else {
                    System.out.println("50:50 already used.");
                }
                continue;
            } else if (action.equalsIgnoreCase("P")) {
                if (engine.lifelines().isAvailable(LifelineManager.Lifeline.PHONE_A_FRIEND)) {
                    String hint = engine.lifelines().phoneAFriend(q);
                    System.out.println("\n[Phone a Friend] HINT: " + hint);
                } else {
                    System.out.println("Phone a Friend already used.");
                }
                continue;
            } else if (action.equalsIgnoreCase("S")) {
                if (engine.lifelines().isAvailable(LifelineManager.Lifeline.SWITCH_QUESTION)) {
                    Question replacement = engine.switchQuestion();
                    System.out.println("\n[Switch the Question] New question drawn from the same level.");
                    q = replacement;
                    printOptions(q, optionMask);
                } else {
                    System.out.println("Switch the Question already used.");
                }
                continue;
            } else if (action.equalsIgnoreCase("V")) {
                if (engine.lifelines().isAvailable(LifelineManager.Lifeline.SPIN_THE_WHEEL)) {
                    LifelineManager.SpinOutcome outcome = engine.lifelines().spinTheWheel();
                    switch (outcome) {
                        case REVEAL_ONE_WRONG_OPTION -> {
                            int idx = firstWrongVisible(q, optionMask);
                            if (idx >= 0) optionMask[idx] = false;
                            System.out.println("\n[Spin the Wheel] Landed on: reveal one wrong option removed.");
                        }
                        case REDUCE_TWO_OPTIONS -> {
                            int correct = q.correctIndex();
                            int kept = correct;
                            for (int i = 0; i < 4; i++) {
                                if (i != correct && optionMask[i]) { kept = i; break; }
                            }
                            optionMask = new boolean[]{false, false, false, false};
                            optionMask[correct] = true;
                            optionMask[kept] = true;
                            System.out.println("\n[Spin the Wheel] Landed on: down to two options.");
                        }
                        case GRANT_BONUS_TIME -> {
                            bonusTimeGranted = true;
                            System.out.println("\n[Spin the Wheel] Landed on: +30 bonus seconds (timer mode).");
                        }
                    }
                    printOptions(q, optionMask);
                } else {
                    System.out.println("Spin the Wheel already used.");
                }
                continue;
            }

            int chosen = parseOptionLetter(action, optionMask);
            if (chosen < 0) {
                System.out.println("Enter A-D, or a lifeline / walk-away letter.");
                continue;
            }

            GameEngine.AnswerOutcome outcome = engine.answer(chosen);
            handleOutcome(engine, q, chosen, outcome);
        }

        showResultsScreen(engine);
        Path logPath = replayLogger.logSession(engine.session());
        System.out.println("Session log saved to: " + logPath.toAbsolutePath());
    }

    private int firstWrongVisible(Question q, boolean[] mask) {
        for (int i = 0; i < 4; i++) {
            if (mask[i] && i != q.correctIndex()) return i;
        }
        return -1;
    }

    private void printQuestionScreen(GameEngine engine, Question q, boolean[] mask) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.printf(Locale.US, "Question %d/%d  |  Tier: %s  |  Prize: $%,d  |  Banked if wrong now: $%,d%n",
                engine.currentSlot(), PrizeLadder.totalSlots(), engine.currentLevel().displayName(),
                engine.currentPrize(), engine.guaranteedBank());
        if (PrizeLadder.isCheckpoint(engine.currentSlot())) {
            System.out.println("*** This is a CHECKPOINT question ***");
        }
        System.out.println(DIVIDER);
        System.out.println(q.prompt());
        printOptions(q, mask);
        System.out.println();
        System.out.println("Lifelines remaining: " + describeLifelines(engine));
    }

    private void printOptions(Question q, boolean[] mask) {
        List<String> opts = q.options();
        for (int i = 0; i < 4; i++) {
            char letter = (char) ('A' + i);
            if (mask[i]) {
                System.out.printf("  %c) %s%n", letter, opts.get(i));
            } else {
                System.out.printf("  %c) [removed]%n", letter);
            }
        }
    }

    private String describeLifelines(GameEngine engine) {
        StringBuilder sb = new StringBuilder();
        if (engine.lifelines().isAvailable(LifelineManager.Lifeline.FIFTY_FIFTY)) sb.append("[5]50:50 ");
        if (engine.lifelines().isAvailable(LifelineManager.Lifeline.SPIN_THE_WHEEL)) sb.append("[V]Spin ");
        if (engine.lifelines().isAvailable(LifelineManager.Lifeline.SWITCH_QUESTION)) sb.append("[S]Switch ");
        if (engine.lifelines().isAvailable(LifelineManager.Lifeline.PHONE_A_FRIEND)) sb.append("[P]Phone ");
        if (sb.length() == 0) sb.append("(none left)");
        return sb.toString();
    }

    private String promptAction(GameEngine engine) {
        return prompt("Answer (A-D), lifeline (5/V/S/P), or Walk Away (W): ");
    }

    private int parseOptionLetter(String input, boolean[] mask) {
        if (input == null || input.length() != 1) return -1;
        char c = Character.toUpperCase(input.charAt(0));
        if (c < 'A' || c > 'D') return -1;
        int idx = c - 'A';
        if (!mask[idx]) {
            System.out.println("That option was removed by a lifeline. Choose a remaining one.");
            return -1;
        }
        return idx;
    }

    private void handleOutcome(GameEngine engine, Question q, int chosen, GameEngine.AnswerOutcome outcome) {
        System.out.println();
        if (outcome == GameEngine.AnswerOutcome.CORRECT) {
            System.out.println("CORRECT!");
        } else {
            System.out.println("WRONG.");
        }
        System.out.println("Correct answer: " + (char) ('A' + q.correctIndex()) + ") " + q.correctOptionText());
        System.out.println("Source: " + q.citation());
    }

    private void showResultsScreen(GameEngine engine) {
        PlayerSession session = engine.session();
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("RESULTS");
        System.out.println(DIVIDER);
        if (session.becameMillionaire()) {
            System.out.println("Congrats! You became a MILLIONAIRE!!! ($" + fmt(session.bankedWinnings()) + ")");
        } else if (session.walkedAway()) {
            System.out.println("You walked away with $" + fmt(session.bankedWinnings()) + ".");
        } else if (session.bankedWinnings() == 0) {
            System.out.println("Too bad! You won $0...");
        } else {
            System.out.println("Congrats! You won $" + fmt(session.bankedWinnings()) + ".");
        }

        System.out.println();
        System.out.println("--- Post-Game Bloom's Report ---");
        Map<BloomLevel, int[]> report = session.bloomReport();
        for (BloomLevel level : BloomLevel.values()) {
            int[] tally = report.get(level);
            int attempts = tally[0] + tally[1];
            String line = String.format("  %-14s %d/%d correct", level.displayName(), tally[0], attempts);
            System.out.println(line);
        }
        BloomLevel strongest = session.strongestLevel();
        BloomLevel weakest = session.weakestLevel();
        if (strongest != null) System.out.println("Strongest level: " + strongest.displayName());
        if (weakest != null) System.out.println("Weakest level:   " + weakest.displayName());
        System.out.println(DIVIDER);
    }

    private static String fmt(long amount) {
        return String.format(Locale.US, "%,d", amount);
    }

    private String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }
}
