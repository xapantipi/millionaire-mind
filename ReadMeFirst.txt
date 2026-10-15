MILLIONAIRE MIND - READ ME FIRST
CMSC 170 Assignment 1

1. PLAY THE STANDALONE WINDOWS GAME

Extract the entire millionaire-mind-windows.zip archive first.
Open the MillionaireMind folder and double-click MillionaireMind.exe.
Keep the app and runtime folders next to the EXE: do not copy the EXE alone.
Java is included, so players do not need to install Java or an IDE.

To play in Command Prompt, open CMD in that folder and enter:
    MillionaireMind-Console.exe

For a custom question bank in CMD:
    MillionaireMind.exe "C:\my questions\bank.csv"
    MillionaireMind-Console.exe --console "C:\my questions\bank.csv"

Normal Mode is untimed. Timed Mode allows 30 seconds per question.
Choose Instructions in the game for the prize ladder and lifeline rules.
Session replays are saved in the logs folder under the working directory.
Run from a writable folder, such as an extracted folder under Documents.

2. COMPILE THE SOURCE CODE USING COMMAND PROMPT

a. Extract millionaire-mind-source.zip completely.
b. Install JDK 17 or later and make its java command available on PATH.
   An IDE, Maven, and third-party Java libraries are not needed.
c. Open CMD. Switch to the extracted source folder, for example:
       cd /d "C:\Users\YourName\Documents\MillionaireMind-source"
d. Check Java:
       java -version
   It must report version 17 or later. Use the JDK, not an old Java 8 runtime.
e. Compile and build the runnable JAR:
       build.cmd
   The build uses UTF-8 and --release 17. It creates:
       dist\millionaire-mind.jar
   All production files are compiled together. Required assets and the bank
   are included in the JAR. No files outside the source folder are required.

3. RUN THE SOURCE VERSION FROM CMD

    run.cmd
    run.cmd --console

run.cmd builds the game before starting it. The first command opens Swing;
the second plays entirely in the terminal. For launch options:
    run.cmd --help

To run an already built JAR without rebuilding:
    java -jar dist\millionaire-mind.jar
    java -jar dist\millionaire-mind.jar --console

4. CHECK THE QUESTION BANK

All official questions are in MCQ\Question Bank.csv. The same MCQ path is
preserved inside the packaged JAR. Open the CSV to inspect prompts, four
options, correctIndex, hints, readings, and page numbers. correctIndex is
zero-based: 0=A, 1=B, 2=C, 3=D. Do not add or remove schema columns.

A full 15-question round selects 4/4/4/3 across Modules 1-4, varying which
module gets three, while retaining the six Bloom tiers. The bank contains
96 questions from all nine team activities, including machine learning and
ensemble methods. New citations use PDF page positions, rather than printed
book page numbers. Source readings identify modules with
the existing "Module N / Reading.pdf" prefix. Switch selects an unused
question from the same Bloom level AND module, preserving module quotas.

5. SUBMISSION CONTENTS

The source ZIP contains the files needed to inspect, compile, and run:
    src\main\java     Game source classes
    MCQ                Official editable question pool
    resources          Used sounds and splash animation
    tools\Project.java Shared JDK-only build helper used by build.cmd/run.cmd
    build.cmd          Compile the game and create the runnable JAR
    run.cmd            Build and launch Swing or console gameplay
    ReadMeFirst.txt     These instructions

Keep tools\Project.java: both CMD scripts depend on it. Building and playing
need only the included files and JDK 17 or later. No tests are needed to
compile or run the submitted game.

Tests and their launchers, developer notes, Git settings, alternate launchers,
and release tooling are maintained in the full project repository rather
than included in this submission. The game JAR and EXEs contain no tests.

Submit the two separate archives:
    millionaire-mind-source.zip    Source, bank, assets, and build instructions
    millionaire-mind-windows.zip   Windows EXEs, bundled Java, and instructions

The Windows ZIP must be extracted completely with its app/runtime folders.
The source ZIP can be built with JDK 17+; the native Windows ZIP needs no
installed Java. Session logs and generated build output are not submitted.
