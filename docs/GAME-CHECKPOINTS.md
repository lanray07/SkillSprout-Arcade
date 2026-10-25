# Game checkpoint recovery

Saved in-progress runs now have an explicit format and a fingerprint of the explorer,
game, activity pool and assignment's identity/year/skill/difficulty/count. The digest
uses sorted activity IDs and explicit JSON fields, including prompts, choices,
answers, hints and diagrams. Changing pool order alone preserves progress; changing
content or relevant assignment settings starts a new run. Due dates and previously
completed mission counts do not invalidate a run. This local fingerprint detects
stale recovery data; it is not authentication, content approval or a security seal.

Recovery accepts only canonical UUID run IDs, integer counters in the unfinished
run range, a current activity in the pool (or pending selection), and a seen list
matching the completed count with every ID still present. Malformed, oversized,
missing, wrong-type, out-of-range, unsupported and mismatched records start a fresh
run. Seen IDs are stored as a JSON array so commas and Unicode remain intact.
Repeated activities are allowed because small pools legitimately repeat questions.
Restored tile indices must be unique integers within the current option range;
invalid selections become empty instead of indexing beyond the available tiles.

The validated preferences are the source of truth for restored progress counters,
rather than unvalidated Compose saved counters. Valid runs retain their run IDs and
challenge number, preserving existing attempt idempotency. Completion still uses
the existing durable Room mission transaction. Finished-screen rotation state is
retained separately. Invalid recovery does not insert an attempt or completion,
delete history, change profiles/PINs, or change daily/break limits.

Upgrade behavior: old unversioned checkpoints cannot establish their original
content/assignment scope, so they start fresh once. This discards only the partial
run position. Recorded attempts, rewards and completed missions remain intact.
No content pack, Room schema, permission, network endpoint or production flag changes.

`GameCheckpointTest` checks format/type/range boundaries, scope changes, repeated
activities, Unicode/comma IDs and tile safety. Phone/tablet Compose tests in both
variants check invalid-record recovery without an attempt and valid resume/advance
with the stable attempt ID. Existing mission-completion regressions remain in the
full suite. `GameCheckpointDeviceTest` writes and reads one generated key using
actual SharedPreferences in the separate offline Voice QA installation, then removes
it. It opens no profile, PIN or Room data and does not simulate OS process death.

Results and hashes: `release/game-checkpoint-software-2026-10-25.json` and
`release/game-checkpoint-device-2026-10-25.json`. Full-app physical restart/switching,
rotation and exact signed-candidate acceptance remain pending. These engineering
checks do not approve the production release or resolve its human review gates.
