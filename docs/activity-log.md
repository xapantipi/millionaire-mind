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
