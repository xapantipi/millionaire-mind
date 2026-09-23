# Millionaire Mind

**Millionaire Mind** is a JavaFX quiz game inspired by *Who Wants to Be a
Millionaire?* and organized around Bloom's Taxonomy. The project combines a
clean, responsive desktop interface with a reusable game engine and a
source-cited question bank.

## Current experience

The default entry point is the JavaFX application:

- A fullscreen title screen with **Play**, **Instructions**, and **Quit**
  actions
- Responsive typography and controls that adapt to the window size
- F11 to toggle fullscreen and Esc to leave fullscreen
- A separate console UI for running the game without the graphical interface

The GUI is still being built out. The underlying game rules, question bank,
lifelines, reporting, and replay logging are already implemented in the
backend.

## Requirements

- JDK 21 or later
- Maven 3.9 or later
- A desktop environment capable of running JavaFX

JavaFX is downloaded by Maven from the dependencies declared in
[`pom.xml`](C:/Users/AARB/millionaire-mind_copy/pom.xml), so no separate
JavaFX installation is required.

## Run the application

From the project root:

```bash
mvn clean javafx:run
```

The application starts in fullscreen mode. Use **F11** to switch between
fullscreen and windowed mode, and **Esc** to exit fullscreen.

## Run the console version

The original console entry point remains available through Maven:

```bash
mvn compile exec:java
```

To use a different question bank, pass its path as the first argument:

```bash
mvn compile exec:java -Dexec.args="path/to/other-questions.csv"
```

The repository also contains `build.sh` and `run.sh` as legacy helpers for the
older standalone console workflow. Maven is the recommended build and run
workflow because it includes the JavaFX dependency and resource configuration.

## Project structure

```text
src/main/java/millionairemind/
  BloomLevel.java       Bloom's six levels and their question slots
  PrizeLadder.java      Prize values and checkpoint amounts
  Question.java         Question prompt, options, answer, hint, and citation
  QuestionBank.java     CSV loading and randomized question selection
  LifelineManager.java  50:50, Spin the Wheel, Switch, and Phone a Friend
  PlayerSession.java    Winnings, answer history, and Bloom's report data
  GameEngine.java       Game state and gameplay rules
  ReplayLogger.java     Local session replay logging
  ConsoleUI.java        Console presentation layer
  gui/
    Main.java           JavaFX application entry point
    GameUI.java         Screen navigation and window behavior
    screens/            Title, instructions, and play screens

resources/
  Question Bank.csv     Official source question bank (see format below)
  millionairemind/
    gui/game.css        JavaFX theme
```

The UI layers depend on `GameEngine`, not the other way around. This keeps the
game rules reusable while the JavaFX screens continue to evolve.

## Gameplay rules

- 15 questions are distributed across six Bloom's levels:
  Remembering (Q1–2), Understanding (Q3–5), Applying (Q6–7),
  Analyzing (Q8–10), Evaluating (Q11–13), and Creating (Q14–15)
- Prizes range from $100 to $1,000,000
- Checkpoints are reached at Q5 and Q10
- A wrong answer returns the player to the most recent checkpoint
- Walking away banks the last checkpoint amount
- Each lifeline can be used once per game:
  **50:50**, **Spin the Wheel**, **Switch the Question**, and
  **Phone a Friend**
- Questions are randomly selected from the unused questions in their Bloom's
  level, and each question's four choices are reshuffled every time it is
  drawn
- A post-game report shows correct and incorrect answers by level
- Each session is written to `logs/session-<name>-<timestamp>.log`

## Question bank format

The bundled file is [`resources/Question Bank.csv`](C:/Users/AARB/millionaire-mind_copy/resources/Question Bank.csv).
It contains one header row followed by one question per row:

```text
id,level,prompt,optionA,optionB,optionC,optionD,correctIndex,hint,sourceReading,pageNumber
```

- `level` must be one of `REMEMBERING`, `UNDERSTANDING`, `APPLYING`,
  `ANALYZING`, `EVALUATING`, or `CREATING`
- `correctIndex` is zero-based: `0=A`, `1=B`, `2=C`, `3=D`. The loader
  resolves this to the answer's text at load time, so shuffling a question's
  displayed choices never changes which one is graded as correct.
- `hint` is revealed by **Phone a Friend**
- `sourceReading` and `pageNumber` provide the citation shown after an answer
- Fields containing commas or quotes must use standard CSV quoting; represent
  an embedded quote as `""`
- Quoted fields may contain line breaks, so one CSV record is not necessarily
  one physical text line
- The loader validates the header and every record before gameplay starts
  (ids, Bloom levels, options, answer indexes, source info) and enforces the
  minimum number of questions needed to cover all 15 slots per Bloom's level

Add more questions to a level to increase variety between playthroughs. Keep
the same 11 columns and four-option format.

## Planned work

- Connect the JavaFX play screen to the full game engine
- Add the question, answer, prize ladder, and lifeline controls to the GUI
- Add the post-game Bloom's report and replay summary to the GUI
- Add the optional timed-question mode
