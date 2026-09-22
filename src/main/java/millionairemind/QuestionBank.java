package millionairemind;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Loads the collected question bank and groups
 * questions by Bloom's level.
 *
 * CSV format:
 *
 * id,level,prompt,optionA,optionB,optionC,optionD,
 * correctAnswer,hint,sourceReading,pageNumber
 */
public final class QuestionBank {

    private final Map<BloomLevel, List<Question>>
            byLevel =
            new EnumMap<>(
                    BloomLevel.class
            );

    private final RandomGenerator random;

    public QuestionBank(
            RandomGenerator random
    ) {

        if (random == null) {
            throw new IllegalArgumentException(
                    "Random generator cannot be null."
            );
        }

        this.random = random;

        for (
                BloomLevel level :
                BloomLevel.values()
        ) {

            byLevel.put(
                    level,
                    new ArrayList<>()
            );
        }
    }

    /**
     * Loads a question bank from a classpath
     * resource such as:
     *
     * /questions_collected.csv
     */
    public static QuestionBank loadFromResource(
            String resourcePath,
            RandomGenerator random
    ) throws IOException {

        QuestionBank bank =
                new QuestionBank(random);

        try (
                InputStream in =
                        QuestionBank.class
                                .getResourceAsStream(
                                        resourcePath
                                )
        ) {

            if (in == null) {
                throw new IOException(
                        "Question bank resource not found: "
                                + resourcePath
                );
            }

            try (
                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            in,
                                            StandardCharsets.UTF_8
                                    )
                            )
            ) {

                bank.parse(reader);
            }
        }

        return bank;
    }

    /**
     * Loads a question bank directly from
     * a filesystem path.
     */
    public static QuestionBank loadFromFile(
            Path path,
            RandomGenerator random
    ) throws IOException {

        QuestionBank bank =
                new QuestionBank(random);

        try (
                BufferedReader reader =
                        Files.newBufferedReader(
                                path,
                                StandardCharsets.UTF_8
                        )
        ) {

            bank.parse(reader);
        }

        return bank;
    }

    private void parse(
            BufferedReader reader
    ) throws IOException {

        String header =
                reader.readLine();

        if (header == null) {
            throw new IOException(
                    "Question bank file is empty."
            );
        }

        String line;

        int lineNo = 1;

        Set<String> ids =
                new HashSet<>();

        while (
                (line = reader.readLine())
                        != null
        ) {

            lineNo++;

            if (line.isBlank()) {
                continue;
            }

            List<String> fields =
                    parseCsvLine(line);

            if (fields.size() != 11) {

                throw new IOException(
                        "Line "
                                + lineNo
                                + " has "
                                + fields.size()
                                + " fields; expected 11."
                );
            }

            String id =
                    fields.get(0).trim();

            if (!ids.add(id)) {

                throw new IOException(
                        "Duplicate question ID at line "
                                + lineNo
                                + ": "
                                + id
                );
            }

            BloomLevel level;

            try {

                level =
                        BloomLevel.fromCsvValue(
                                fields.get(1)
                        );

            } catch (
                    IllegalArgumentException e
            ) {

                throw new IOException(
                        "Invalid Bloom level at line "
                                + lineNo
                                + ": "
                                + fields.get(1),
                        e
                );
            }

            String prompt =
                    fields.get(2);

            List<String> options =
                    Arrays.asList(
                            fields.get(3).trim(),
                            fields.get(4).trim(),
                            fields.get(5).trim(),
                            fields.get(6).trim()
                    );

            String correctAnswer =
                    fields.get(7).trim();

            String hint =
                    fields.get(8).trim();

            String sourceReading =
                    fields.get(9).trim();

            String pageNumber =
                    fields.get(10).trim();

            try {

                Question question =
                        new Question(
                                id,
                                level,
                                prompt,
                                options,
                                correctAnswer,
                                hint,
                                sourceReading,
                                pageNumber
                        );

                byLevel
                        .get(level)
                        .add(question);

            } catch (
                    IllegalArgumentException e
            ) {

                throw new IOException(
                        "Invalid question at line "
                                + lineNo
                                + ": "
                                + id
                                + " - "
                                + e.getMessage(),
                        e
                );
            }
        }
    }

    /**
     * CSV parser supporting:
     *
     * - quoted fields
     * - commas inside quoted fields
     * - escaped double quotes
     */
    private static List<String> parseCsvLine(
            String line
    ) {

        List<String> fields =
                new ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        boolean inQuotes = false;

        for (
                int i = 0;
                i < line.length();
                i++
        ) {

            char c =
                    line.charAt(i);

            if (inQuotes) {

                if (c == '"') {

                    if (
                            i + 1 < line.length()
                                    && line.charAt(i + 1)
                                    == '"'
                    ) {

                        current.append('"');

                        i++;

                    } else {

                        inQuotes = false;
                    }

                } else {

                    current.append(c);
                }

            } else {

                if (c == '"') {

                    inQuotes = true;

                } else if (c == ',') {

                    fields.add(
                            current.toString()
                    );

                    current.setLength(0);

                } else {

                    current.append(c);
                }
            }
        }

        fields.add(
                current.toString()
        );

        return fields;
    }

    public int countFor(
            BloomLevel level
    ) {

        return byLevel
                .get(level)
                .size();
    }

    public int totalCount() {

        return byLevel
                .values()
                .stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Draws a random unused question from
     * the requested Bloom level.
     *
     * The choices are shuffled immediately after
     * the question is selected.
     */
    public Question drawQuestion(
            BloomLevel level,
            Set<String> usedIds
    ) {

        List<Question> pool =
                new ArrayList<>(
                        byLevel.get(level)
                );

        pool.removeIf(
                question ->
                        usedIds.contains(
                                question.id()
                        )
        );

        if (pool.isEmpty()) {

            throw new IllegalStateException(
                    "No unused questions remain for level "
                            + level.displayName()
            );
        }

        int index =
                random.nextInt(
                        pool.size()
                );

        Question question =
                pool.get(index);

        /*
         * Requirement C:
         * randomize the order of choices.
         */
        question.shuffleOptions(
                random
        );

        return question;
    }

    /**
     * Draws another unused question from
     * the same Bloom level.
     */
    public Question drawAlternate(
            BloomLevel level,
            Set<String> excludeSet
    ) {

        return drawQuestion(
                level,
                excludeSet
        );
    }
}