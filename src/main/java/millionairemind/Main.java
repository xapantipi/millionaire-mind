package millionairemind;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Shared entry point for source launchers and the executable JAR. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        int status = run(args);
        if (status != 0) System.exit(status);
    }

    static int run(String[] args) {
        boolean console = false;
        String bankPath = null;
        int index = 0;
        if (args.length > 0 && args[0].equals("--help")) {
            System.out.println("Usage: java -jar millionaire-mind.jar [--console|--swing] [question-bank.csv]");
            System.out.println("Default: Swing with Normal and Timed modes. --console: untimed terminal game.");
            System.out.println("Requires Java 17+. Replay logs are saved in ./logs/.");
            return args.length == 1 ? 0 : 2;
        }
        if (args.length > 0 && (args[0].equals("--console") || args[0].equals("--swing"))) {
            console = args[0].equals("--console");
            index++;
        }
        if (index < args.length && !args[index].startsWith("--")) bankPath = args[index++];
        if (index != args.length) {
            System.err.println("Invalid arguments. Use --help for launch options.");
            return 2;
        }
        String[] bankArgs = bankPath == null ? new String[0] : new String[]{bankPath};
        try {
            if (console) new ConsoleUI().run(bankArgs);
            else {
                if (GraphicsEnvironment.isHeadless()) {
                    System.err.println("Swing needs a graphical desktop. Run with --console for terminal play.");
                    return 1;
                }
                SwingUI.launch(bankArgs);
            }
            return 0;
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            System.err.println("Cannot start Millionaire Mind: " + e.getMessage());
            return 1;
        }
    }
}
