package millionairemind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

/** Small dependency-free regression tests for QuestionBank. */
public final class QuestionBankTest {

    private static final String HEADER = String.join(",",
            "id", "level", "prompt", "optionA", "optionB", "optionC", "optionD",
            "correctIndex", "hint", "sourceReading", "pageNumber");

    private QuestionBankTest() {
    }

    public static void main(String[] args) throws Exception {
        loadsQuotedAndMultilineFields();
        rejectsInvalidQuestionData();
        System.out.println("QuestionBankTest passed");
    }

    private static void loadsQuotedAndMultilineFields() throws IOException {
        List<String[]> rows = minimumRows();
        rows.get(0)[2] = "Prompt with, comma and \"quote\"\ncontinued on the next line";
        rows.get(0)[3] = "Option A, with a comma";
        rows.get(0)[8] = "Hint line one\nHint line two";

        QuestionBank bank = load(toCsv(rows, HEADER).replace("\n", "\r\n"));
        check(bank.totalCount() == 15, "expected 15 questions");
        check(bank.countFor(BloomLevel.REMEMBERING) == 2, "expected two remembering questions");
        check(bank.countFor(BloomLevel.UNDERSTANDING) == 3, "expected three understanding questions");

        Set<String> usedIds = new HashSet<>();
        Question special = null;
        for (int i = 0; i < bank.countFor(BloomLevel.REMEMBERING); i++) {
            Question question = bank.drawQuestion(BloomLevel.REMEMBERING, usedIds);
            usedIds.add(question.id());
            if (question.id().equals("R1")) {
                special = question;
            }
        }

        check(special != null, "special question was not drawn");
        check(special.prompt().equals("Prompt with, comma and \"quote\"\r\ncontinued on the next line"),
                "quoted multiline prompt was not preserved");
        check(special.options().get(0).equals("Option A, with a comma"),
                "quoted comma in an option was not preserved");
        check(special.hint().equals("Hint line one\r\nHint line two"),
                "quoted multiline hint was not preserved");
    }

    private static void rejectsInvalidQuestionData() throws IOException {
        List<String[]> rows = minimumRows();

        rows.get(0)[0] = rows.get(1)[0];
        expectFailure(toCsv(rows, HEADER), "duplicate id");

        rows = minimumRows();
        rows.get(0)[1] = "NOT_A_LEVEL";
        expectFailure(toCsv(rows, HEADER), "invalid Bloom level");

        rows = minimumRows();
        rows.get(0)[3] = "";
        expectFailure(toCsv(rows, HEADER), "optionA is required");

        rows = minimumRows();
        rows.get(0)[7] = "4";
        expectFailure(toCsv(rows, HEADER), "correctIndex must be from 0 to 3");

        rows = minimumRows();
        rows.get(0)[9] = "";
        expectFailure(toCsv(rows, HEADER), "sourceReading is required");

        rows = minimumRows();
        rows.remove(rows.size() - 1);
        expectFailure(toCsv(rows, HEADER), "at least 2 required");

        expectFailure(toCsv(minimumRows(), HEADER.replace("pageNumber", "wrongHeader")), "header must be");
        expectFailure(HEADER + "\n\"R1\"oops", "unexpected character after closing quote");
    }

    private static QuestionBank load(String csv) throws IOException {
        Path path = Files.createTempFile("millionaire-mind-question-bank-", ".csv");
        try {
            Files.writeString(path, csv, StandardCharsets.UTF_8);
            return QuestionBank.loadFromFile(path, RandomGenerator.getDefault());
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private static void expectFailure(String csv, String expectedMessage) throws IOException {
        try {
            load(csv);
            throw new AssertionError("expected failure containing: " + expectedMessage);
        } catch (IOException e) {
            check(e.getMessage().contains(expectedMessage),
                    "expected error containing '" + expectedMessage + "' but got '" + e.getMessage() + "'");
        }
    }

    private static String toCsv(List<String[]> rows, String header) {
        StringBuilder csv = new StringBuilder(header).append('\n');
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    csv.append(',');
                }
                csv.append(csvField(row[i]));
            }
            csv.append('\n');
        }
        return csv.toString();
    }

    private static String csvField(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static List<String[]> minimumRows() {
        List<String[]> rows = new ArrayList<>();
        addRows(rows, "R", "REMEMBERING", 2);
        addRows(rows, "U", "UNDERSTANDING", 3);
        addRows(rows, "A", "APPLYING", 2);
        addRows(rows, "N", "ANALYZING", 3);
        addRows(rows, "E", "EVALUATING", 3);
        addRows(rows, "C", "CREATING", 2);
        return rows;
    }

    private static void addRows(List<String[]> rows, String prefix, String level, int count) {
        for (int i = 1; i <= count; i++) {
            rows.add(new String[]{
                    prefix + i,
                    level,
                    "Prompt " + prefix + i,
                    "Option A",
                    "Option B",
                    "Option C",
                    "Option D",
                    "0",
                    "Hint " + prefix + i,
                    "Reading " + prefix,
                    "1"});
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
