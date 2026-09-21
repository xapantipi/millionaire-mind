package millionairemind;

/**
 * The 15-rung prize ladder. Slot 5 (end of Understanding) and slot 10
 * (end of Analyzing) are guaranteed checkpoints, matching Section 3 of
 * the proposal.
 */
public final class PrizeLadder {

    private static final long[] PRIZES = {
        100,        // Q1  - Remembering
        200,        // Q2  - Remembering
        500,        // Q3  - Understanding
        1_000,      // Q4  - Understanding
        2_000,      // Q5  - Understanding  (checkpoint)
        4_000,      // Q6  - Applying
        8_000,      // Q7  - Applying
        16_000,     // Q8  - Analyzing
        32_000,     // Q9  - Analyzing
        64_000,     // Q10 - Analyzing      (checkpoint)
        125_000,    // Q11 - Evaluating
        250_000,    // Q12 - Evaluating
        500_000,    // Q13 - Evaluating
        750_000,    // Q14 - Creating
        1_000_000   // Q15 - Creating
    };

    private static final int[] CHECKPOINT_SLOTS = {5, 10};

    private PrizeLadder() {
    }

    /** Prize value for a 1-indexed slot (1-15). */
    public static long prizeFor(int slot) {
        validate(slot);
        return PRIZES[slot - 1];
    }

    public static boolean isCheckpoint(int slot) {
        for (int c : CHECKPOINT_SLOTS) {
            if (c == slot) return true;
        }
        return false;
    }

    /** The prize banked at the last checkpoint at or before the given slot, or 0. */
    public static long checkpointBankFor(int slotJustAnswered) {
        long banked = 0;
        for (int c : CHECKPOINT_SLOTS) {
            if (c <= slotJustAnswered) {
                banked = prizeFor(c);
            }
        }
        return banked;
    }

    public static int totalSlots() {
        return PRIZES.length;
    }

    private static void validate(int slot) {
        if (slot < 1 || slot > PRIZES.length) {
            throw new IllegalArgumentException("Slot out of range: " + slot);
        }
    }
}
