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

## 2026-10-15

- Integrated `recovered-work-v2` into `finalize-swing-game`, created from current `origin/main`, in an isolated managed checkout. Preserved the original checkout, its uncommitted files, and historical session logs. No branches were pushed and main was not changed.
- Backend fixes: restored the untimed constructor; centralized 30-second countdown, 15-second bonus, and switch reset rules; rejected hidden/invalid answers; prevented duplicate terminal results; kept combined lifelines from restoring options; reserved enough unused questions before consuming Switch; made question options and returned history read-only.
- Swing fixes: guarded repeated actions; stopped timers outside gameplay and on disposal; moved audio loading/playback/cleanup onto one worker with PCM conversion; kept long questions, hints, and feedback accessible with vertical scrolling at smaller resolutions; clarified replay-save errors without losing results.
- Delivery: added a shared launcher and dependency-free JDK build driver, Windows and Bash build/run/test commands, compatibility Swing wrappers, and a standalone resource-bundled JAR. Source launchers rebuild before playing. Added Git line-ending rules and executable shell-script modes; generated output stays ignored.
- Cleanup/documentation: removed redundant comments and duplicated audio code; rewrote README around the actual commands, modes, rules, question-bank format, and test workflow.
- Files changed: backend and UI classes under src/main/java/millionairemind; regression tests under src/test/java/millionairemind; tools/Project.java; build/run/test launchers and compatibility wrappers; .gitattributes, .gitignore, README.md, and this log.
- Checks passed: Windows build.cmd and test.cmd --gui (all nine test programs); Git Bash ./build.sh and test.sh; Java 17 class-file major version 61; git diff --check. Tests include timeout/checkpoint boundaries, bonus/switch timing, combined lifelines, exhausted pools, shuffled answer identity, CSV quoting/validation, immutable history, logging collisions/failures, all game outcomes, and repeated games.
- Portable checks passed: copied JAR in a directory without source resources completed a shuffled-answer console win and the full desktop suite; both source launchers built and ran from a path containing spaces while called from another directory. Desktop checks cover 1366x768 long content and emit previews under ignored build/previews/. Rendered menu, game, long choices, feedback, and results were inspected.
- Expected diagnostic: the deliberate blocked-log test prints a replay-save error and verifies that results remain available; it is not an unresolved failure.
- Limits: verification used installed JDK 21.0.8 targeting Java 17; a native JDK 17 runtime and native macOS/Linux were unavailable. Bash scripts were exercised using Git Bash on Windows. Opening replay files depends on the desktop's file association.
- Recommended next step: review the local integration branch, then push/open a PR only when authorized. No release-blocking defects were found in the checked scenarios.
- Final visual follow-up: replaced the prize ladder's shrinking BoxLayout with equal-height rows and compact labels after a 1366x768 preview exposed a collapsed lowest rung. Layout assertions now require positive width and height for every checked control and rung. All nine test programs passed again; the official CSV hash matches the bundled resource, and the question bank and historical logs are unchanged from recovered-work-v2.

## 2026-10-16

- Compared the finalized integration branch with all three pages of the supplied CMSC 170 Assignment Guide #1; no program changes were made, and the existing local GameEngine comment edit was preserved.
- Missing guide requirements: an MCQ source directory used for the question pool, equal module counts in selected game questions, a Windows standalone executable, ReadMeFirst.txt, and separate source/executable ZIP deliverables.
- Evidence: the bank is currently resources/Question Bank.csv; QuestionBank selects by Bloom level without module quotas; packaging produces only a JAR; no MCQ directory, ReadMeFirst.txt, EXE, or submission ZIPs were found in this checkout. The source pool has 24 Module 1, 26 Module 2, and 22 Module 3 questions; unequal pool sizes are not themselves the requirement violation.
- Confirmed: the splash GIF includes the new Millionaire Mind: AI Fundamentals title. Existing verified functionality covers random questions/choices, prize ladder, instructions, AI-themed Swing presentation, mouse input, audio, and command-prompt launch.
- Limits: approved group proposal and the calendar date corresponding to Thursday of Week 10 were not supplied; group-specific obligations and the exact deadline remain unverified.
- Recommended next step: plan the module-balanced draw and MCQ packaging without changing the CSV schema or Bloom ladder, then add the required Windows distribution and submission documents/ZIPs.
- Proposal follow-up: reviewed all four pages of MillionaireMindProposal.pdf, including wireframes. The core Bloom ladder, checkpoints, four-option questions, source feedback, four lifelines, untimed base/optional timer, local reporting, and backend separation are represented in the implementation.
- Additional definite gap: proposal promises every drawn question in the replay, but current history records answers/timeouts only; switched-out questions and the current unanswered question on Walk Away are absent.
- Clarification needed: proposal says Walk Away banks current winnings; the previously approved implementation pays the last checkpoint. Preserve that rule unless the user explicitly approves a change.
- Verification limits: the bank exceeds the proposal's estimated 24-30 questions (72 currently); extra questions are not inherently a defect, but individual authorship and correctness against the assigned readings cannot be established from the proposal/code alone. The proposal does not explicitly waive the guide's equal-module selection rule.
- No program changes or test reruns were needed for this document/code review; existing local edits remain preserved.

### Assignment-guide gaps implemented

- Moved the unchanged 72-question bank to MCQ/Question Bank.csv and preserved that path in the JAR. Added MCQ/README.md; the CSV schema and source citations remain unchanged.
- Added ModuleBalancedSelection and module metadata derived from sourceReading. QuestionBank validates that equal module quotas can fill the existing Bloom tiers, then reserves a shuffled complete round. GameEngine uses that plan; Switch selects a spare at the same Bloom level and module without consuming the lifeline on failure. A full official round contains five questions per module. Walk Away payouts and replay-history scope remain unchanged.
- Added release.cmd and Windows JDK jpackage application-image packaging with graphical and console EXEs and a bundled runtime. Included jdk.random explicitly after a native smoke test exposed the missing default random provider. Generated read-only launcher files are handled during safe rebuild cleanup.
- Added ReadMeFirst.txt with CMD compilation, gameplay, tests, module-bank rules, and packaging instructions. Release creates separate source and Windows executable ZIPs, omitting Git data, generated source output, and historical session logs. Updated README and Swing instructions; refreshed Swing layout validation after varied module-balanced draws exposed stale card bounds.
- Files changed: MCQ bank/readme; Question, QuestionBank, ModuleBalancedSelection, GameEngine, ConsoleUI, SwingUI; ModuleSelectionTest, PackagingTest, SwingUITest; tools/Project.java, release.cmd, ReadMeFirst.txt, README.md, and this log.
- Checks passed: Windows test.cmd --gui (all ten test programs), Git Bash ./build.sh and ./test.sh, release.cmd, and fresh source-ZIP extraction/build/all nine non-GUI test programs. Module tests cover 90 complete varied official-bank rounds with switches, sparse feasible pools, impossible module/Bloom coverage, and preserved citations.
- Distribution checks passed: ZIP integrity and allowed contents; identical official CSV in source and native JAR; both EXE headers and bundled JVM present. From a separate folder, the extracted console EXE completed all 15 questions with five per module and saved a replay. The extracted graphical EXE opened and closed cleanly. Native tests removed system Java from PATH and unset Java environment overrides.
- Limits: build/test JDK and bundled Windows runtime are 21.0.8; game classes target Java 17. Bash was checked through Git Bash on Windows. The native image must retain its app/runtime directories. Proposal-only replay-history expansion and Walk Away wording remain outside these five approved changes.
- Recommended next step: review the local finalize-swing-game branch and submit the two ZIPs after the team's content review. No push or merge into main was performed; the pre-existing GameEngine comment deletion remains uncommitted.

### Minimal source submission

- At the user's request, restricted the source ZIP to production Java sources, the official MCQ CSV, used WAV/GIF resources, tools/Project.java, build.cmd, run.cmd, and ReadMeFirst.txt. Excluded tests, developer documentation, Git settings, alternate launchers, release/test scripts, and unused OGG copies from the submission; retained all of them in the repository.
- Updated tools/Project.java's release file selection, README.md, and ReadMeFirst.txt. The build helper stays because both CMD launchers depend on it. Game behavior and required resources are unchanged; most archive size comes from audio and the splash animation.
- Checks passed: release.cmd rebuilt both ZIPs, all nine non-GUI test programs passed, native console smoke test passed without system Java, and Git Bash ./build.sh passed. A fresh minimal-ZIP extraction built from another working directory, launched console successfully, and rebuilt through run.cmd --help. Verified exact ZIP contents and integrity and matching instructions in the Windows ZIP.
- No game/UI changes required another desktop suite run. No push or main merge; pre-existing GameEngine comment deletion remains untouched. Next step: submit the refreshed source and Windows ZIPs after the team's review.

### Activities 8 and 9 question-bank update

- Read all nine Word activities from the user-authorized team1-jumagdao-activities folder. Compared all 96 prompts, choice sets, answer keys, and Bloom tags with the bank. Existing Activities 1-7 already contain 72 matching entries apart from earlier typography, spacing, and typo cleanups; preserved those entries unchanged.
- Imported ACT8-Q01 through ACT8-Q12 and ACT9-Q01 through ACT9-Q12. Added reading-based hints and verified citations against the four supplied machine-learning PDFs. New references explicitly identify PDF page positions; Question.citation avoids adding a duplicate page prefix. Added docs/question-bank-sources.md with the complete new citation map and activity counts.
- The bank now has 96 questions: 24 Module 1, 26 Module 2, 22 Module 3, and 24 Module 4. The user explicitly approved 4/4/4/3 selection across four modules because exact equality is impossible within 15 questions. ModuleBalancedSelection tries alternative balanced quotas when needed for sparse pools; existing Bloom tiers and same-module Switch behavior remain intact. Three-module custom banks still select five per module.
- Updated ModuleSelectionTest for 96 entries, all new shuffled answer keys, explicit PDF citations, every new question's reachability, all four smaller quotas over 90 complete rounds, sparse four-module allocation, and retained three-module behavior. Updated README.md, MCQ/README.md, and ReadMeFirst.txt to describe the actual bank and approved balance.
- Checks passed: Windows test.cmd --gui (all ten test programs), Git Bash ./build.sh and ./test.sh, final ./build.sh after citation formatting, release.cmd with all nine non-GUI programs and native smoke test, and git diff --check. The intentional blocked-log error is a passing failure-handling test.
- Rebuilt both minimal submission ZIPs. Fresh extraction checks passed: integrity and exact source allowlist; no tests/OGG/development files in the source ZIP; identical 96-question CSV in source and native JAR; original 72 entries unchanged; current ReadMeFirst.txt in both. From separate working directories, the extracted source built and run.cmd rebuilt successfully. Both source JAR and native console EXE completed full official-bank wins including new questions, 4/4/4/3 counts, and saved replay logs. Native runs had Java removed from PATH and Java overrides unset. The actual extracted graphical EXE also opened and closed cleanly with its bundled runtime.
- Limits: source classes target Java 17; build/test and bundled Windows runtime remain JDK 21.0.8. The approved near-equal four-module rule differs from the guide's exact-equality wording; instructor acceptance remains unverified. Original documents/readings were read only. No push or main merge; existing GameEngine comment deletion remains uncommitted.
- Recommended next step: use the refreshed source and Windows ZIPs, and confirm the four-module distribution with the instructor before submission.
