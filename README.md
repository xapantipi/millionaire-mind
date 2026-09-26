# Millionaire Mind

A Java 17 game for *Millionaire Mind*, the Bloom's Taxonomy variant
of *Who Wants to Be a Millionaire?* on AI fundamentals, following the
architecture in the team proposal (Section 6). The console launcher remains
available; a separate Swing UI is an isolated visual experiment.

## Requirements

- JDK 17 or later (uses `java.util.random.RandomGenerator`, switch
  expressions, and `String.isBlank()`). No external libraries or network
  access are required -- the whole game is self-contained.

## Build & run

From this experimental worktree on Windows PowerShell:

```powershell
.\run-swing.cmd
```

The Windows launcher compiles the Java 17 sources into a temporary directory,
copies the bundled question bank, then opens Swing. It can also accept a path
to another question-bank CSV: `.\run-swing.cmd "path\to\questions.csv"`.
Run it from the `millionaire-mind-swing-ui` worktree; the original
`millionaire-mind` checkout does not contain this experiment.
The Swing UI opens fullscreen at the primary display's current resolution,
without window borders. During the splash screen, click anywhere or press
**Enter** to skip it immediately; otherwise, it fades away automatically
after 20 seconds. The title screen offers **Play**, **Instructions**, and
**Exit**. Play opens a centered player-name form. In the game, select an
answer and then choose **Lock in answer**. Long answer choices wrap in taller
cards. For unusually long text, the Swing view reduces its text size to fit
the display.
The Swing screens use a navy node theme, a question-progress strip, and
color-coded prize checkpoints. `resources/bg.wav` loops on the menu,
instructions, mode selection, and name screens, and stops for gameplay,
feedback, and results. `resources/sfx/niera_sound_5.wav` plays on buttons
that advance toward the game loop or open Instructions, and
`resources/sfx/niera_sound_2.wav` plays on buttons that return to the main
menu. `resources/sfx/select_005.wav` plays when selecting an answer and
locking it in, `resources/sfx/confirmation_004.wav` plays for a correct
answer. `resources/sfx/error_006.wav` plays when an attempted action cannot
proceed, such as starting without a name or trying to reuse a spent lifeline.
Spent lifelines keep their disabled appearance but can be clicked to play the
error sound and show a notice. Successfully using a lifeline plays
`resources/sfx/niera_sound_2.wav`.

From a Bash shell:

```bash
./build.sh      # compiles to ./out and copies the question bank alongside it
./run.sh        # console game with the official bank
./run-swing.sh  # rebuilds, then opens the Swing game

# or, to try a different/expanded question bank file:
./run.sh "path/to/other-questions.csv"
./run-swing.sh "path/to/other-questions.csv"
```

Manually, without the scripts:

```bash
javac --release 17 -d out $(find src/main/java -name "*.java")
cp "resources/Question Bank.csv" out/questions.csv
java -cp out millionairemind.ConsoleUI
```

To launch Swing manually with its background and splash audio/assets:

```bash
javac --release 17 -d out-swing $(find src/main/java -name "*.java" -not -name "ConsoleUI.java")
cp "resources/Question Bank.csv" out-swing/questions.csv
cp "resources/bg.wav" out-swing/bg.wav
cp "resources/splash.gif" out-swing/splash.gif
cp "resources/splash.wav" out-swing/splash.wav
mkdir -p out-swing/sfx
cp "resources/sfx/niera_sound_5.wav" out-swing/sfx/niera_sound_5.wav
cp "resources/sfx/niera_sound_2.wav" out-swing/sfx/niera_sound_2.wav
cp "resources/sfx/select_005.wav" out-swing/sfx/select_005.wav
cp "resources/sfx/confirmation_004.wav" out-swing/sfx/confirmation_004.wav
cp "resources/sfx/error_006.wav" out-swing/sfx/error_006.wav
java -cp out-swing millionairemind.SwingUI
```

`SwingUI` also falls back to reading `bg.wav` and the `sfx/` sounds straight
from `resources/` on disk (relative to the working directory) if they were
not copied onto the classpath, so a manual run still gets sound even if a
copy step is skipped.

Session replay logs are written to `logs/replay-YYMMDD-HHMMSS.log`. A numeric
suffix keeps files distinct when two sessions finish within the same second.
Launch from the project directory so both UIs write logs there. In PowerShell
without Bash, use:

```powershell
$javaFiles = @(Get-ChildItem .\src\main\java -Recurse -Filter '*.java' -File |
    Select-Object -ExpandProperty FullName)
javac --release 17 -d out $javaFiles
Copy-Item -LiteralPath 'resources\Question Bank.csv' -Destination 'out\questions.csv'
java -cp out millionairemind.SwingUI
# Or: java -cp out millionairemind.ConsoleUI
```

## Project layout

```
src/main/java/millionairemind/
  BloomLevel.java       Bloom's six levels + which question slots (1-15) each owns
  PrizeLadder.java       The 15-rung prize table and the two checkpoint slots (Q5, Q10)
  Question.java           POJO: prompt, 4 options, correct index, hint, source citation
  QuestionBank.java       Loads the CSV question bank, draws unused questions per level
  LifelineManager.java    50:50 / Spin the Wheel / Switch the Question / Phone a Friend
  PlayerSession.java      Banked winnings, answer history, Bloom's report tallying
  GameEngine.java         Game state + rules (checkpoints, drop-on-wrong, walk away)
  ReplayLogger.java       Writes the full session + Bloom's report to a local log file
  ConsoleUI.java          Thin console presentation layer (talks only to GameEngine)
  SwingUI.java            Separate Swing menu, instructions, name, game, and results screens

resources/
  Question Bank.csv       Official source question bank (see format below)
  bg.wav                  Background music for non-gameplay Swing screens
  sfx/niera_sound_5.wav   Click sound for buttons progressing to the game/instructions
  sfx/niera_sound_2.wav   Click sound for buttons returning to the main menu
```

This mirrors the "Backend Architecture (Java)" section of the proposal
exactly: `GameEngine` owns state and rules, `QuestionBank` only knows how
to load and draw questions, `LifelineManager` only knows lifelines,
`PlayerSession` only tracks progress/results, `ReplayLogger` only persists
logs, and both `ConsoleUI` and `SwingUI` present the same `GameEngine`
state. The engine also owns the current question's visible-option mask so
combined lifelines behave consistently in both UIs.

## Question bank format (`resources/Question Bank.csv`)

One header row, then one row per question:

```
id,level,prompt,optionA,optionB,optionC,optionD,correctIndex,hint,sourceReading,pageNumber
```

- `level` is one of `REMEMBERING`, `UNDERSTANDING`, `APPLYING`,
  `ANALYZING`, `EVALUATING`, `CREATING`.
- `correctIndex` is 0-based (0=A, 1=B, 2=C, 3=D).
- `hint` is the text revealed by the **Phone a Friend** lifeline.
- `sourceReading` + `pageNumber` are shown after every answer as the
  citation, per the proposal's "Source Citation on Every Question" feature.
- Wrap any field containing a comma or quote in double quotes (standard
  CSV quoting, `""` for an embedded quote).
- Quoted fields may contain line breaks, so one CSV record is not necessarily
  one physical text line. The loader validates the header and all records
  before gameplay, including IDs, Bloom levels, options, answer indexes,
  source information, and the minimum questions needed for the 15 slots.

The official `Question Bank.csv` is the editable source file. Keep adding
source-cited questions from the assigned readings using the same 11 columns
and four-option format. The loader requires enough questions for every slot
in the 15-question game; additional questions provide more variety between
playthroughs.

## Gameplay rules implemented

- 15 questions across six Bloom's tiers (`Q1-2` Remembering, `Q3-5`
  Understanding, `Q6-7` Applying, `Q8-10` Analyzing, `Q11-13` Evaluating,
  `Q14-15` Creating), prizes from $100 up to $1,000,000.
- Checkpoints at Q5 and Q10: a wrong answer drops the player back to the
  last checkpoint they passed (or to $0 if none yet).
- Walk Away at any time banks the last checkpoint amount.
- Four lifelines, each usable once per game: 50:50, Spin the Wheel
  (weighted: reveal-one-wrong / reduce-to-two / bonus-time), Switch the
  Question (redraws from the same Bloom's level), Phone a Friend (reveals
  the question's hint).
- Every question in a level is drawn at random from the unused pool for
  that level, so repeat playthroughs vary even though the bank is fixed.
- Post-game Bloom's report: per-level correct/incorrect tally plus
  strongest/weakest level, computed entirely locally.
- Full session replay log (every question drawn, chosen answer, outcome,
  and the Bloom's breakdown) written to `logs/`.

## Swing experiment

Start by entering a player name. Select an answer and use **Lock in answer**
to submit it; the feedback screen shows the correct answer and source before
continuing. The right ladder shows all 15 prizes, with Q5 and Q10 highlighted
as checkpoints. The guaranteed amount and Walk Away payout come from the
engine. The centered feedback screen marks correct answers in green and
incorrect answers in red. A Fifty-Fifty removal leaves the choice card blank.
The results screen shows winnings, the Bloom report, and an **Open replay**
button that opens the saved log in the desktop's default application. The
window supports keyboard focus, answer mnemonics Alt+A through
Alt+D, Alt+L to lock, Alt+1 through Alt+4 for lifelines, and Alt+W to walk
away. All fifteen prize rungs are visible without scrolling.

Spin the Wheel's bonus-time result is displayed as a timer-mode outcome; it
does not start a timer in this untimed game.

## Not yet implemented

- The optional 30-second-per-question timer mode mentioned in Section 3
  (the base implementation is untimed, as specified).
