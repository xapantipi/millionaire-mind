# Millionaire Mind

A Java 17 quiz game about AI fundamentals, inspired by *Who Wants to Be a Millionaire?* and organized around Bloom's Taxonomy. Play with the Swing desktop interface or in a terminal. No IDE, Maven, external libraries, or network connection is needed.

## Requirements

- Building or launching from source: **JDK 17 or later**, with `java` on PATH. Check with `java -version`.
- Running the packaged JAR: **Java 17 or later**. No source files are needed.
- Swing requires a graphical desktop. Console play works without one.
- Windows source launchers use Command Prompt or PowerShell; macOS/Linux launchers use Bash.

## Play from source

In Windows PowerShell:

```powershell
.\run.cmd
.\run.cmd --console
```

On macOS/Linux or Git Bash:

```bash
./run.sh
./run.sh --console
```

The default is Swing. Each source launcher rebuilds the production code and packages resources before starting, so edits cannot leave the game running stale classes. Paths containing spaces are supported. Existing `run-swing.cmd`, `run-swing.ps1`, and `run-swing.sh` commands remain aliases for the default Swing launch.

To use another CSV bank, add its path:

```powershell
.\run.cmd --console "C:\my questions\bank.csv"
.\run.cmd "C:\my questions\bank.csv"
```

`--swing` explicitly selects Swing. `--help` prints the launch options. Mode flags must come before the optional CSV path. Relative CSV paths and replay logs use the terminal's current working directory, including when a launcher is invoked from elsewhere.

## Build and distribute

```powershell
.\build.cmd
```

```bash
./build.sh
```

The build compiles every production class together for Java 17 with UTF-8 and creates **`dist/millionaire-mind.jar`**. Only JDK tools are used. The common build driver is `tools/Project.java`; both sets of scripts call it, keeping packaging identical across platforms.

For the assignment's two Windows submission ZIPs, use a Windows JDK that
includes `jpackage` and run:

```cmd
release.cmd
```

This produces `dist/millionaire-mind-source.zip` and
`dist/millionaire-mind-windows.zip`. Extract the Windows ZIP completely and
open `MillionaireMind/MillionaireMind.exe`. The adjacent `app` and `runtime`
folders are required; Java is bundled, so the player needs no Java installation.
`MillionaireMind-Console.exe` opens terminal gameplay. The unpackaged native
application is also under `build/windows/MillionaireMind/`.

`ReadMeFirst.txt` includes the required Command Prompt compilation, run,
test, question-bank, and distribution instructions. Release packaging uses
[JDK jpackage](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html)
in application-image mode, so it needs no third-party installer tools.

Copy the JAR anywhere and run:

```bash
java -jar millionaire-mind.jar
java -jar millionaire-mind.jar --console
java -jar millionaire-mind.jar --help
```

The JAR includes the official question bank, splash animation, and all referenced WAV sounds. It does not need a separate resources directory and contains no tests, development sources, or session logs. Audio failures are reported without preventing gameplay. Generated classes are kept under `build/`; generated classes and the JAR are ignored by Git. Historical session files remain preserved in the repository.

## How to play

Swing opens fullscreen. Click the splash screen or press Enter to skip it. Choose Play, choose a mode, and enter a name. Select an answer, then **Lock in answer**. Feedback gives the correct answer and its source before continuing. Exceptionally long question content and smaller screens can scroll vertically to keep text and controls accessible; the prize ladder stays separate. Results show winnings and the Bloom report, with an option to open the replay file or play again.

- **Normal Mode:** no time limit.
- **Timed Mode:** 30 seconds per question. A timeout ends the game and pays the last passed checkpoint. Feedback and results do not consume time.
- **Console:** untimed; enter A-D, use the displayed lifeline shortcuts, or W to Walk Away. Closing terminal input exits cleanly, banking the checkpoint if a session is active.
- Answer 15 questions: Q1-2 Remembering, Q3-5 Understanding, Q6-7 Applying, Q8-10 Analyzing, Q11-13 Evaluating, Q14-15 Creating.
- Prizes range from $100 to $1,000,000. Passing Q5 secures $2,000; passing Q10 secures $64,000. Wrong answers and Walk Away pay the last secured checkpoint, rather than the most recent question's prize.
- Each full round contains five questions from each of Modules 1, 2, and 3, without repeats and with the same Bloom tiers. The round is selected before play to ensure both constraints can be met. Answer positions are shuffled while preserving the correct answer and citation.
- Each lifeline can be used once: **50:50** removes wrong options; **Spin the Wheel** has a 45% chance to remove one wrong option, 35% to reduce to at most two choices, and 20% to add 15 seconds in Timed Mode; **Switch the Question** draws another unused question at the same Bloom level and module and resets the timer to 30 seconds; **Phone a Friend** reveals the authored hint.
- Combined lifelines never restore hidden choices. A switch is refused without consuming the lifeline when no spare question is available outside the planned round at that Bloom level and module. Bonus time has no effect in untimed play.

Swing supports Tab navigation, Alt+A through Alt+D for answers, Alt+L to lock, Alt+1 through Alt+4 for lifelines, and Alt+W to Walk Away. Used lifelines remain visibly unavailable and explain their status when clicked.

Session logs are written to **`logs/replay-YYMMDD-HHMMSS.log`** under the working directory. Numeric suffixes prevent overwriting sessions completed in the same second. Logs include answered questions, timeout entries, selected/correct answers, citations, final winnings, and per-level performance. Hover over the saved-replay status or Open replay button for its full path. The game reports save failures clearly.

## Tests

```powershell
.\test.cmd
.\test.cmd --gui
```

```bash
./test.sh
./test.sh --gui
```

The default suite tests CSV loading and validation, shuffled answers, game rules, timers, checkpoints, Walk Away, combined lifelines, exhausted switch pools, session history, replay logs, console behavior, and the standalone JAR. Packaging tests copy the JAR to a temporary directory without repository resources and complete a full console win using shuffled answers.

`--gui` also runs real Swing controls on a graphical desktop, including mode selection, timer expiry/bonus/reset, feedback, full-game outcomes, repeat games, and layout checks. It opens temporary test windows. The default command explicitly reports that desktop tests were not run; requesting GUI tests without a desktop fails clearly. Production and tests are compiled separately, and a failing test returns a nonzero exit code.

## Question bank

The editable source is `MCQ/Question Bank.csv`, currently containing 72 questions. The build packages it as `/MCQ/Question Bank.csv`. Preserve this header:

```text
id,level,prompt,optionA,optionB,optionC,optionD,correctIndex,hint,sourceReading,pageNumber
```

`level` must be REMEMBERING, UNDERSTANDING, APPLYING, ANALYZING, EVALUATING, or CREATING. `correctIndex` is zero-based: 0=A, 1=B, 2=C, 3=D. All four options, a hint, and source information are required. IDs must be unique. The loader validates the complete bank before play and requires enough questions for each level's slots. Include spare questions to support switching. Modules are derived from the existing `sourceReading` prefix (`Module N / Reading.pdf`); no new column is used. Multi-module banks must label every source consistently, the module count must divide 15, and the pool must support all Bloom tiers with equal module quotas. Incompatible pools fail validation before play. A custom bank with no module prefixes is treated as one unspecified module. See `MCQ/README.md` for checking details.

Standard CSV quoting is supported: quote fields containing commas, quotes, or line breaks and double embedded quotes. A quoted multiline field is one record even when it spans several physical lines.

## Code responsibilities

- `Question` and `BloomLevel`: question data and level mapping.
- `QuestionBank`: CSV validation, module-balanced round selection, spare-question selection, and answer shuffling.
- `ModuleBalancedSelection`: allocates Bloom slots to module quotas using a small capacity graph, avoiding greedy choices that could leave later slots unfillable.
- `GameEngine`: progression, visible choices, countdown rules, checkpoints, and payouts.
- `LifelineManager`: lifeline availability and randomized outcomes.
- `PlayerSession`: answer history and Bloom reporting.
- `ReplayLogger`: persistent session logs.
- `SwingUI` and `ConsoleUI`: input and presentation; both call the same engine.
- `GameAudio`: asynchronous audio loading and playback cleanup.
- `Main`: shared launch arguments and startup errors.

Tests live in `src/test/java/millionairemind`. No JavaFX branch is needed for this version.
