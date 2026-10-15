# Question pool

`Question Bank.csv` is the official editable pool of 96 questions. The build
preserves the `MCQ/Question Bank.csv` path inside the game JAR. Images and
audio live separately under `resources/`.

The unchanged schema is:

```text
id,level,prompt,optionA,optionB,optionC,optionD,correctIndex,hint,sourceReading,pageNumber
```

`correctIndex` is zero-based: 0=A, 1=B, 2=C, 3=D. Standard CSV quoting supports
commas, quotes, and multiline fields. Each row contains the full question,
choices, answer, hint, Bloom level, and source citation for checking.

The source pool contains 24 Module 1, 26 Module 2, 22 Module 3, and 24 Module 4 questions.
Pool sizes need not be equal: every completed 15-question round selects
**4/4/4/3 across the four modules**, while preserving the Bloom tier assigned to
each slot. Which module gets three varies across rounds.

Modules are identified from the existing `sourceReading` prefix, for example
`Module 2 / A3_Problem Representation.pdf`. No new CSV column is required.
Multi-module banks must label every source this way. A bank must support
module counts differing by at most one and all Bloom tiers; incompatible banks fail validation
before gameplay. A custom bank with no module prefixes is treated as one
unspecified module. A bank may have up to 15 modules. A three-module bank
still selects five per module; four modules cannot divide 15 exactly.

Activities 1-7 retain their existing 72 entries and reading references.
Activities 8-9 add 24 entries on machine learning and ensembles, with four
questions per Bloom tier. Their prompts, choices, and answers match the
activity documents; hints were added from the supplied readings. New page
references explicitly use PDF page positions, which can differ from printed
book pages. See `docs/question-bank-sources.md` in the full repository for
the activity counts and reading map.

Switch the Question stays in the current module and Bloom level, using a
spare question outside the planned round. If none is available, the original
question and lifeline are preserved. Do not remove questions solely to make
the source pool totals equal.
