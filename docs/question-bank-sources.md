# Question bank sources

The bank contains all 96 questions from the nine supplied team activity
documents. Activities 1-4 have 12 questions each, Activities 5-7 have eight
each, and Activities 8-9 have 12 each. IDs retain the `ACTn-Qnn` format.

The existing 72 entries from Activities 1-7 are unchanged, including their
answer keys, citations, and earlier formatting/typo cleanups. The imported
24 entries preserve the prompts, options, answer keys, and Bloom levels in
Activities 8-9. Hints were added from the supplied readings. Activity 8 adds
four each of Remembering, Understanding, and Applying; Activity 9 adds four
each of Analyzing, Evaluating, and Creating.

The new machine-learning topic is labeled Module 4. On 2026-10-16 the user
approved 4/4/4/3 selection across four modules in the existing 15-question
game, varying which module receives three. This replaces the previous
five-per-module rule. It is the closest possible balance, rather than exact
equality; instructor acceptance of this departure from the assignment's
equal-count wording remains a team responsibility.

## New reading references

Page numbers below are positions in the supplied PDF files, starting at 1.
They may differ from the printed book page numbers. The CSV explicitly
marks these references as `PDF p.` or `PDF pp.`. Source PDFs and activity
documents are read-only inputs and are not copied into submission ZIPs.

| Question | Reading | PDF pages |
| --- | --- | --- |
| ACT8-Q01 | A1_Machine Learning.pdf | 1 |
| ACT8-Q02 | A1_Machine Learning.pdf | 5 |
| ACT8-Q03 | A1_Machine Learning.pdf | 5 |
| ACT8-Q04 | A1_Machine Learning.pdf | 2 |
| ACT8-Q05 | A1_Machine Learning.pdf | 2-3 |
| ACT8-Q06 | A1_Machine Learning.pdf | 5-6 |
| ACT8-Q07 | A2_What is machine learning.pdf | 2 |
| ACT8-Q08 | A2_What is machine learning.pdf | 2 |
| ACT8-Q09 | A2_What is machine learning.pdf | 4 |
| ACT8-Q10 | A1_Machine Learning.pdf | 4 |
| ACT8-Q11 | A1_Machine Learning.pdf | 4 |
| ACT8-Q12 | A2_What is machine learning.pdf | 5 |
| ACT9-Q01 | A4_Combination Methods.pdf | 3 |
| ACT9-Q02 | A4_Combination Methods.pdf | 3, 7 |
| ACT9-Q03 | A3_Ensemble Methods.pdf | 3 |
| ACT9-Q04 | A4_Combination Methods.pdf | 2, 4 |
| ACT9-Q05 | A4_Combination Methods.pdf | 5 |
| ACT9-Q06 | A4_Combination Methods.pdf | 2 |
| ACT9-Q07 | A4_Combination Methods.pdf | 6-7 |
| ACT9-Q08 | A4_Combination Methods.pdf | 6 |
| ACT9-Q09 | A4_Combination Methods.pdf | 7 |
| ACT9-Q10 | A3_Ensemble Methods.pdf | 1 |
| ACT9-Q11 | A4_Combination Methods.pdf | 1 |
| ACT9-Q12 | A4_Combination Methods.pdf | 17 |

## Verification

CSV validation runs before gameplay. Regression tests verify the activity
counts, all 24 new correct-answer texts after shuffling, the new reading
references, every new question's reachability, full round completion,
4/4/4/3 quotas, all four possible smaller quotas, Bloom slots, and switches.
Three-module custom banks retain exactly five questions per module. Sparse
banks are checked against alternative balanced quota assignments before
being rejected.
