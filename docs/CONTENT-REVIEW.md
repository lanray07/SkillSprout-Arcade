# Content authoring and review

`tools/generate_content.py` produces the reproducible English starter development pack. Each activity has an ID, game/subject/skill, year, objective/reference, difficulty, prerequisites, question type, choices/answer, explanation, progressive hints, status and version. The manifest pins the exact bundle bytes by SHA-256.

Statuses: Draft → Automated Validation Passed → Awaiting Teacher Review → Approved, or Rejected. Retired versions remain in historical evidence but are excluded from selection. Automated validation never implies pedagogical approval. The generator creates Awaiting Teacher Review items; it never creates Approved content.

References are selected objective pointers based on the [DfE mathematics programme](https://www.gov.uk/government/publications/national-curriculum-in-england-mathematics-programmes-of-study/national-curriculum-in-england-mathematics-programmes-of-study) and [DfE English programme](https://www.gov.uk/government/publications/national-curriculum-in-england-english-programmes-of-study/national-curriculum-in-england-english-programmes-of-study). The objective summaries and URL fragments need teacher checking. England alignment does not establish alignment with Scotland, Wales or Northern Ireland. Some reasoning material is explicitly enrichment.

Review every prompt, correct answer, distractor, hint, worked example, narration, prerequisite and age/difficulty assignment. In particular, higher-year arithmetic currently includes warm-up material and reading passages repeat by key-stage band. A reviewer must not infer full curriculum coverage from the presence of year tags.

A human supplies a review JSON with `reviewer`, `qualifications`, `reviewedAt`, `evidence`, `packSha256` and `status` (Approved/Rejected). `python tools/review_content.py record-review --review-file <file>` validates and records that supplied evidence without changing the pack. A publisher-controlled release update then applies only the actually approved activity IDs, increments the version and regenerates its manifest. No reviewer names or endorsements are provided by this repository.

Prerequisites use stable skill IDs. The engine supports a prerequisite mastery check; the generated starter list is empty pending authoring. Content ID reuse for a different learning item is forbidden. Deleting an optional pack preserves attempt history and does not erase skill evidence.
