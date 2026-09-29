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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.View;

/** A separate, untimed Swing presentation for the existing game engine. */
public final class SwingUI {

    private static final Color BACKGROUND = new Color(14, 34, 62);
    private static final Color PANEL = new Color(19, 44, 77);
    private static final Color PANEL_DARK = new Color(16, 38, 68);
    private static final Color BORDER = new Color(60, 94, 139);
    private static final Color TEXT = new Color(245, 248, 255);
    private static final Color MUTED = new Color(185, 204, 231);
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
    private int secondsRemaining;
    private JButton menuPlayButton;
    private JButton startButton;
    private JPanel resultsPage;
    private Clip backgroundMusic;
    private String backgroundMusicResource;
    private Clip splashAudio;
    private Clip resultsAudio;
    private SplashOverlay splashOverlay;
    private Timer splashHoldTimer;
    private Timer splashFadeTimer;
    private boolean splashFading;
    private static final int SPLASH_HOLD_MS = 20000;
    private static final int SPLASH_FADE_MS = 600;
    private static final float BACKGROUND_MUSIC_LEVEL = 0.30f;
    private static final float BUTTON_SFX_GAIN_DB = 3.0f;
    private static final String SFX_PROGRESS = "niera_sound_5.wav";
    private static final String SFX_BACK = "niera_sound_2.wav";
    private static final String SFX_ANSWER_SELECT = "select_005.wav";
    private static final String SFX_CORRECT_ANSWER = "confirmation_004.wav";
    private static final String SFX_ACTION_ERROR = "error_006.wav";
    private static final String SFX_WRONG_ANSWER = "wrong.WAV";
    private static final String SFX_REGULAR_WIN = "regular winning.WAV";
    private static final String SFX_SUPREME_VICTORY = "supreme victory.WAV";
    private static final String SFX_NO_CHECKPOINT = "Clapping Sound Effects.WAV";
    private static final String CHECKPOINT_MUSIC = "checkpoint-suspense.wav";
    private static final String GAMEPLAY_MUSIC = "gameplay-theme.wav";

    private Image splashImage;

    public static void main(String[] args) {
        RandomGenerator random = RandomGeneratorFactory.getDefault().create();
        try {
            QuestionBank bank = args.length > 0
                    ? QuestionBank.loadFromFile(Paths.get(args[0]), random)
                    : QuestionBank.loadFromResource("/questions.csv", random);
            ReplayLogger logger = new ReplayLogger(Paths.get("logs"));
            SwingUtilities.invokeLater(() -> new SwingUI(bank, random, logger).show());
        } catch (IOException | UncheckedIOException e) {
            System.err.println("Failed to start Millionaire Mind: " + e.getMessage());
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
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopBackgroundMusic, "bg-music-shutdown"));
        applyFullScreenBounds();
        showMainMenu();
    }

    // Fills the whole display so the window always matches the current screen's
    // resolution instead of a fixed pixel size.
    private void applyFullScreenBounds() {
        frame.setUndecorated(true);
        frame.setResizable(false);
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Rectangle screenBounds = device.getDefaultConfiguration().getBounds();
        frame.setBounds(screenBounds);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    // Stops the music and releases the audio line immediately so playback doesn't
    // linger after the window closes, then shuts the JVM down.
    private void exitApplication() {
        stopBackgroundMusic();
        stopResultsAudio();
        frame.dispose();
        System.exit(0);
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
            return; // asset missing — skip splash rather than crash
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
        card.setPreferredSize(new Dimension(640, 520));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(Box.createVerticalStrut(6));
        NodeEmblem emblem = new NodeEmblem();
        emblem.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(emblem);
        content.add(Box.createVerticalStrut(10));
        JLabel eyebrow = label("THE AI KNOWLEDGE CHALLENGE", 13, true, PERIWINKLE);
        eyebrow.setAlignmentX(Component.CENTER_ALIGNMENT);
        eyebrow.setHorizontalAlignment(SwingConstants.CENTER);
        eyebrow.setPreferredSize(new Dimension(490, 22));
        eyebrow.setMaximumSize(new Dimension(490, 22));
        content.add(eyebrow);
        content.add(Box.createVerticalStrut(7));
        JLabel title = label("Millionaire Mind", 38, true, TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(8));
        JLabel subtitle = label("AI fundamentals  ·  " + PrizeLadder.totalSlots()
        + " questions  ·  Normal or Timed", 17, false, MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        subtitle.setPreferredSize(new Dimension(490, 30));
        subtitle.setMaximumSize(new Dimension(490, 30));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(31));

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
        JPanel card = panel(new BorderLayout(), 34);
        card.setName("modeCard");
        card.setPreferredSize(new Dimension(640, 480));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = label("Choose your mode", 32, true, TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(10));
        JLabel subtitle = label("Normal Mode is untimed. Timed Mode gives you "
                + GameEngine.TIMER_SECONDS + " seconds per question.", 15, false, MUTED);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setPreferredSize(new Dimension(500, 40));
        subtitle.setMaximumSize(new Dimension(500, 40));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(26));

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
        button.setPreferredSize(new Dimension(380, 66));
        button.setMaximumSize(new Dimension(380, 66));
        return button;
    }

    private JPanel buildInstructionsScreen1() {
        JPanel card = panel(new BorderLayout(0, 12), 22);
        card.setName("instructionsCard");
        card.setPreferredSize(new Dimension(900, 610));
        JLabel title = label("How to Play", 28, true, TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setOpaque(false);

        JPanel textColumn = new JPanel();
        textColumn.setOpaque(false);
        textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));

        JLabel gameplayTitle = label("General Gameplay", 18, true, GOLD);
        gameplayTitle.setAlignmentX(0);
        textColumn.add(gameplayTitle);
        textColumn.add(Box.createVerticalStrut(6));
        JTextArea gameplayText = instructionText("Answer 15 multiple-choice questions about "
                + "AI fundamentals across six levels of Bloom's Taxonomy. Choose an answer, "
                + "then select Lock in answer. Keep answering correctly to climb the prize "
                + "ladder and reach the $1,000,000 question.", 14, 620, 92);
        gameplayText.setName("instructionsText");
        textColumn.add(gameplayText);
        textColumn.add(Box.createVerticalStrut(14));

        JLabel prizeTitle = label("Prize Money", 18, true, GOLD);
        prizeTitle.setAlignmentX(0);
        textColumn.add(prizeTitle);
        textColumn.add(Box.createVerticalStrut(6));
        JTextArea prizeIntro = instructionText("Every correct answer moves you one step "
                + "closer to becoming a millionaire.", 14, 620, 24);
        prizeIntro.setName("prizeIntroText");
        textColumn.add(prizeIntro);
        textColumn.add(Box.createVerticalStrut(10));

        JPanel checkpointBox = panel(new BorderLayout(), 12);
        checkpointBox.setName("checkpointSummary");
        checkpointBox.setBackground(PANEL_DARK);
        checkpointBox.setAlignmentX(0);
        checkpointBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        JPanel checkpointLines = new JPanel(new GridLayout(2, 1, 0, 4));
        checkpointLines.setOpaque(false);
        checkpointLines.add(label("Reach Question 5: $2,000 is guaranteed", 14, true, TEXT));
        checkpointLines.add(label("Reach Question 10: $64,000 is guaranteed", 14, true, TEXT));
        checkpointBox.add(checkpointLines, BorderLayout.CENTER);
        textColumn.add(checkpointBox);
        textColumn.add(Box.createVerticalStrut(10));

        JTextArea prizeTerms = instructionText("Get an answer wrong and you'll fall back "
                + "to the latest checkpoint you've reached. You can also Walk Away "
                + "whenever you want and keep your guaranteed winnings.", 14, 620, 56);
        prizeTerms.setName("prizeTerms");
        textColumn.add(prizeTerms);

        body.add(textColumn, BorderLayout.CENTER);
        JPanel ladder = buildInstructionsLadder();
        ladder.setPreferredSize(new Dimension(190, 420));
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
        JPanel card = panel(new BorderLayout(0, 12), 22);
        card.setName("instructionsCard2");
        card.setPreferredSize(new Dimension(900, 610));
        JLabel title = label("How to Play", 28, true, TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(title, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel lifelineTitle = label("Lifelines", 18, true, GOLD);
        lifelineTitle.setAlignmentX(0);
        content.add(lifelineTitle);
        content.add(Box.createVerticalStrut(8));

        JPanel lifelineBox = panel(new BorderLayout(0, 6), 14);
        lifelineBox.setName("lifelinePreviewBox");
        lifelineBox.setBackground(PANEL_DARK);
        lifelineBox.setAlignmentX(0);
        lifelineBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        lifelineBox.add(label("Lifelines", 13, false, MUTED), BorderLayout.NORTH);
        JPanel lifelineRow = new JPanel(new GridLayout(1, 4, 12, 0));
        lifelineRow.setOpaque(false);
        String[] names = {"50:50", "Spin the Wheel", "Switch the Question", "Phone a Friend"};
        for (int i = 0; i < names.length; i++) {
            StyledButton preview = new StyledButton(names[i], false, i);
            preview.setName("lifelinePreview" + i);
            preview.setPreferredSize(new Dimension(140, 80));
            preview.setEnabled(false);
            preview.setFocusable(false);
            lifelineRow.add(preview);
        }
        lifelineBox.add(lifelineRow, BorderLayout.CENTER);
        content.add(lifelineBox);
        content.add(Box.createVerticalStrut(12));

        JTextArea intro = instructionText("You're not completely on your own. You have "
                + "four lifelines, and each one can only be used once.", 15, 800, 30);
        intro.setName("lifelineIntroText");
        content.add(intro);
        content.add(Box.createVerticalStrut(6));

        addInstructionBullet(content, "lifelineInstruction0",
                "•  50:50 – Removes two wrong answers, leaving you with two choices.", 800, 26);
        content.add(Box.createVerticalStrut(4));
        addInstructionBullet(content, "lifelineInstruction1",
                "•  Spin the Wheel – Gives you a random advantage — it might remove a "
                + "wrong answer, narrow your choices, or give you bonus time.", 800, 42);
        content.add(Box.createVerticalStrut(4));
        addInstructionBullet(content, "lifelineInstruction2",
                "•  Switch the Question – Don't like the question? Swap it for another "
                + "one from the same Bloom's level.", 800, 42);
        content.add(Box.createVerticalStrut(4));
        addInstructionBullet(content, "lifelineInstruction3",
                "•  Phone a Friend – Get a hint to help you figure out the answer.", 800, 26);
        content.add(Box.createVerticalStrut(10));

        JTextArea outro = instructionText("Use them wisely. Once they're gone, they're gone.",
                15, 800, 24);
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

    /** A compact, read-only rendering of the prize ladder for the instructions screen. */
    private JPanel buildInstructionsLadder() {
        JPanel ladder = panel(new BorderLayout(), 10);
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
            JLabel number = label(Integer.toString(slot), 13, true, rowText);
            JLabel value = label(money(PrizeLadder.prizeFor(slot)), 13, true, rowText);
            row.add(number, BorderLayout.WEST);
            row.add(value, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            rows.add(row);
            rows.add(Box.createVerticalStrut(1));
        }
        ladder.add(rows, BorderLayout.CENTER);
        return ladder;
    }

    /** Back/Next pager plus a home button that always returns to the main menu. */
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
        JTextArea bullet = instructionText(text, 15, width, height);
        bullet.setName(name);
        content.add(bullet);
    }

    private JPanel buildNameScreen() {
        JPanel card = panel(new BorderLayout(), 34);
        card.setName("nameCard");
        card.setPreferredSize(new Dimension(600, 430));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel title = label("Ready to play?", 34, true, TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(Box.createVerticalStrut(9));
        content.add(title);
        content.add(Box.createVerticalStrut(16));
        JLabel description = label("Enter your name to begin the 15-question game.", 17, false, MUTED);
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
        nameField.setFont(BODY);
        nameField.setBackground(PANEL_DARK);
        nameField.setForeground(TEXT);
        nameField.setCaretColor(TEXT);
        nameField.setSelectionColor(BLUE);
        nameField.setSelectedTextColor(TEXT);
        nameField.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        nameField.setPreferredSize(new Dimension(360, 48));
        nameField.setMaximumSize(new Dimension(360, 48));
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
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private static void addMenuButton(JPanel content, JButton button) {
        if (button.getPreferredSize().width < 300) {
            button.setPreferredSize(new Dimension(260, 50));
            button.setMaximumSize(new Dimension(260, 50));
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
        ladder.setPreferredSize(new Dimension(310, 650));
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
        // ---------------------------------------
        // timerLabel.setName("questionTimer");
        // left.add(timerLabel);
        // ---------------------------------------
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

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        questionBody = body;
        promptBox = panel(new BorderLayout(10, 0), 10);
        promptBox.setBackground(PANEL_DARK);
        JPanel promptAccent = new JPanel();
        promptAccent.setBackground(PERIWINKLE);
        promptAccent.setPreferredSize(new Dimension(4, 10));
        promptBox.add(promptAccent, BorderLayout.WEST);
        promptText.setName("questionText");
        promptText.setFont(new Font("Segoe UI", Font.BOLD, 25));
        promptText.setEditable(false);
        promptText.setFocusable(false);
        promptText.setBackground(PANEL_DARK);
        promptText.setPreferredSize(new Dimension(400, 90));
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
        answerGrid.setPreferredSize(new Dimension(650, 174));
        answerGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 174));
        answerGrid.setAlignmentX(0);
        body.add(answerGrid);
        body.add(Box.createVerticalStrut(5));

        JPanel lockRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        lockRow.setOpaque(false);
        lockButton.setName("lockAnswer");
        lockButton.setMnemonic(KeyEvent.VK_L);
        lockButton.setEnabled(false);
        lockButton.setPreferredSize(new Dimension(270, 42));
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
        String[] names = {"50:50", "Spin the Wheel", "Switch the Question", "Phone a Friend"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            StyledButton button = new StyledButton(names[i], false, i);
            button.setName("lifeline" + i);
            button.setMnemonic(KeyEvent.VK_1 + i);
            button.setPreferredSize(new Dimension(120, 60));
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
        noticeText.setPreferredSize(new Dimension(500, 28));
        noticeText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        noticeText.setAlignmentX(0);
        body.add(noticeText);
        main.add(body, BorderLayout.CENTER);
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
        feedbackDetail.setPreferredSize(new Dimension(620, 220));
        feedbackDetail.setMaximumSize(new Dimension(620, 220));
        feedbackDetail.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(feedbackDetail);
        card.add(body, BorderLayout.CENTER);
        continueButton.setName("continue");
        continueButton.setMnemonic(KeyEvent.VK_C);
        continueButton.setPreferredSize(new Dimension(220, 54));
        continueButton.addActionListener(event -> {
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
        ladderRows.setLayout(new BoxLayout(ladderRows, BoxLayout.Y_AXIS));
        ladder.add(ladderRows, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.add(label("Guaranteed", 15, false, MUTED));
        guaranteedLabel.setName("guaranteedAmount");
        footer.add(guaranteedLabel);
        checkpointLabel.setName("checkpointStatus");
        footer.add(checkpointLabel);
        footer.add(Box.createVerticalStrut(11));
        walkButton.setName("walkAway");
        walkButton.setMnemonic(KeyEvent.VK_W);
        walkButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        walkButton.addActionListener(event -> {
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

    private void selectAnswer(int index) {
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
                                secondsRemaining += GameEngine.BONUS_TIME_SECONDS;
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
        startOrStopQuestionTimer();
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
        int height = Math.max(90, (int) Math.ceil(textView.getPreferredSpan(View.Y_AXIS)) + 8);
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
        for (int step = 0; step <= 12; step++) {
            int promptSize = Math.max(18, 25 - step);
            int answerSize = Math.max(13, 17 - step / 2);
            if (promptText.getFont().getSize() != promptSize) {
                promptText.setFont(new Font("Segoe UI", Font.BOLD, promptSize));
            }
            for (AnswerButton answer : answerButtons) answer.setOptionFontSize(answerSize);
            adjustPromptHeight();
            adjustAnswerGridHeight();
            frame.validate();
            if (questionBody.getPreferredSize().height <= questionBody.getHeight()) break;
        }
    }

    private void adjustAnswerGridHeight() {
        int width = answerGrid.getWidth();
        if (width <= 0) width = answerGrid.getPreferredSize().width;
        int buttonWidth = Math.max(150, (width - 14) / 2);
        int[] heights = {80, 80};
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
        if (engine.isTimedMode()) {
            timerBox.setVisible(true);
            secondsRemaining = GameEngine.TIMER_SECONDS;
            updateTimerLabel();
            questionTimer = new Timer(1000, event -> tickTimer());
            questionTimer.start();
        } else {
            timerBox.setVisible(false);
        }
    }

    private void tickTimer() {
        secondsRemaining--;
        if (secondsRemaining <= 0) {
            if (questionTimer != null) {
                questionTimer.stop();
                questionTimer = null;
            }
            handleTimeExpired();
            return;
        }
        updateTimerLabel();
    }

    private void updateTimerLabel() {
        timerLabel.setText(secondsRemaining + "s");
        timerLabel.setUrgent(secondsRemaining <= 10);
    }

    private void stopQuestionTimer() {
        if (questionTimer != null) {
            questionTimer.stop();
            questionTimer = null;
        }
    }

    private void playResultsSound(String fileName) {
        stopResultsAudio();
        URL url = resolveAudioResource("/sfx/" + fileName, "resources/sfx/" + fileName);
        if (url == null) {
            System.err.println("Results sound not found: " + fileName);
            return;
        }
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(url)) {
            Clip clip = AudioSystem.getClip();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    if (resultsAudio == clip) resultsAudio = null;
                    clip.close();
                }
            });
            clip.open(audioStream);
            resultsAudio = clip;
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            resultsAudio = null;
            System.err.println("Could not play results sound " + fileName + ": " + e.getMessage());
        }
    }

    private void stopResultsAudio() {
        if (resultsAudio != null) {
            Clip clip = resultsAudio;
            resultsAudio = null;
            clip.stop();
            clip.flush();
            clip.close();
        }
    }

    private void handleTimeExpired() {
        if (engine.isGameOver()) return;
        Question resolved = engine.currentQuestion();
        engine.timeExpired();
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
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 27));
            ladderRows.add(row);
            ladderRows.add(Box.createVerticalStrut(1));
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
        }
        if (resultsPage != null) root.remove(resultsPage);
        resultsPage = buildResultsScreen(logPath, logError);
        root.add(resultsPage, "results");
        showScreen("results", false);
    }

    private JPanel buildResultsScreen(Path logPath, String logError) {
        JPanel card = panel(new BorderLayout(0, 10), 20);
        card.setName("resultsCard");
        card.setPreferredSize(new Dimension(760, 620));
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel title = label("Game report", 31, true, TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        details.add(title);
        details.add(Box.createVerticalStrut(9));
        PlayerSession session = engine.session();
        JLabel player = label("Player: " + session.playerName(), 17, false, MUTED);
        player.setAlignmentX(Component.CENTER_ALIGNMENT);
        details.add(player);
        details.add(Box.createVerticalStrut(11));
        String outcome = session.becameMillionaire() ? "Millionaire!"
                : session.walkedAway() ? "Walked away" : "Game over";
        JLabel result = label(outcome + "  ·  " + money(session.bankedWinnings()),
                27, true, GOLD);
        result.setName("resultOutcome");
        result.setAlignmentX(Component.CENTER_ALIGNMENT);
        details.add(result);
        details.add(Box.createVerticalStrut(20));

        JPanel reportPanel = panel(new BorderLayout(0, 8), 13);
        reportPanel.setName("bloomReportPanel");
        reportPanel.setBackground(PANEL_DARK);
        reportPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        reportPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 236));
        reportPanel.add(label("Bloom report", 20, true, TEXT), BorderLayout.NORTH);
        JPanel reportRows = new JPanel(new GridLayout(BloomLevel.values().length, 1, 0, 4));
        reportRows.setOpaque(false);
        Map<BloomLevel, int[]> report = session.bloomReport();
        for (BloomLevel level : BloomLevel.values()) {
            int[] count = report.get(level);
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.add(label(level.displayName(), 16, false, MUTED), BorderLayout.WEST);
            row.add(label(count[0] + " / " + (count[0] + count[1]) + " correct",
                    16, true, TEXT), BorderLayout.EAST);
            reportRows.add(row);
        }
        reportPanel.add(reportRows, BorderLayout.CENTER);
        details.add(reportPanel);
        details.add(Box.createVerticalStrut(14));
        JLabel summary = null;
        if (session.becameMillionaire()) {
            summary = label("All Bloom levels tied at 100%; no unique strongest or weakest.",
                    15, false, MUTED);
        } else if (!session.history().isEmpty()
                && session.strongestLevel() != session.weakestLevel()) {
            summary = label("Strongest: " + session.strongestLevel().displayName()
                    + "  ·  Weakest: " + session.weakestLevel().displayName(), 15, false, MUTED);
        }
        if (summary != null) {
            summary.setAlignmentX(Component.CENTER_ALIGNMENT);
            details.add(summary);
        }
        details.add(Box.createVerticalStrut(15));
        JLabel logStatus = label(logPath != null ? "Session replay saved"
                : "Replay log could not be saved: " + logError, 15, false, MUTED);
        logStatus.setName("logStatus");
        logStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        details.add(logStatus);
        if (logPath != null) {
            details.add(Box.createVerticalStrut(7));
            JButton openLog = outlineButton("Open " + logPath.getFileName());
            openLog.setName("openSessionLog");
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
        card.add(details, BorderLayout.CENTER);
        JButton again = primaryButton("Play again");
        again.setName("playAgain");
        again.addActionListener(event -> {
            nameField.setText("");
            playProgressSound();
            showNameScreen();
        });
        JButton menu = outlineButton("Main menu");
        menu.setName("resultsMenu");
        menu.addActionListener(event -> { playBackSound(); showMainMenu(); });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        actions.setOpaque(false);
        again.setPreferredSize(new Dimension(170, 48));
        menu.setPreferredSize(new Dimension(170, 48));
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
        NodeEmblem() {
            setOpaque(false);
            setPreferredSize(new Dimension(76, 76));
            setMaximumSize(new Dimension(76, 76));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
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
            setPreferredSize(new Dimension(200, 8));
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
            int gap = 4;
            int segment = Math.max(3, (getWidth() - (count - 1) * gap) / count);
            for (int i = 1; i <= count; i++) {
                g.setColor(i == slot ? PERIWINKLE : PrizeLadder.isCheckpoint(i)
                        ? GOLD : i < slot ? new Color(72, 114, 189)
                        : new Color(58, 83, 121));
                g.fillRoundRect((i - 1) * (segment + gap), 0,
                        segment, getHeight() - 1, 6, 6);
            }
            g.dispose();
        }
    }

    private static final class StyledButton extends JButton {
        private final boolean primary;
        private final int lifeline;
        private boolean available = true;

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

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean actionable = isEnabled() && available;
            boolean hover = getModel().isRollover();
            boolean pressed = getModel().isPressed();
            Color top = !actionable ? new Color(34, 55, 84)
                    : primary ? hover ? new Color(110, 154, 255)
                    : new Color(89, 137, 251)
                    : hover ? new Color(36, 67, 109)
                    : new Color(24, 49, 82);
            Color bottom = !actionable ? new Color(25, 44, 71)
                    : primary ? pressed ? new Color(54, 94, 197)
                    : new Color(58, 105, 225)
                    : new Color(18, 40, 70);
            g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 12, 12);
            g.setColor(isFocusOwner() ? GOLD : !actionable
                    ? new Color(65, 88, 122) : primary ? PERIWINKLE
                    : hover ? PERIWINKLE : BORDER);
            g.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1.2f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
            if (primary && actionable) {
                g.setColor(new Color(240, 246, 255, 48));
                g.drawLine(13, 3, Math.max(13, getWidth() - 14), 3);
            }
            g.setFont(getFont());
            g.setColor(actionable ? TEXT : MUTED);
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
            int iconX = (getWidth() - 28) / 2;
            int iconY = 9;
            boolean actionable = isEnabled() && available;
            g.setColor(actionable ? PERIWINKLE : MUTED);
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            switch (lifeline) {
                case 0 -> {
                    g.drawLine(iconX + 5, iconY + 4, iconX + 23, iconY + 22);
                    g.drawLine(iconX + 23, iconY + 4, iconX + 5, iconY + 22);
                    g.drawOval(iconX + 1, iconY + 20, 8, 8);
                    g.drawOval(iconX + 19, iconY + 20, 8, 8);
                }
                case 1 -> {
                    g.drawOval(iconX + 2, iconY + 2, 24, 24);
                    g.drawLine(iconX + 14, iconY + 3, iconX + 14, iconY + 25);
                    g.drawLine(iconX + 3, iconY + 14, iconX + 25, iconY + 14);
                    g.drawLine(iconX + 6, iconY + 6, iconX + 22, iconY + 22);
                    g.drawLine(iconX + 22, iconY + 6, iconX + 6, iconY + 22);
                }
                case 2 -> {
                    g.drawArc(iconX + 3, iconY + 3, 22, 22, 30, 150);
                    g.drawArc(iconX + 3, iconY + 3, 22, 22, 210, 150);
                    g.drawLine(iconX + 4, iconY + 9, iconX + 3, iconY + 3);
                    g.drawLine(iconX + 4, iconY + 9, iconX + 10, iconY + 7);
                    g.drawLine(iconX + 24, iconY + 19, iconX + 25, iconY + 25);
                    g.drawLine(iconX + 24, iconY + 19, iconX + 18, iconY + 21);
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
                    g.draw(handset);
                }
                default -> throw new IllegalStateException("Unknown lifeline icon");
            }
            g.setColor(actionable ? TEXT : MUTED);
            List<String> lines = AnswerButton.wrap(getText(), metrics,
                    Math.max(60, getWidth() - 16));
            int baseline = getHeight() / 2 + 20 - (lines.size() - 1) * 10;
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
            setFont(new Font("Segoe UI", Font.BOLD, 19));
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
            boolean hover = getModel().isRollover();
            Color top = hover ? new Color(36, 67, 109) : new Color(24, 49, 82);
            Color bottom = hover ? new Color(24, 51, 88) : new Color(18, 40, 70);
            g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 16, 16);
            g.setColor(isFocusOwner() ? GOLD : hover ? PERIWINKLE : BORDER);
            g.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1.2f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);

            FontMetrics titleMetrics = g.getFontMetrics(getFont());
            int titleY = getHeight() / 2 - 6;
            g.setFont(getFont());
            g.setColor(TEXT);
            g.drawString(getText(), (getWidth() - titleMetrics.stringWidth(getText())) / 2, titleY);

            Font subFont = new Font("Segoe UI", Font.PLAIN, 13);
            FontMetrics subMetrics = g.getFontMetrics(subFont);
            g.setFont(subFont);
            g.setColor(MUTED);
            int subtitleY = titleY + subMetrics.getHeight() + 2;
            g.drawString(subtitle, (getWidth() - subMetrics.stringWidth(subtitle)) / 2, subtitleY);
            g.dispose();
        }
    }

    /** The white, round home button that returns to the main menu from the instructions. */
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
            Color fill = hover ? Color.WHITE : new Color(240, 244, 252);
            int d = Math.min(getWidth(), getHeight()) - 2;
            int x = (getWidth() - d) / 2;
            int y = (getHeight() - d) / 2;
            g.setColor(fill);
            g.fillOval(x, y, d, d);
            g.setColor(isFocusOwner() ? GOLD : BORDER);
            g.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1.2f));
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
            g.setColor(BACKGROUND);
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
        private Font optionFont = BOLD;

        AnswerButton(int index) {
            this.index = index;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setOpaque(false);
            setPreferredSize(new Dimension(250, 80));
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
            return Math.max(80, lines * (metrics.getHeight() + 2) + 20);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean hover = getModel().isRollover() && isEnabled();
            Color top = isSelected() ? new Color(107, 151, 255)
                    : hover ? new Color(35, 66, 109) : new Color(23, 49, 82);
            Color bottom = isSelected() ? BLUE
                    : hover ? new Color(24, 51, 88) : PANEL_DARK;
            g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 14, 14);
            g.setColor(isFocusOwner() ? GOLD : isSelected() ? PERIWINKLE
                    : hover ? new Color(109, 151, 215) : BORDER);
            g.setStroke(new BasicStroke(isFocusOwner() ? 2 : 1));
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
            g.setFont(new Font("Segoe UI", Font.BOLD, 23));
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
        if (!"results".equals(screen)) stopResultsAudio();
        if ("game".equals(screen) && engine != null) {
            if (PrizeLadder.isCheckpoint(engine.currentSlot())
                    || engine.currentSlot() == PrizeLadder.totalSlots()) {
                startBackgroundMusic("/" + CHECKPOINT_MUSIC,
                        "resources/sfx/suspense.WAV");
            } else {
                startBackgroundMusic("/" + GAMEPLAY_MUSIC,
                        "resources/sfx/q12.WAV");
            }
        } else if (playBackgroundMusic) {
            startBackgroundMusic("/bg.wav", "resources/bg.wav");
        } else {
            stopBackgroundMusic();
        }
        screens.show(root, screen);
    }

    // Falls back to a filesystem path (relative to the working directory) when the
    // asset wasn't copied onto the classpath, e.g. bg.wav omitted from a manual java -cp run.
    private URL resolveAudioResource(String classpathResource, String... relativeFilePaths) {
        URL url = getClass().getResource(classpathResource);
        if (url != null) {
            return url;
        }
        for (String relativePath : relativeFilePaths) {
            Path candidate = Paths.get(relativePath);
            if (Files.isRegularFile(candidate)) {
                try {
                    return candidate.toUri().toURL();
                } catch (MalformedURLException e) {
                    // try the next candidate path
                }
            }
        }
        return null;
    }

    private void startBackgroundMusic() {
        startBackgroundMusic("/bg.wav", "resources/bg.wav");
    }

    private void startBackgroundMusic(String classpathResource, String filePath) {
        if (backgroundMusic != null && backgroundMusic.isRunning()
                && classpathResource.equals(backgroundMusicResource)) {
            return;
        }
        stopBackgroundMusic();

        URL url = resolveAudioResource(classpathResource, filePath);
        if (url == null) {
            System.err.println("Background music not found: " + classpathResource + " or " + filePath);
            return;
        }

        try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(url)) {
            AudioFormat sourceFormat = sourceStream.getFormat();
            AudioFormat playbackFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sourceFormat.getSampleRate(),
                    16,
                    sourceFormat.getChannels(),
                    sourceFormat.getChannels() * 2,
                    sourceFormat.getSampleRate(),
                    false);
            if (!AudioSystem.isConversionSupported(playbackFormat, sourceFormat)) {
                System.err.println("Background music format cannot be converted for playback: "
                        + sourceFormat);
                return;
            }

            try (AudioInputStream playbackStream =
                    AudioSystem.getAudioInputStream(playbackFormat, sourceStream)) {
                Clip clip = AudioSystem.getClip();
                try {
                    clip.open(playbackStream);
                    setBackgroundMusicVolume(clip);
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                    clip.start();
                    backgroundMusic = clip;
                    backgroundMusicResource = classpathResource;
                } catch (LineUnavailableException e) {
                    clip.close();
                    throw e;
                }
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            backgroundMusic = null;
            System.err.println("Could not play background music: " + e.getMessage());
        }
    }

    private void setBackgroundMusicVolume(Clip clip) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            System.err.println("Background music volume control is not supported by the audio mixer.");
            return;
        }

        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float gainDecibels = (float) (20.0 * Math.log10(BACKGROUND_MUSIC_LEVEL));
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), gainDecibels)));
    }

    private void stopBackgroundMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.flush();
            backgroundMusic.close();
            backgroundMusic = null;
            backgroundMusicResource = null;
        }
    }

    // Click sound for buttons that advance toward the game loop or open Instructions.
    private void playProgressSound() {
        playSfx(SFX_PROGRESS);
    }

    // Click sound for buttons that navigate back to the main menu.
    private void playBackSound() {
        playSfx(SFX_BACK);
    }

    // Fire-and-forget short sound effect; the clip closes itself once playback stops.
    private void playSfx(String fileName) {
        URL url = resolveAudioResource("/sfx/" + fileName, "resources/sfx/" + fileName);
        if (url == null) {
            System.err.println("Sound effect not found on classpath or in resources/sfx/" + fileName);
            return;
        }

        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(url)) {
            Clip clip = AudioSystem.getClip();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.open(audioStream);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(Math.min(gain.getMaximum(),
                        Math.max(gain.getMinimum(), BUTTON_SFX_GAIN_DB)));
            } else {
                System.err.println("Button sound volume control is not supported by the audio mixer.");
            }
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Could not play sound effect " + fileName + ": " + e.getMessage());
        }
    }

    private void playSplashAudio() {
        URL url = getClass().getResource("/splash.wav");
        if (url == null) {
            System.err.println("Splash audio not found on classpath: /splash.wav");
            return;
        }
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(url)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            splashAudio = clip;
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    if (splashAudio == clip) {
                        splashAudio = null;
                    }
                    clip.close();
                }
            });
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Could not play splash audio: " + e.getMessage());
        }
    }

    private void stopSplashAudio() {
        if (splashAudio != null) {
            Clip clip = splashAudio;
            splashAudio = null;
            clip.stop();
            clip.flush();
            clip.close();
        }
    }
}
