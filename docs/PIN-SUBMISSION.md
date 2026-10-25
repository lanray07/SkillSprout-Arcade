# Parent PIN submission reliability

The PIN screen now captures immutable PIN/confirmation values before launching its
background work. Both input fields and the submit button are disabled while that
submission runs, preventing edits or overlapping clicks from changing what is
checked or saved. A synchronous busy guard also ignores a second click before the
next frame disables the button. Input callbacks also ignore edits while busy; the UI regression invokes the captured action twice
in one UI turn to check this. The fields use the existing vault contract: 6–12 ASCII digits.
Short, non-ASCII or oversized input cannot consume a verification attempt. Setup
still requires matching confirmation; mismatches do not call the vault.

Vault setup/verification and the post-failure lock-state query run on Dispatchers.IO.
A wrong PIN queries the remaining lock duration once, and the UI renders that stable
result. Exceptions escaping setup/verification/lock-state reads return generic
unavailable feedback and never invoke the success callback. Exception text is not
shown or logged. Cancellation propagates, and a cancelled UI coroutine cannot open
the parent area. ArcadeApp also binds success to a gate-session generation and
requires the gate still be foreground and open. Backgrounding, dismissal and new
gate requests invalidate earlier results, even before Compose disposal completes.
A delayed background result cannot authorize a subsequent foreground session.
Setup is offered only if no credential exists or the current parent session is
already unlocked. If an initial save finishes during backgrounding, return requires
verification of that credential rather than offering another unauthenticated setup.
Input fields are cleared in finally, including cancellation.
Results contain only status and lock duration, with no credential or exception.

ParentSecurity.kt is unchanged: no credential, key alias, encryption, hashing,
storage format, failure counter, five-attempt lockout or 60-second lock policy was
modified. Valid wrong PINs still reach that backend and count normally. This is not
PIN recovery, a reset mechanism or a keystore migration. The existing backend may
already translate crypto failures into a failed verification; this UI helper does
not change that backend behavior. A synchronous credential write already started
inside the vault may finish after the UI is cancelled; no rollback is promised,
and the cancelled caller still cannot unlock through a later callback.

The new generic error is resource-backed and uses the existing polite error live
region. Retry re-enables the field and requires re-entering a valid PIN. New copy is
English and awaits the existing launch language review. No saved-state PIN, file,
telemetry, permission, Room schema, content or production-flag change was added.

PinSubmissionTest covers format boundaries, mismatch, exact inputs, worker threads,
match/no-match/lock states, failures and cancellation using synthetic vaults.
Phone/tablet Compose tests in both app variants check disabled fields during setup,
invalid-input gating, delayed background result rejection/fresh retry, lock feedback and service-error retry with no success callback.
PinSubmissionDeviceTest runs synthetic in-memory vault fixtures on the physical
Fire to check Android dispatch/results/errors/cancellation, including submission
from Android main with vault work on IO. It never constructs
ParentSecurity, reads a profile or stored PIN, writes a credential, opens the app
PIN UI or tests spoken output. It is not a physical keystore or real-PIN acceptance
test. Original app/settings are preserved.

Results/hashes: release/pin-submission-software-2026-10-25.json and
release/pin-submission-device-2026-10-25.json. Physical UI/VoiceView, actual vault
and final signed-candidate acceptance remain pending. Production approvals are
unchanged; no human review or release certification is inferred.
