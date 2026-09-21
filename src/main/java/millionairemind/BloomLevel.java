package millionairemind;

/**
 * The six cognitive levels of Bloom's Taxonomy that the 15-question ladder
 * is organized around. Each level owns a contiguous range of question slots
 * (1-indexed, inclusive) as defined in the proposal's Section 3.
 */
public enum BloomLevel {
    REMEMBERING("Remembering", 1, 2),
    UNDERSTANDING("Understanding", 3, 5),
    APPLYING("Applying", 6, 7),
    ANALYZING("Analyzing", 8, 10),
    EVALUATING("Evaluating", 11, 13),
    CREATING("Creating", 14, 15);

    private final String displayName;
    private final int startSlot;
    private final int endSlot;

    BloomLevel(String displayName, int startSlot, int endSlot) {
        this.displayName = displayName;
        this.startSlot = startSlot;
        this.endSlot = endSlot;
    }

    public String displayName() {
        return displayName;
    }

    /** Returns the Bloom's level that owns the given 1-indexed question slot (1-15). */
    public static BloomLevel forSlot(int slot) {
        for (BloomLevel level : values()) {
            if (slot >= level.startSlot && slot <= level.endSlot) {
                return level;
            }
        }
        throw new IllegalArgumentException("No Bloom's level owns slot " + slot);
    }

    public static int totalSlots() {
        BloomLevel last = values()[values().length - 1];
        return last.endSlot;
    }
}
