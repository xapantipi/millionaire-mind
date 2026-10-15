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
        sparseFeasibleBankWorks();
        impossibleModuleCoverageFailsBeforePlay();
        moduleMetadataDoesNotChangeTheSchema();
        System.out.println("ModuleSelectionTest passed");
    }

    private static void officialRoundsStayBalancedAndVaried() throws Exception {
        Set<String> rounds = new HashSet<>();
        for (int seed = 0; seed < 90; seed++) {
            var random = new Random(seed);
            QuestionBank bank = QuestionBank.loadFromResource("/MCQ/Question Bank.csv", random);
            BackendTestSupport.check(bank.totalCount() == 72, "official questions must be preserved");
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
                signature.append(question.id()).append(':').append(question.correctIndex()).append(';');
                engine.answer(question.correctIndex());
            }
            BackendTestSupport.check(counts.equals(Map.of("Module 1", 5, "Module 2", 5, "Module 3", 5)),
                    "unequal selected module counts: " + counts);
            BackendTestSupport.check(engine.session().becameMillionaire(), "balanced round did not complete");
            rounds.add(signature.toString());
        }
        BackendTestSupport.check(rounds.size() > 80, "rounds must remain varied");
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
            BackendTestSupport.check(e.getMessage().contains("equal module counts"), "unclear coverage error");
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
