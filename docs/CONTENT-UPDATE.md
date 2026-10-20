# Development content update

The bundled en-GB pack contains 444 development activities, all awaiting teacher review. The generator preserves the original activity IDs so existing progress remains linked. Pack/activity version 2 reflects revised support text and prerequisite metadata. `tools/content_expansion.py` appends 36 mathematics questions and 12 comprehension questions across six new passages.

New maths examples cover place value, shape properties, time, money, statistics, perimeter, area, decimals, negative numbers, fractions, percentages, volume, angles, ratio and algebra. Older-year questions now include multi-step reasoning and unit-specific distractors. The six additional passages progress from simple retrieval to inference, evidence and balanced summaries. Spelling questions use textual clues rather than revealing the answer in the prompt, so they remain usable without TTS. Addition/subtraction/measurement hints now match their operations.

Prerequisites are skill identifiers, not activity IDs. The generator defines an acyclic graph across practised skills; validation rejects local cycles and tests check that all bundled references resolve. The adaptive engine treats missing prerequisite mastery as a signal to offer easier practice, not a hard lock. This is scaffolding metadata for teacher review, not proof of a validated learning sequence.

The library is still an incomplete development selection. It does not cover every statutory objective or establish age suitability, reading level, cultural neutrality or educational efficacy. Early phonics/dictation, visual shape/sorting activities, more advanced-year depth and a complete reviewed progression remain work. The original curriculum links are mapping references, not DfE endorsement. No status has been automatically promoted to Approved.

Run `python tools/generate_content.py` to regenerate exact JSON and the SHA-256 manifest, then `python tools/review_content.py validate`. Actual teacher evidence must bind the exact resulting pack digest. Production builds remain blocked until the reviewed library and all release gates are satisfied.
