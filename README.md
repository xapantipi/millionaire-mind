# Millionaire Mind

An AI fundamentals quiz inspired by Who Wants to Be a Millionaire, with
Swing graphics, sound, six Bloom tiers, and a 15-question prize ladder.

Install **JDK 17 or later**, extract/download this repository, and open
Command Prompt in its folder. Check `java -version` reports 17 or later, then run:

```cmd
run.cmd
```

For terminal gameplay:

```cmd
run.cmd --console
```

To build without launching:

```cmd
build.cmd
java -jar dist\millionaire-mind.jar
```

Both CMD launchers rebuild from source and use `tools/Project.java`.
No IDE or external libraries are required. Use `run.cmd --help` for options
or add a custom CSV path after the mode flag.

The official bank is `MCQ/Question Bank.csv`, with 96 questions from
Activities 1-9. Rounds select 4/4/4/3 across four modules, varying which gets
three, while preserving Bloom tiers. Normal and console modes are untimed;
Timed Swing mode gives 30 seconds per question, a 15-second Spin bonus,
and a fresh 30 seconds after a successful Switch. Wrong answers and Walk
Away pay the last checkpoint: $2,000 at Q5 or $64,000 at Q10.

`src/main/java/` contains game code; `resources/` contains required sounds
and the splash animation. Replay logs are created in `logs/` under the
working directory.

The separate Windows submission ZIP contains `MillionaireMind.exe` and
`MillionaireMind-Console.exe` with bundled Java. Extract the entire ZIP and
keep its `app` and `runtime` folders beside the EXEs. No Java installation
is needed for that version. The assignment-required `ReadMeFirst.txt` is
included in the submission ZIPs; this repository uses this README instead.

The main branch contains the essentials for building and playing. Tests,
verification notes, Bash launchers, and Windows release tooling are
preserved on the [finalize-swing-game development branch](https://github.com/xapantipi/millionaire-mind/tree/finalize-swing-game).
Generated JARs, ZIPs, and player logs are not tracked.
