# Question pool

`Question Bank.csv` is the official editable pool of 72 questions. The build
preserves the `MCQ/Question Bank.csv` path inside the game JAR. Images and
audio live separately under `resources/`.

The unchanged schema is:

```text
id,level,prompt,optionA,optionB,optionC,optionD,correctIndex,hint,sourceReading,pageNumber
```

`correctIndex` is zero-based: 0=A, 1=B, 2=C, 3=D. Standard CSV quoting supports
commas, quotes, and multiline fields. Each row contains the full question,
choices, answer, hint, Bloom level, and source citation for checking.

The source pool contains 24 Module 1, 26 Module 2, and 22 Module 3 questions.
Pool sizes need not be equal: every completed 15-question round selects
**five per module**, while preserving the Bloom tier assigned to each slot.

Modules are identified from the existing `sourceReading` prefix, for example
`Module 2 / A3_Problem Representation.pdf`. No new CSV column is required.
Multi-module banks must label every source this way. A bank must support
equal module quotas and all Bloom tiers; incompatible banks fail validation
before gameplay. A custom bank with no module prefixes is treated as one
unspecified module. The module count must divide 15 equally.

Switch the Question stays in the current module and Bloom level, using a
spare question outside the planned round. If none is available, the original
question and lifeline are preserved. Do not remove questions solely to make
the source pool totals equal.
