package millionairemind;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Balancing is checked across complete rounds, including question switches. */
public final class ModuleSelectionTest {
    private static final String HEADER = "id,level,prompt,optionA,optionB,optionC,optionD,correctIndex,hint,sourceReading,pageNumber\n";

    public static void main(String[] args) throws Exception {
        officialRoundsStayBalancedAndVaried();
        officialActivityAnswersAndSourcesArePreserved();
        sparseFeasibleBankWorks();
        sparseFourModuleBankTriesOtherQuotas();
        impossibleModuleCoverageFailsBeforePlay();
        moduleMetadataDoesNotChangeTheSchema();
        System.out.println("ModuleSelectionTest passed");
    }

    private static void officialActivityAnswersAndSourcesArePreserved() throws Exception {
        String[] answers = {
            "Improve through experience", "They help it learn patterns in faces", "Supervised learning",
            "Knowledge base", "To find errors and improve", "Failure-driven learning", "Data mining",
            "It can learn new patterns from examples", "Butter", "Learning by analogy",
            "Induction learns rules from examples; deduction draws conclusions from facts", "Classification",
            "Their errors are too similar", "Incorrect, because models can share the same errors",
            "Train accurate models with different errors and combine their votes", "Model B lets some estimates count more",
            "Simple averaging, since no weights need to be learned", "Average the three price estimates",
            "No label has over half the votes, but A and B tie for the most", "Majority voting",
            "Use plurality voting with a rule for breaking ties", "The first is homogeneous; the second is heterogeneous",
            "They may find different solutions that work better together",
            "Train a second model using predictions from the first models"
        };
        QuestionBank bank = QuestionBank.loadFromResource("/MCQ/Question Bank.csv", new Random(81));
        Set<String> ids = new HashSet<>();
        Map<String, Integer> activities = new HashMap<>();
        Map<BloomLevel, Integer> newLevels = new HashMap<>();
        for (BloomLevel level : BloomLevel.values()) {
            for (int i = 0; i < bank.countFor(level); i++) {
                Question question = bank.drawQuestion(level, ids);
                BackendTestSupport.check(ids.add(question.id()), "duplicate official question");
                String activity = question.id().split("-")[0];
                activities.merge(activity, 1, Integer::sum);
                if (!activity.equals("ACT8") && !activity.equals("ACT9")) continue;
                int offset = activity.equals("ACT8") ? 0 : 12;
                int number = Integer.parseInt(question.id().substring(question.id().indexOf('Q') + 1));
                BackendTestSupport.check(question.correctOptionText().equals(answers[offset + number - 1]),
                        "activity answer key changed after shuffling: " + question.id());
                BackendTestSupport.check(question.module().equals("Module 4")
                                && question.sourceReading().endsWith(".pdf")
                                && question.pageNumber().startsWith("PDF p")
                                && question.citation().contains(", PDF p")
                                && !question.citation().contains(", p. PDF"),
                        "new question needs a reading and explicit PDF page: " + question.id());
                newLevels.merge(level, 1, Integer::sum);
            }
        }
        for (int activity = 1; activity <= 9; activity++) {
            int expected = activity >= 5 && activity <= 7 ? 8 : 12;
            BackendTestSupport.check(activities.getOrDefault("ACT" + activity, 0) == expected,
                    "missing or extra questions in activity " + activity);
        }
        BackendTestSupport.check(newLevels.size() == 6
                        && newLevels.values().stream().allMatch(count -> count == 4),
                "Activities 8 and 9 should add four questions per Bloom tier");
    }

    private static void officialRoundsStayBalancedAndVaried() throws Exception {
        Set<String> rounds = new HashSet<>();
        Set<String> smallerModules = new HashSet<>();
        Set<String> newQuestions = new HashSet<>();
        for (int seed = 0; seed < 90; seed++) {
            var random = new Random(seed * 0x9E3779B97F4A7C15L);
            QuestionBank bank = QuestionBank.loadFromResource("/MCQ/Question Bank.csv", random);
            BackendTestSupport.check(bank.totalCount() == 96, "all nine activities must be included");
            GameEngine engine = new GameEngine(bank, random, "Module Test", seed % 2 == 0);
            Map<String, Integer> counts = new HashMap<>();
            Set<String> presented = new HashSet<>();
            StringBuilder signature = new StringBuilder();
            for (int slot = 1; slot <= 15; slot++) {
                Question question = engine.currentQuestion();
                BackendTestSupport.check(question.level() == BloomLevel.forSlot(slot), "Bloom tier changed");
                BackendTestSupport.check(presented.add(question.id()), "question repeated");
                if (slot == seed % 15 + 1) {
                    try {
                        Question replacement = engine.switchQuestion();
                        BackendTestSupport.check(replacement.level() == question.level()
                                        && replacement.module().equals(question.module()),
                                "switch must preserve both Bloom tier and module quota");
                        BackendTestSupport.check(presented.add(replacement.id()), "switch repeated a question");
                        question = replacement;
                    } catch (IllegalStateException e) {
                        BackendTestSupport.check(e.getMessage().contains("No spare")
                                        && engine.currentQuestion() == question
                                        && engine.lifelines().isAvailable(LifelineManager.Lifeline.SWITCH_QUESTION),
                                "failed switch must preserve the round and lifeline");
                    }
                }
                counts.merge(question.module(), 1, Integer::sum);
                if (question.id().startsWith("ACT8-") || question.id().startsWith("ACT9-")) {
                    newQuestions.add(question.id());
                }
                signature.append(question.id()).append(':').append(question.correctIndex()).append(';');
                engine.answer(question.correctIndex());
            }
            BackendTestSupport.check(counts.keySet().equals(Set.of("Module 1", "Module 2", "Module 3", "Module 4"))
                            && counts.values().stream().filter(count -> count == 4).count() == 3
                            && counts.values().stream().filter(count -> count == 3).count() == 1,
                    "expected 4/4/4/3 module counts: " + counts);
            counts.forEach((module, count) -> { if (count == 3) smallerModules.add(module); });
            BackendTestSupport.check(engine.session().becameMillionaire(), "balanced round did not complete");
            rounds.add(signature.toString());
        }
        BackendTestSupport.check(rounds.size() > 80, "rounds must remain varied");
        BackendTestSupport.check(smallerModules.size() == 4, "the smaller quota must vary across all modules");
        BackendTestSupport.check(newQuestions.size() == 24, "every new question must be reachable");
    }

    private static void sparseFourModuleBankTriesOtherQuotas() throws Exception {
        StringBuilder csv = new StringBuilder(HEADER);
        for (int slot = 1; slot <= 15; slot++) {
            csv.append(row("F" + slot, BloomLevel.forSlot(slot), "Module " + ((slot - 1) / 4 + 1)));
        }
        QuestionBank bank = load(csv.toString());
        for (int seed = 0; seed < 30; seed++) {
            GameEngine engine = new GameEngine(bank, new Random(seed), "Sparse Four");
            Map<String, Integer> counts = new HashMap<>();
            while (!engine.isGameOver()) {
                Question question = engine.currentQuestion();
                counts.merge(question.module(), 1, Integer::sum);
                engine.answer(question.correctIndex());
            }
            BackendTestSupport.check(counts.equals(Map.of("Module 1", 4, "Module 2", 4, "Module 3", 4, "Module 4", 3)),
                    "sparse four-module bank should find its only feasible quotas");
        }
    }

    private static void sparseFeasibleBankWorks() throws Exception {
        StringBuilder csv = new StringBuilder(HEADER);
        for (int slot = 1; slot <= 15; slot++) {
            csv.append(row("S" + slot, BloomLevel.forSlot(slot), "Module " + (slot % 3 + 1)));
        }
        QuestionBank bank = load(csv.toString());
        for (int seed = 0; seed < 10; seed++) {
            GameEngine engine = new GameEngine(bank, new Random(seed), "Sparse");
            Map<String, Integer> counts = new HashMap<>();
            while (!engine.isGameOver()) {
                Question q = engine.currentQuestion();
                counts.merge(q.module(), 1, Integer::sum);
                engine.answer(q.correctIndex());
            }
            BackendTestSupport.check(counts.values().stream().allMatch(count -> count == 5), "sparse quotas failed");
        }
    }

    private static void impossibleModuleCoverageFailsBeforePlay() throws Exception {
        StringBuilder csv = new StringBuilder(HEADER);
        for (BloomLevel level : BloomLevel.values()) {
            for (int i = 0; i < 6; i++) {
                String module = level.ordinal() < 3 ? "Module 1" : "Module " + (2 + i % 2);
                csv.append(row(level + "-" + i, level, module));
            }
        }
        try {
            load(csv.toString());
            throw new AssertionError("incompatible Bloom/module distribution accepted");
        } catch (IOException e) {
            BackendTestSupport.check(e.getMessage().contains("balanced module counts"), "unclear coverage error");
        }
    }

    private static void moduleMetadataDoesNotChangeTheSchema() {
        Question q = new Question("M", BloomLevel.REMEMBERING, "Prompt", java.util.List.of("A", "B", "C", "D"),
                0, "Hint", "module 03 / Assigned reading.pdf", "1");
        BackendTestSupport.check(q.module().equals("Module 3"), "module name should be normalized");
        BackendTestSupport.check(q.sourceReading().equals("module 03 / Assigned reading.pdf"), "citation must stay unchanged");
        BackendTestSupport.check(BackendTestSupport.question("U", BloomLevel.REMEMBERING, 0)
                .module().equals("Unspecified"), "legacy single-module fixtures should remain usable");
    }

    private static String row(String id, BloomLevel level, String module) {
        return id + "," + level + ",Prompt,A,B,C,D,0,Hint," + module + " / Reading.pdf,1\n";
    }

    private static QuestionBank load(String csv) throws Exception {
        var path = Files.createTempFile("module-bank-", ".csv");
        try {
            Files.writeString(path, csv);
            return QuestionBank.loadFromFile(path, new Random(12));
        } finally { Files.deleteIfExists(path); }
    }
}
