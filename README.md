# Millionaire Mind -- CLI Backend

A Java console backend for *Millionaire Mind*, the Bloom's Taxonomy variant
of *Who Wants to Be a Millionaire?* on AI fundamentals, following the
architecture in the team proposal (Section 6).

## Requirements

- JDK 17 or later (uses `java.util.random.RandomGenerator`, switch
  expressions, and `String.isBlank()`). No external libraries or network
  access are required -- the whole game is self-contained.

## Build & run

```bash
./build.sh      # compiles to ./out and copies the question bank alongside it
./run.sh        # plays with the bundled bank (resources/questions.csv)

# or, to try a different/expanded question bank file:
./run.sh path/to/other-questions.csv
```

Manually, without the scripts:

```bash
javac -d out $(find src -name "*.java")
cp resources/questions.csv out/
java -cp out millionairemind.ConsoleUI
```

Session replay logs are written to `logs/session-<name>-<timestamp>.log`.

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

resources/
  questions.csv           The compiled question bank (see format below)
```

This mirrors the "Backend Architecture (Java)" section of the proposal
exactly: `GameEngine` owns state and rules, `QuestionBank` only knows how
to load and draw questions, `LifelineManager` only knows lifelines,
`PlayerSession` only tracks progress/results, `ReplayLogger` only persists
logs, and `ConsoleUI` is a thin layer that talks solely to `GameEngine`.
Swapping in a Swing/JavaFX `GameUI` later means writing a new class against
the same `GameEngine` API -- nothing above the UI layer needs to change.

## Question bank format (`resources/questions.csv`)

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

**The bundled `questions.csv` ships 30 placeholder questions (5 per
Bloom's level) covering common AI-fundamentals topics (supervised vs.
unsupervised learning, overfitting, bias, NLP/computer vision, evaluation
metrics, etc.), each with an invented `"AI Fundamentals Course Packet"`
citation.** Per Section 4 of the proposal, replace these with each team
member's own six hand-authored, source-cited questions (one per Bloom's
level) drawn from your actual assigned readings -- just keep the same 11
columns and 4-option format. The engine doesn't care how many questions
sit in a level's pool beyond the 2 (or more) needed per playthrough; more
rows per level just means more variety across replays, per the
"Randomized Draw Within Each Level" feature.

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

## Not yet implemented (left for the Swing/JavaFX or timer follow-up)

- The optional 30-second-per-question timer mode mentioned in Section 3
  (the base implementation is untimed, as specified).
- A graphical `GameUI` matching the wireframes -- `ConsoleUI` can be
  swapped for one without touching `GameEngine`.
