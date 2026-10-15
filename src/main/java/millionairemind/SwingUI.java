package millionairemind;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.View;

/** Swing presentation for normal and timed games. */
public final class SwingUI {

    private static final Color BACKGROUND = new Color(14, 34, 62);
    private static final Color PANEL = new Color(19, 44, 77);
    private static final Color PANEL_DARK = new Color(16, 38, 68);
    private static final Color BORDER = new Color(60, 94, 139);
    private static final Color TEXT = new Color(245, 248, 255);
    private static final Color MUTED = new Color(185, 204, 231);
    private static final Color DISABLED_TOP = new Color(67, 75, 88);
    private static final Color DISABLED_BOTTOM = new Color(47, 54, 66);
    private static final Color DISABLED_BORDER = new Color(137, 146, 160);
    private static final Color DISABLED_TEXT = new Color(201, 207, 216);
    private static final Color BLUE = new Color(80, 126, 241);
    private static final Color PERIWINKLE = new Color(143, 174, 255);
    private static final Color GOLD = new Color(255, 205, 102);
    private static final Color GREEN = new Color(111, 225, 169);
    private static final Color RED = new Color(255, 126, 143);
    private static final Font BODY = new Font("Segoe UI", Font.PLAIN, 17);
    private static final Font BOLD = new Font("Segoe UI", Font.BOLD, 17);

    private final QuestionBank questionBank;
    private final RandomGenerator random;
    private final ReplayLogger replayLogger;
    private final JFrame frame = new JFrame("Millionaire Mind");
    private final CardLayout screens = new CardLayout();
    private final JPanel root = new JPanel(screens);
    private final ButtonGroup answerGroup = new ButtonGroup();
    private final AnswerButton[] answerButtons = new AnswerButton[4];
    private final JPanel answerGrid = new JPanel();
    private final JPanel[] answerRows = new JPanel[2];
    private JPanel questionBody;
    private final StyledButton[] lifelineButtons = new StyledButton[4];
    private final JPanel ladderRows = new JPanel();
    private final JTextArea promptText = textArea(25, true);
    private JPanel promptBox;
    private final JTextArea noticeText = textArea(15, false);
    private final JLabel playerLabel = label("", 20, true, TEXT);
    private final JLabel correctCountLabel = label("", 15, false, MUTED);
    private final JLabel levelLabel = new BadgeLabel();
    private final JLabel positionLabel = label("", 18, true, TEXT);
    private final JLabel prizeLabel = label("", 25, true, GOLD);
    private final JLabel guaranteedLabel = label("", 29, true, GOLD);
    private final JLabel checkpointLabel = label("", 14, false, MUTED);
    private final JLabel feedbackTitle = label("", 28, true, TEXT);
    private final JTextArea feedbackDetail = textArea(19, false);
    private final OutcomeEmblem feedbackEmblem = new OutcomeEmblem();
    private final JButton lockButton = primaryButton("Lock in answer");
    private final JButton continueButton = primaryButton("Continue");
    private final JButton walkButton = outlineButton("");
    private final JTextField nameField = new JTextField(20);
    private final JLabel nameError = label("", 14, false, GOLD);
    private final ProgressStrip progressStrip = new ProgressStrip();

    private GameEngine engine;
    private int selectedIndex = -1;
    private boolean selectedTimedMode = false;
    private final TimerBadge timerLabel = new TimerBadge();
    private final JPanel timerBox = buildTimerBox();
    private Timer questionTimer;
    private long lastTimerNanos;
    private String currentScreen;
    private JButton menuPlayButton;
    private JButton startButton;
    private JPanel resultsPage;
    private final GameAudio audio = new GameAudio();
    private SplashOverlay splashOverlay;
    private Timer splashHoldTimer;
    private Timer splashFadeTimer;
    private boolean splashFading;
    private static final int SPLASH_HOLD_MS = 20000;
    private static final int SPLASH_FADE_MS = 600;
    private static final float BACKGROUND_MUSIC_LEVEL = 0.30f;
    private static final String SFX_PROGRESS = "niera_sound_5.wav";
    private static final String SFX_BACK = "niera_sound_2.wav";
    private static final String SFX_ANSWER_SELECT = "select_005.wav";
    private static final String SFX_CORRECT_ANSWER = "confirmation_004.wav";
    private static final String SFX_ACTION_ERROR = "error_006.wav";
    private static final String SFX_WRONG_ANSWER = "wrong.WAV";
    private static final String SFX_REGULAR_WIN = "regular winning.WAV";
    private static final String SFX_SUPREME_VICTORY = "supreme victory.WAV";
    private static final String SFX_NO_CHECKPOINT = "Clapping Sound Effects.WAV";
    private static final String CHECKPOINT_MUSIC = "sfx/suspense.WAV";
    private static final String GAMEPLAY_MUSIC = "sfx/q12.WAV";

    private Image splashImage;

    public static void main(String[] args) {
        Main.main(args);
    }

    static void launch(String[] args) throws IOException {
        RandomGenerator random = RandomGeneratorFactory.getDefault().create();
        try {
            QuestionBank bank = args.length > 0
                    ? QuestionBank.loadFromFile(Paths.get(args[0]), random)
                    : QuestionBank.loadFromResource("/questions.csv", random);
            ReplayLogger logger = new ReplayLogger(Paths.get("logs"));
            SwingUtilities.invokeAndWait(() -> new SwingUI(bank, random, logger).show());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while opening the game", e);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new IOException("Could not open the game window: " + e.getCause().getMessage(), e);
        }
    }

    SwingUI(QuestionBank questionBank, RandomGenerator random, ReplayLogger replayLogger) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("SwingUI must be created on the event-dispatch thread");
        }
        this.questionBank = questionBank;
        this.random = random;
        this.replayLogger = replayLogger;
        playerLabel.setName("playerDisplay");
        correctCountLabel.setName("correctCount");

        root.setBackground(BACKGROUND);
        root.add(buildMainMenuScreen(), "menu");
        root.add(buildModeScreen(), "mode");
        root.add(buildInstructionsScreen1(), "instructions1");
        root.add(buildInstructionsScreen2(), "instructions2");
        root.add(buildNameScreen(), "name");
        root.add(buildGameScreen(), "game");
        root.add(buildFeedbackScreen(), "feedback");
        frame.setContentPane(root);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                exitApplication();
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                stopQuestionTimer();
                stopSplash();
                audio.close();
            }
        });
        applyFullScreenBounds();
        showMainMenu();
    }

    private void applyFullScreenBounds() {
        frame.setUndecorated(true);
        frame.setResizable(false);
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Rectangle screenBounds = device.getDefaultConfiguration().getBounds();
        frame.setBounds(screenBounds);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void exitApplication() {
        stopQuestionTimer();
        stopSplash();
        audio.close();
        frame.dispose();
    }

    void show() {
        frame.setVisible(true);
        showSplashScreen();
        menuPlayButton.requestFocusInWindow();
    }

    JFrame frame() { return frame; }
    GameEngine engine() { return engine; }

    private void showSplashScreen() {
        URL url = getClass().getResource("/splash.gif");
        if (url == null) {
            return;
        }
        stopBackgroundMusic();
        splashImage = new ImageIcon(url).getImage();

        splashOverlay = new SplashOverlay();
        frame.setGlassPane(splashOverlay);
        splashOverlay.setOpacity(1f);
        splashOverlay.setVisible(true);
        splashOverlay.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                skipSplashImmediately(splashOverlay);
            }
        });
        installSplashEnterBinding();

        splashFading = false;
        splashHoldTimer = new Timer(SPLASH_HOLD_MS, event -> fadeOutSplash(splashOverlay));
        splashHoldTimer.setRepeats(false);
        splashHoldTimer.start();
        playSplashAudio();
    }

    private void installSplashEnterBinding() {
        InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = frame.getRootPane().getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "skipSplash");
        actionMap.put("skipSplash", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                skipSplashImmediately(splashOverlay);
            }
        });
    }

    private void removeSplashEnterBinding() {
        InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        frame.getRootPane().getActionMap().remove("skipSplash");
    }

    private void skipSplashImmediately(SplashOverlay overlay) {
        if (overlay == null || overlay != splashOverlay) {
            return;
        }
        if (splashHoldTimer != null) {
            splashHoldTimer.stop();
            splashHoldTimer = null;
        }
        if (splashFadeTimer != null) {
            splashFadeTimer.stop();
            splashFadeTimer = null;
        }
        stopSplashAudio();
        removeSplashEnterBinding();
        overlay.setVisible(false);
        splashImage = null;
        splashOverlay = null;
        splashFading = false;
        startBackgroundMusic();
    }

    private void fadeOutSplash(SplashOverlay overlay) {
        if (overlay == null || overlay != splashOverlay || splashFading) {
            return;
        }
        splashFading = true;
        if (splashHoldTimer != null) {
            splashHoldTimer.stop();
            splashHoldTimer = null;
        }
        stopSplashAudio();
        removeSplashEnterBinding();

        int stepMs = 30;
        int totalSteps = SPLASH_FADE_MS / stepMs;
        int[] step = {0};
        splashFadeTimer = new Timer(stepMs, null);
        splashFadeTimer.addActionListener(event -> {
            step[0]++;
            float opacity = 1f - (float) step[0] / totalSteps;
            if (opacity <= 0f) {
                overlay.setVisible(false);
                splashImage = null;
                splashOverlay = null;
                splashFading = false;
                splashFadeTimer.stop();
                splashFadeTimer = null;
                startBackgroundMusic();
            } else {
                overlay.setOpacity(opacity);
            }
        });
        splashFadeTimer.start();
    }

    private JPanel buildMainMenuScreen() {
        JPanel card = panel(new BorderLayout(), 38);
        card.setPreferredSize(new Dimension(780, 640));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(Box.createVerticalStrut(6));
        NodeEmblem emblem = new NodeEmblem(92);
        emblem.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(emblem);
        content.add(Box.createVerticalStrut(10));
        JLabel eyebrow = label("THE AI KNOWLEDGE CHALLENGE", 15, true, PERIWINKLE);
        eyebrow.setAlignmentX(Component.CENTER_ALIGNMENT);
        eyebrow.setHorizontalAlignment(SwingConstants.CENTER);
        eyebrow.setPreferredSize(new Dimension(490, 22));
        eyebrow.setMaximumSize(new Dimension(490, 22));
        content.add(eyebrow);
        content.add(Box.createVerticalStrut(7));
        JLabel title = label("Millionaire Mind", 48, true, TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(8));
        JLabel subtitle = label("AI fundamentals  ·  " + PrizeLadder.totalSlots()
        + " questions  ·  Normal or Timed", 19, false, MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        subtitle.setPreferredSize(new Dimension(490, 30));
        subtitle.setMaximumSize(new Dimension(490, 30));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(34));

        menuPlayButton = primaryButton("Play");
        menuPlayButton.setName("menuPlay");
        menuPlayButton.setMnemonic(KeyEvent.VK_P);
        menuPlayButton.addActionListener(event -> { playProgressSound(); showModeScreen(); });
        addMenuButton(content, menuPlayButton);
        content.add(Box.createVerticalStrut(12));
        JButton instructions = outlineButton("Instructions");
        instructions.setName("menuInstructions");
        instructions.setMnemonic(KeyEvent.VK_I);
        instructions.addActionListener(event -> { playProgressSound(); showInstructionsScreen(); });
        addMenuButton(content, instructions);
        content.add(Box.createVerticalStrut(12));
        JButton exit = outlineButton("Exit");
        exit.setName("menuExit");
        exit.setMnemonic(KeyEvent.VK_X);
        exit.addActionListener(event -> exitApplication());
        addMenuButton(content, exit);
        card.add(content, BorderLayout.CENTER);
        return centeredPage(card);
    }

    private JPanel buildModeScreen() {
        JPanel card = panel(new BorderLayout(), 38);
        card.setName("modeCard");
        card.setPreferredSize(new Dimension(780, 640));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = label("Choose your mode", 48, true, TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(10));
        JLabel subtitle = label("<html><div style='text-align: center;'>Normal Mode is untimed. Timed Mode gives you "
                + GameEngine.TIMER_SECONDS + " seconds per question.</div></html>", 21, false, MUTED);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setPreferredSize(new Dimension(650, 60));
        subtitle.setMaximumSize(new Dimension(650, 60));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(34));

        JButton normalMode = modeOptionButton("Normal Mode", "Unlimited time per question");
        normalMode.setName("modeNormal");
        normalMode.setMnemonic(KeyEvent.VK_N);
        normalMode.addActionListener(event -> { selectedTimedMode = false; playProgressSound(); showNameScreen(); });
        addMenuButton(content, normalMode);
        content.add(Box.createVerticalStrut(14));

        JButton timedMode = modeOptionButton("Timed Mode", GameEngine.TIMER_SECONDS + " seconds per question");
        timedMode.setName("modeTimed");
        timedMode.setMnemonic(KeyEvent.VK_T);
        timedMode.addActionListener(event -> { selectedTimedMode = true; playProgressSound(); showNameScreen(); });
        addMenuButton(content, timedMode);
        content.add(Box.createVerticalStrut(18));

        JButton back = outlineButton("Back to menu");
        back.setName("modeBack");
        back.setMnemonic(KeyEvent.VK_B);
        back.addActionListener(event -> { playBackSound(); showMainMenu(); });
        addMenuButton(content, back);

        card.add(content, BorderLayout.CENTER);
        return centeredPage(card);
    }

    private void showModeScreen() {
        showScreen("mode", true);
        frame.getRootPane().setDefaultButton(null);
    }

    private static JButton modeOptionButton(String title, String subtitle) {
        ModeOptionButton button = new ModeOptionButton(title, subtitle);
        button.setPreferredSize(new Dimension(560, 100));
        button.setMaximumSize(new Dimension(560, 100));
        return button;
    }

    private JPanel buildInstructionsScreen1() {
        JPanel card = panel(new BorderLayout(0, 16), 28);
        card.setName("instructionsCard");
        card.setPreferredSize(new Dimension(1000, 720));
        JLabel title = label("How to Play", 36, true, TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(24, 0));
        body.setOpaque(false);

        JPanel textColumn = new JPanel();
        textColumn.setOpaque(false);
        textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));

        JLabel gameplayTitle = label("General Gameplay", 25, true, GOLD);
        gameplayTitle.setAlignmentX(0);
        textColumn.add(gameplayTitle);
        textColumn.add(Box.createVerticalStrut(12));
        JTextArea gameplayText = instructionText("Answer 15 multiple-choice questions about "
                + "AI fundamentals across six levels of Bloom's Taxonomy. Choose an answer, "
                + "then select Lock in answer. Keep answering correctly to climb the prize "
                + "ladder and reach the $1,000,000 question.", 21, 680, 140);
        gameplayText.setName("instructionsText");
        textColumn.add(gameplayText);
        textColumn.add(Box.createVerticalStrut(24));

        JLabel prizeTitle = label("Prize Money", 25, true, GOLD);
        prizeTitle.setAlignmentX(0);
        textColumn.add(prizeTitle);
        textColumn.add(Box.createVerticalStrut(12));
        JTextArea prizeIntro = instructionText("Every correct answer moves you one step "
                + "closer to becoming a millionaire.", 21, 680, 40);
        prizeIntro.setName("prizeIntroText");
        textColumn.add(prizeIntro);
        textColumn.add(Box.createVerticalStrut(16));

        JPanel checkpointBox = panel(new BorderLayout(), 16);
        checkpointBox.setName("checkpointSummary");
        checkpointBox.setBackground(PANEL_DARK);
        checkpointBox.setAlignmentX(0);
        checkpointBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        JPanel checkpointLines = new JPanel(new GridLayout(2, 1, 0, 6));
        checkpointLines.setOpaque(false);
        checkpointLines.add(label("Reach Question 5: $2,000 is guaranteed", 20, true, TEXT));
        checkpointLines.add(label("Reach Question 10: $64,000 is guaranteed", 20, true, TEXT));
        checkpointBox.add(checkpointLines, BorderLayout.CENTER);
        textColumn.add(checkpointBox);
        textColumn.add(Box.createVerticalStrut(16));

        JTextArea prizeTerms = instructionText("Get an answer wrong and you'll fall back "
                + "to the latest checkpoint you've reached. You can also Walk Away "
                + "whenever you want and keep your guaranteed winnings.", 21, 680, 90);
        prizeTerms.setName("prizeTerms");
        textColumn.add(prizeTerms);

        body.add(textColumn, BorderLayout.CENTER);
        JPanel ladder = buildInstructionsLadder();
        ladder.setPreferredSize(new Dimension(240, 500));
        body.add(ladder, BorderLayout.EAST);
        card.add(body, BorderLayout.CENTER);

        JButton back = outlineButton("Back");
        back.setName("instructionsBack1");
        back.setEnabled(false);
        JButton next = outlineButton("Next");
        next.setName("instructionsNext1");
        next.setMnemonic(KeyEvent.VK_N);
        next.addActionListener(event -> { playProgressSound(); showInstructionsScreen2(); });
        card.add(buildInstructionsNavBar(back, next, "instructionsHome1"), BorderLayout.SOUTH);
        return centeredPage(card);
    }

    private JPanel buildInstructionsScreen2() {
        JPanel card = panel(new BorderLayout(0, 16), 28);
        card.setName("instructionsCard2");
        card.setPreferredSize(new Dimension(1000, 720));
        JLabel title = label("How to Play", 36, true, TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(title, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel lifelineTitle = label("Lifelines", 25, true, GOLD);
        lifelineTitle.setAlignmentX(0);
        content.add(lifelineTitle);
        content.add(Box.createVerticalStrut(14));

        JPanel lifelineBox = panel(new BorderLayout(0, 8), 16);
        lifelineBox.setName("lifelinePreviewBox");
        lifelineBox.setBackground(PANEL_DARK);
        lifelineBox.setAlignmentX(0);
        lifelineBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        lifelineBox.add(label("Lifelines", 18, false, MUTED), BorderLayout.NORTH);
        JPanel lifelineRow = new JPanel(new GridLayout(1, 4, 16, 0));
        lifelineRow.setOpaque(false);
        String[] names = {"50:50", "Spin the Wheel", "Switch the Question", "Phone a Friend"};
        for (int i = 0; i < names.length; i++) {
            StyledButton preview = new StyledButton(names[i], false, i);
            preview.setName("lifelinePreview" + i);
            preview.setFont(new Font("Segoe UI", Font.BOLD, 21));
            preview.setPreferredSize(new Dimension(160, 95));
            preview.setEnabledAppearanceWhenDisabled(true);
            preview.setEnabled(false);
            preview.setFocusable(false);
            lifelineRow.add(preview);
        }
        lifelineBox.add(lifelineRow, BorderLayout.CENTER);
        content.add(lifelineBox);
        content.add(Box.createVerticalStrut(22));

        JTextArea intro = instructionText("You're not completely on your own. You have "
                + "four lifelines, and each one can only be used once.", 21, 900, 40);
        intro.setName("lifelineIntroText");
        content.add(intro);
        content.add(Box.createVerticalStrut(12));

        addInstructionBullet(content, "lifelineInstruction0",
                "•  50:50 – Removes two wrong answers, leaving you with two choices.", 900, 36);
        content.add(Box.createVerticalStrut(10));
        addInstructionBullet(content, "lifelineInstruction1",
                "•  Spin the Wheel – Gives you a random advantage — it might remove a "
                + "wrong answer, narrow your choices, or give you bonus time.", 900, 60);
        content.add(Box.createVerticalStrut(10));
        addInstructionBullet(content, "lifelineInstruction2",
                "•  Switch the Question – Don't like the question? Swap it for another "
                + "one from the same Bloom's level.", 900, 60);
        content.add(Box.createVerticalStrut(10));
        addInstructionBullet(content, "lifelineInstruction3",
                "•  Phone a Friend – Get a hint to help you figure out the answer.", 900, 36);
        content.add(Box.createVerticalStrut(20));

        JTextArea outro = instructionText("Use them wisely. Once they're gone, they're gone.",
                21, 900, 40);
        outro.setName("lifelineOutroText");
        content.add(outro);

        card.add(content, BorderLayout.CENTER);

        JButton back = outlineButton("Back");
        back.setName("instructionsBack2");
        back.setMnemonic(KeyEvent.VK_B);
        back.addActionListener(event -> { playBackSound(); showInstructionsScreen(); });
        JButton next = outlineButton("Next");
        next.setName("instructionsNext2");
        next.setEnabled(false);
        card.add(buildInstructionsNavBar(back, next, "instructionsHome2"), BorderLayout.SOUTH);
        return centeredPage(card);
    }

    private JPanel buildInstructionsLadder() {
        JPanel ladder = panel(new BorderLayout(), 12);
        ladder.setName("instructionsLadder");
        ladder.setBackground(PANEL_DARK);
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        for (int slot = PrizeLadder.totalSlots(); slot >= 1; slot--) {
            boolean current = slot == 1;
            boolean checkpoint = PrizeLadder.isCheckpoint(slot);
            Color rowColor = current ? BLUE : checkpoint ? GOLD : PANEL_DARK;
            Color rowText = checkpoint && !current ? BACKGROUND : TEXT;
            JPanel row = new LadderRow(current, checkpoint);
            row.setName("instructionsLadderQ" + slot);
            row.setBackground(rowColor);
            row.setBorder(new EmptyBorder(2, 10, 2, 10));
            JLabel number = label(Integer.toString(slot), 18, true, rowText);
            JLabel value = label(money(PrizeLadder.prizeFor(slot)), 18, true, rowText);
            row.add(number, BorderLayout.WEST);
            row.add(value, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            rows.add(row);
            rows.add(Box.createVerticalStrut(1));
        }
        ladder.add(rows, BorderLayout.CENTER);
        return ladder;
    }

    private JPanel buildInstructionsNavBar(JButton back, JButton next, String homeName) {
        back.setPreferredSize(new Dimension(150, 42));
        next.setPreferredSize(new Dimension(150, 42));
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JPanel pager = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        pager.setOpaque(false);
        pager.add(back);
        pager.add(next);
        bar.add(pager, BorderLayout.CENTER);
        JPanel homeSlot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        homeSlot.setOpaque(false);
        HomeButton home = new HomeButton();
        home.setName(homeName);
        home.setToolTipText("Back to main menu");
        home.addActionListener(event -> { playBackSound(); showMainMenu(); });
        homeSlot.add(home);
        bar.add(homeSlot, BorderLayout.EAST);
        return bar;
    }

    private static JTextArea instructionText(String text, int size, int width, int height) {
        JTextArea area = textArea(size, false);
        area.setText(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setAlignmentX(0);
        area.setPreferredSize(new Dimension(width, height));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return area;
    }

    private static void addInstructionBullet(JPanel content, String name,
            String text, int width, int height) {
        JTextArea bullet = instructionText(text, 21, width, height);
        bullet.setName(name);
        content.add(bullet);
    }

    private JPanel buildNameScreen() {
        JPanel card = panel(new BorderLayout(), 34);
        card.setName("nameCard");
        card.setPreferredSize(new Dimension(760, 540));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel title = label("Ready to play?", 42, true, TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(Box.createVerticalStrut(9));
        content.add(title);
        content.add(Box.createVerticalStrut(16));
        JLabel description = label("Enter your name to begin the 15-question game.", 19, false, MUTED);
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        description.setHorizontalAlignment(SwingConstants.CENTER);
        description.setPreferredSize(new Dimension(490, 30));
        description.setMaximumSize(new Dimension(490, 30));
        content.add(description);
        content.add(Box.createVerticalStrut(28));
        JLabel namePrompt = label("Player name", 17, true, TEXT);
        namePrompt.setName("namePrompt");
        namePrompt.setHorizontalAlignment(SwingConstants.CENTER);
        namePrompt.setAlignmentX(Component.CENTER_ALIGNMENT);
        namePrompt.setPreferredSize(new Dimension(360, 27));
        namePrompt.setMaximumSize(new Dimension(360, 27));
        namePrompt.setLabelFor(nameField);
        content.add(namePrompt);
        content.add(Box.createVerticalStrut(8));
        nameField.setName("playerName");
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 21));
        nameField.setBackground(PANEL_DARK);
        nameField.setForeground(TEXT);
        nameField.setCaretColor(TEXT);
        nameField.setSelectionColor(BLUE);
        nameField.setSelectedTextColor(TEXT);
        nameField.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        nameField.setPreferredSize(new Dimension(440, 58));
        nameField.setMaximumSize(new Dimension(440, 58));
        nameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(nameField);
        content.add(Box.createVerticalStrut(6));
        nameError.setName("nameError");
        nameError.setHorizontalAlignment(SwingConstants.CENTER);
        nameError.setPreferredSize(new Dimension(400, 22));
        nameError.setMaximumSize(new Dimension(400, 22));
        nameError.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(nameError);
        content.add(Box.createVerticalStrut(13));
        startButton = primaryButton("Start game");
        startButton.setName("startGame");
        startButton.setMnemonic(KeyEvent.VK_S);
        startButton.addActionListener(event -> startSession());
        addMenuButton(content, startButton);
        content.add(Box.createVerticalStrut(9));
        JButton back = outlineButton("Back to menu");
        back.setName("nameBack");
        back.setMnemonic(KeyEvent.VK_B);
        back.addActionListener(event -> { playBackSound(); showMainMenu(); });
        addMenuButton(content, back);
        card.add(content, BorderLayout.CENTER);
        return centeredPage(card);
    }

    private JPanel centeredPage(JPanel card) {
        JPanel page = page();
        page.add(header(false), BorderLayout.NORTH);
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(20, 20, 20, 20));
        center.add(card);
        // Preserve readable card sizes; smaller displays can scroll instead of clipping controls.
        JScrollPane scroll = new JScrollPane(center);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    private static void addMenuButton(JPanel content, JButton button) {
        if (button.getPreferredSize().width < 300) {
            button.setPreferredSize(new Dimension(320, 60));
            button.setMaximumSize(new Dimension(320, 60));
            button.setFont(new Font("Segoe UI", Font.BOLD, 20));
        }
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(button);
    }

    private void showMainMenu() {
        showScreen("menu", true);
        frame.getRootPane().setDefaultButton(menuPlayButton);
        menuPlayButton.requestFocusInWindow();
    }

    private void showInstructionsScreen() {
        showScreen("instructions1", true);
        frame.getRootPane().setDefaultButton(null);
    }

    private void showInstructionsScreen2() {
        showScreen("instructions2", true);
        frame.getRootPane().setDefaultButton(null);
    }

    private void showNameScreen() {
        showScreen("name", true);
        nameError.setText("");
        frame.getRootPane().setDefaultButton(startButton);
        nameField.requestFocusInWindow();
    }

    private JPanel buildGameScreen() {
        JPanel page = page();
        page.setName("gamePage");
        page.add(header(true), BorderLayout.NORTH);
        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(8, 12, 8, 12));
        content.add(buildQuestionPanel(), BorderLayout.CENTER);
        JPanel ladder = buildLadderPanel();
        ladder.setPreferredSize(new Dimension(350, 650));
        content.add(ladder, BorderLayout.EAST);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildQuestionPanel() {
        JPanel main = panel(new BorderLayout(0, 6), 6);
        main.setName("questionPanel");
        main.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                fitQuestionContent();
            }
        });

        JPanel top = new JPanel(new BorderLayout(14, 0));
        top.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(levelLabel);
        left.add(positionLabel);
        top.add(left, BorderLayout.WEST);
        JPanel prize = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        prize.setOpaque(false);
        prize.add(label("Playing for", 15, false, MUTED));
        prize.add(prizeLabel);
        top.add(prize, BorderLayout.EAST);
        JPanel heading = new JPanel(new BorderLayout(0, 7));
        heading.setOpaque(false);
        heading.add(top, BorderLayout.NORTH);
        heading.add(progressStrip, BorderLayout.SOUTH);
        main.add(heading, BorderLayout.NORTH);

        JPanel body = new ScrollableColumn();
        questionBody = body;
        promptBox = panel(new BorderLayout(10, 0), 10);
        promptBox.setBackground(PANEL_DARK);
        JPanel promptAccent = new JPanel();
        promptAccent.setBackground(PERIWINKLE);
        promptAccent.setPreferredSize(new Dimension(4, 10));
        promptBox.add(promptAccent, BorderLayout.WEST);
        promptText.setName("questionText");
        promptText.setFont(new Font("Segoe UI", Font.BOLD, 30));
        promptText.setEditable(false);
        promptText.setFocusable(false);
        promptText.setBackground(PANEL_DARK);
        promptText.setPreferredSize(new Dimension(400, 112));
        promptText.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                adjustPromptHeight();
            }
        });
        promptBox.add(promptText, BorderLayout.CENTER);
        promptBox.setAlignmentX(0);
        body.add(promptBox);
        body.add(Box.createVerticalStrut(6));

        answerGrid.setOpaque(false);
        answerGrid.setName("answerGrid");
        answerGrid.setLayout(new BoxLayout(answerGrid, BoxLayout.Y_AXIS));
        answerGrid.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                adjustAnswerGridHeight();
            }
        });
        for (int i = 0; i < answerButtons.length; i++) {
            if (i % 2 == 0) {
                if (i > 0) answerGrid.add(Box.createVerticalStrut(14));
                JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
                row.setOpaque(false);
                row.setAlignmentX(0);
                answerRows[i / 2] = row;
                answerGrid.add(row);
            }
            final int index = i;
            AnswerButton answer = new AnswerButton(i);
            answer.setName("answer" + (char) ('A' + i));
            answer.setMnemonic(KeyEvent.VK_A + i);
            answer.addActionListener(event -> selectAnswer(index));
            answerGroup.add(answer);
            answerButtons[i] = answer;
            answerRows[i / 2].add(answer);
        }
        answerGrid.setPreferredSize(new Dimension(650, 206));
        answerGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 206));
        answerGrid.setAlignmentX(0);
        body.add(answerGrid);
        body.add(Box.createVerticalStrut(5));

        JPanel lockRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        lockRow.setOpaque(false);
        lockButton.setName("lockAnswer");
        lockButton.setMnemonic(KeyEvent.VK_L);
        lockButton.setEnabled(false);
        lockButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lockButton.setPreferredSize(new Dimension(320, 54));
        lockButton.addActionListener(event -> lockAnswer());
        lockRow.add(lockButton);
        lockRow.setAlignmentX(0);
        body.add(lockRow);
        body.add(Box.createVerticalStrut(4));

        JPanel divider = new JPanel();
        divider.setBackground(BORDER);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setAlignmentX(0);
        body.add(divider);
        body.add(Box.createVerticalStrut(4));
        JLabel lifelineTitle = label("Lifelines", 19, true, MUTED);
        lifelineTitle.setAlignmentX(0);
        body.add(lifelineTitle);
        body.add(Box.createVerticalStrut(3));
        JPanel lifelines = new JPanel(new GridLayout(1, 4, 10, 0));
        lifelines.setOpaque(false);
        lifelines.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        String[] names = {"50:50", "Spin the Wheel", "Switch the Question", "Phone a Friend"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            StyledButton button = new StyledButton(names[i], false, i);
            button.setName("lifeline" + i);
            button.setMnemonic(KeyEvent.VK_1 + i);
            button.setFont(new Font("Segoe UI", Font.BOLD, 22));
            button.setPreferredSize(new Dimension(150, 115));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));
            button.addActionListener(event -> useLifeline(index));
            lifelineButtons[i] = button;
            lifelines.add(button);
        }
        lifelines.setAlignmentX(0);
        body.add(lifelines);
        body.add(Box.createVerticalStrut(3));
        noticeText.setName("lifelineNotice");
        noticeText.setEditable(false);
        noticeText.setFocusable(false);
        noticeText.setForeground(MUTED);
        noticeText.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        noticeText.setPreferredSize(new Dimension(500, 36));
        noticeText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        noticeText.setAlignmentX(0);
        body.add(noticeText);
        JScrollPane questionScroll = new JScrollPane(body);
        questionScroll.setName("questionContentScroll");
        questionScroll.setBorder(null);
        questionScroll.setOpaque(false);
        questionScroll.getViewport().setOpaque(false);
        questionScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        questionScroll.getVerticalScrollBar().setUnitIncrement(24);
        main.add(questionScroll, BorderLayout.CENTER);
        return main;
    }

    private JPanel buildTimerBox() {
        JPanel box = panel(new FlowLayout(FlowLayout.CENTER, 8, 0), 10);
        box.setName("timerBox");
        box.setBackground(PANEL_DARK);
        JLabel caption = label("Time Left:", 14, false, MUTED);
        caption.setName("timerCaption");
        box.add(caption);
        box.add(timerLabel);
        return box;
    }

    private JPanel buildFeedbackScreen() {
        JPanel page = page();
        page.add(header(false), BorderLayout.NORTH);
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(24, 24, 24, 24));
        JPanel card = panel(new BorderLayout(0, 18), 30);
        card.setName("feedbackPanel");
        card.setPreferredSize(new Dimension(720, 540));
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        feedbackEmblem.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(feedbackEmblem);
        body.add(Box.createVerticalStrut(13));
        feedbackTitle.setName("feedbackTitle");
        feedbackTitle.setHorizontalAlignment(SwingConstants.CENTER);
        feedbackTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        feedbackTitle.setPreferredSize(new Dimension(620, 42));
        feedbackTitle.setMaximumSize(new Dimension(620, 42));
        body.add(feedbackTitle);
        body.add(Box.createVerticalStrut(20));
        feedbackDetail.setName("feedbackDetail");
        feedbackDetail.setEditable(false);
        feedbackDetail.setFocusable(false);
        feedbackDetail.setBackground(PANEL_DARK);
        feedbackDetail.setBorder(new EmptyBorder(15, 18, 15, 18));
        JScrollPane feedbackScroll = new JScrollPane(feedbackDetail);
        feedbackScroll.setBorder(null);
        feedbackScroll.setPreferredSize(new Dimension(620, 220));
        feedbackScroll.setMaximumSize(new Dimension(620, 220));
        feedbackScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(feedbackScroll);
        card.add(body, BorderLayout.CENTER);
        continueButton.setName("continue");
        continueButton.setMnemonic(KeyEvent.VK_C);
        continueButton.setPreferredSize(new Dimension(220, 54));
        continueButton.addActionListener(event -> {
            if (!"feedback".equals(currentScreen)) return;
            playProgressSound();
            if (engine.isGameOver()) finishGame();
            else {
                renderQuestion();
                showScreen("game", false);
                frame.validate();
                fitQuestionContent();
            }
        });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER));
        actions.setOpaque(false);
        actions.add(continueButton);
        card.add(actions, BorderLayout.SOUTH);
        center.add(card);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildLadderPanel() {
        JPanel ladder = panel(new BorderLayout(0, 12), 16);
        ladder.setName("prizeLadder");
        ladder.setBackground(PANEL_DARK);
        ladder.add(label("Prize ladder", 23, true, TEXT), BorderLayout.NORTH);

        ladderRows.setOpaque(false);
        ladderRows.setLayout(new GridLayout(PrizeLadder.totalSlots(), 1, 0, 1));
        ladder.add(ladderRows, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        
        JLabel guaranteedTitle = label("Guaranteed", 15, false, MUTED);
        guaranteedTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(guaranteedTitle);
        
        guaranteedLabel.setName("guaranteedAmount");
        guaranteedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(guaranteedLabel);
        
        checkpointLabel.setName("checkpointStatus");
        checkpointLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(checkpointLabel);
        
        footer.add(Box.createVerticalStrut(11));
        
        walkButton.setName("walkAway");
        walkButton.setMnemonic(KeyEvent.VK_W);
        walkButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        walkButton.setPreferredSize(new Dimension(260, 58));
        walkButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        walkButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        walkButton.addActionListener(event -> {
            if (!activeGame()) return;
            tickTimer();
            if (!activeGame()) return;
            playProgressSound();
            stopQuestionTimer();
            engine.walkAway();
            finishGame();
        });
        footer.add(walkButton);
        ladder.add(footer, BorderLayout.SOUTH);
        return ladder;
    }

    private JPanel header(boolean showPlayer) {
        JPanel header = new NodeHeader(new BorderLayout());
        header.setName(showPlayer ? "gameHeader" : "plainHeader");
        header.setBorder(new EmptyBorder(8, 28, 8, 28));
        JPanel brand = new JPanel(new BorderLayout(16, 0));
        brand.setOpaque(false);
        brand.add(new JLabel(new NodeIcon()), BorderLayout.WEST);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(label("Millionaire Mind", 30, true, TEXT));
        titleBlock.add(label("AI fundamentals", 16, false, MUTED));
        brand.add(titleBlock, BorderLayout.CENTER);
        header.add(brand, BorderLayout.WEST);
        if (showPlayer) {
            timerLabel.setName("questionTimer");
            JPanel timerSlot = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            timerSlot.setOpaque(false);
            timerSlot.add(timerBox);
            header.add(timerSlot, BorderLayout.CENTER);

            JPanel player = new JPanel();
            player.setOpaque(false);
            player.setLayout(new BoxLayout(player, BoxLayout.Y_AXIS));
            player.add(playerLabel);
            player.add(correctCountLabel);
            JPanel profile = new JPanel(new BorderLayout(12, 0));
            profile.setOpaque(false);
            profile.add(new JLabel(new AvatarIcon()), BorderLayout.WEST);
            profile.add(player, BorderLayout.CENTER);
            header.add(profile, BorderLayout.EAST);
        }
        return header;
    }

    private void startSession() {
        if (!"name".equals(currentScreen)) return;
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            playSfx(SFX_ACTION_ERROR);
            nameError.setText("Enter a player name to start.");
            nameField.requestFocusInWindow();
            nameField.setBorder(new CompoundBorder(new LineBorder(GOLD, 2, true),
                    new EmptyBorder(9, 11, 9, 11)));
            return;
        }
        playProgressSound();
        nameError.setText("");
        nameField.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        engine = new GameEngine(questionBank, random, name, selectedTimedMode);
        renderQuestion();
        showScreen("game", false);
        frame.validate();
        fitQuestionContent();
    }

    private boolean activeGame() {
        return "game".equals(currentScreen) && engine != null && !engine.isGameOver();
    }

    private void selectAnswer(int index) {
        if (!activeGame()) return;
        if (!engine.isOptionVisible(index)) {
            playSfx(SFX_ACTION_ERROR);
            return;
        }
        selectedIndex = index;
        playSfx(SFX_ANSWER_SELECT);
        lockButton.setEnabled(true);
        for (AnswerButton button : answerButtons) button.repaint();
    }

    private void lockAnswer() {
        if (!activeGame()) return;
        tickTimer();
        if (!activeGame()) return;
        if (selectedIndex < 0 || !engine.isOptionVisible(selectedIndex)) {
            playSfx(SFX_ACTION_ERROR);
            return;
        }
        playSfx(SFX_ANSWER_SELECT);
        stopQuestionTimer();
        Question resolved = engine.currentQuestion();
        GameEngine.AnswerOutcome outcome = engine.answer(selectedIndex);
        boolean correct = outcome == GameEngine.AnswerOutcome.CORRECT;
        if (correct) {
            playSfx(SFX_CORRECT_ANSWER);
        } else {
            playSfx(SFX_WRONG_ANSWER);
        }
        feedbackTitle.setText(outcome == GameEngine.AnswerOutcome.CORRECT
                ? "Correct answer" : "Incorrect answer");
        feedbackTitle.setForeground(correct ? GREEN : RED);
        feedbackEmblem.setCorrect(correct);
        feedbackDetail.setText("Correct answer: " + (char) ('A' + resolved.correctIndex())
                + ") " + resolved.correctOptionText() + "\n\nSource: " + resolved.citation());
        updateSessionHeader();
        showScreen("feedback", false);
        frame.getRootPane().setDefaultButton(continueButton);
        continueButton.requestFocusInWindow();
    }

    private void useLifeline(int index) {
        if (!activeGame()) return;
        tickTimer();
        if (!activeGame()) return;
        LifelineManager.Lifeline[] types = LifelineManager.Lifeline.values();
        if (index < 0 || index >= types.length) {
            throw new IllegalArgumentException("Unknown lifeline");
        }
        if (!engine.lifelines().isAvailable(types[index])) {
            playSfx(SFX_ACTION_ERROR);
            noticeText.setText("That lifeline has already been used.");
            return;
        }

        try {
            switch (index) {
                case 0 -> {
                    engine.useFiftyFifty();
                    noticeText.setText("50:50 removed two incorrect options.");
                }
                case 1 -> {
                    LifelineManager.SpinOutcome outcome = engine.useSpinTheWheel();
                    switch (outcome) {
                        case REVEAL_ONE_WRONG_OPTION ->
                            noticeText.setText("Spin the Wheel removed one incorrect option.");
                        case REDUCE_TWO_OPTIONS ->
                            noticeText.setText("Spin the Wheel reduced the choices to two.");
                        case GRANT_BONUS_TIME -> {
                            if (engine.isTimedMode()) {
                                updateTimerLabel();
                                noticeText.setText("Spin the Wheel granted +" + GameEngine.BONUS_TIME_SECONDS + " bonus seconds!");
                            } else {
                                noticeText.setText("Spin the Wheel granted +" + GameEngine.BONUS_TIME_SECONDS
                                        + " bonus seconds. This only applies in Timed Mode.");
                            }
                        }
                    }
                }
                case 2 -> {
                    engine.switchQuestion();
                    renderQuestion();
                    startOrStopQuestionTimer();
                    frame.validate();
                    fitQuestionContent();
                    noticeText.setText("New question from the same Bloom level.");
                }
                case 3 -> noticeText.setText("Phone a Friend: " + engine.usePhoneAFriend());
                default -> throw new IllegalArgumentException("Unknown lifeline");
            }
            playBackSound();
            refreshAnswerButtons();
            refreshLifelines();
            frame.validate();
            fitQuestionContent();
        } catch (IllegalStateException e) {
            playSfx(SFX_ACTION_ERROR);
            noticeText.setText(e.getMessage());
            refreshLifelines();
        }
    }

    private void renderQuestion() {
        Question question = engine.currentQuestion();
        updateSessionHeader();
        levelLabel.setText(engine.currentLevel().displayName());
        positionLabel.setText("Question " + engine.currentSlot() + " of " + PrizeLadder.totalSlots());
        progressStrip.setSlot(engine.currentSlot());
        prizeLabel.setText(money(engine.currentPrize()));
        promptText.setText(question.prompt());
        promptText.setCaretPosition(0);
        promptText.setToolTipText(question.prompt());
        adjustPromptHeight();
        answerGroup.clearSelection();
        selectedIndex = -1;
        lockButton.setEnabled(false);
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setOptionText(question.options().get(i));
        }
        adjustAnswerGridHeight();
        noticeText.setText("");
        refreshAnswerButtons();
        refreshLifelines();
        renderLadder();
        frame.getRootPane().setDefaultButton(lockButton);
    }

    private void updateSessionHeader() {
        playerLabel.setText(engine.session().playerName());
        long correct = engine.session().history().stream().filter(result -> result.correct).count();
        String modeText = engine.isTimedMode() ? "Timed" : "Untimed";
        correctCountLabel.setText(correct + " correct  ·  " + modeText);
    }

    private void adjustPromptHeight() {
        int width = promptText.getWidth();
        if (width <= 0) return;
        View textView = promptText.getUI().getRootView(promptText);
        textView.setSize(Math.max(80, width), Short.MAX_VALUE);
        int height = Math.max(112, (int) Math.ceil(textView.getPreferredSpan(View.Y_AXIS)) + 12);
        Dimension preferred = promptText.getPreferredSize();
        if (preferred.width != width || preferred.height != height) {
            promptText.setPreferredSize(new Dimension(width, height));
            promptBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, height + 22));
            promptText.revalidate();
            promptBox.revalidate();
        }
    }

    private void fitQuestionContent() {
        if (engine == null || questionBody.getHeight() <= 0) return;
        int availableHeight = questionBody.getParent().getHeight();
        View noticeView = noticeText.getUI().getRootView(noticeText);
        noticeView.setSize(Math.max(80, questionBody.getWidth()), Short.MAX_VALUE);
        int noticeHeight = Math.max(42, (int) Math.ceil(noticeView.getPreferredSpan(View.Y_AXIS)) + 6);
        noticeText.setPreferredSize(new Dimension(500, noticeHeight));
        noticeText.setMaximumSize(new Dimension(Integer.MAX_VALUE, noticeHeight));
        for (int step = 0; step <= 12; step++) {
            int promptSize = Math.max(20, 30 - step);
            int answerSize = Math.max(15, 20 - step / 2);
            if (promptText.getFont().getSize() != promptSize) {
                promptText.setFont(new Font("Segoe UI", Font.BOLD, promptSize));
            }
            for (AnswerButton answer : answerButtons) answer.setOptionFontSize(answerSize);
            adjustPromptHeight();
            adjustAnswerGridHeight();
            frame.validate();
            if (questionBody.getPreferredSize().height <= availableHeight) break;
        }

        int spare = availableHeight - questionBody.getPreferredSize().height;
        if (spare > 24 && answerRows[0] != null && answerRows[1] != null) {
            int addedPerRow = Math.min(64, spare / 2);
            int[] heights = new int[2];
            for (int row = 0; row < answerRows.length; row++) {
                heights[row] = answerRows[row].getPreferredSize().height + addedPerRow;
                answerRows[row].setPreferredSize(new Dimension(650, heights[row]));
                answerRows[row].setMaximumSize(new Dimension(Integer.MAX_VALUE, heights[row]));
                for (int option = row * 2; option < row * 2 + 2; option++) {
                    answerButtons[option].setPreferredSize(new Dimension(250, heights[row]));
                }
            }
            int gridHeight = heights[0] + heights[1] + 14;
            answerGrid.setPreferredSize(new Dimension(650, gridHeight));
            answerGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, gridHeight));
            answerGrid.revalidate();
        }
    }

    private void adjustAnswerGridHeight() {
        int width = answerGrid.getWidth();
        if (width <= 0) width = answerGrid.getPreferredSize().width;
        int buttonWidth = Math.max(150, (width - 14) / 2);
        int[] heights = {96, 96};
        for (int i = 0; i < answerButtons.length; i++) {
            if (answerButtons[i] != null) {
                heights[i / 2] = Math.max(heights[i / 2],
                        answerButtons[i].requiredHeight(buttonWidth));
            }
        }
        int height = heights[0] + heights[1] + 14;
        if (answerGrid.getPreferredSize().height == height
                && answerRows[0].getPreferredSize().height == heights[0]
                && answerRows[1].getPreferredSize().height == heights[1]) return;
        for (int row = 0; row < answerRows.length; row++) {
            answerRows[row].setPreferredSize(new Dimension(650, heights[row]));
            answerRows[row].setMaximumSize(new Dimension(Integer.MAX_VALUE, heights[row]));
            for (int option = row * 2; option < row * 2 + 2; option++) {
                answerButtons[option].setPreferredSize(new Dimension(250, heights[row]));
            }
        }
        answerGrid.setPreferredSize(new Dimension(650, height));
        answerGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        answerGrid.revalidate();
    }

    private void startOrStopQuestionTimer() {
        stopQuestionTimer();
        timerBox.setVisible(engine.isTimedMode());
        if (engine.isTimedMode()) {
            lastTimerNanos = System.nanoTime();
            updateTimerLabel();
            questionTimer = new Timer(100, event -> tickTimer());
            questionTimer.start();
        }
    }

    private void tickTimer() {
        if (questionTimer == null || !activeGame()) return;
        long now = System.nanoTime();
        long elapsed = (now - lastTimerNanos) / 1_000_000_000L;
        if (elapsed == 0) return;
        lastTimerNanos += elapsed * 1_000_000_000L;
        engine.advanceTime((int) Math.min(elapsed, Integer.MAX_VALUE));
        updateTimerLabel();
        if (engine.isGameOver()) {
            stopQuestionTimer();
            handleTimeExpired();
        }
    }

    private void updateTimerLabel() {
        timerLabel.setText(engine.secondsRemaining() + "s");
        timerLabel.setUrgent(engine.secondsRemaining() <= 10);
    }

    private void stopQuestionTimer() {
        if (questionTimer != null) {
            questionTimer.stop();
            questionTimer = null;
        }
    }

    private void playResultsSound(String fileName) {
        audio.play(GameAudio.Channel.RESULTS, "/sfx/" + fileName, 1f, false);
    }

    private void stopResultsAudio() {
        audio.stop(GameAudio.Channel.RESULTS);
    }

    private void handleTimeExpired() {
        Question resolved = engine.currentQuestion();
        feedbackTitle.setText("Time's up!");
        feedbackTitle.setForeground(RED);
        feedbackEmblem.setCorrect(false);
        feedbackDetail.setText("You ran out of time.\n\nCorrect answer: "
                + (char) ('A' + resolved.correctIndex()) + ") " + resolved.correctOptionText()
                + "\n\nSource: " + resolved.citation());
        updateSessionHeader();
        showScreen("feedback", false);
        frame.getRootPane().setDefaultButton(continueButton);
        continueButton.requestFocusInWindow();
        playSfx(SFX_WRONG_ANSWER);
    }

    private void refreshAnswerButtons() {
        for (int i = 0; i < answerButtons.length; i++) {
            boolean visible = engine.isOptionVisible(i);
            answerButtons[i].setOptionVisible(visible);
            if (!visible && selectedIndex == i) {
                answerGroup.clearSelection();
                selectedIndex = -1;
                lockButton.setEnabled(false);
            }
        }
    }

    private void refreshLifelines() {
        LifelineManager.Lifeline[] types = LifelineManager.Lifeline.values();
        for (int i = 0; i < types.length; i++) {
            lifelineButtons[i].setAvailable(engine.lifelines().isAvailable(types[i]));
        }
    }

    private void renderLadder() {
        ladderRows.removeAll();
        for (int slot = PrizeLadder.totalSlots(); slot >= 1; slot--) {
            boolean current = slot == engine.currentSlot();
            boolean checkpoint = PrizeLadder.isCheckpoint(slot);
            Color rowColor = current ? BLUE : checkpoint ? GOLD : PANEL_DARK;
            Color rowText = checkpoint && !current ? BACKGROUND : TEXT;
            JPanel row = new LadderRow(current, checkpoint);
            row.setName("ladderQ" + slot);
            row.setBackground(rowColor);
            row.setBorder(new EmptyBorder(2, 11, 2, 11));
            JLabel number = label(Integer.toString(slot), 16, true, rowText);
            JLabel value = label(money(PrizeLadder.prizeFor(slot)), 16, true, rowText);
            row.add(number, BorderLayout.WEST);
            row.add(value, BorderLayout.EAST);
            ladderRows.add(row);
        }
        long guaranteed = engine.guaranteedBank();
        guaranteedLabel.setText(money(guaranteed));
        checkpointLabel.setText(guaranteed >= PrizeLadder.prizeFor(10)
                ? "Q10 checkpoint secured"
                : guaranteed >= PrizeLadder.prizeFor(5)
                ? "Q5 checkpoint secured" : "No checkpoint secured");
        walkButton.setText("Walk Away · " + money(guaranteed));
        ladderRows.revalidate();
        ladderRows.repaint();
    }

    private void finishGame() {
        stopQuestionTimer();
        PlayerSession session = engine.session();
        if (session.becameMillionaire()) {
            playResultsSound(SFX_SUPREME_VICTORY);
        } else if (session.bankedWinnings() >= PrizeLadder.prizeFor(5)) {
            playResultsSound(SFX_REGULAR_WIN);
        } else {
            playResultsSound(SFX_NO_CHECKPOINT);
        }
        Path logPath = null;
        String logError = null;
        try {
            logPath = replayLogger.logSession(engine.session());
        } catch (UncheckedIOException e) {
            logError = e.getMessage();
            System.err.println("Replay log could not be saved: " + logError);
        }
        if (resultsPage != null) root.remove(resultsPage);
        resultsPage = buildResultsScreen(logPath, logError);
        root.add(resultsPage, "results");
        showScreen("results", false);
    }

    private JPanel buildResultsScreen(Path logPath, String logError) {
        JPanel card = panel(new BorderLayout(0, 10), 24);
        card.setName("resultsCard");
        card.setPreferredSize(new Dimension(920, 780));
        
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        JLabel title = label("Game report", 40, true, TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        details.add(title);
        details.add(Box.createVerticalStrut(6));

        PlayerSession session = engine.session();
        JLabel player = label("Player: " + session.playerName(), 20, false, MUTED);
        player.setAlignmentX(Component.CENTER_ALIGNMENT);
        details.add(player);
        details.add(Box.createVerticalStrut(8));

        String outcome = session.becameMillionaire() ? "Millionaire!"
                : session.walkedAway() ? "Walked away" : "Game over";
        JLabel result = label(outcome + "  ·  " + money(session.bankedWinnings()),
                34, true, GOLD);
        result.setName("resultOutcome");
        result.setAlignmentX(Component.CENTER_ALIGNMENT);
        details.add(result);
        details.add(Box.createVerticalStrut(14));

        JPanel reportPanel = panel(new BorderLayout(0, 8), 18);
        reportPanel.setName("bloomReportPanel");
        reportPanel.setBackground(PANEL_DARK);
        reportPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        reportPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        reportPanel.add(label("Bloom report", 22, true, TEXT), BorderLayout.NORTH);

        JPanel reportRows = new JPanel(new GridLayout(BloomLevel.values().length, 1, 0, 4));
        reportRows.setOpaque(false);
        Map<BloomLevel, int[]> report = session.bloomReport();
        for (BloomLevel level : BloomLevel.values()) {
            int[] count = report.get(level);
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.add(label(level.displayName(), 19, false, MUTED), BorderLayout.WEST);
            row.add(label(count[0] + " / " + (count[0] + count[1]) + " correct",
                    19, true, TEXT), BorderLayout.EAST);
            reportRows.add(row);
        }
        reportPanel.add(reportRows, BorderLayout.CENTER);
        details.add(reportPanel);
        details.add(Box.createVerticalStrut(10));

        JLabel summary = null;
        if (session.becameMillionaire()) {
            summary = label("All Bloom levels tied at 100%; no unique strongest or weakest.",
                    17, false, MUTED);
        } else if (!session.history().isEmpty()
                && session.strongestLevel() != session.weakestLevel()) {
            summary = label("Strongest: " + session.strongestLevel().displayName()
                    + "  ·  Weakest: " + session.weakestLevel().displayName(), 17, false, MUTED);
        }
        if (summary != null) {
            summary.setAlignmentX(Component.CENTER_ALIGNMENT);
            details.add(summary);
            details.add(Box.createVerticalStrut(10));
        }

        JLabel logStatus = label(logPath != null ? "Replay saved"
                : "Replay could not be saved. Check that the logs folder is writable.", 17, false, MUTED);
        logStatus.setToolTipText(logPath != null ? logPath.toAbsolutePath().toString() : logError);
        logStatus.setName("logStatus");
        logStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        details.add(logStatus);

        if (logPath != null) {
            details.add(Box.createVerticalStrut(6));
            JButton openLog = outlineButton("Open " + logPath.getFileName());
            openLog.setName("openSessionLog");
            openLog.setFont(new Font("Segoe UI", Font.BOLD, 18));
            openLog.setToolTipText(logPath.toAbsolutePath().toString());
            openLog.getAccessibleContext().setAccessibleDescription(
                    "Open session replay file " + logPath.getFileName());
            openLog.setAlignmentX(Component.CENTER_ALIGNMENT);
            openLog.addActionListener(event -> {
                playProgressSound();
                openReplay(logPath, logStatus);
            });
            details.add(openLog);
        }

        JScrollPane scrollPane = new JScrollPane(details);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        card.add(scrollPane, BorderLayout.CENTER);

        JButton again = primaryButton("Play again");
        again.setName("playAgain");
        again.setFont(new Font("Segoe UI", Font.BOLD, 20));
        again.addActionListener(event -> {
            nameField.setText("");
            playProgressSound();
            showNameScreen();
        });
        JButton menu = outlineButton("Main menu");
        menu.setName("resultsMenu");
        menu.setFont(new Font("Segoe UI", Font.BOLD, 20));
        menu.addActionListener(event -> { playBackSound(); showMainMenu(); });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        actions.setOpaque(false);
        again.setPreferredSize(new Dimension(220, 54));
        menu.setPreferredSize(new Dimension(220, 54));
        actions.add(again);
        actions.add(menu);
        card.add(actions, BorderLayout.SOUTH);

        return centeredPage(card);
    }

    private static void openReplay(Path logPath, JLabel status) {
        if (!Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            status.setText("Opening files is unavailable on this desktop.");
            return;
        }
        try {
            Desktop.getDesktop().open(logPath.toFile());
        } catch (IOException | SecurityException e) {
            status.setText("Could not open " + logPath.getFileName() + ": " + e.getMessage());
        }
    }

    private static JPanel page() {
        return new PageBackground();
    }

    /** Keeps wrapped text complete when a small display needs a vertical scrollbar. */
    private static final class ScrollableColumn extends JPanel implements javax.swing.Scrollable {
        ScrollableColumn() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 24; }
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(24, visible.height - 24);
        }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() >= getPreferredSize().height;
        }
    }

    private static JPanel panel(java.awt.LayoutManager layout, int padding) {
        return new SurfacePanel(layout, padding);
    }

    private static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    private static JTextArea textArea(int size, boolean bold) {
        JTextArea area = new JTextArea();
        area.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        area.setForeground(TEXT);
        area.setBackground(PANEL);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        return area;
    }

    private static JButton primaryButton(String text) {
        return new StyledButton(text, true, -1);
    }

    private static JButton outlineButton(String text) {
        return new StyledButton(text, false, -1);
    }

    private static String money(long amount) {
        return String.format(Locale.US, "$%,d", amount);
    }

    private final class SplashOverlay extends JPanel {
        private float opacity = 1f;

        SplashOverlay() {
            setOpaque(false);
        }

        void setOpacity(float opacity) {
            this.opacity = opacity;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (splashImage != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int imgWidth = splashImage.getWidth(this);
                int imgHeight = splashImage.getHeight(this);
                if (imgWidth > 0 && imgHeight > 0) {
                    double scale = Math.max((double) getWidth() / imgWidth,
                            (double) getHeight() / imgHeight);
                    int drawWidth = (int) Math.round(imgWidth * scale);
                    int drawHeight = (int) Math.round(imgHeight * scale);
                    int drawX = (getWidth() - drawWidth) / 2;
                    int drawY = (getHeight() - drawHeight) / 2;
                    g2.drawImage(splashImage, drawX, drawY, drawWidth, drawHeight, this);
                }
            }
            g2.dispose();
        }
    }

    private static final class PageBackground extends JPanel {
        PageBackground() {
            super(new BorderLayout());
            setBackground(BACKGROUND);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(12, 29, 53),
                    getWidth(), getHeight(), new Color(20, 44, 79)));
            g.fillRect(0, 0, getWidth(), getHeight());
            drawNodes(g, getWidth() / 8, getHeight() * 2 / 3, 1);
            drawNodes(g, getWidth() * 7 / 8, getHeight() / 3, -1);
            g.dispose();
        }

        private void drawNodes(Graphics2D g, int centerX, int centerY, int direction) {
            int[][] points = {{0, 0}, {-54, -30}, {-26, -90}, {48, -78},
                    {90, -20}, {47, 54}, {-42, 72}};
            int[][] edges = {{0, 1}, {0, 2}, {0, 3}, {0, 4}, {0, 5},
                    {0, 6}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}};
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(new Color(133, 167, 229, 43));
            for (int[] edge : edges) {
                int[] start = points[edge[0]];
                int[] end = points[edge[1]];
                g.drawLine(centerX + direction * start[0], centerY + start[1],
                        centerX + direction * end[0], centerY + end[1]);
            }
            for (int[] point : points) {
                int x = centerX + direction * point[0];
                int y = centerY + point[1];
                g.setColor(new Color(113, 155, 229, 54));
                g.fillOval(x - 5, y - 5, 10, 10);
                g.setColor(new Color(183, 207, 250, 85));
                g.fillOval(x - 2, y - 2, 4, 4);
            }
        }
    }

    private static final class SurfacePanel extends JPanel {
        SurfacePanel(java.awt.LayoutManager layout, int padding) {
            super(layout);
            setOpaque(false);
            setBackground(PANEL);
            setBorder(new EmptyBorder(padding + 1, padding + 1,
                    padding + 1, padding + 1));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            boolean dark = PANEL_DARK.equals(getBackground());
            Color top = dark ? new Color(23, 49, 84) : new Color(28, 57, 96);
            Color bottom = dark ? new Color(16, 38, 68) : new Color(19, 43, 75);
            g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g.setColor(new Color(118, 154, 210, 125));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g.dispose();
        }
    }

    private static final class NodeEmblem extends JPanel {
        private final int size;

        NodeEmblem(int size) {
            this.size = size;
            setOpaque(false);
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(size / 76.0, size / 76.0);
            g.setColor(new Color(73, 113, 204, 49));
            g.fillOval(3, 3, 70, 70);
            g.setColor(new Color(139, 176, 255, 145));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(5, 5, 66, 66);
            int[][] nodes = {{20, 49}, {38, 20}, {57, 49}};
            g.setColor(PERIWINKLE);
            g.setStroke(new BasicStroke(2.8f));
            g.drawLine(nodes[0][0], nodes[0][1], nodes[1][0], nodes[1][1]);
            g.drawLine(nodes[1][0], nodes[1][1], nodes[2][0], nodes[2][1]);
            g.drawLine(nodes[0][0], nodes[0][1], nodes[2][0], nodes[2][1]);
            for (int[] node : nodes) {
                g.setColor(BLUE);
                g.fillOval(node[0] - 7, node[1] - 7, 14, 14);
                g.setColor(TEXT);
                g.fillOval(node[0] - 2, node[1] - 2, 4, 4);
            }
            g.dispose();
        }
    }

    private static final class OutcomeEmblem extends JPanel {
        private boolean correct;

        OutcomeEmblem() {
            setOpaque(false);
            setPreferredSize(new Dimension(86, 86));
            setMaximumSize(new Dimension(86, 86));
        }

        void setCorrect(boolean correct) {
            this.correct = correct;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = correct ? GREEN : RED;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 34));
            g.fillOval(3, 3, 80, 80);
            g.setColor(color);
            g.setStroke(new BasicStroke(2.5f));
            g.drawOval(5, 5, 76, 76);
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            if (correct) {
                g.drawLine(25, 44, 37, 56);
                g.drawLine(37, 56, 62, 29);
            } else {
                g.drawLine(29, 29, 57, 57);
                g.drawLine(57, 29, 29, 57);
            }
            g.dispose();
        }
    }

    private static final class BadgeLabel extends JLabel {
        BadgeLabel() {
            setFont(new Font("Segoe UI", Font.BOLD, 16));
            setForeground(TEXT);
            setOpaque(false);
            setBorder(new EmptyBorder(7, 14, 7, 14));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, PERIWINKLE,
                    0, getHeight(), BLUE));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class ProgressStrip extends JPanel {
        private int slot = 1;

        ProgressStrip() {
            setOpaque(false);
            setPreferredSize(new Dimension(200, 24));
        }

        void setSlot(int slot) {
            this.slot = slot;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int count = PrizeLadder.totalSlots();
            int gap = 6;
            int segment = Math.max(3, (getWidth() - (count - 1) * gap) / count);
            for (int i = 1; i <= count; i++) {
                g.setColor(i == slot ? PERIWINKLE : PrizeLadder.isCheckpoint(i)
                        ? GOLD : i < slot ? new Color(72, 114, 189)
                        : new Color(58, 83, 121));
                g.fillRoundRect((i - 1) * (segment + gap), 0,
                        segment, getHeight() - 1, 9, 9);
            }
            g.dispose();
        }
    }

    private static final class StyledButton extends JButton {
        private final boolean primary;
        private final int lifeline;
        private boolean available = true;
        private boolean enabledAppearanceWhenDisabled;

        StyledButton(String text, boolean primary, int lifeline) {
            super(text);
            this.primary = primary;
            this.lifeline = lifeline;
            setFont(lifeline >= 0 ? new Font("Segoe UI", Font.BOLD, 15)
                    : primary ? BOLD : BODY);
            setForeground(TEXT);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(9, 13, 9, 13));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        void setAvailable(boolean available) {
            this.available = available;
            setEnabled(true);
            getAccessibleContext().setAccessibleDescription(
                    available ? null : "This lifeline has already been used.");
            repaint();
        }

        void setEnabledAppearanceWhenDisabled(boolean enabledAppearanceWhenDisabled) {
            this.enabledAppearanceWhenDisabled = enabledAppearanceWhenDisabled;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean actionable = isEnabled() && available || enabledAppearanceWhenDisabled;
            boolean hover = actionable && getModel().isRollover();
            boolean pressed = actionable && getModel().isPressed();
            Color top = !actionable ? DISABLED_TOP
                    : primary ? hover ? new Color(110, 154, 255)
                    : new Color(89, 137, 251)
                    : hover ? new Color(36, 67, 109)
                    : new Color(24, 49, 82);
            Color bottom = !actionable ? DISABLED_BOTTOM
                    : primary ? pressed ? new Color(54, 94, 197)
                    : new Color(58, 105, 225)
                    : new Color(18, 40, 70);
            g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 12, 12);
            g.setColor(!actionable ? DISABLED_BORDER : isFocusOwner() ? GOLD
                    : primary ? PERIWINKLE
                    : hover ? PERIWINKLE : BORDER);
            g.setStroke(!actionable
                    ? new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                            10f, new float[] {5f, 4f}, 0f)
                    : new BasicStroke(isFocusOwner() ? 2f : 1.2f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
            if (primary && actionable) {
                g.setColor(new Color(240, 246, 255, 48));
                g.drawLine(13, 3, Math.max(13, getWidth() - 14), 3);
            }
            g.setFont(getFont());
            g.setColor(actionable ? TEXT : DISABLED_TEXT);
            FontMetrics metrics = g.getFontMetrics();
            if (lifeline >= 0) {
                paintLifeline(g, metrics);
            } else {
                String text = getText();
                g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2,
                        (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            }
            g.dispose();
        }

        private void paintLifeline(Graphics2D g, FontMetrics metrics) {
            int iconSize = 48;
            boolean actionable = isEnabled() && available || enabledAppearanceWhenDisabled;
            
            List<String> lines = AnswerButton.wrap(getText(), metrics, Math.max(60, getWidth() - 16));
            int gap = 8;
            int totalTextHeight = lines.size() * metrics.getHeight();
            int totalContentHeight = iconSize + gap + totalTextHeight;
            int startY = (getHeight() - totalContentHeight) / 2;
            
            Graphics2D icon = (Graphics2D) g.create();
            icon.translate((getWidth() - iconSize) / 2, startY);
            icon.scale(iconSize / 28.0, iconSize / 28.0);
            icon.setColor(actionable ? PERIWINKLE : DISABLED_TEXT);
            icon.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            int iconX = 0;
            int iconY = 0;
            switch (lifeline) {
                case 0 -> {
                    icon.drawLine(iconX + 5, iconY + 4, iconX + 23, iconY + 22);
                    icon.drawLine(iconX + 23, iconY + 4, iconX + 5, iconY + 22);
                    icon.drawOval(iconX + 1, iconY + 20, 8, 8);
                    icon.drawOval(iconX + 19, iconY + 20, 8, 8);
                }
                case 1 -> {
                    icon.drawOval(iconX + 2, iconY + 2, 24, 24);
                    icon.drawLine(iconX + 14, iconY + 3, iconX + 14, iconY + 25);
                    icon.drawLine(iconX + 3, iconY + 14, iconX + 25, iconY + 14);
                    icon.drawLine(iconX + 6, iconY + 6, iconX + 22, iconY + 22);
                    icon.drawLine(iconX + 22, iconY + 6, iconX + 6, iconY + 22);
                }
                case 2 -> {
                    icon.drawArc(iconX + 3, iconY + 3, 22, 22, 30, 150);
                    icon.drawArc(iconX + 3, iconY + 3, 22, 22, 210, 150);
                    icon.drawLine(iconX + 4, iconY + 9, iconX + 3, iconY + 3);
                    icon.drawLine(iconX + 4, iconY + 9, iconX + 10, iconY + 7);
                    icon.drawLine(iconX + 24, iconY + 19, iconX + 25, iconY + 25);
                    icon.drawLine(iconX + 24, iconY + 19, iconX + 18, iconY + 21);
                }
                case 3 -> {
                    Path2D handset = new Path2D.Double();
                    handset.moveTo(iconX + 6, iconY + 3);
                    handset.lineTo(iconX + 2, iconY + 8);
                    handset.curveTo(iconX + 1, iconY + 15,
                            iconX + 13, iconY + 27, iconX + 20, iconY + 26);
                    handset.lineTo(iconX + 26, iconY + 21);
                    handset.lineTo(iconX + 21, iconY + 17);
                    handset.lineTo(iconX + 17, iconY + 20);
                    handset.curveTo(iconX + 13, iconY + 18,
                            iconX + 10, iconY + 15, iconX + 8, iconY + 11);
                    handset.lineTo(iconX + 11, iconY + 8);
                    handset.closePath();
                    icon.draw(handset);
                }
                default -> throw new IllegalStateException("Unknown lifeline icon");
            }
            icon.dispose();
            
            g.setColor(actionable ? TEXT : DISABLED_TEXT);
            int baseline = startY + iconSize + gap + metrics.getAscent();
            for (String line : lines) {
                g.drawString(line, (getWidth() - metrics.stringWidth(line)) / 2, baseline);
                baseline += metrics.getHeight();
            }
        }
    }

    private static final class ModeOptionButton extends JButton {
        private final String subtitle;

        ModeOptionButton(String title, String subtitle) {
            super(title);
            this.subtitle = subtitle;
            setFont(new Font("Segoe UI", Font.BOLD, 26));
            setForeground(TEXT);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean enabled = isEnabled();
            boolean hover = enabled && getModel().isRollover();
            Color top = !enabled ? DISABLED_TOP
                    : hover ? new Color(36, 67, 109) : new Color(24, 49, 82);
            Color bottom = !enabled ? DISABLED_BOTTOM
                    : hover ? new Color(24, 51, 88) : new Color(18, 40, 70);
            g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 16, 16);
            g.setColor(!enabled ? DISABLED_BORDER : isFocusOwner() ? GOLD
                    : hover ? PERIWINKLE : BORDER);
            g.setStroke(!enabled
                    ? new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                            10f, new float[] {5f, 4f}, 0f)
                    : new BasicStroke(isFocusOwner() ? 2f : 1.2f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);

            FontMetrics titleMetrics = g.getFontMetrics(getFont());
            int titleY = getHeight() / 2 - 8;
            g.setFont(getFont());
            g.setColor(enabled ? TEXT : DISABLED_TEXT);
            g.drawString(getText(), (getWidth() - titleMetrics.stringWidth(getText())) / 2, titleY);

            Font subFont = new Font("Segoe UI", Font.PLAIN, 19);
            FontMetrics subMetrics = g.getFontMetrics(subFont);
            g.setFont(subFont);
            g.setColor(enabled ? MUTED : DISABLED_TEXT);
            int subtitleY = titleY + subMetrics.getHeight() + 4;
            g.drawString(subtitle, (getWidth() - subMetrics.stringWidth(subtitle)) / 2, subtitleY);
            g.dispose();
        }
    }

    private static final class HomeButton extends JButton {
        HomeButton() {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setPreferredSize(new Dimension(44, 44));
            setMaximumSize(new Dimension(44, 44));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hover = getModel().isRollover();
            Color fill = !isEnabled() ? DISABLED_TOP
                    : hover ? Color.WHITE : new Color(240, 244, 252);
            int d = Math.min(getWidth(), getHeight()) - 2;
            int x = (getWidth() - d) / 2;
            int y = (getHeight() - d) / 2;
            g.setColor(fill);
            g.fillOval(x, y, d, d);
            g.setColor(isEnabled() && isFocusOwner() ? GOLD
                    : isEnabled() ? BORDER : DISABLED_BORDER);
            g.setStroke(!isEnabled()
                    ? new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                            10f, new float[] {4f, 3f}, 0f)
                    : new BasicStroke(isFocusOwner() ? 2f : 1.2f));
            g.drawOval(x, y, d - 1, d - 1);

            int cx = x + d / 2;
            int cy = y + d / 2;
            int half = (int) Math.round(d * 0.20);
            int roofTop = cy - (int) Math.round(d * 0.26);
            int baseTop = cy - (int) Math.round(d * 0.02);
            int baseBottom = cy + (int) Math.round(d * 0.22);
            Path2D house = new Path2D.Double();
            house.moveTo(cx - half - 4, baseTop);
            house.lineTo(cx, roofTop);
            house.lineTo(cx + half + 4, baseTop);
            house.lineTo(cx + half, baseTop);
            house.lineTo(cx + half, baseBottom);
            house.lineTo(cx - half, baseBottom);
            house.lineTo(cx - half, baseTop);
            house.closePath();
            g.setColor(isEnabled() ? BACKGROUND : DISABLED_TEXT);
            g.fill(house);
            int doorWidth = Math.max(3, (int) Math.round(d * 0.09));
            int doorHeight = (int) Math.round(d * 0.14);
            g.setColor(fill);
            g.fillRect(cx - doorWidth / 2, baseBottom - doorHeight, doorWidth, doorHeight);
            g.dispose();
        }
    }

    private static final class TimerBadge extends JLabel {
        TimerBadge() {
            setFont(new Font("Segoe UI", Font.BOLD, 30));
            setForeground(TEXT);
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(false);
        }

        void setUrgent(boolean urgent) {
            setForeground(urgent ? RED : TEXT);
        }
    }

    private static final class LadderRow extends JPanel {
        private final boolean current;
        private final boolean checkpoint;

        LadderRow(boolean current, boolean checkpoint) {
            super(new BorderLayout());
            this.current = current;
            this.checkpoint = checkpoint;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (!current && !checkpoint) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Color top = current ? new Color(97, 146, 255)
                    : new Color(255, 216, 132);
            Color bottom = current ? BLUE : GOLD;
            g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            if (current && checkpoint) {
                g.setColor(GOLD);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
            }
            g.setColor(current ? TEXT : BACKGROUND);
            g.fillOval(46, getHeight() / 2 - 3, 6, 6);
            g.dispose();
        }
    }

    private static final class AvatarIcon implements javax.swing.Icon {
        public int getIconWidth() { return 44; }
        public int getIconHeight() { return 44; }

        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(x, y, PERIWINKLE,
                    x, y + 44, new Color(93, 132, 211)));
            g.fillOval(x, y, 44, 44);
            g.setColor(BACKGROUND);
            g.fillOval(x + 17, y + 9, 10, 10);
            g.fillRoundRect(x + 11, y + 24, 22, 13, 12, 12);
            g.dispose();
        }
    }

    private static final class NodeHeader extends JPanel {
        NodeHeader(java.awt.LayoutManager layout) {
            super(layout);
            setBackground(PANEL);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(24, 53, 89),
                    0, getHeight(), new Color(20, 45, 79)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(126, 159, 213, 80));
            g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            g.setColor(new Color(96, 139, 202, 100));
            g.setStroke(new BasicStroke(2));
            int right = getWidth() - 210;
            int[] x = {right - 115, right - 83, right - 49, right - 18, right - 70};
            int[] y = {25, 47, 23, 41, 68};
            int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {1, 4}, {2, 4}};
            for (int[] edge : edges) g.drawLine(x[edge[0]], y[edge[0]], x[edge[1]], y[edge[1]]);
            for (int i = 0; i < x.length; i++) g.fillOval(x[i] - 4, y[i] - 4, 8, 8);
            g.dispose();
        }
    }

    private static final class NodeIcon implements javax.swing.Icon {
        public int getIconWidth() { return 48; }
        public int getIconHeight() { return 48; }

        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(BLUE);
            g.setStroke(new BasicStroke(3));
            g.drawLine(x + 10, y + 32, x + 24, y + 10);
            g.drawLine(x + 24, y + 10, x + 39, y + 33);
            g.drawLine(x + 10, y + 32, x + 39, y + 33);
            for (int[] point : new int[][]{{10, 32}, {24, 10}, {39, 33}}) {
                g.fillOval(x + point[0] - 7, y + point[1] - 7, 14, 14);
            }
            g.setColor(TEXT);
            for (int[] point : new int[][]{{10, 32}, {24, 10}, {39, 33}}) {
                g.fillOval(x + point[0] - 2, y + point[1] - 2, 4, 4);
            }
            g.dispose();
        }
    }

    private static final class AnswerButton extends JToggleButton {
        private final int index;
        private String optionText = "";
        private Font optionFont = new Font("Segoe UI", Font.BOLD, 20);

        AnswerButton(int index) {
            this.index = index;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setOpaque(false);
            setPreferredSize(new Dimension(250, 96));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        void setOptionText(String text) {
            optionText = text;
            setOptionVisible(isEnabled());
            repaint();
        }

        void setOptionVisible(boolean visible) {
            setEnabled(visible);
            setToolTipText(visible ? optionText : null);
            getAccessibleContext().setAccessibleName("Answer " + (char) ('A' + index)
                    + (visible ? ": " + optionText : " unavailable"));
        }

        void setOptionFontSize(int size) {
            if (optionFont.getSize() == size) return;
            optionFont = new Font("Segoe UI", Font.BOLD, size);
            repaint();
        }

        int requiredHeight(int width) {
            FontMetrics metrics = getFontMetrics(optionFont);
            int lines = wrap(optionText, metrics, Math.max(60, width - 92)).size();
            return Math.max(96, lines * (metrics.getHeight() + 2) + 24);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean hover = getModel().isRollover() && isEnabled();
            boolean enabled = isEnabled();
            Color top = !enabled ? DISABLED_TOP
                    : isSelected() ? new Color(107, 151, 255)
                    : hover ? new Color(35, 66, 109) : new Color(23, 49, 82);
            Color bottom = !enabled ? DISABLED_BOTTOM
                    : isSelected() ? BLUE
                    : hover ? new Color(24, 51, 88) : PANEL_DARK;
            g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 14, 14);
            g.setColor(!enabled ? DISABLED_BORDER
                    : isFocusOwner() ? GOLD : isSelected() ? PERIWINKLE
                    : hover ? new Color(109, 151, 215) : BORDER);
            g.setStroke(!enabled
                    ? new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                            10f, new float[] {5f, 4f}, 0f)
                    : new BasicStroke(isFocusOwner() ? 2 : 1));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
            if (!isEnabled()) {
                g.dispose();
                return;
            }
            if (isSelected()) {
                g.setColor(new Color(241, 247, 255, 90));
                g.drawLine(16, 3, Math.max(16, getWidth() - 17), 3);
            }
            g.setColor(isSelected() ? new Color(45, 89, 184)
                    : new Color(56, 81, 119));
            g.fillRoundRect(16, getHeight() / 2 - 22, 44, 44, 8, 8);
            g.setFont(new Font("Segoe UI", Font.BOLD, 26));
            g.setColor(TEXT);
            String letter = String.valueOf((char) ('A' + index));
            FontMetrics badgeMetrics = g.getFontMetrics();
            g.drawString(letter, 38 - badgeMetrics.stringWidth(letter) / 2,
                    getHeight() / 2 + badgeMetrics.getAscent() / 2 - 3);

            g.setFont(optionFont);
            FontMetrics metrics = g.getFontMetrics();
            List<String> lines = wrap(optionText, metrics,
                    Math.max(60, getWidth() - 92));
            int lineHeight = metrics.getHeight() + 2;
            int firstBaseline = (getHeight() - lines.size() * lineHeight) / 2
                    + metrics.getAscent();
            g.setColor(TEXT);
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(lines.get(i), 77, firstBaseline + i * lineHeight);
            }
            g.dispose();
        }

        private static List<String> wrap(String text, FontMetrics metrics, int maxWidth) {
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : text.split("\\s+")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (metrics.stringWidth(candidate) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    if (line.length() > 0) lines.add(line.toString());
                    line.setLength(0);
                    while (metrics.stringWidth(word) > maxWidth && word.length() > 1) {
                        int count = 1;
                        while (count < word.length()
                                && metrics.stringWidth(word.substring(0, count + 1)) <= maxWidth) count++;
                        lines.add(word.substring(0, count));
                        word = word.substring(count);
                    }
                    line.append(word);
                }
            }
            if (line.length() > 0) lines.add(line.toString());
            if (lines.isEmpty()) lines.add("");
            return lines;
        }
    }

    private void showScreen(String screen, boolean playBackgroundMusic) {
        currentScreen = screen;
        stopQuestionTimer();
        if (!"results".equals(screen)) stopResultsAudio();
        if ("game".equals(screen) && engine != null) {
            if (PrizeLadder.isCheckpoint(engine.currentSlot())
                    || engine.currentSlot() == PrizeLadder.totalSlots()) {
                startBackgroundMusic("/" + CHECKPOINT_MUSIC);
            } else {
                startBackgroundMusic("/" + GAMEPLAY_MUSIC);
            }
        } else if (playBackgroundMusic) {
            startBackgroundMusic("/bg.wav");
        } else {
            stopBackgroundMusic();
        }
        screens.show(root, screen);
        if ("game".equals(screen)) startOrStopQuestionTimer();
    }

    private void startBackgroundMusic() {
        startBackgroundMusic("/bg.wav");
    }

    private void startBackgroundMusic(String resource) {
        audio.play(GameAudio.Channel.MUSIC, resource, BACKGROUND_MUSIC_LEVEL, true);
    }

    private void stopBackgroundMusic() {
        audio.stop(GameAudio.Channel.MUSIC);
    }

    private void playProgressSound() { playSfx(SFX_PROGRESS); }
    private void playBackSound() { playSfx(SFX_BACK); }

    private void playSfx(String fileName) {
        audio.play(GameAudio.Channel.EFFECT, "/sfx/" + fileName, 1f, false);
    }

    private void playSplashAudio() {
        audio.play(GameAudio.Channel.SPLASH, "/splash.wav", 1f, false);
    }

    private void stopSplashAudio() {
        audio.stop(GameAudio.Channel.SPLASH);
    }

    private void stopSplash() {
        if (splashHoldTimer != null) splashHoldTimer.stop();
        if (splashFadeTimer != null) splashFadeTimer.stop();
        stopSplashAudio();
        removeSplashEnterBinding();
        if (splashOverlay != null) splashOverlay.setVisible(false);
        splashOverlay = null;
        if (splashImage != null) splashImage.flush();
        splashImage = null;
    }
}
