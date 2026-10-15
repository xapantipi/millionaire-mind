package millionairemind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/** Allocates Bloom slots to module counts that differ by at most one. */
final class ModuleBalancedSelection {
    private final Map<BloomLevel, List<Question>> bank;
    private final List<String> modules;
    private final RandomGenerator random;

    ModuleBalancedSelection(Map<BloomLevel, List<Question>> bank, RandomGenerator random) {
        this.bank = bank;
        this.random = random;
        modules = bank.values().stream().flatMap(List::stream).map(Question::module).distinct().sorted().toList();
        if (modules.isEmpty() || modules.size() > PrizeLadder.totalSlots()) {
            throw new IllegalArgumentException("The bank must contain between 1 and 15 modules");
        }
        if (modules.size() > 1 && modules.contains("Unspecified")) {
            throw new IllegalArgumentException("Every sourceReading must start with 'Module N /' in a multi-module bank");
        }
    }

    void validate() { allocation(); }

    List<Question> select() {
        int[][] counts = allocation();
        List<Question> selected = new ArrayList<>();
        for (BloomLevel level : BloomLevel.values()) {
            List<Question> tier = new ArrayList<>();
            for (int module = 0; module < modules.size(); module++) {
                String name = modules.get(module);
                List<Question> pool = new ArrayList<>(bank.get(level).stream()
                        .filter(q -> q.module().equals(name)).toList());
                shuffle(pool);
                tier.addAll(pool.subList(0, counts[level.ordinal()][module]));
            }
            shuffle(tier);
            selected.addAll(tier);
        }
        return List.copyOf(selected);
    }

    private int[][] allocation() {
        int[] quotas = new int[modules.size()];
        Arrays.fill(quotas, PrizeLadder.totalSlots() / modules.size());
        List<Integer> order = new ArrayList<>();
        for (int module = 0; module < modules.size(); module++) order.add(module);
        if (random != null) shuffle(order);
        int[][] counts = tryQuotas(quotas, order, 0, PrizeLadder.totalSlots() % modules.size());
        if (counts == null) {
            throw new IllegalArgumentException("Question bank cannot fill every Bloom tier with balanced module counts");
        }
        return counts;
    }

    private int[][] tryQuotas(int[] quotas, List<Integer> order, int start, int extras) {
        if (extras == 0) return allocationFor(quotas);
        // Try other balanced quotas when a sparse bank cannot fill the first choice.
        for (int i = start; i <= order.size() - extras; i++) {
            int module = order.get(i);
            quotas[module]++;
            int[][] counts = tryQuotas(quotas, order, i + 1, extras - 1);
            quotas[module]--;
            if (counts != null) return counts;
        }
        return null;
    }

    private int[][] allocationFor(int[] quotas) {
        int levelCount = BloomLevel.values().length;
        int moduleStart = 1 + levelCount;
        int sink = moduleStart + modules.size();
        int[][] capacity = new int[sink + 1][sink + 1];
        int[][] original = new int[levelCount][modules.size()];
        for (int slot = 1; slot <= PrizeLadder.totalSlots(); slot++) {
            capacity[0][1 + BloomLevel.forSlot(slot).ordinal()]++;
        }
        for (BloomLevel level : BloomLevel.values()) {
            for (Question question : bank.get(level)) {
                int module = modules.indexOf(question.module());
                capacity[1 + level.ordinal()][moduleStart + module]++;
                original[level.ordinal()][module]++;
            }
        }
        for (int module = 0; module < modules.size(); module++) {
            capacity[moduleStart + module][sink] = quotas[module];
        }

        // A capacity graph avoids greedy draws that strand later Bloom tiers or module quotas.
        int total = 0;
        List<Integer> nodeOrder = new ArrayList<>();
        for (int node = 0; node <= sink; node++) nodeOrder.add(node);
        while (total < PrizeLadder.totalSlots()) {
            if (random != null) shuffle(nodeOrder);
            int[] parent = new int[sink + 1];
            Arrays.fill(parent, -1);
            parent[0] = 0;
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(0);
            while (!queue.isEmpty() && parent[sink] < 0) {
                int from = queue.remove();
                for (int to : nodeOrder) {
                    if (parent[to] < 0 && capacity[from][to] > 0) {
                        parent[to] = from;
                        queue.add(to);
                    }
                }
            }
            if (parent[sink] < 0) return null;
            int amount = PrizeLadder.totalSlots();
            for (int to = sink; to != 0; to = parent[to]) {
                amount = Math.min(amount, capacity[parent[to]][to]);
            }
            for (int to = sink; to != 0; to = parent[to]) {
                int from = parent[to];
                capacity[from][to] -= amount;
                capacity[to][from] += amount;
            }
            total += amount;
        }
        int[][] counts = new int[levelCount][modules.size()];
        for (BloomLevel level : BloomLevel.values()) {
            for (int module = 0; module < modules.size(); module++) {
                counts[level.ordinal()][module] = original[level.ordinal()][module]
                        - capacity[1 + level.ordinal()][moduleStart + module];
            }
        }
        return counts;
    }

    private <T> void shuffle(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int other = random.nextInt(i + 1);
            T value = list.get(i);
            list.set(i, list.get(other));
            list.set(other, value);
        }
    }
}
