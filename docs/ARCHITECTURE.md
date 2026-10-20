# Architecture and data flow

The offline APK has no INTERNET, RECORD_AUDIO, contacts or location permission. No advertising, analytics, Google Play services or child-facing external link is integrated. Android minSdk 26 is a deliberate first-build compatibility floor; older Fire OS devices may be excluded. Orientation is not locked.

```mermaid
flowchart LR
  Child[Child touch / keyboard input] --> Games[Six Compose activity modes]
  Bundle[Bundled SHA-256 content manifest] --> Validate[Pack schema and integrity validation]
  ParentFile[Parent-selected pack file + digest] --> Validate
  Validate --> Selector[Deterministic activity selector]
  Selector --> Games
  Games --> Room[(Private Room database)]
  Room --> Mastery[Per-skill estimates]
  Mastery --> Selector
  Games --> TTS[Installed offline English TTS voice]
  Parent[Parent PIN] --> Vault[PBKDF2 + AES-GCM / Android Keystore]
  Vault --> Dashboard[Parent dashboard]
  Dashboard --> Room
  Dashboard --> Time[Private monotonic play ledger]
  Time --> Games
  Dashboard --> SAF[Parent-chosen CSV document destination]
```

No child data leaves the app automatically. An exported CSV can leave the device through the destination a parent chooses; that copy is outside app erasure. Optional pack content comes through the Android document picker, is capped at 5 MB and checked before install. SHA-256 detects corruption; it is not publisher authentication. A future public pack catalogue needs signed manifests and rollback protection.

Room owns profiles, attempts and assignments. A challenge's first submission is idempotent by session/activity ID. Later retries support understanding without rewriting that first-try evidence. Sessions are stored separately in private preferences so an interrupted mission can resume. Profile deletion removes attempts, assignments, session records and play accounting. Android backups are disabled; there is no cross-device sync or automatic recovery.

The selector filters by game, selected year, recent activity IDs and optional assignment skill/difficulty. It prefers the target difficulty and less-practised skills. Three independent correct answers can increase difficulty; repeated errors reduce it. Missing prerequisite mastery keeps practice at the easiest level. Skill estimates use the last ten first submissions; hints prevent an answer from counting as independent evidence. Timing does not determine mastery. The starter pack still needs meaningful prerequisite graph authoring and higher-year depth.

The play ledger charges foreground child-area use, including browsing the arcade. Parent use, pause screens and background time do not count. Each tick is committed, limiting restart leakage to approximately one second. Backwards calendar changes do not refresh an allowance; a rooted device or deliberate forward date manipulation needs additional controls. Bedtime uses local wall-clock hours. A parent bonus extends the daily allowance and does not bypass bedtime. Breaks are suggestions, not a medical recommendation.

The release build must use approved-only content, no development access entitlement, configured verified Amazon billing, a production signing identity, and reviewed language resources. Current unreviewed packs deliberately prevent a release-readiness check from passing.
