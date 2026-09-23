package millionairemind;

/**
 * The six Bloom's Taxonomy levels used by the
 * 15-question Millionaire Mind ladder.
 */
public enum BloomLevel {

    REMEMBERING(
            "Remembering",
            1,
            2
    ),

    UNDERSTANDING(
            "Understanding",
            3,
            5
    ),

    APPLYING(
            "Applying",
            6,
            7
    ),

    ANALYZING(
            "Analyzing",
            8,
            10
    ),

    EVALUATING(
            "Evaluating",
            11,
            13
    ),

    CREATING(
            "Creating",
            14,
            15
    );

    private final String displayName;
    private final int startSlot;
    private final int endSlot;

    BloomLevel(
            String displayName,
            int startSlot,
            int endSlot
    ) {

        this.displayName = displayName;
        this.startSlot = startSlot;
        this.endSlot = endSlot;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Returns the Bloom level that owns the
     * specified 1-indexed question slot.
     */
    public static BloomLevel forSlot(
            int slot
    ) {

        for (
                BloomLevel level :
                values()
        ) {

            if (
                    slot >= level.startSlot
                            && slot <= level.endSlot
            ) {
                return level;
            }
        }

        throw new IllegalArgumentException(
                "No Bloom's level owns slot "
                        + slot
        );
    }

    /**
     * Converts the classification stored in
     * the question bank CSV into the internal
     * BloomLevel representation.
     *
     * Some question banks use "SYNTHESIS"
     * for the highest level, while the project
     * proposal uses "Creating".
     */
    public static BloomLevel fromCsvValue(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Bloom level cannot be blank."
            );
        }

        return switch (
                value
                        .trim()
                        .toUpperCase()
        ) {

            case "REMEMBERING" ->
                    REMEMBERING;

            case "UNDERSTANDING" ->
                    UNDERSTANDING;

            case "APPLYING" ->
                    APPLYING;

            case "ANALYZING" ->
                    ANALYZING;

            case "EVALUATING" ->
                    EVALUATING;

            case "CREATING",
                 "SYNTHESIS" ->
                    CREATING;

            default ->
                    throw new IllegalArgumentException(
                            "Unknown Bloom level: "
                                    + value
                    );
        };
    }

    public static int totalSlots() {

        BloomLevel last =
                values()[
                        values().length - 1
                ];

        return last.endSlot;
    }
}