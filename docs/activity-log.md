# Activity Log

## 2026-09-22

- CSV question-bank loading and validation.
- Decisions: preserved the 11-column schema; added logical-record parsing for quoted commas, escaped quotes, and multiline fields; enforced the documented per-level minimums for a 15-question game.
- Files changed: `src/main/java/millionairemind/QuestionBank.java`, `src/test/java/millionairemind/QuestionBankTest.java`, `README.md`, `docs/task-1-csv-question-bank.md`.
- Checks: isolated `javac` compilation passed; `QuestionBankTest` passed; ConsoleUI smoke test loaded `resources/Question Bank.csv` and quit normally; `bash ./build.sh` was unavailable because Bash is not installed in this PowerShell environment.
- Follow-up: kept `resources/Question Bank.csv` as the official source file and updated packaging to copy it into the classpath as `questions.csv`; no source CSV rename or content change was needed.
- Follow-up files: `build.sh`, `run.sh`, `README.md`, `docs/task-1-csv-question-bank.md`.
- Checks: isolated compile and regression tests passed; default bundled-resource and explicit-path launch smoke tests passed.
- Unresolved: Bash is unavailable in the current PowerShell environment, so `build.sh` itself could not be run here. The tracked deletion of the old `resources/questions.csv` was preserved.
- Backend rule regression tests for `GameEngine`, `LifelineManager`, `PlayerSession`, and `ReplayLogger`.
- Decisions: followed the existing dependency-free `main`-based test style and compiled into a temporary directory to preserve tracked `out/` artifacts.
- Files changed: added `BackendTestSupport.java`, `GameEngineTest.java`, `LifelineManagerTest.java`, `PlayerSessionTest.java`, and `ReplayLoggerTest.java` under `src/test/java/millionairemind/`; added `docs/task-2-backend-rule-tests.md`; updated this activity log.
- Checks: Java 17 compilation of all source and test files passed; the official CSV was copied into the isolated output; `QuestionBankTest` and all four new test mains passed.
- Unresolved: `build.sh` was not run because Bash is unavailable; its Java compilation and resource-packaging steps were checked in the isolated output instead.
- Recommended next step: review and merge this test-only change separately from PR #3.
- Follow-up after interactive testing: normalized remaining curly apostrophes, corrected two spelling mistakes and the incomplete `ACT7-Q07` answer, and fixed console lifeline option visibility.
- Decisions: option removals persist for the current question and reset on question switch or advancement; kept the game untimed per the user's choice.
- Files changed: `resources/Question Bank.csv`, `src/main/java/millionairemind/ConsoleUI.java`, and new `src/test/java/millionairemind/ConsoleUITest.java`.
- Checks: Java 17 isolated compilation and all six test mains passed, including a console integration test that checks 50:50 persistence and question-switch reset; current CSV loaded through the packaged resource; encoding, dangling-text, and whitespace scans passed.
- Unresolved: the Spin the Wheel bonus-time result remains timer-only in the untimed game.
- Recommended next step: replay the lifelines interactively against the corrected question bank.
- Perfect-win Bloom report tie display.
- Decision: retain the per-level score breakdown and replace misleading strongest/weakest labels with a 100% tie message after a millionaire run; scoring and game rules are unchanged.
- Files changed: `src/main/java/millionairemind/ConsoleUI.java` and this activity log.
- Checks: Java 17 isolated compilation and all test mains passed, including a ConsoleUI integration case for a perfect 15-question win.

## 2026-09-23

- Built an isolated Swing UI experiment on `experiment/swing-ui` from the `backend-rule-tests` HEAD; the original checkout and its five untracked files were left untouched.
- Decisions: kept the console launcher; moved current-question option visibility into `GameEngine` so both UIs preserve combined lifeline and switch behavior; displayed Spin's bonus-time result without adding a timer.
- Files changed: `GameEngine.java`, `ConsoleUI.java`, `SwingUI.java`, `GameEngineTest.java`, `SwingUITest.java`, `build.sh`, `run-swing.sh`, `README.md`, this log, and `docs/swing-ui-preview.png`. The CSV schema and question bank were unchanged.
- Checks: `build.sh` passed with Git Bash in a temporary staging copy; Swing class output targets Java 17 and the packaged CSV matches the source. All seven test mains passed, including desktop checks for selection, long prompts, lifelines, Q5 guarantee, Walk Away, wrong-answer and millionaire results, citations, and replay logs; `git diff --check` passed.
- Unresolved: the optional timer mode remains unimplemented. Smaller windows use scrollbars for the question area and ladder.
- Recommended next step: review the visual experiment and decide whether to adopt or revise it before any commit.
- Launch follow-up: added `run-swing.cmd` and `run-swing.ps1` for Windows PowerShell. The launcher builds into the system temporary directory, avoiding the tracked and initially stale `out/` folder. `run-swing.sh` now always rebuilds before launching.
- Checks: the Windows launcher compiled Swing and packaged the question bank; a valid launch started the Java game process. The original checkout's build-output changes from the user's own command were left as-is.
- UI follow-up after player testing: added a title menu with Play, Instructions, and Exit; moved player-name entry to a centered form; made answer cards grow by wrapped text and kept the question area scrollable. Results now offer a return to the menu.
- Files changed: `SwingUI.java`, `SwingUITest.java`, `README.md`, this log, and Swing preview images. No game rules, CSV fields, or question data changed.
- Checks: `build.sh` passed under Git Bash in a temporary staging copy; all seven test mains passed. Desktop checks covered menu navigation and Exit, name-form centering, a long choice at the minimum window size, and the existing game flow. Screenshots of the menu, instructions, name form, and long choice were inspected. The packaged question bank matched the source; Swing compiled to Java 17 class version 61; `git diff --check` passed.
- Unresolved: very long prompts and answers can require scrolling in a small window. The optional timer mode remains unimplemented.
- Visual polish follow-up: enriched the existing navy Swing theme with subtle node artwork, rounded gradient surfaces and buttons, a question-progress strip, clearer answer hover/selection states, lifeline icons, dark scrollbars, and refined ladder highlights. Game rules and data were unchanged.
- Files changed: `SwingUI.java`, `README.md`, this log, and the five Swing preview images.
- Checks: `build.sh` passed with Git Bash in a temporary staging copy; all seven automated test mains passed, including the desktop Swing smoke test. Java 17 class version 61 and the packaged question-bank hash were verified; the refreshed menu, name, game, and long-choice screenshots were inspected.
- Recommended next step: review the visual treatment in the isolated worktree before deciding whether to commit it.
- UI revision after playtesting: centered green and red answer feedback; blank cards for Fifty-Fifty removals; complete question, choices, and all fifteen prize rungs without game scrollbars; centered game report with an Open replay button; shorter replay filenames; and structured instructions with checkpoint prizes and lifeline bullets.
- Decisions: kept game rules in `GameEngine`; used Swing's wrapped text measurements and smaller text only when needed to fit the question view; used Java `Desktop` to open a saved replay in the default application. Replay names now use `replay-YYMMDD-HHMMSS.log` with a collision suffix, while player names stay in the file contents.
- Files changed: `SwingUI.java`, `SwingUITest.java`, `ReplayLogger.java`, `ReplayLoggerTest.java`, `README.md`, this log, and the Swing preview images. The CSV schema, question bank, console launch path, and payout rules were unchanged.
- Checks: `build.sh` passed with Git Bash in a temporary staging copy; all seven test mains passed. Swing smoke checks covered feedback colors and centering, full long text, blank Fifty-Fifty choices, all ladder rungs at the minimum window size, instructions and report layout, Walk Away, and a saved replay target. Java 17 class version 61 and matching packaged question-bank hash were verified; revised screenshots were inspected.
- Limitations: opening a replay needs a desktop application associated with `.log` files; the optional timed mode remains unimplemented. The no-scroll layout was checked at the current desktop's 1400-by-800 minimum window size.
- Recommended next step: play through the revised experiment using `run-swing.cmd` and review it before any commit.
