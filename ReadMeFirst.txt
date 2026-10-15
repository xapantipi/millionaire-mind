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

A full 15-question round selects five questions from each of Modules 1-3,
while retaining the six Bloom tiers. Source readings identify modules with
the existing "Module N / Reading.pdf" prefix. Switch selects an unused
question from the same Bloom level AND module, preserving module quotas.

5. RUN THE AUTOMATED TESTS

    test.cmd
    test.cmd --gui

The first command runs the non-GUI suite. --gui additionally opens test
windows and exercises the Swing controls. Tests are kept in the source ZIP
for checking and maintenance; test code is not inside the game JAR or EXE.

6. RECREATE THE TWO SUBMISSION ZIPS ON WINDOWS

Use a Windows JDK 17 or later that includes jpackage.exe, then enter:
    release.cmd

This rebuilds the JAR, runs non-GUI tests, creates the native Windows game
with its own Java runtime, verifies its console launcher without system
Java, and creates:
    dist\millionaire-mind-source.zip
    dist\millionaire-mind-windows.zip

The unpackaged Windows image is also available at:
    build\windows\MillionaireMind\MillionaireMind.exe

The executable ZIP is for Windows. The JAR and source support Java 17+ on
other platforms with a graphical desktop for Swing. In Bash, source ZIP
users can run bash ./build.sh, bash ./run.sh, and bash ./test.sh.

7. SOURCE FOLDERS

    src\main\java     Game source classes
    src\test\java     Dependency-free regression tests
    MCQ                Official editable question pool
    resources          Bundled sounds and splash animation
    tools              Shared JDK-only build and release driver
    docs               Project activity and verification notes

The source ZIP omits .git, generated output, and player session logs.
The executable ZIP contains only the native application, bundled runtime,
game resources in the JAR, and these instructions.
