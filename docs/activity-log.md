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
