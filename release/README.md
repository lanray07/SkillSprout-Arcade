# Release evidence

`evidence.json` records actual completed review work. Pending records are deliberately incomplete. The hosted-site record is automated deployment/browser verification only; it does not approve privacy law, educational content or physical hardware compatibility.

Each approved record needs `status: Approved`, an identifiable actual reviewer, `approvedAt` in YYYY-MM-DD form, an `artifact` file path (relative to the repository or absolute for private local evidence), and `artifactSha256` matching that file's exact bytes. Do not commit confidential review records to the public repository. A changed or missing artifact invalidates its gate. The publisher remains responsible for the substance and authenticity of supplied approvals.

Teacher approval also requires every bundled activity to have been actually reviewed and marked Approved through the content-review process. The current starter pack remains Awaiting Teacher Review. A publisher signature does not substitute for teacher review.

Run `python tools/verify_release.py` to see the outstanding gates. Keep physical Fire, live Amazon purchases, translation review, legal/privacy and brand clearances pending until those activities have actually happened. See `docs/RELEASE-BLOCKERS.md` for the remaining product scope as well as review requirements.

The local review desk is `release/review-kit/index.html` (or run `python tools/serve_review.py`). It includes a 444-item teacher form, connected-edition privacy draft, private encrypted-key-backup instructions and the actions for every remaining gate. On 21 October the publisher said they are qualified to review learning content, can review English, want all current language listings at launch, and have not confirmed a secure backup. These statements are review intentions, not completed approvals. Exact professional qualifications belong in the private completed review, not the public template. No new evidence gate has been approved by generating the desk.
