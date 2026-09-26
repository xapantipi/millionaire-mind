package millionairemind;

import java.awt.Component;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Desktop smoke checks using the real Swing controls and question bank. */
public final class SwingUITest {

    private SwingUITest() {
    }

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new AssertionError("SwingUITest requires a graphical desktop");
        }
        Path temp = Files.createTempDirectory("millionaire-mind-swing-test-");
        try {
            Path preview = args.length > 0 ? Path.of(args[0]) : null;
            Path menuPreview = args.length > 1 ? Path.of(args[1]) : null;
            Path longChoicePreview = args.length > 2 ? Path.of(args[2]) : null;
            Path namePreview = args.length > 3 ? Path.of(args[3]) : null;
            Path instructionsPreview = args.length > 4 ? Path.of(args[4]) : null;
            Path feedbackPreview = args.length > 5 ? Path.of(args[5]) : null;
            Path resultsPreview = args.length > 6 ? Path.of(args[6]) : null;
            Path fiftyPreview = args.length > 7 ? Path.of(args[7]) : null;
            Path wrongPreview = args.length > 8 ? Path.of(args[8]) : null;
            launchesWithBundledQuestionBank(menuPreview, instructionsPreview);
            playsToCheckpointAndLogsWalkAway(temp.resolve("logs"), preview,
                    namePreview, feedbackPreview, resultsPreview, fiftyPreview);
            showsBonusTimeAsUntimed(temp.resolve("bonus-logs"));
            wrapsLongQuestionAndChoices(temp.resolve("long-logs"),
                    temp.resolve("long-bank.csv"), longChoicePreview);
            showsWrongAndMillionaireResults(temp.resolve("results-logs"), wrongPreview);
            System.out.println("SwingUITest passed");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                for (java.awt.Window window : java.awt.Window.getWindows()) window.dispose();
            });
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void launchesWithBundledQuestionBank(Path menuPreview,
            Path instructionsPreview) throws Exception {
        SwingUI.main(new String[0]);
        SwingUtilities.invokeAndWait(() -> {
            JFrame launched = null;
            for (java.awt.Window window : java.awt.Window.getWindows()) {
                if (window instanceof JFrame frame && frame.isVisible()
                        && "Millionaire Mind".equals(frame.getTitle())) {
                    launched = frame;
                }
            }
            check(launched != null, "SwingUI.main should open the bundled game");
            check(find(launched, "menuPlay", AbstractButton.class).isShowing(),
                    "the game should open on the title menu");
            find(launched, "menuInstructions", AbstractButton.class).doClick();
            JTextArea instructions = find(launched, "instructionsText", JTextArea.class);
            check(instructions.isShowing()
                            && instructions.getText().contains("artificial intelligence")
                            && containsLabel(launched.getContentPane(),
                                    "$2,000 guaranteed after Question 5")
                            && containsLabel(launched.getContentPane(),
                                    "$64,000 guaranteed after Question 10")
                            && find(launched, "prizeTerms", JTextArea.class).getText()
                                    .contains("Walk Away"),
                    "Instructions should explain play and emphasize both checkpoint prizes");
            JPanel instructionsCard = find(launched, "instructionsCard", JPanel.class);
            check(instructionsCard.getWidth() >= 800,
                    "the instructions card should retain a readable width");
            checkCentered(launched, "instructionsCard");
            for (String name : new String[] {"instructionsText", "prizeTerms",
                    "afterAnswerText", "instructionsBack"}) {
                checkWithin(launched, instructionsCard, name);
            }
            for (int i = 0; i < 4; i++) {
                checkWithin(launched, instructionsCard, "lifelineInstruction" + i);
                String bullet = find(launched, "lifelineInstruction" + i,
                        JTextArea.class).getText();
                check(bullet.startsWith("•  "),
                        "each lifeline should have its own bullet point");
                check(!bullet.contains("50:50") && !bullet.contains("Q5"),
                        "Instructions should spell out abbreviations");
            }
            if (instructionsPreview != null) capture(launched, instructionsPreview);
            find(launched, "instructionsBack", AbstractButton.class).doClick();
            check(find(launched, "menuPlay", AbstractButton.class).isShowing(),
                    "Instructions should return to the main menu");
            if (menuPreview != null) capture(launched, menuPreview);
            find(launched, "menuExit", AbstractButton.class).doClick();
            check(!launched.isDisplayable(), "Exit should close the game window");
        });
    }

    private static void playsToCheckpointAndLogsWalkAway(Path logDirectory, Path preview,
            Path namePreview, Path feedbackPreview, Path resultsPreview, Path fiftyPreview)
            throws Exception {
        QuestionBank bank = QuestionBank.loadFromResource("/questions.csv", new Random(17));
        SwingUI[] holder = new SwingUI[1];
        SwingUtilities.invokeAndWait(() -> {
            SwingUI ui = new SwingUI(bank, cyclingSpinRandom(),
                    new ReplayLogger(logDirectory));
            holder[0] = ui;
            ui.show();
            click(ui, "menuPlay");
            ui.frame().validate();
            checkNameFormCentered(ui);
            if (namePreview != null) capture(ui.frame(), namePreview);
            click(ui, "startGame");
            check(ui.engine() == null, "blank name must not start a session");
            check(find(ui.frame(), "nameError", JLabel.class).getText()
                            .contains("Enter a player name"),
                    "the start screen should explain why a blank name was rejected");
            find(ui.frame(), "playerName", JTextField.class).setText("GUI Smoke");
            click(ui, "startGame");
            check("GUI Smoke".equals(ui.engine().session().playerName()),
                    "the entered name should start the session");
            check(find(ui.frame(), "playerDisplay", JLabel.class).isShowing()
                            && find(ui.frame(), "playerDisplay", JLabel.class)
                                    .getText().equals("GUI Smoke"),
                    "header should show the live player name");
            check(find(ui.frame(), "questionText", JTextArea.class).getText()
                            .equals(ui.engine().currentQuestion().prompt()),
                    "the question should come from the current game state");
            check(find(ui.frame(), "questionText", JTextArea.class).getLineWrap(),
                    "question text should wrap");
            check(!find(ui.frame(), "lockAnswer", AbstractButton.class).isEnabled(),
                    "Lock in answer should wait for a selection");
            click(ui, "answer" + answerLetter(ui.engine().currentQuestion().correctIndex()));
            check(ui.engine().session().history().isEmpty(),
                    "selecting an answer should not submit it");
            check(find(ui.frame(), "lockAnswer", AbstractButton.class).isEnabled(),
                    "a selected answer should enable confirmation");
            click(ui, "lockAnswer");
            check(ui.engine().session().history().size() == 1,
                    "Lock in answer should record one answer");
            check(find(ui.frame(), "feedbackDetail", JTextArea.class).getText().contains("Source: "),
                    "answer feedback should include the source citation");
            check(find(ui.frame(), "feedbackPanel", JPanel.class).isShowing()
                            && find(ui.frame(), "feedbackTitle", JLabel.class).getForeground()
                                    .equals(new Color(111, 225, 169)),
                    "correct-answer feedback should be centered and green");
            checkCentered(ui.frame(), "feedbackPanel");
            if (feedbackPreview != null) capture(ui.frame(), feedbackPreview);
            click(ui, "continue");

            while (ui.engine().currentSlot() < 7) answerCurrentCorrect(ui);
            ui.frame().validate();
            check(ui.engine().guaranteedBank() == PrizeLadder.prizeFor(5),
                    "Q7 should carry the Q5 guarantee");
            check(find(ui.frame(), "guaranteedAmount", JLabel.class).getText().equals("$2,000"),
                    "the guaranteed display should reflect the backend");
            check(find(ui.frame(), "checkpointStatus", JLabel.class).getText()
                            .contains("Q5 checkpoint secured"),
                    "the checkpoint display should reflect progress");
            check(find(ui.frame(), "walkAway", AbstractButton.class).getText()
                            .contains("$2,000"),
                    "Walk Away should show the last passed checkpoint amount");
            check(find(ui.frame(), "ladderQ5", javax.swing.JPanel.class).getBackground()
                            .equals(find(ui.frame(), "ladderQ10", javax.swing.JPanel.class)
                                    .getBackground()),
                    "both checkpoint rungs should use the same highlight");
            check(!find(ui.frame(), "ladderQ7", javax.swing.JPanel.class).getBackground()
                            .equals(find(ui.frame(), "ladderQ5", javax.swing.JPanel.class)
                                    .getBackground()),
                    "the current rung should use a distinct highlight");
            javax.swing.JPanel currentRung =
                    find(ui.frame(), "ladderQ7", javax.swing.JPanel.class);
            check(currentRung.isShowing() && currentRung.getWidth() > 0
                            && currentRung.getHeight() > 0,
                    "the current rung should be laid out visibly: " + currentRung.getBounds());
            checkGameFits(ui.frame());
            click(ui, "answerB");
            check(ui.engine().session().history().size() == 6,
                    "preview selection should remain unsubmitted");
        });
        SwingUI ui = holder[0];
        if (preview != null) {
            java.awt.Dimension originalSize = ui.frame().getSize();
            SwingUtilities.invokeAndWait(() -> ui.frame().setSize(1536, 1024));
            SwingUtilities.invokeAndWait(() -> {
                ui.frame().validate();
                java.awt.Container content = ui.frame().getContentPane();
                javax.swing.JPanel gameHeader =
                        find(ui.frame(), "gameHeader", javax.swing.JPanel.class);
                java.awt.Rectangle headerOnContent = SwingUtilities.convertRectangle(
                        gameHeader.getParent(), gameHeader.getBounds(), content);
                check(gameHeader.isShowing() && gameHeader.getHeight() > 0,
                        "the game header should remain laid out: " + gameHeader.getBounds());
                check(headerOnContent.y >= 0,
                        "the game header should stay within the content: " + headerOnContent);
                checkGameFits(ui.frame());
                capture(ui.frame(), preview);
            });
            SwingUtilities.invokeAndWait(() -> ui.frame().setSize(originalSize));
        }
        SwingUtilities.invokeAndWait(() -> {
            click(ui, "lifeline0");
            check(visibleCount(ui) == 2, "50:50 should disable two choices");
            for (char letter = 'A'; letter <= 'D'; letter++) {
                AbstractButton answer = find(ui.frame(), "answer" + letter,
                        AbstractButton.class);
                if (!answer.isEnabled()) {
                    check(answer.getToolTipText() == null
                                    && answer.getAccessibleContext().getAccessibleName()
                                            .endsWith("unavailable"),
                            "removed choices should be blank and reveal no answer text");
                }
            }
            checkGameFits(ui.frame());
            if (fiftyPreview != null) capture(ui.frame(), fiftyPreview);
            click(ui, "lifeline1");
            check(visibleCount(ui) == 1,
                    "reveal-one Spin should combine with 50:50");
            click(ui, "lifeline2");
            check(visibleCount(ui) == 4,
                    "Switch should restore four choices for its replacement");
            click(ui, "lifeline3");
            check(find(ui.frame(), "lifelineNotice", JTextArea.class).getText()
                            .contains(ui.engine().currentQuestion().hint()),
                    "Phone a Friend should show the current question's hint");
            for (int i = 0; i < 4; i++) {
                AbstractButton lifeline = find(ui.frame(), "lifeline" + i, AbstractButton.class);
                check(lifeline.isEnabled()
                                && lifeline.getAccessibleContext().getAccessibleDescription()
                                .contains("already been used"),
                        "each used lifeline should stay clickable and be marked unavailable");
            }
            click(ui, "lifeline0");
            check(find(ui.frame(), "lifelineNotice", JTextArea.class).getText()
                            .contains("already been used"),
                    "trying to reuse a lifeline should show an unavailable notice");
            click(ui, "walkAway");
            check(ui.engine().session().walkedAway()
                            && ui.engine().session().bankedWinnings() == 2_000,
                    "Walk Away should pay the Q5 guarantee after Q6");
            check(find(ui.frame(), "resultOutcome", JLabel.class).getText()
                            .contains("$2,000"),
                    "results should show the banked winnings");
            check(find(ui.frame(), "resultsCard", JPanel.class).isShowing()
                            && find(ui.frame(), "logStatus", JLabel.class).getText()
                                    .contains("Session replay saved"),
                    "the centered game report should show log status");
            checkCentered(ui.frame(), "resultsCard");
            JPanel resultsCard = find(ui.frame(), "resultsCard", JPanel.class);
            check(resultsCard.getWidth() >= 700,
                    "the game report card should retain a readable width");
            for (String name : new String[] {"resultOutcome", "bloomReportPanel",
                    "logStatus", "openSessionLog", "playAgain", "resultsMenu"}) {
                checkWithin(ui.frame(), resultsCard, name);
            }
            AbstractButton openLog = find(ui.frame(), "openSessionLog", AbstractButton.class);
            Path savedLog = Path.of(openLog.getToolTipText());
            check(openLog.isShowing() && Files.isRegularFile(savedLog)
                            && savedLog.getFileName().toString().length() <= 30
                            && openLog.getActionListeners().length > 0,
                    "the short replay filename should appear in a clickable file button");
            if (resultsPreview != null) capture(ui.frame(), resultsPreview);
            click(ui, "playAgain");
            check(find(ui.frame(), "playerName", JTextField.class).getText().isEmpty(),
                    "Play again should request a new player name");
            find(ui.frame(), "playerName", JTextField.class).setText("Second Player");
            click(ui, "startGame");
            check(find(ui.frame(), "playerDisplay", JLabel.class).getText()
                            .equals("Second Player"),
                    "a second game should restore the live player header");
            ui.frame().dispose();
        });
        try (var paths = Files.list(logDirectory)) {
            List<Path> logs = paths.toList();
            check(logs.size() == 1, "one completed session should write one replay log");
            String replay = Files.readString(logs.get(0), StandardCharsets.UTF_8);
            check(replay.contains("Player: GUI Smoke")
                            && replay.contains("Outcome: Walked away")
                            && replay.contains("Final winnings: $2000")
                            && replay.contains("Bloom's report:")
                            && replay.contains("Source: "),
                    "the replay log should retain session results and citations");
        }
    }

    private static void showsBonusTimeAsUntimed(Path logDirectory) throws Exception {
        QuestionBank bank = QuestionBank.loadFromResource("/questions.csv", new Random(5));
        SwingUtilities.invokeAndWait(() -> {
            SwingUI ui = new SwingUI(bank, BackendTestSupport.fixedRandom(0.9, 0),
                    new ReplayLogger(logDirectory));
            ui.show();
            click(ui, "menuPlay");
            find(ui.frame(), "playerName", JTextField.class).setText("Bonus Smoke");
            click(ui, "startGame");
            click(ui, "lifeline1");
            check(visibleCount(ui) == 4,
                    "bonus-time Spin should leave all answers visible");
            check(find(ui.frame(), "lifelineNotice", JTextArea.class).getText()
                            .contains("This game is untimed"),
                    "bonus-time result should explain untimed behavior");
            ui.frame().dispose();
        });
    }

    private static void wrapsLongQuestionAndChoices(Path logDirectory, Path bankFile,
            Path longChoicePreview) throws Exception {
        String longPrompt = "When a system examines a long description of an artificial "
                + "intelligence problem, compares several possible explanations, and must "
                + "choose the best-supported response from a set of alternatives, which "
                + "process helps it connect evidence to the final decision? ".repeat(2);
        String longChoice = ("A detailed explanation that compares evidence from an AI "
                + "system and evaluates alternative conclusions before choosing the "
                + "best-supported response. ").repeat(2);
        StringBuilder csv = new StringBuilder(
                "id,level,prompt,optionA,optionB,optionC,optionD,correctIndex,"
                        + "hint,sourceReading,pageNumber\n");
        int id = 1;
        for (BloomLevel level : BloomLevel.values()) {
            for (int i = 0; i < 4; i++) {
                csv.append("LONG").append(id++).append(',').append(level.name())
                        .append(',').append('"').append(longPrompt).append('"')
                        .append(",\"").append(longChoice)
                        .append("\",Answer B,Answer C,Answer D,0,Hint,Reading,1\n");
            }
        }
        Files.writeString(bankFile, csv.toString(), StandardCharsets.UTF_8);
        QuestionBank bank = QuestionBank.loadFromFile(bankFile, new Random(4));
        SwingUI[] holder = new SwingUI[1];
        SwingUtilities.invokeAndWait(() -> {
            SwingUI ui = new SwingUI(bank, new Random(4), new ReplayLogger(logDirectory));
            holder[0] = ui;
            ui.show();
            click(ui, "menuPlay");
            find(ui.frame(), "playerName", JTextField.class).setText("Long Prompt");
            click(ui, "startGame");
            ui.frame().setSize(ui.frame().getMinimumSize());
        });
        SwingUtilities.invokeAndWait(() -> {
            SwingUI ui = holder[0];
            JTextArea prompt = find(ui.frame(), "questionText", JTextArea.class);
            ui.frame().validate();
            check(prompt.getText().equals(longPrompt) && prompt.getLineWrap(),
                    "long prompts should remain complete and wrapped");
            checkGameFits(ui.frame());
            check(prompt.getHeight() >= prompt.getPreferredSize().height,
                    "the whole long prompt should fit without scrolling: actual="
                            + prompt.getSize() + " preferred=" + prompt.getPreferredSize());
            JPanel grid = find(ui.frame(), "answerGrid", JPanel.class);
            AbstractButton answer = find(ui.frame(), "answerA", AbstractButton.class);
            check(longChoice.equals(answer.getToolTipText()),
                    "the full long answer should remain available");
            check(grid.getPreferredSize().height > 174
                            && answer.getHeight() >= answer.getPreferredSize().height,
                    "long answers should increase the answer-card height: grid="
                            + grid.getPreferredSize() + " answer=" + answer.getSize());
            if (longChoicePreview != null) {
                ui.frame().setSize(1650, 960);
                ui.frame().validate();
                capture(ui.frame(), longChoicePreview);
            }
            click(ui, "answerA");
            click(ui, "lockAnswer");
            check(textFullyVisible(find(ui.frame(), "feedbackDetail", JTextArea.class)),
                    "long correct answers and citations should fit in feedback");
            ui.frame().dispose();
        });
    }

    private static void showsWrongAndMillionaireResults(Path logDirectory,
            Path wrongPreview) throws Exception {
        QuestionBank bank = QuestionBank.loadFromResource("/questions.csv", new Random(21));
        SwingUtilities.invokeAndWait(() -> {
            SwingUI ui = new SwingUI(bank, new Random(21), new ReplayLogger(logDirectory));
            ui.show();
            click(ui, "menuPlay");
            find(ui.frame(), "playerName", JTextField.class).setText("Wrong Answer");
            click(ui, "startGame");
            int wrong = (ui.engine().currentQuestion().correctIndex() + 1) % 4;
            click(ui, "answer" + answerLetter(wrong));
            click(ui, "lockAnswer");
            check(find(ui.frame(), "feedbackTitle", JLabel.class).getText()
                            .equals("Incorrect answer"),
                    "wrong-answer feedback should be explicit");
            check(find(ui.frame(), "feedbackTitle", JLabel.class).getForeground()
                            .equals(new Color(255, 126, 143)),
                    "incorrect-answer feedback should be red");
            checkCentered(ui.frame(), "feedbackPanel");
            if (wrongPreview != null) capture(ui.frame(), wrongPreview);
            click(ui, "continue");
            check(ui.engine().isGameOver()
                            && find(ui.frame(), "resultOutcome", JLabel.class).getText()
                                    .contains("Game over  ·  $0"),
                    "a wrong Q1 answer should end the Swing game at $0");

            click(ui, "playAgain");
            find(ui.frame(), "playerName", JTextField.class).setText("Millionaire Smoke");
            click(ui, "startGame");
            for (int slot = 1; slot <= PrizeLadder.totalSlots(); slot++) {
                click(ui, "answer" + answerLetter(ui.engine().currentQuestion().correctIndex()));
                click(ui, "lockAnswer");
                click(ui, "continue");
            }
            check(ui.engine().session().becameMillionaire()
                            && find(ui.frame(), "resultOutcome", JLabel.class).getText()
                                    .contains("$1,000,000"),
                    "fifteen correct answers should reach the millionaire results screen");
            check(containsLabel(ui.frame().getContentPane(),
                            "All Bloom levels tied at 100%; no unique strongest or weakest."),
                    "the perfect-win result should explain the Bloom report tie");
            ui.frame().dispose();
        });
        try (var paths = Files.list(logDirectory)) {
            check(paths.count() == 2, "both terminal results should write a replay log");
        }
    }

    private static void answerCurrentCorrect(SwingUI ui) {
        click(ui, "answer" + answerLetter(ui.engine().currentQuestion().correctIndex()));
        click(ui, "lockAnswer");
        click(ui, "continue");
    }

    private static void checkNameFormCentered(SwingUI ui) {
        JPanel card = find(ui.frame(), "nameCard", JPanel.class);
        JLabel prompt = find(ui.frame(), "namePrompt", JLabel.class);
        JTextField field = find(ui.frame(), "playerName", JTextField.class);
        int promptCenter = SwingUtilities.convertPoint(prompt.getParent(),
                prompt.getLocation(), card).x + prompt.getWidth() / 2;
        int fieldCenter = SwingUtilities.convertPoint(field.getParent(),
                field.getLocation(), card).x + field.getWidth() / 2;
        int cardCenter = card.getWidth() / 2;
        check(Math.abs(promptCenter - cardCenter) <= 2
                        && Math.abs(fieldCenter - cardCenter) <= 2,
                "the Player name label and field should be centered in the card: "
                        + promptCenter + ", " + fieldCenter + ", " + cardCenter);
    }

    private static void checkCentered(JFrame frame, String name) {
        JPanel card = find(frame, name, JPanel.class);
        frame.validate();
        java.awt.Container parent = card.getParent();
        int horizontalOffset = Math.abs(card.getX() + card.getWidth() / 2
                - parent.getWidth() / 2);
        int verticalOffset = Math.abs(card.getY() + card.getHeight() / 2
                - parent.getHeight() / 2);
        check(card.isShowing() && horizontalOffset <= 2 && verticalOffset <= 2,
                name + " should be centered: " + card.getBounds()
                        + " in " + parent.getSize());
    }

    private static void checkGameFits(JFrame frame) {
        JPanel game = find(frame, "gamePage", JPanel.class);
        check(game.isShowing(), "the game screen should be visible");
        check(findRecursive(game, "questionScroll") == null
                        && findRecursive(game, "ladderScroll") == null
                        && findRecursive(game, "promptScroll") == null,
                "the game and prize ladder should have no scroll panes");
        for (String name : new String[] {"questionText", "answerA", "answerB",
                "answerC", "answerD", "lockAnswer", "lifeline0", "lifeline1",
                "lifeline2", "lifeline3", "walkAway"}) {
            checkFits(frame, game, name);
        }
        for (int slot = 1; slot <= PrizeLadder.totalSlots(); slot++) {
            checkFits(frame, game, "ladderQ" + slot);
            checkWithin(frame, find(frame, "prizeLadder", JPanel.class),
                    "ladderQ" + slot);
        }
        JTextArea prompt = find(frame, "questionText", JTextArea.class);
        check(prompt.getHeight() >= prompt.getPreferredSize().height
                        && textFullyVisible(prompt),
                "the question should show every wrapped line");
        for (char letter = 'A'; letter <= 'D'; letter++) {
            AbstractButton answer = find(frame, "answer" + letter, AbstractButton.class);
            check(answer.getHeight() >= answer.getPreferredSize().height,
                    "answer " + letter + " should show its entire wrapped choice");
        }
    }

    private static void checkFits(JFrame frame, JPanel game, String name) {
        Component component = find(frame, name, Component.class);
        java.awt.Rectangle bounds = SwingUtilities.convertRectangle(
                component.getParent(), component.getBounds(), game);
        check(component.isShowing() && bounds.x >= 0 && bounds.y >= 0
                        && bounds.x + bounds.width <= game.getWidth()
                        && bounds.y + bounds.height <= game.getHeight(),
                name + " should fit in the game view: " + bounds
                        + " inside " + game.getSize());
    }

    private static void checkWithin(JFrame frame, JPanel parent, String name) {
        Component component = find(frame, name, Component.class);
        java.awt.Rectangle bounds = SwingUtilities.convertRectangle(
                component.getParent(), component.getBounds(), parent);
        check(component.isShowing() && bounds.x >= 0 && bounds.y >= 0
                        && bounds.x + bounds.width <= parent.getWidth()
                        && bounds.y + bounds.height <= parent.getHeight(),
                name + " should fit within " + parent.getName() + ": " + bounds
                        + " in " + parent.getSize());
    }

    private static boolean textFullyVisible(JTextArea area) {
        try {
            int lastCharacter = Math.max(0, area.getDocument().getLength() - 1);
            java.awt.geom.Rectangle2D end = area.modelToView2D(lastCharacter);
            return end != null && end.getMaxY() <= area.getHeight()
                    - area.getInsets().bottom;
        } catch (javax.swing.text.BadLocationException e) {
            throw new AssertionError(e);
        }
    }

    private static void capture(JFrame frame, Path path) {
        java.awt.Container content = frame.getContentPane();
        BufferedImage image = new BufferedImage(content.getWidth(),
                content.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        content.printAll(graphics);
        graphics.dispose();
        try {
            ImageIO.write(image, "png", path.toFile());
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static char answerLetter(int index) {
        return (char) ('A' + index);
    }

    private static java.util.random.RandomGenerator cyclingSpinRandom() {
        return new java.util.random.RandomGenerator() {
            private int nextChoice;

            public long nextLong() { return 0; }
            public double nextDouble() { return 0.2; }
            public int nextInt(int bound) { return nextChoice++ % bound; }
        };
    }

    private static int visibleCount(SwingUI ui) {
        int visible = 0;
        for (char letter = 'A'; letter <= 'D'; letter++) {
            if (find(ui.frame(), "answer" + letter, AbstractButton.class).isEnabled()) visible++;
        }
        return visible;
    }

    private static void click(SwingUI ui, String name) {
        find(ui.frame(), name, AbstractButton.class).doClick();
    }

    private static <T extends Component> T find(JFrame frame, String name, Class<T> type) {
        Component found = findRecursive(frame.getContentPane(), name);
        if (!type.isInstance(found)) throw new AssertionError("Missing component: " + name);
        return type.cast(found);
    }

    private static Component findRecursive(Component component, String name) {
        if (name.equals(component.getName())) return component;
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                Component found = findRecursive(child, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean containsLabel(Component component, String text) {
        if (component instanceof JLabel label && text.equals(label.getText())) return true;
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                if (containsLabel(child, text)) return true;
            }
        }
        return false;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
